package com.datathecodie.witgrid.engine

import com.datathecodie.witgrid.engine.binary.BinaryEngine
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.crowns.CrownsCodec
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsState
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
    fun remainingStubOperationsFailWithTheirLaneId() {
        val lanes: List<Pair<GameEngine<Unit, Unit>, String>> = listOf(
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
    fun crownsUnimplementedSolverAndSaveOperationsKeepTheirLaneId() {
        val puzzle = crownsPuzzle()
        val state = CrownsState.initial(puzzle)
        val operations: List<() -> Any?> = listOf(
            { CrownsEngine.nextStep(puzzle, state) },
            { CrownsEngine.grade(puzzle) },
            { CrownsEngine.generate(SplitMix64(0), Pack.EASY) },
            { CrownsEngine.encodeState(state) },
            { CrownsEngine.decodeState(byteArrayOf()) },
        )

        for (operation in operations) {
            val error = assertThrows(NotImplementedError::class.java) { operation() }
            assertEquals("CR", error.message)
        }
    }

    @Test
    fun heterogeneousRegistrySupportsGenericCaptureWithoutCasts() {
        val line = CrownsCodec.encode(crownsPuzzle())
        val crowns = Registry.engines.single { it.id == "crowns" }

        assertEquals(line, roundTrip(crowns, line))
        for (engine in Registry.engines.filter { it.id != "crowns" }) {
            assertThrows(NotImplementedError::class.java) { roundTrip(engine, "line") }
        }
    }

    private fun crownsPuzzle(): CrownsPuzzle {
        val size = 6
        return CrownsPuzzle(
            id = "CR-REGISTRY",
            size = size,
            regions = (0 until size).flatMap { row -> List(size) { row } },
            solutionColumns = listOf(1, 3, 5, 0, 2, 4),
            pack = Pack.EASY,
            storedGrade = "pending",
        )
    }

    private fun <P : Any, S : Any> roundTrip(engine: GameEngine<P, S>, line: String): String =
        engine.encodePuzzle(engine.decodePuzzle(line))
}
