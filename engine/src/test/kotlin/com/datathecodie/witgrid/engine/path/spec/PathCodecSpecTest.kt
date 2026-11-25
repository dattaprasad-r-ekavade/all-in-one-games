package com.datathecodie.witgrid.engine.path.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.path.PathEngine
import com.datathecodie.witgrid.engine.path.PathPackBands
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.corridorPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.rowsPuzzle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Path cases: plan board sizes, pack bands, and bank and state round trips. */
class PathCodecSpecTest {

    @Test
    fun planSizesMapToPlanPacks() {
        assertEquals(Pack.EASY, PathPackBands.packForSize(6))
        assertEquals(Pack.MEDIUM, PathPackBands.packForSize(8))
        assertEquals(Pack.HARD, PathPackBands.packForSize(10))
        assertEquals(Pack.EXPERT, PathPackBands.packForSize(12))
    }

    @Test
    fun packsMapToPlanSizes() {
        assertEquals(6, PathPackBands.sizeForPack(Pack.EASY))
        assertEquals(8, PathPackBands.sizeForPack(Pack.MEDIUM))
        assertEquals(10, PathPackBands.sizeForPack(Pack.HARD))
        assertEquals(12, PathPackBands.sizeForPack(Pack.EXPERT))
    }

    @Test
    fun bankLineRoundTripKeepsEndpointsWallsAndSolution() {
        for (puzzle in listOf(rowsPuzzle, corridorPuzzle)) {
            val decoded = PathEngine.decodePuzzle(PathEngine.encodePuzzle(puzzle))
            assertTrue(decoded == puzzle, "round trip ${puzzle.id}")
        }
    }

    @Test
    fun wallsAreWrittenAsHashAndPairsAsDigits() {
        val line = PathEngine.encodePuzzle(rowsPuzzle)
        val payload = line.split('|')[2]
        assertEquals("1..12..23..34.4#", payload)
    }

    @Test
    fun playerStateRoundTripKeepsDigits() {
        val state = PathState.initial(rowsPuzzle).withMove(Move.SetDigit(Cell(5), 2))
        val decoded = PathEngine.decodeState(PathEngine.encodeState(state))
        assertEquals(state, decoded)
    }
}
