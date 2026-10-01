package ru.gamestelegrambot.mapper

import ru.gamestelegrambot.entity.DuelInvitationEntity
import ru.gamestelegrambot.model.DuelInvitation

fun DuelInvitationEntity.toModel(): DuelInvitation =
    DuelInvitation(
        id = id,
        chatId = chatId,
        challengerId = challengerId,
        opponentId = opponentId,
        stake = stake,
        format = format,
        status = status,
    )
