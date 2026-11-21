package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MosaicCodecTest {
    private val solution8x8 = listOf(
        1, 0, 0, 1, 1, 0, 0, 1,
        0, 1, 1, 0, 0, 1, 1, 0,
        0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1,
        1, 0, 0, 1, 1, 0, 0, 1,
        0, 1, 1, 0, 0, 1, 1, 0,
        0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1,
    )
    private val grid8 = Grid(8)
    private val clues8x8 = (0 until 64).map { MosaicRules.clueAt(grid8, solution8x8, Cell(it)) }

    @Test
    fun roundtripEncodesAndDecodesCanonicalBankLine() {
        val puzzle = MosaicPuzzle(
            id = "MO-BANK-001",
            size = 8,
            clues = clues8x8,
            solution = solution8x8,
            pack = Pack.EASY,
            storedGrade = "none:1",
        )

        val encoded = MosaicCodec.encode(puzzle)
        val decoded = MosaicCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(encoded, MosaicCodec.encode(decoded))
    }

    @Test
    fun roundtripPreservesUnspecifiedCluesAsDots() {
        // Mask out every second clue
        val partialClues = clues8x8.mapIndexed { index, clue ->
            if (index % 2 == 0) clue else null
        }
        val puzzle = MosaicPuzzle(
            id = "MO-BANK-002",
            size = 8,
            clues = partialClues,
            solution = solution8x8,
            pack = Pack.EASY,
            storedGrade = "none:1",
        )

        val encoded = MosaicCodec.encode(puzzle)
        val decoded = MosaicCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(encoded, MosaicCodec.encode(decoded))
    }

    @Test
    fun decodeRejectsInvalidSizesAndMismatchedPacks() {
        val validLine = MosaicCodec.encode(
            MosaicPuzzle("MO-1", 8, clues8x8, solution8x8, Pack.EASY, "none:1")
        )

        // Wrong size (7 is not a launch size)
        val sizeParts = validLine.split('|').toMutableList()
        sizeParts[1] = "7"
        assertThrows(IllegalArgumentException::class.java) {
            MosaicCodec.decode(sizeParts.joinToString("|"))
        }

        // Mismatched pack (size 8 with MEDIUM)
        val packParts = validLine.split('|').toMutableList()
        packParts[4] = "MEDIUM"
        assertThrows(IllegalArgumentException::class.java) {
            MosaicCodec.decode(packParts.joinToString("|"))
        }
    }

    @Test
    fun decodeRejectsCorruptedPayloadAndSolution() {
        val validLine = MosaicCodec.encode(
            MosaicPuzzle("MO-1", 8, clues8x8, solution8x8, Pack.EASY, "none:1")
        )

        // Invalid character in payload (e.g. 'x')
        val badPayloadParts = validLine.split('|').toMutableList()
        badPayloadParts[2] = "x" + badPayloadParts[2].substring(1)
        assertThrows(IllegalArgumentException::class.java) {
            MosaicCodec.decode(badPayloadParts.joinToString("|"))
        }

        // Solution bit changed to violate clues
        val parts = validLine.split('|').toMutableList()
        val badSolution = (if (parts[3][0] == '1') '0' else '1') + parts[3].substring(1)
        parts[3] = badSolution
        val violatingLine = parts.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) {
            MosaicCodec.decode(violatingLine)
        }
    }
}
