package ru.gamestelegrambot.mapper

import ru.gamestelegrambot.entity.DiceAttemptRequestEntity
import ru.gamestelegrambot.model.DiceAttemptRequest

fun DiceAttemptRequestEntity.toModel(): DiceAttemptRequest =
    DiceAttemptRequest(
        id = id,
        duelId = duelId,
        roundNumber = roundNumber,
        side = side,
        promptedAt = promptedAt,
        deadline = deadline,
        messageId = messageId,
        value = value,
        sentAt = sentAt,
    )
