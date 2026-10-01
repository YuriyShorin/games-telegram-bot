package ru.gamestelegrambot.mapper

import ru.gamestelegrambot.entity.PvpWalletEntity
import ru.gamestelegrambot.model.PvpWallet

fun PvpWalletEntity.toModel(): PvpWallet =
    PvpWallet(
        chatId = id.chatId,
        playerId = id.playerId,
        available = available,
        committed = committed,
    )
