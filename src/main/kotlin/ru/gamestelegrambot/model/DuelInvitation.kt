package ru.gamestelegrambot.model

import java.math.BigDecimal
import java.util.UUID

data class DuelInvitation(
    val id: UUID,
    val chatId: Long,
    val challengerId: Long,
    val opponentId: Long,
    val stake: BigDecimal,
    val format: SeriesFormat,
    val status: InvitationStatus,
)
