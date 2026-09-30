package ru.gamestelegrambot.model

data class DiceMatch(
    val format: SeriesFormat,
    val roundsPlayed: Long = 0,
    val firstScore: Long = 0,
    val secondScore: Long = 0,
    val winner: MatchSide? = null,
    val isTiebreak: Boolean = false,
) {
    val isFinished: Boolean
        get() = winner != null
}
