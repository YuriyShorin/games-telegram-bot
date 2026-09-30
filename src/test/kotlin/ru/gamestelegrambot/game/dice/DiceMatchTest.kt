package ru.gamestelegrambot.game.dice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiceMatchTest {
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
                assertEquals(expected, DiceRound(first, second).winner)
            }
        }
    }

    @Test
    fun `invalid native values are rejected for either player`() {
        for (invalid in listOf(Int.MIN_VALUE, 0, 7, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { DiceRound(invalid, 1) }
            assertThrows(IllegalArgumentException::class.java) { DiceRound(1, invalid) }
        }
    }

    @Test
    fun `fixed series plays every round even with an unbeatable lead`() {
        for (format in listOf(SeriesFormat.FIVE_ROUNDS, SeriesFormat.SEVEN_ROUNDS)) {
            val match = DiceMatch(format)
            repeat(format.rounds - 1) {
                match.recordRound(DiceRound(6, 1))
                assertFalse(match.isFinished)
            }
            match.recordRound(DiceRound(2, 2))
            assertEquals(format.rounds.toLong(), match.roundsPlayed)
            assertEquals((format.rounds - 1).toLong(), match.firstScore)
            assertEquals(0L, match.secondScore)
            assertEquals(MatchSide.FIRST, match.winner)
            assertFalse(match.isTiebreak)
        }
    }

    @Test
    fun `round wins determine series winner rather than sum of faces`() {
        val match = DiceMatch(SeriesFormat.FIVE_ROUNDS)
        repeat(3) { match.recordRound(DiceRound(2, 1)) }
        repeat(2) { match.recordRound(DiceRound(1, 6)) }
        assertEquals(MatchSide.FIRST, match.winner)
        assertEquals(3L, match.firstScore)
        assertEquals(2L, match.secondScore)
    }

    @Test
    fun `equal series score continues through tied tiebreaks until decisive pair`() {
        for (format in listOf(SeriesFormat.FIVE_ROUNDS, SeriesFormat.SEVEN_ROUNDS)) {
            val match = DiceMatch(format)
            repeat(format.rounds / 2) {
                match.recordRound(DiceRound(6, 1))
                match.recordRound(DiceRound(1, 6))
            }
            match.recordRound(DiceRound(3, 3))
            assertTrue(match.isTiebreak)
            repeat(100) { match.recordRound(DiceRound(4, 4)) }
            assertNull(match.winner)
            assertTrue(match.isTiebreak)
            match.recordRound(DiceRound(1, 2))
            assertEquals(MatchSide.SECOND, match.winner)
            assertEquals((format.rounds + 101).toLong(), match.roundsPlayed)
        }
    }

    @Test
    fun `until victory finishes on first decisive pair after any number of ties`() {
        val match = DiceMatch(SeriesFormat.UNTIL_VICTORY)
        repeat(100) { match.recordRound(DiceRound(1, 1)) }
        assertFalse(match.isFinished)
        assertFalse(match.isTiebreak)
        match.recordRound(DiceRound(5, 6))
        assertEquals(MatchSide.SECOND, match.winner)
        assertEquals(101L, match.roundsPlayed)
    }

    @Test
    fun `finished match rejects extra results without changing score`() {
        val match = DiceMatch(SeriesFormat.UNTIL_VICTORY)
        match.recordRound(DiceRound(6, 1))
        assertThrows(IllegalStateException::class.java) { match.recordRound(DiceRound(1, 6)) }
        assertEquals(1L, match.roundsPlayed)
        assertEquals(1L, match.firstScore)
        assertEquals(0L, match.secondScore)
        assertEquals(MatchSide.FIRST, match.winner)
    }
}
