package ru.gamestelegrambot.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gamestelegrambot.entity.DuelBank
import java.math.BigDecimal
import java.util.UUID

interface DuelBankRepository : JpaRepository<DuelBank, UUID> {
    @Modifying
    @Query(
        value = """
            INSERT INTO games_bot.duel_bank
                (id, chat_id, first_player_id, second_player_id, stake, status)
            VALUES (:id, :chat, :first, :second, :stake, 'LOCKED')
            ON CONFLICT (id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") duelId: UUID,
        @Param("chat") chatId: Long,
        @Param("first") firstPlayerId: Long,
        @Param("second") secondPlayerId: Long,
        @Param("stake") stake: BigDecimal,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from DuelBank b where b.id = :id")
    fun findLockedById(
        @Param("id") id: UUID,
    ): DuelBank?
}
