package com.datathecodie.witgrid.engine

import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsCodec
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegistryTest {
    @Test
    fun listsTenStableUniqueGameIdsInProductOrder() {
        assertEquals(
            listOf(
                "crowns",
                "binary",
                "nonogram",
                "minisudoku",
                "futoshiki",
                "skyscrapers",
                "path",
                "sudoku",
                "killersudoku",
                "mosaic",
            ),
            Registry.engines.map { it.id },
        )
        assertEquals(10, Registry.engines.map { it.id }.toSet().size)
    }

    @Test
    fun heterogeneousRegistrySupportsGenericCrownsRoundTripWithoutCasts() {
        val puzzle = crownsPuzzle()
        val line = CrownsCodec.encode(puzzle)
        val engine = Registry.engines.single { it.id == "crowns" }

        assertEquals(line, roundTrip(engine, line))
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
