package ru.gamestelegrambot

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager

@Tag("integration")
@Testcontainers
class DiceDuelMigrationIntegrationTest {
    @Test
    fun `upgrade restores only outstanding accepted duels without moving funds`() {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), JdbcConnection(connection)).use { liquibase ->
                liquibase.update(3, Contexts(), LabelExpression())
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        INSERT INTO games_bot.pvp_wallet (chat_id, player_id, available, committed)
                        VALUES (-1, 1, 900.00, 100.00), (-1, 2, 900.00, 100.00);
                        INSERT INTO games_bot.duel_bank
                            (id, chat_id, first_player_id, second_player_id, stake, status, winner_id)
                        VALUES
                            ('00000000-0000-0000-0000-000000000001', -1, 1, 2, 50.00, 'LOCKED', NULL),
                            ('00000000-0000-0000-0000-000000000002', -1, 1, 2, 50.00, 'WON', 1),
                            ('00000000-0000-0000-0000-000000000003', -1, 1, 2, 50.00, 'LOCKED', NULL);
                        INSERT INTO games_bot.duel_invitation
                            (id, chat_id, challenger_id, opponent_id, stake, format, status)
                        VALUES
                            ('00000000-0000-0000-0000-000000000001', -1, 1, 2, 50.00, 'SEVEN_ROUNDS', 'ACCEPTED'),
                            ('00000000-0000-0000-0000-000000000002', -1, 1, 2, 50.00, 'FIVE_ROUNDS', 'ACCEPTED'),
                            ('00000000-0000-0000-0000-000000000003', -1, 1, 2, 50.00, 'UNTIL_VICTORY', 'PENDING');
                        """.trimIndent(),
                    )
                }
                liquibase.update(Contexts(), LabelExpression())
                liquibase.update(Contexts(), LabelExpression())
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT * FROM games_bot.dice_duel").use { rows ->
                        assertTrue(rows.next())
                        assertEquals("00000000-0000-0000-0000-000000000001", rows.getString("id"))
                        assertEquals(0L, rows.getLong("version"))
                        assertEquals(0L, rows.getLong("rounds_played"))
                        assertEquals(0L, rows.getLong("first_score"))
                        assertEquals(0L, rows.getLong("second_score"))
                        assertEquals(null, rows.getString("winner"))
                        assertFalse(rows.getBoolean("is_tiebreak"))
                        assertFalse(rows.next())
                    }
                    statement.executeQuery("SELECT available, committed FROM games_bot.pvp_wallet ORDER BY player_id").use { rows ->
                        repeat(2) {
                            assertTrue(rows.next())
                            assertEquals("900.00", rows.getBigDecimal("available").toPlainString())
                            assertEquals("100.00", rows.getBigDecimal("committed").toPlainString())
                        }
                        assertFalse(rows.next())
                    }
                    statement.executeQuery("SELECT count(*) FROM games_bot.pvp_ledger").use { rows ->
                        assertTrue(rows.next())
                        assertEquals(0, rows.getInt(1))
                    }
                }
            }
        }
    }

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:18.6")
    }
}
