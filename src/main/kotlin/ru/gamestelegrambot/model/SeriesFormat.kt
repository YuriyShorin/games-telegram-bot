package ru.gamestelegrambot.model

enum class SeriesFormat(
    val rounds: Int,
) {
    FIVE_ROUNDS(5),
    SEVEN_ROUNDS(7),
    UNTIL_VICTORY(0),
}
