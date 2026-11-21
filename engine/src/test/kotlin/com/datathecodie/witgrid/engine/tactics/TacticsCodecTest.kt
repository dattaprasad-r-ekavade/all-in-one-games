package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.BankLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TacticsCodecTest {

    @Test
    fun roundTripsMateInOnePuzzle() {
        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val encoded = TacticsCodec.encode(puzzle)
        val decoded = TacticsCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(puzzle.id, decoded.id)
        assertEquals(puzzle.mateIn, decoded.mateIn)
        assertEquals(puzzle.solutionLine, decoded.solutionLine)
        assertEquals(puzzle.pack, decoded.pack)
        assertEquals(puzzle.storedGrade, decoded.storedGrade)
    }

    @Test
    fun roundTripsMateInTwoPuzzle() {
        val puzzle = TacticsRulesTest.mate2UniquePuzzle()
        val encoded = TacticsCodec.encode(puzzle)
        val decoded = TacticsCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(puzzle.mateIn, decoded.mateIn)
        assertEquals(puzzle.solutionLine, decoded.solutionLine)
    }

    @Test
    fun rejectsMalformedBankLine() {
        // Size not 8
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-size|6|7k/3Q4/5K2/8/8/8/8/8 w - - 1|d7g7|EASY|MATE_1")
        }

        // Invalid FEN payload
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-fen|8|not-a-fen|d7g7|EASY|MATE_1")
        }

        // Invalid solution move
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-move|8|7k/3Q4/5K2/8/8/8/8/8 w - - 1|z9z9|EASY|MATE_1")
        }

        // Solution does not deliver checkmate
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("no-mate|8|7k/3Q4/5K2/8/8/8/8/8 w - - 1|d7d8|EASY|MATE_1")
        }
    }
}
