package ru.gamestelegrambot.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.gamestelegrambot.model.DiceAttemptRequest
import ru.gamestelegrambot.model.MatchSide
import ru.gamestelegrambot.model.SeriesFormat
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("integration")
@SpringBootTest
@Testcontainers
class DiceAttemptIntegrationTest {
    @Autowired
    private lateinit var attempts: DiceAttemptService

    @Autowired
    private lateinit var invitations: DuelInvitationService

    @Autowired
    private lateinit var wallets: PvpWalletService

    @Autowired
    private lateinit var duels: DiceDuelService

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    private var chatId = 0L
    private val start = Instant.parse("2026-10-01T12:00:00Z")

    @BeforeEach
    fun setup() {
        chatId = -System.nanoTime()
        (1L..3L).forEach { wallets.register(chatId, it) }
    }

    @Test
    fun `sequential requests persist random order and incomplete pair across transactions`() {
        val id = duel()
        val first = attempts.prepareRequest(id, chatId, 1)
        assertNull(first.deadline)
        assertEquals(first, attempts.prepareRequest(id, chatId, 1))
        assertEquals(first, attempts.findRequest(id, chatId, first.id))
        assertThrows(IllegalStateException::class.java) { record(first, 1, 6) }
        val prompted = attempts.confirmPrompt(id, chatId, first.id, start)
        assertEquals(start.plusSeconds(60), prompted.deadline)
        assertEquals(prompted, attempts.confirmPrompt(id, chatId, first.id, start))
        assertThrows(IllegalArgumentException::class.java) {
            attempts.confirmPrompt(id, chatId, first.id, start.plusSeconds(1))
        }
        record(first, 1, 6)
        assertEquals(0L, duels.find(id, chatId).match.roundsPlayed)
        assertEquals(6, attempts.findRequest(id, chatId, first.id).value)
        val second = attempts.prepareRequest(id, chatId, 1)
        assertNotEquals(first.side, second.side)
        assertEquals(second, attempts.prepareRequest(id, chatId, 1))
        attempts.confirmPrompt(id, chatId, second.id, start.plusSeconds(120))
        attempts.recordAttempt(id, chatId, second.id, player(second), 2, 2, start.plusSeconds(180))
        val match = duels.find(id, chatId).match
        assertEquals(1L, match.roundsPlayed)
        assertEquals(if (first.side == MatchSide.FIRST) 1L else 0L, match.firstScore)
        assertEquals(if (first.side == MatchSide.SECOND) 1L else 0L, match.secondScore)
        assertEquals(2, requestCount(id))
        assertThrows(IllegalArgumentException::class.java) { attempts.prepareRequest(id, chatId, 1) }
    }

    @Test
    fun `authorization isolation result validation and inclusive deadline leave rejected attempts unchanged`() {
        val id = duel()
        val first = prompted(id, 1)
        val other = duel()
        assertThrows(IllegalArgumentException::class.java) {
            attempts.prepareRequest(id, chatId - 1, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            attempts.findRequest(id, chatId - 1, first.id)
        }
        assertThrows(IllegalArgumentException::class.java) {
            attempts.confirmPrompt(other, chatId, first.id, start)
        }
        assertThrows(IllegalArgumentException::class.java) {
            attempts.recordAttempt(other, chatId, first.id, player(first), 1, 6, start)
        }
        for (actor in listOf(3L, if (player(first) == 1L) 2L else 1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                attempts.recordAttempt(id, chatId, first.id, actor, 1, 6, start)
            }
        }
        for (value in listOf(0, 7)) {
            assertThrows(IllegalArgumentException::class.java) { record(first, 1, value) }
        }
        assertThrows(IllegalArgumentException::class.java) { record(first, 0, 6) }
        for (timestamp in listOf(start.minusSeconds(1), start.plusSeconds(61))) {
            assertThrows(IllegalArgumentException::class.java) {
                attempts.recordAttempt(id, chatId, first.id, player(first), 1, 6, timestamp)
            }
        }
        assertNull(attempts.findRequest(id, chatId, first.id).value)
        attempts.recordAttempt(id, chatId, first.id, player(first), 1, 6, start.plusSeconds(60))
        assertEquals(6, attempts.findRequest(id, chatId, first.id).value)
        assertThrows(IllegalArgumentException::class.java) {
            attempts.recordAttempt(id, chatId - 1, first.id, player(first), 1, 6, start.plusSeconds(60))
        }
    }

    @Test
    fun `message cannot be consumed twice within round or across matches`() {
        val id = duel()
        val first = prompted(id, 1)
        record(first, 1, 6)
        assertEquals(duels.find(id, chatId), record(first, 1, 6))
        assertThrows(IllegalArgumentException::class.java) { record(first, 2, 6) }
        assertThrows(IllegalArgumentException::class.java) { record(first, 1, 5) }
        val second = prompted(id, 1)
        assertThrows(IllegalArgumentException::class.java) { record(second, 1, 2) }
        val other = prompted(duel(), 1)
        assertThrows(IllegalArgumentException::class.java) { record(other, 1, 6) }
        assertNull(attempts.findRequest(other.duelId, chatId, other.id).value)
        record(second, 2, 2)
        record(first, 1, 6)
        assertEquals(1L, duels.find(id, chatId).match.roundsPlayed)
    }

    @Test
    fun `concurrent preparation and delivery retries keep one request and one round`() {
        val id = duel()
        val prepared = concurrently { attempts.prepareRequest(id, chatId, 1) }
        assertEquals(prepared[0], prepared[1])
        val first = prepared.first()
        attempts.confirmPrompt(id, chatId, first.id, start)
        concurrently { record(first, 1, 4) }
        val second = prompted(id, 1)
        val results = concurrently { record(second, 2, 2) }
        assertEquals(results[0], results[1])
        assertEquals(1L, duels.find(id, chatId).match.roundsPlayed)
        assertEquals(2, requestCount(id))
    }

    @Test
    fun `all formats persist tied rounds tiebreaks and decisive results without paying bank`() {
        for (format in SeriesFormat.entries) {
            val id = duel(format)
            var messageId = 1L
            val tiedRounds = maxOf(1, format.rounds)
            repeat(tiedRounds) { index ->
                val round = index + 1L
                val first = prompted(id, round)
                record(first, messageId++, 3)
                val second = prompted(id, round)
                record(second, messageId++, 3)
            }
            val tied = duels.find(id, chatId).match
            assertEquals(tiedRounds.toLong(), tied.roundsPlayed)
            assertNull(tied.winner)
            assertEquals(format != SeriesFormat.UNTIL_VICTORY, tied.isTiebreak)
            val first = prompted(id, tiedRounds + 1L)
            record(first, messageId++, if (first.side == MatchSide.FIRST) 6 else 1)
            val second = prompted(id, tiedRounds + 1L)
            record(second, messageId++, if (second.side == MatchSide.FIRST) 6 else 1)
            val finished = duels.find(id, chatId).match
            assertEquals(MatchSide.FIRST, finished.winner)
            assertEquals(1L, finished.firstScore)
            assertThrows(IllegalStateException::class.java) {
                attempts.prepareRequest(id, chatId, tiedRounds + 2L)
            }
            assertEquals(
                "LOCKED",
                jdbc.queryForObject(
                    "SELECT status FROM games_bot.duel_bank WHERE id = ?",
                    String::class.java,
                    id,
                ),
            )
            // Different formats share a chat, so subsequent native message IDs must be distinct.
            chatId--
            (1L..3L).forEach { wallets.register(chatId, it) }
        }
    }

    @Test
    fun `fixed series consumes every round and can select second participant as winner`() {
        val id = duel()
        var messageId = 1L
        repeat(5) { index ->
            val first = prompted(id, index + 1L)
            record(first, messageId++, if (first.side == MatchSide.SECOND) 6 else 1)
            val second = prompted(id, index + 1L)
            record(second, messageId++, if (second.side == MatchSide.SECOND) 6 else 1)
            val match = duels.find(id, chatId).match
            assertEquals(index + 1L, match.secondScore)
            assertEquals(if (index == 4) MatchSide.SECOND else null, match.winner)
        }
    }

    @Test
    fun `database failure rolls back both attempt and score`() {
        val id = duel()
        val first = prompted(id, 1)
        record(first, 1, 6)
        val second = prompted(id, 1)
        jdbc.execute(
            "ALTER TABLE games_bot.dice_duel ADD CONSTRAINT reject_attempt_test " +
                "CHECK (id <> '$id' OR rounds_played = 0)",
        )
        try {
            assertThrows(Exception::class.java) { record(second, 2, 1) }
            assertNull(attempts.findRequest(id, chatId, second.id).value)
            assertEquals(0L, duels.find(id, chatId).match.roundsPlayed)
        } finally {
            jdbc.execute("ALTER TABLE games_bot.dice_duel DROP CONSTRAINT reject_attempt_test")
        }
        record(second, 2, 1)
        assertEquals(1L, duels.find(id, chatId).match.roundsPlayed)
    }

    private fun duel(format: SeriesFormat = SeriesFormat.FIVE_ROUNDS): UUID {
        val id = UUID.randomUUID()
        invitations.challenge(id, chatId, 1, 2, BigDecimal("10"), format)
        invitations.accept(id, chatId, 2)
        return id
    }

    private fun prompted(
        id: UUID,
        round: Long,
    ): DiceAttemptRequest {
        val request = attempts.prepareRequest(id, chatId, round)
        return attempts.confirmPrompt(id, chatId, request.id, start)
    }

    private fun player(request: DiceAttemptRequest): Long = if (request.side == MatchSide.FIRST) 1 else 2

    private fun record(
        request: DiceAttemptRequest,
        messageId: Long,
        value: Int,
    ) = attempts.recordAttempt(request.duelId, chatId, request.id, player(request), messageId, value, start)

    private fun requestCount(id: UUID): Int? =
        jdbc.queryForObject(
            "SELECT count(*) FROM games_bot.dice_attempt_request WHERE duel_id = ?",
            Int::class.java,
            id,
        )

    private fun <T> concurrently(action: () -> T): List<T> {
        val startGate = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures =
                (1..2).map {
                    executor.submit(
                        Callable {
                            assertTrue(startGate.await(10, TimeUnit.SECONDS))
                            action()
                        },
                    )
                }
            startGate.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.3-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
