package ru.gamestelegrambot.mapper

import ru.gamestelegrambot.entity.DiceDuelEntity
import ru.gamestelegrambot.model.DiceDuel
import ru.gamestelegrambot.model.DiceMatch

fun DiceDuelEntity.toModel(): DiceDuel =
    DiceDuel(
        id = id,
        chatId = invitation.chatId,
        firstPlayerId = invitation.challengerId,
        secondPlayerId = invitation.opponentId,
        stake = invitation.stake,
        match =
            DiceMatch(
                format = invitation.format,
                roundsPlayed = roundsPlayed,
                firstScore = firstScore,
                secondScore = secondScore,
                winner = winner,
                isTiebreak = isTiebreak,
            ),
    )
