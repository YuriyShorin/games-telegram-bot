package ru.gamestelegrambot.game.dice

enum class SeriesFormat(
    val rounds: Int,
) {
    FIVE_ROUNDS(5),
    SEVEN_ROUNDS(7),
    UNTIL_VICTORY(0),
}

enum class MatchSide {
    FIRST,
    SECOND,
}

data class DiceRound(
    val first: Int,
    val second: Int,
) {
    init {
        require(first in 1..6 && second in 1..6) { "Dice values must be between 1 and 6" }
    }

    val winner: MatchSide?
        get() =
            when {
                first > second -> MatchSide.FIRST
                second > first -> MatchSide.SECOND
                else -> null
            }
}

/** Rules only: the caller supplies a complete pair of trusted native Telegram results. */
class DiceMatch(
    val format: SeriesFormat,
) {
    var roundsPlayed: Long = 0
        private set

    var firstScore: Long = 0
        private set

    var secondScore: Long = 0
        private set

    var winner: MatchSide? = null
        private set

    val isFinished: Boolean
        get() = winner != null

    val isTiebreak: Boolean
        get() = !isFinished && format != SeriesFormat.UNTIL_VICTORY && roundsPlayed >= format.rounds

    fun recordRound(round: DiceRound) {
        check(!isFinished) { "The match has already finished" }

        roundsPlayed++
        when (round.winner) {
            MatchSide.FIRST -> firstScore++
            MatchSide.SECOND -> secondScore++
            null -> Unit
        }

        if (roundsPlayed >= format.rounds) {
            winner =
                when {
                    firstScore > secondScore -> MatchSide.FIRST
                    secondScore > firstScore -> MatchSide.SECOND
                    else -> null
                }
        }
    }
}
