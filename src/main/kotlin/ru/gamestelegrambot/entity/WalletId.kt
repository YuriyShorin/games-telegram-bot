package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class WalletId(
    @Column(name = "chat_id")
    val chatId: Long = 0,
    @Column(name = "player_id")
    val playerId: Long = 0,
) : Serializable
