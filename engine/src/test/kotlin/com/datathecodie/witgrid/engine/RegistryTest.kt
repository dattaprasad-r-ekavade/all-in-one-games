package com.datathecodie.witgrid.engine

import com.datathecodie.witgrid.engine.binary.BinaryEngine
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.nonogram.NonogramEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RegistryTest {
    @Test
    fun listsFourStableUniqueGameIdsInProductOrder() {
        assertEquals(listOf("crowns", "binary", "nonogram", "minisudoku"), Registry.engines.map { it.id })
        assertEquals(4, Registry.engines.map { it.id }.toSet().size)
    }

    @Test
    fun everyStubOperationFailsWithItsLaneId() {
        val lanes: List<Pair<GameEngine<Unit, Unit>, String>> = listOf(
            CrownsEngine to "CR",
            BinaryEngine to "BI",
            NonogramEngine to "NO",
            MiniSudokuEngine to "MS",
        )
        for ((engine, lane) in lanes) {
            val operations: List<() -> Any?> = listOf(
                { engine.decodePuzzle("line") },
                { engine.encodePuzzle(Unit) },
                { engine.initialState(Unit) },
                { engine.applyMove(Unit, Move.SetMark(Cell(0), Mark.FILLED)) },
                { engine.isSolved(Unit, Unit) },
                { engine.mistakes(Unit, Unit) },
                { engine.countSolutions(Unit) },
                { engine.nextStep(Unit, Unit) },
                { engine.grade(Unit) },
                { engine.generate(SplitMix64(0), Pack.EASY) },
                { engine.encodeState(Unit) },
                { engine.decodeState(byteArrayOf()) },
            )
            for (operation in operations) {
                val error = assertThrows(NotImplementedError::class.java) { operation() }
                assertEquals(lane, error.message)
            }
        }
    }

    @Test
    fun heterogeneousRegistrySupportsGenericCaptureWithoutCasts() {
        for (engine in Registry.engines) {
            assertThrows(NotImplementedError::class.java) { roundTrip(engine, "line") }
        }
    }

    private fun <P : Any, S : Any> roundTrip(engine: GameEngine<P, S>, line: String): String =
        engine.encodePuzzle(engine.decodePuzzle(line))
}
