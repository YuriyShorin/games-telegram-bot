package ru.gamestelegrambot.mapper

import ru.gamestelegrambot.entity.DuelBankEntity
import ru.gamestelegrambot.model.DuelBank

fun DuelBankEntity.toModel(): DuelBank =
    DuelBank(
        id = id,
        chatId = chatId,
        firstPlayerId = firstPlayerId,
        secondPlayerId = secondPlayerId,
        stake = stake,
        status = status,
        winnerId = winnerId,
    )
