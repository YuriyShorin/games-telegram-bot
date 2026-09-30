package ru.gamestelegrambot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GamesTelegramBotApplication

fun main(args: Array<String>) {
    runApplication<GamesTelegramBotApplication>(*args)
}
