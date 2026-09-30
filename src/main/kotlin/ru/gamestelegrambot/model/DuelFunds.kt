package ru.gamestelegrambot.model

import java.math.BigDecimal
import java.util.UUID

data class DuelFunds(
    val id: UUID,
    val chatId: Long,
    val firstPlayerId: Long,
    val secondPlayerId: Long,
    val stake: BigDecimal,
    val status: BankStatus,
    val winnerId: Long?,
)
