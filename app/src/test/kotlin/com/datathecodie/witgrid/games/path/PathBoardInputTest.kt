package com.datathecodie.witgrid.games.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.path.PathCodec
import com.datathecodie.witgrid.engine.path.PathEngine
import com.datathecodie.witgrid.engine.path.PathPuzzle
import com.datathecodie.witgrid.engine.path.PathState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathBoardInputTest {
    private val unique5x5Line = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|EASY|easy:1"

    @Test
    fun dragPaintsEmptyCellsAlongPathFromEndpoint() {
        val puzzle = PathCodec.decode(unique5x5Line)
        val state = PathState.initial(puzzle)

        val moves = PathBoardInput.dragMoves(
            state,
            listOf(Cell(0), Cell(1), Cell(2), Cell(3), Cell(4)),
        )
        assertEquals(
            listOf(
                Move.SetDigit(Cell(1), 1),
                Move.SetDigit(Cell(2), 1),
                Move.SetDigit(Cell(3), 1),
                Move.SetDigit(Cell(4), 1),
            ),
            moves,
        )
    }

    @Test
    fun dragStartingOnEmptyOrWallProducesNoMoves() {
        val state = PathState.initial(PathCodec.decode(unique5x5Line))

        assertEquals(emptyList<Move>(), PathBoardInput.dragMoves(state, listOf(Cell(1), Cell(2), Cell(0))))
        assertEquals(emptyList<Move>(), PathBoardInput.dragMoves(state, listOf(Cell(12), Cell(13), Cell(6))))
    }

    @Test
    fun dragSkipsWallsAndGivenEndpoints() {
        val puzzle = PathCodec.decode(unique5x5Line)
        val state = PathState.initial(puzzle)

        assertEquals(emptyList<Move>(), PathBoardInput.dragMoves(state, listOf(Cell(12), Cell(13))))
        assertEquals(emptyList<Move>(), PathBoardInput.dragMoves(state, listOf(Cell(0), Cell(0))))
        assertEquals(emptyList<Move>(), PathBoardInput.clearMoves(state, Cell(0)))
        assertEquals(emptyList<Move>(), PathBoardInput.clearMoves(state, Cell(12)))

        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(0), 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(12), 1))
        }
    }

    @Test
    fun clearUsesZeroAndDragDoesNotOverwriteOtherColours() {
        var state = PathCodec.decode(unique5x5Line).let(PathState::initial)
        state = apply(state, PathBoardInput.dragMoves(state, listOf(Cell(0), Cell(1))))

        assertEquals(listOf(Move.SetDigit(Cell(1), 0)), PathBoardInput.clearMoves(state, Cell(1)))

        state = state.withMove(Move.SetDigit(Cell(2), 2))
        assertEquals(emptyList<Move>(), PathBoardInput.dragMoves(state, listOf(Cell(0), Cell(1), Cell(2))))
    }

    @Test
    fun fixturePuzzleSolvesThroughDragInput() {
        val puzzle = PathCodec.decode(unique5x5Line)
        var state = PathState.initial(puzzle)
        assertFalse(PathEngine.isSolved(puzzle, state))

        for (pair in 1..puzzle.pairCount) {
            val cells = solutionPathCells(puzzle, pair)
            val moves = PathBoardInput.dragMoves(state, cells)
            state = apply(state, moves)
        }

        assertTrue(PathEngine.isSolved(puzzle, state))
    }

    private fun apply(state: PathState, moves: List<Move>): PathState =
        moves.fold(state) { current, move -> PathEngine.applyMove(current, move) }

    private fun solutionPathCells(puzzle: PathPuzzle, pair: Int): List<Cell> {
        val size = puzzle.size
        val endpoints = puzzle.endpoints.indices.filter { puzzle.endpoints[it] == pair }
        require(endpoints.size == 2) { "Pair $pair must have two endpoints" }
        val start = endpoints[0]
        val target = endpoints[1]
        val path = mutableListOf(Cell(start))
        var previous = -1
        var current = start
        while (current != target) {
            val row = current / size
            val column = current % size
            val next = buildList {
                if (row > 0) add((row - 1) * size + column)
                if (row < size - 1) add((row + 1) * size + column)
                if (column > 0) add(row * size + column - 1)
                if (column < size - 1) add(row * size + column + 1)
            }.single { neighbor ->
                neighbor != previous && puzzle.solution[neighbor] == pair
            }
            path += Cell(next)
            previous = current
            current = next
        }
        return path
    }
}