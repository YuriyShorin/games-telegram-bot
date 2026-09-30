package ru.gamestelegrambot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class EconomyConfiguration {
    @Bean
    fun economyClock(): Clock = Clock.systemUTC()
}
