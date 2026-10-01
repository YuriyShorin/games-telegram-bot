package ru.gamestelegrambot.model

import java.time.Instant
import java.util.UUID

data class DiceAttemptRequest(
    val id: UUID,
    val duelId: UUID,
    val roundNumber: Long,
    val side: MatchSide,
    val promptedAt: Instant?,
    val deadline: Instant?,
    val messageId: Long?,
    val value: Int?,
    val sentAt: Instant?,
)
