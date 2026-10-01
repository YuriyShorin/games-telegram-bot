package ru.gamestelegrambot.model

import java.math.BigDecimal

data class PvpWallet(
    val chatId: Long,
    val playerId: Long,
    val available: BigDecimal,
    val committed: BigDecimal,
) {
    val wealth: BigDecimal
        get() = available + committed
}
