package ru.gamestelegrambot.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.gamestelegrambot.model.DiceMatch
import ru.gamestelegrambot.model.DiceRound
import ru.gamestelegrambot.model.MatchSide
import ru.gamestelegrambot.model.SeriesFormat

class DiceMatchServiceTest {
    private val service = DiceMatchService()

    @Test
    fun `one service processes independent matches without mutating previous states`() {
        val first = DiceMatch(SeriesFormat.FIVE_ROUNDS)
        val second = DiceMatch(SeriesFormat.UNTIL_VICTORY)
        val updatedFirst = service.recordRound(first, DiceRound(6, 1))
        val updatedSecond = service.recordRound(second, DiceRound(1, 6))
        assertEquals(DiceMatch(SeriesFormat.FIVE_ROUNDS), first)
        assertEquals(DiceMatch(SeriesFormat.UNTIL_VICTORY), second)
        assertEquals(1L, updatedFirst.firstScore)
        assertFalse(updatedFirst.isFinished)
        assertEquals(MatchSide.SECOND, updatedSecond.winner)
        val nextFirst = service.recordRound(updatedFirst, DiceRound(1, 6))
        assertEquals(0L, updatedFirst.secondScore)
        assertEquals(1L, nextFirst.secondScore)
    }

    @Test
    fun `all dice pairs compare face values`() {
        for (first in 1..6) {
            for (second in 1..6) {
                val expected =
                    when {
                        first > second -> MatchSide.FIRST
                        second > first -> MatchSide.SECOND
                        else -> null
                    }
                assertEquals(expected, service.compareRound(DiceRound(first, second)))
            }
        }
    }

    @Test
    fun `invalid native values are rejected for either player`() {
        for (invalid in listOf(Int.MIN_VALUE, 0, 7, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { service.compareRound(DiceRound(invalid, 1)) }
            assertThrows(IllegalArgumentException::class.java) { service.compareRound(DiceRound(1, invalid)) }
        }
    }

    @Test
    fun `fixed series plays every round even with an unbeatable lead`() {
        for (format in listOf(SeriesFormat.FIVE_ROUNDS, SeriesFormat.SEVEN_ROUNDS)) {
            var match = DiceMatch(format)
            repeat(format.rounds - 1) {
                match = service.recordRound(match, DiceRound(6, 1))
                assertFalse(match.isFinished)
            }
            match = service.recordRound(match, DiceRound(2, 2))
            assertEquals(format.rounds.toLong(), match.roundsPlayed)
            assertEquals((format.rounds - 1).toLong(), match.firstScore)
            assertEquals(0L, match.secondScore)
            assertEquals(MatchSide.FIRST, match.winner)
            assertFalse(match.isTiebreak)
        }
    }

    @Test
    fun `round wins determine series winner rather than sum of faces`() {
        var match = DiceMatch(SeriesFormat.FIVE_ROUNDS)
        repeat(3) { match = service.recordRound(match, DiceRound(2, 1)) }
        repeat(2) { match = service.recordRound(match, DiceRound(1, 6)) }
        assertEquals(MatchSide.FIRST, match.winner)
        assertEquals(3L, match.firstScore)
        assertEquals(2L, match.secondScore)
    }

    @Test
    fun `equal series score continues through tied tiebreaks until decisive pair`() {
        for (format in listOf(SeriesFormat.FIVE_ROUNDS, SeriesFormat.SEVEN_ROUNDS)) {
            var match = DiceMatch(format)
            repeat(format.rounds / 2) {
                match = service.recordRound(match, DiceRound(6, 1))
                match = service.recordRound(match, DiceRound(1, 6))
            }
            match = service.recordRound(match, DiceRound(3, 3))
            assertTrue(match.isTiebreak)
            repeat(100) { match = service.recordRound(match, DiceRound(4, 4)) }
            assertNull(match.winner)
            assertTrue(match.isTiebreak)
            match = service.recordRound(match, DiceRound(1, 2))
            assertEquals(MatchSide.SECOND, match.winner)
            assertEquals((format.rounds + 101).toLong(), match.roundsPlayed)
        }
    }

    @Test
    fun `until victory finishes on first decisive pair after any number of ties`() {
        var match = DiceMatch(SeriesFormat.UNTIL_VICTORY)
        repeat(100) { match = service.recordRound(match, DiceRound(1, 1)) }
        assertFalse(match.isFinished)
        assertFalse(match.isTiebreak)
        match = service.recordRound(match, DiceRound(5, 6))
        assertEquals(MatchSide.SECOND, match.winner)
        assertEquals(101L, match.roundsPlayed)
    }

    @Test
    fun `finished match rejects extra results without changing score`() {
        var match = DiceMatch(SeriesFormat.UNTIL_VICTORY)
        match = service.recordRound(match, DiceRound(6, 1))
        assertThrows(IllegalStateException::class.java) { match = service.recordRound(match, DiceRound(1, 6)) }
        assertEquals(1L, match.roundsPlayed)
        assertEquals(1L, match.firstScore)
        assertEquals(0L, match.secondScore)
        assertEquals(MatchSide.FIRST, match.winner)
    }
}
