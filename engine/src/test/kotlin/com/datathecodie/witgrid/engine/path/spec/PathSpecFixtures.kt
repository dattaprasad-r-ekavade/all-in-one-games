package com.datathecodie.witgrid.engine.path.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.path.PathPuzzle

/** Hand-made Path fixtures. Each board was checked by hand against the plan rules in section 12.3. */
internal object PathSpecFixtures {

    /**
     * 4x4 board with four horizontal paths and one wall in the bottom-right corner.
     * Rows 1 to 3 are full. Row 4 is pair 4 on the first three cells; the last cell is a wall.
     */
    val rowsSolution: List<Int> = listOf(
        1, 1, 1, 1,
        2, 2, 2, 2,
        3, 3, 3, 3,
        4, 4, 4, 0,
    )

    /** Endpoints for [rowsSolution]: two per pair, no endpoint on the wall. */
    val rowsEndpoints: List<Int?> = listOf(
        1, null, null, 1,
        2, null, null, 2,
        3, null, null, 3,
        4, null, 4, null,
    )

    /** Wall flags for [rowsSolution]: only the bottom-right corner is a wall. */
    val rowsWalls: List<Boolean> = List(16) { it == 15 }

    /** Valid 4x4 board with a wall. Used for rule checks, not for uniqueness. */
    val rowsPuzzle: PathPuzzle = PathPuzzle(
        id = "PA-SPEC-ROWS",
        size = 4,
        endpoints = rowsEndpoints,
        walls = rowsWalls,
        solution = rowsSolution,
        pack = Pack.EASY,
        storedGrade = "none:0",
    )

    /**
     * 4x4 corridor. The only non-wall cells form one induced path from the top-left corner
     * to the bottom-right corner: along the top row, then down the right column.
     * With one pair and no other route, the colouring is unique.
     */
    /** Cells of the corridor in path order. */
    val corridorCells: Set<Int> = setOf(0, 1, 2, 3, 7, 11, 15)

    /** Stored solution of the corridor: colour 1 on the path, 0 on walls. */
    val corridorSolution: List<Int> = List(16) { index -> if (index in corridorCells) 1 else 0 }

    /** Endpoints of the corridor: the two ends of the single path. */
    val corridorEndpoints: List<Int?> = List(16) { index -> if (index == 0 || index == 15) 1 else null }

    /** Wall flags of the corridor: every cell that is not on the corridor. */
    val corridorWalls: List<Boolean> = List(16) { index -> index !in corridorCells }

    /** Unique 4x4 corridor puzzle with one pair. */
    val corridorPuzzle: PathPuzzle = PathPuzzle(
        id = "PA-SPEC-CORRIDOR",
        size = 4,
        endpoints = corridorEndpoints,
        walls = corridorWalls,
        solution = corridorSolution,
        pack = Pack.EASY,
        storedGrade = "none:0",
    )

    /**
     * 4x4 board where the top-left corner is an endpoint with both neighbours walled.
     * No path can leave that endpoint, so the board has no solution.
     */
    val deadEndPuzzle: PathPuzzle = run {
        val endpoints = List<Int?>(16) { null }.toMutableList()
        endpoints[0] = 1
        endpoints[15] = 1
        endpoints[3] = 2
        endpoints[12] = 2
        val walls = List(16) { it == 1 || it == 4 }
        val solution = List(16) { index ->
            when {
                walls[index] -> 0
                index == 3 || index == 12 -> 2
                else -> 1
            }
        }
        PathPuzzle(
            id = "PA-SPEC-DEADEND",
            size = 4,
            endpoints = endpoints,
            walls = walls,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "none:0",
        )
    }

    /** Returns a player state that holds the full stored solution of [puzzle] in every non-wall cell. */
    fun solvedDigits(puzzle: PathPuzzle): List<Int?> = List(puzzle.grid.cellCount) { index ->
        if (puzzle.walls[index]) null else puzzle.solution[index]
    }
}
