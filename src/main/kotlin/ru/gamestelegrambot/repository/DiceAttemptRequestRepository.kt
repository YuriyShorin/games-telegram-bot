package ru.gamestelegrambot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.gamestelegrambot.entity.DiceAttemptRequestEntity
import java.util.UUID

interface DiceAttemptRequestRepository : JpaRepository<DiceAttemptRequestEntity, UUID> {
    fun findAllByDuelIdAndRoundNumberOrderByPromptedAtAsc(
        duelId: UUID,
        roundNumber: Long,
    ): List<DiceAttemptRequestEntity>

    fun findByChatIdAndMessageId(
        chatId: Long,
        messageId: Long,
    ): DiceAttemptRequestEntity?
}
