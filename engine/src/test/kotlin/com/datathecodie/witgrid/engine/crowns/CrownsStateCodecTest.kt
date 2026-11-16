package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CrownsStateCodecTest {
    @Test
    fun roundTripRetainsPuzzleContextAndPlayerMarks() {
        val puzzle = puzzle()
        val state = CrownsState.initial(puzzle).withMove(Move.SetMark(Cell(2), Mark.CROSSED))

        val restored = CrownsEngine.decodeState(CrownsEngine.encodeState(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionAndTruncatedSaves() {
        val encoded = CrownsEngine.encodeState(CrownsState.initial(puzzle()))
        val unsupported = encoded.copyOf().apply { this[4] = 2 }

        assertThrows(IllegalArgumentException::class.java) { CrownsEngine.decodeState(unsupported) }
        assertThrows(IllegalArgumentException::class.java) {
            CrownsEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
    }

    private fun puzzle() = CrownsPuzzle(
        id = "CR-STATE-6",
        size = 6,
        regions = (0 until 6).flatMap { row -> List(6) { row } },
        solutionColumns = listOf(1, 3, 5, 0, 2, 4),
        pack = CrownsPackBands.packForSize(6),
        storedGrade = "fixture",
    )
}
