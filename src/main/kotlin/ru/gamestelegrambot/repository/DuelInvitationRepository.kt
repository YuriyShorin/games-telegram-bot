package ru.gamestelegrambot.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gamestelegrambot.entity.DuelInvitationEntity
import java.math.BigDecimal
import java.util.UUID

interface DuelInvitationRepository : JpaRepository<DuelInvitationEntity, UUID> {
    // Atomic creation retries must not invalidate the transaction on a duplicate ID.
    @Modifying
    @Query(
        value = """
            INSERT INTO games_bot.duel_invitation
                (id, chat_id, challenger_id, opponent_id, stake, format, status)
            VALUES (:id, :chat, :challenger, :opponent, :stake, :format, 'PENDING')
            ON CONFLICT (id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("chat") chatId: Long,
        @Param("challenger") challengerId: Long,
        @Param("opponent") opponentId: Long,
        @Param("stake") stake: BigDecimal,
        @Param("format") format: String,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from DuelInvitationEntity i where i.id = :id")
    fun findLockedById(
        @Param("id") id: UUID,
    ): DuelInvitationEntity?
}
