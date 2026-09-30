package ru.gamestelegrambot.service

import org.springframework.stereotype.Service
import ru.gamestelegrambot.model.DiceMatch
import ru.gamestelegrambot.model.DiceRound
import ru.gamestelegrambot.model.MatchSide
import ru.gamestelegrambot.model.SeriesFormat

/** Stateless rules service; callers supply complete pairs of trusted native Telegram results. */
@Service
class DiceMatchService {
    fun compareRound(round: DiceRound): MatchSide? {
        require(round.first in 1..6 && round.second in 1..6) { "Dice values must be between 1 and 6" }
        return when {
            round.first > round.second -> MatchSide.FIRST
            round.second > round.first -> MatchSide.SECOND
            else -> null
        }
    }

    fun recordRound(
        match: DiceMatch,
        round: DiceRound,
    ): DiceMatch {
        check(!match.isFinished) { "The match has already finished" }
        val roundWinner = compareRound(round)
        val roundsPlayed = match.roundsPlayed + 1
        val firstScore = match.firstScore + if (roundWinner == MatchSide.FIRST) 1 else 0
        val secondScore = match.secondScore + if (roundWinner == MatchSide.SECOND) 1 else 0
        val winner =
            if (roundsPlayed >= match.format.rounds) {
                when {
                    firstScore > secondScore -> MatchSide.FIRST
                    secondScore > firstScore -> MatchSide.SECOND
                    else -> null
                }
            } else {
                null
            }
        val isTiebreak =
            winner == null && match.format != SeriesFormat.UNTIL_VICTORY && roundsPlayed >= match.format.rounds
        return match.copy(
            roundsPlayed = roundsPlayed,
            firstScore = firstScore,
            secondScore = secondScore,
            winner = winner,
            isTiebreak = isTiebreak,
        )
    }
}
