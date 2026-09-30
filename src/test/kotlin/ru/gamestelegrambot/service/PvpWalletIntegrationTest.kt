package ru.gamestelegrambot.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.gamestelegrambot.model.BankStatus
import ru.gamestelegrambot.service.PvpWalletService
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("integration")
@SpringBootTest
@Testcontainers
@Import(PvpWalletIntegrationTest.TimeConfiguration::class)
class PvpWalletIntegrationTest {
    @Autowired
    private lateinit var service: PvpWalletService

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var clock: MutableClock

    private var chatId: Long = 0

    @BeforeEach
    fun setup() {
        chatId = -System.nanoTime()
        clock.current = Instant.parse("2026-09-30T20:59:00Z")
    }

    @Test
    fun `registration grants once per group including concurrent retries`() {
        race({ service.register(chatId, 1) }, { service.register(chatId, 1) })
        assertBalance(1, "1000.00", "0.00")
        service.register(chatId, 1)
        assertEquals(1L, ledgerCount("STARTING_GRANT"))
        assertEquals(BigDecimal("1000.00"), service.register(chatId - 1, 1).available)
        assertThrows(IllegalArgumentException::class.java) { service.register(chatId, 0) }
    }

    @Test
    fun `acceptance locks exact stakes once and settlement conserves funds`() {
        registerPlayers()
        val duel = UUID.randomUUID()
        repeat(2) { service.acceptDuel(duel, chatId, 1, 2, BigDecimal("50.25")) }
        assertBalance(1, "949.75", "50.25")
        assertBalance(2, "949.75", "50.25")
        assertEquals(BigDecimal("1000.00"), service.balance(chatId, 1).wealth)
        repeat(2) { service.awardWinner(duel, 2) }
        assertBalance(1, "949.75", "0.00")
        assertBalance(2, "1050.25", "0.00")
        assertEquals(2L, ledgerCount("DUEL_COMMITMENT"))
        assertEquals(2L, ledgerCount("WON"))
        assertThrows(IllegalStateException::class.java) { service.interrupt(duel) }
        assertThrows(IllegalStateException::class.java) { service.awardWinner(duel, 1) }
        assertThrows(IllegalArgumentException::class.java) {
            service.acceptDuel(duel, chatId, 1, 2, BigDecimal("51"))
        }
        assertEquals(BankStatus.WON, service.acceptDuel(duel, chatId, 1, 2, BigDecimal("50.25")).status)
        assertLedgerMatchesBalances()
    }

    @Test
    fun `failed acceptance rolls back the bank and both wallets`() {
        registerPlayers()
        val existing = UUID.randomUUID()
        service.acceptDuel(existing, chatId, 2, 3, BigDecimal("1000"))
        val rejected = UUID.randomUUID()
        assertThrows(IllegalStateException::class.java) {
            service.acceptDuel(rejected, chatId, 1, 2, BigDecimal("50"))
        }
        assertBalance(1, "1000.00", "0.00")
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM games_bot.duel_bank WHERE id = ?", Int::class.java, rejected))
        service.interrupt(existing)
        service.acceptDuel(rejected, chatId, 1, 2, BigDecimal("50"))
        assertBalance(1, "950.00", "50.00")
        assertLedgerMatchesBalances()
    }

    @Test
    fun `invalid stakes participants and unknown winners do not move funds`() {
        registerPlayers()
        for (stake in listOf("0", "-10", "9.99")) {
            assertThrows(IllegalArgumentException::class.java) {
                service.acceptDuel(UUID.randomUUID(), chatId, 1, 2, BigDecimal(stake))
            }
        }
        assertThrows(ArithmeticException::class.java) {
            service.acceptDuel(UUID.randomUUID(), chatId, 1, 2, BigDecimal("10.001"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.acceptDuel(UUID.randomUUID(), chatId, 1, 1, BigDecimal("10"))
        }
        val duel = UUID.randomUUID()
        service.acceptDuel(duel, chatId, 1, 2, BigDecimal("10"))
        assertThrows(IllegalArgumentException::class.java) { service.awardWinner(duel, 3) }
        assertThrows(IllegalArgumentException::class.java) { service.forfeit(duel, 3) }
        assertBalance(1, "990.00", "10.00")
        assertLedgerMatchesBalances()
    }

    @Test
    fun `forfeit gives opponent full bank and interruption refunds stakes only once`() {
        registerPlayers()
        val forfeit = UUID.randomUUID()
        service.acceptDuel(forfeit, chatId, 1, 2, BigDecimal("1000"))
        repeat(2) { service.forfeit(forfeit, 1) }
        assertBalance(1, "0.00", "0.00")
        assertBalance(2, "2000.00", "0.00")
        val interruption = UUID.randomUUID()
        service.acceptDuel(interruption, chatId, 2, 3, BigDecimal("500"))
        repeat(2) { service.interrupt(interruption) }
        assertBalance(2, "2000.00", "0.00")
        assertBalance(3, "1000.00", "0.00")
        assertLedgerMatchesBalances()
    }

    @Test
    fun `concurrent duels cannot spend the same money twice`() {
        registerPlayers()
        val outcomes =
            race(
                { runCatching { service.acceptDuel(UUID.randomUUID(), chatId, 1, 2, BigDecimal("700")) } },
                { runCatching { service.acceptDuel(UUID.randomUUID(), chatId, 3, 1, BigDecimal("700")) } },
            )
        assertEquals(1, outcomes.count { it.isSuccess })
        assertTrue(outcomes.single { it.isFailure }.exceptionOrNull() is IllegalStateException)
        assertBalance(1, "300.00", "700.00")
        assertEquals(2L, ledgerCount("DUEL_COMMITMENT"))
        assertLedgerMatchesBalances()
    }

    @Test
    fun `concurrent acceptance and settlement retries debit and pay once`() {
        registerPlayers()
        val duel = UUID.randomUUID()
        race(
            { service.acceptDuel(duel, chatId, 1, 2, BigDecimal("50")) },
            { service.acceptDuel(duel, chatId, 1, 2, BigDecimal("50")) },
        )
        race({ service.awardWinner(duel, 1) }, { service.awardWinner(duel, 1) })
        assertBalance(1, "1050.00", "0.00")
        assertBalance(2, "950.00", "0.00")
        assertEquals(2L, ledgerCount("WON"))
        assertLedgerMatchesBalances()
    }

    @Test
    fun `recovery counts commitments and resets at Moscow midnight`() {
        registerPlayers()
        val allIn = UUID.randomUUID()
        service.acceptDuel(allIn, chatId, 1, 2, BigDecimal("1000"))
        service.claimRecovery(chatId, 1)
        assertBalance(1, "0.00", "1000.00")
        service.awardWinner(allIn, 2)
        race({ service.claimRecovery(chatId, 1) }, { service.claimRecovery(chatId, 1) })
        assertBalance(1, "100.00", "0.00")
        val loss = UUID.randomUUID()
        service.acceptDuel(loss, chatId, 1, 2, BigDecimal("100"))
        service.forfeit(loss, 1)
        service.claimRecovery(chatId, 1)
        assertBalance(1, "0.00", "0.00")
        clock.current = Instant.parse("2026-09-30T21:00:00Z")
        service.claimRecovery(chatId, 1)
        assertBalance(1, "100.00", "0.00")
        clock.current = Instant.parse("2026-10-05T21:00:00Z")
        service.claimRecovery(chatId, 1)
        assertBalance(1, "200.00", "0.00")
        assertEquals(3L, ledgerCount("DAILY_RECOVERY"))
        assertLedgerMatchesBalances()
    }

    @Test
    fun `recovery is capped by wealth threshold including fractional units`() {
        registerPlayers()
        val duel = UUID.randomUUID()
        service.acceptDuel(duel, chatId, 1, 2, BigDecimal("750.01"))
        service.awardWinner(duel, 2)
        service.claimRecovery(chatId, 1)
        assertBalance(1, "300.00", "0.00")
        service.claimRecovery(chatId, 2)
        assertEquals(1L, ledgerCount("DAILY_RECOVERY"))
        assertLedgerMatchesBalances()
    }

    private fun registerPlayers() {
        (1L..3L).forEach { service.register(chatId, it) }
    }

    private fun assertBalance(
        player: Long,
        available: String,
        committed: String,
    ) {
        val actual = service.balance(chatId, player)
        assertEquals(BigDecimal(available), actual.available)
        assertEquals(BigDecimal(committed), actual.committed)
    }

    private fun ledgerCount(reason: String): Long =
        jdbc.queryForObject(
            "SELECT count(*) FROM games_bot.pvp_ledger WHERE chat_id = ? AND reason = ?",
            Long::class.java,
            chatId,
            reason,
        )!!

    private fun assertLedgerMatchesBalances() {
        assertEquals(
            0,
            jdbc.queryForObject(
                """
                SELECT count(*) FROM games_bot.pvp_wallet w
                WHERE w.chat_id = ? AND (
                    w.available <> (SELECT sum(available_delta) FROM games_bot.pvp_ledger l
                        WHERE l.chat_id = w.chat_id AND l.player_id = w.player_id)
                    OR w.committed <> (SELECT sum(committed_delta) FROM games_bot.pvp_ledger l
                        WHERE l.chat_id = w.chat_id AND l.player_id = w.player_id)
                )
                """.trimIndent(),
                Int::class.java,
                chatId,
            ),
        )
    }

    private fun <T> race(
        first: () -> T,
        second: () -> T,
    ): List<T> {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures =
                listOf(first, second).map { operation ->
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

    class MutableClock : Clock() {
        @Volatile
        var current: Instant = Instant.EPOCH

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)

        override fun instant(): Instant = current
    }

    @TestConfiguration
    class TimeConfiguration {
        @Bean
        @Primary
        fun testClock(): MutableClock = MutableClock()
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
