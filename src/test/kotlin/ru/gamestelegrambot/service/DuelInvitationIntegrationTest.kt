package ru.gamestelegrambot.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import ru.gamestelegrambot.model.DiceMatch
import ru.gamestelegrambot.model.InvitationStatus
import ru.gamestelegrambot.model.SeriesFormat
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("integration")
@SpringBootTest
@Testcontainers
class DuelInvitationIntegrationTest {
    @Autowired
    private lateinit var service: DuelInvitationService

    @Autowired
    private lateinit var wallets: PvpWalletService

    @Autowired
    private lateinit var duels: DiceDuelService

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    private var chatId: Long = 0

    @BeforeEach
    fun setup() {
        chatId = -System.nanoTime()
        (1L..3L).forEach { wallets.register(chatId, it) }
    }

    @Test
    fun `creation publishes immutable terms without moving funds`() {
        val id = UUID.randomUUID()
        val invitation = challenge(id)
        assertEquals(InvitationStatus.PENDING, invitation.status)
        assertEquals(invitation, service.find(id, chatId))
        assertEquals(invitation, challenge(id))
        assertEquals(BigDecimal("1000.00"), wallets.balance(chatId, 1).available)
        assertEquals(BigDecimal("0.00"), wallets.balance(chatId, 2).committed)
        assertThrows(IllegalArgumentException::class.java) { challenge(id, "51") }
        assertThrows(IllegalArgumentException::class.java) {
            service.challenge(id, chatId, 1, 2, BigDecimal("50"), SeriesFormat.SEVEN_ROUNDS)
        }
        assertThrows(IllegalArgumentException::class.java) { service.find(id, chatId - 1) }
    }

    @Test
    fun `only opponent in originating chat may accept and retries commit once`() {
        val id = challenge().id
        for (actor in listOf(1L, 3L)) {
            assertThrows(IllegalArgumentException::class.java) { service.accept(id, chatId, actor) }
        }
        assertThrows(IllegalArgumentException::class.java) { service.accept(id, chatId - 1, 2) }
        val accepted = service.accept(id, chatId, 2)
        assertEquals(InvitationStatus.ACCEPTED, accepted.status)
        assertEquals(accepted, service.accept(id, chatId, 2))
        assertEquals(accepted, service.find(id, chatId))
        assertEquals(BigDecimal("950.00"), wallets.balance(chatId, 1).available)
        assertEquals(BigDecimal("50.00"), wallets.balance(chatId, 2).committed)
        assertEquals(2, commitments(id))
        wallets.awardWinner(id, 1)
        service.accept(id, chatId, 2)
        assertEquals(BigDecimal("1050.00"), wallets.balance(chatId, 1).available)
        assertEquals(2, commitments(id))
    }

    @Test
    fun `failed acceptance preserves pending invitation and both balances`() {
        val spent = UUID.randomUUID()
        wallets.acceptDuel(spent, chatId, 2, 3, BigDecimal("1000"))
        val id = challenge().id
        assertThrows(IllegalStateException::class.java) { service.accept(id, chatId, 2) }
        assertEquals(InvitationStatus.PENDING, service.find(id, chatId).status)
        assertEquals(BigDecimal("1000.00"), wallets.balance(chatId, 1).available)
        assertEquals(0, commitments(id))
        assertEquals(0, duelCount(id))
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM games_bot.duel_bank WHERE id = ?", Int::class.java, id))
        wallets.interrupt(spent)
        service.accept(id, chatId, 2)
        assertEquals(2, commitments(id))
    }

    @Test
    fun `accepted duel survives separate transactions with original participants and format`() {
        for (format in SeriesFormat.entries) {
            val id = UUID.randomUUID()
            service.challenge(id, chatId, 2, 1, BigDecimal("50.25"), format)
            assertThrows(IllegalStateException::class.java) { duels.find(id, chatId) }
            service.accept(id, chatId, 1)
            val duel = duels.find(id, chatId)
            assertEquals(id, duel.id)
            assertEquals(chatId, duel.chatId)
            assertEquals(2L, duel.firstPlayerId)
            assertEquals(1L, duel.secondPlayerId)
            assertEquals(BigDecimal("50.25"), duel.stake)
            assertEquals(DiceMatch(format), duel.match)
            service.accept(id, chatId, 1)
            assertEquals(duel, duels.find(id, chatId))
            assertEquals(1, duelCount(id))
            assertThrows(IllegalArgumentException::class.java) { duels.find(id, chatId - 1) }
        }
    }

    @Test
    fun `failure to persist duel rolls back invitation bank balances and ledger`() {
        val id = challenge().id
        // Reject this fixture at the database boundary after both stakes have been committed.
        jdbc.execute("ALTER TABLE games_bot.dice_duel ADD CONSTRAINT reject_test_duel CHECK (id <> '$id'::uuid)")
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException::class.java) { service.accept(id, chatId, 2) }
        } finally {
            jdbc.execute("ALTER TABLE games_bot.dice_duel DROP CONSTRAINT reject_test_duel")
        }
        assertEquals(InvitationStatus.PENDING, service.find(id, chatId).status)
        for (player in listOf(1L, 2L)) {
            assertEquals(BigDecimal("1000.00"), wallets.balance(chatId, player).available)
            assertEquals(BigDecimal("0.00"), wallets.balance(chatId, player).committed)
        }
        assertEquals(0, commitments(id))
        assertEquals(0, duelCount(id))
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM games_bot.duel_bank WHERE id = ?", Int::class.java, id))
        service.accept(id, chatId, 2)
        assertEquals(1, duelCount(id))
        assertEquals(2, commitments(id))
    }

    @Test
    fun `invalid terms and unknown invitations are rejected`() {
        for (stake in listOf("-10", "0", "9.99")) {
            assertThrows(IllegalArgumentException::class.java) { challenge(stake = stake) }
        }
        assertThrows(ArithmeticException::class.java) { challenge(stake = "10.001") }
        for (opponent in listOf(0L, 1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                service.challenge(UUID.randomUUID(), chatId, 1, opponent, BigDecimal("10"), SeriesFormat.UNTIL_VICTORY)
            }
        }
        assertThrows(IllegalStateException::class.java) { service.accept(UUID.randomUUID(), chatId, 2) }
        val unregistered = service.challenge(UUID.randomUUID(), chatId, 1, 4, BigDecimal("10"), SeriesFormat.UNTIL_VICTORY)
        assertThrows(IllegalStateException::class.java) { service.accept(unregistered.id, chatId, 4) }
        assertEquals(InvitationStatus.PENDING, service.find(unregistered.id, chatId).status)
    }

    @Test
    fun `concurrent creation and acceptance retries are idempotent`() {
        val id = UUID.randomUUID()
        val created = race { challenge(id, "50.25") }
        assertEquals(created[0], created[1])
        val accepted = race { service.accept(id, chatId, 2) }
        assertEquals(accepted[0], accepted[1])
        assertEquals(1, duelCount(id))
        assertEquals(DiceMatch(SeriesFormat.FIVE_ROUNDS), duels.find(id, chatId).match)
        assertEquals(InvitationStatus.ACCEPTED, accepted[0].status)
        assertEquals(BigDecimal("949.75"), wallets.balance(chatId, 1).available)
        assertEquals(BigDecimal("50.25"), wallets.balance(chatId, 2).committed)
        assertEquals(2, commitments(id))
    }

    @Test
    fun `pending invitation cannot start against a previously settled bank`() {
        val id = challenge().id
        wallets.acceptDuel(id, chatId, 1, 2, BigDecimal("50"))
        wallets.awardWinner(id, 1)
        assertThrows(IllegalStateException::class.java) { service.accept(id, chatId, 2) }
        assertEquals(InvitationStatus.PENDING, service.find(id, chatId).status)
        assertEquals(0, duelCount(id))
        assertEquals(BigDecimal("1050.00"), wallets.balance(chatId, 1).available)
        assertEquals(BigDecimal("950.00"), wallets.balance(chatId, 2).available)
        assertEquals(2, commitments(id))
    }

    private fun challenge(
        id: UUID = UUID.randomUUID(),
        stake: String = "50",
    ) = service.challenge(id, chatId, 1, 2, BigDecimal(stake), SeriesFormat.FIVE_ROUNDS)

    private fun commitments(id: UUID): Int =
        jdbc.queryForObject(
            "SELECT count(*) FROM games_bot.pvp_ledger WHERE duel_id = ? AND reason = 'DUEL_COMMITMENT'",
            Int::class.java,
            id,
        )!!

    private fun duelCount(id: UUID): Int =
        jdbc.queryForObject("SELECT count(*) FROM games_bot.dice_duel WHERE id = ?", Int::class.java, id)!!

    private fun <T> race(operation: () -> T): List<T> {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures =
                List(2) {
                    executor.submit(
                        Callable {
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            operation()
                        },
                    )
                }
            check(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            return futures.map { it.get(20, TimeUnit.SECONDS) }
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:18.6")

        @DynamicPropertySource
        @JvmStatic
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
