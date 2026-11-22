package com.datathecodie.witgrid.games.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiEngine
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiPuzzle
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections

class FutoshikiBoardPlayTest {

    private fun handFixture5x5(): FutoshikiPuzzle {
        val solution = listOf(
            1, 2, 3, 4, 5,
            2, 3, 4, 5, 1,
            3, 4, 5, 1, 2,
            4, 5, 1, 2, 3,
            5, 1, 2, 3, 4,
        )
        val horizontalSigns = ("<<<<" + "<<<>" + "<<><" + "<><<" + "><<<").map {
            FutoshikiSign.fromHorizontalChar(it)
        }
        val verticalSigns = ("^^^^v" + "^^^v^" + "^^v^^" + "^v^^^").map {
            FutoshikiSign.fromVerticalChar(it)
        }
        return FutoshikiPuzzle(
            id = "FU-5-HAND-FIXTURE",
            size = 5,
            givens = Collections.nCopies(25, null),
            horizontalSigns = horizontalSigns,
            verticalSigns = verticalSigns,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "sign-bounds:1",
        )
    }

    @Test
    fun playsHandFixturePuzzleToSolvedThroughBoardInput() {
        val puzzle = handFixture5x5()
        var state = FutoshikiState.initial(puzzle)

        assertFalse(FutoshikiEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), FutoshikiEngine.mistakes(puzzle, state))

        // Play each cell from solution
        for (index in 0 until puzzle.grid.cellCount) {
            val cell = Cell(index)
            val digit = puzzle.solution[index]
            val moves = FutoshikiBoardInput.digitMoves(state, cell, digit, autoNotesEnabled = false)
            assertEquals(1, moves.size)
            state = moves.fold(state, FutoshikiState::withMove)
        }

        assertTrue(FutoshikiEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), FutoshikiEngine.mistakes(puzzle, state))

        // Check horizontal signs are not broken
        for (row in 0 until puzzle.size) {
            for (col in 0 until puzzle.size - 1) {
                val sign = puzzle.horizontalSign(row, col)
                val left = state.digits[row * puzzle.size + col]
                val right = state.digits[row * puzzle.size + col + 1]
                assertFalse(FutoshikiBoardInput.isHorizontalBroken(sign, left, right))
            }
        }

        // Check vertical signs are not broken
        for (row in 0 until puzzle.size - 1) {
            for (col in 0 until puzzle.size) {
                val sign = puzzle.verticalSign(row, col)
                val top = state.digits[row * puzzle.size + col]
                val bottom = state.digits[(row + 1) * puzzle.size + col]
                assertFalse(FutoshikiBoardInput.isVerticalBroken(sign, top, bottom))
            }
        }
    }

    @Test
    fun playsGeneratedEasyPuzzleToSolvedThroughBoardInput() {
        val rng = SplitMix64(42L)
        val puzzle = FutoshikiEngine.generate(rng, Pack.EASY)
        var state = FutoshikiState.initial(puzzle)

        assertFalse(FutoshikiEngine.isSolved(puzzle, state))

        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.givens[index] == null) {
                val cell = Cell(index)
                val digit = puzzle.solution[index]
                val moves = FutoshikiBoardInput.digitMoves(state, cell, digit, autoNotesEnabled = true)
                state = moves.fold(state, FutoshikiState::withMove)
            }
        }

        assertTrue(FutoshikiEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), FutoshikiEngine.mistakes(puzzle, state))
    }

    @Test
    fun mistakesAreIdentifiedAndCleared() {
        val puzzle = handFixture5x5()
        var state = FutoshikiState.initial(puzzle)

        // Enter wrong digit at cell 0 (solution is 1, enter 5)
        val moves = FutoshikiBoardInput.digitMoves(state, Cell(0), 5, autoNotesEnabled = false)
        state = moves.fold(state, FutoshikiState::withMove)

        assertEquals(listOf(Cell(0)), FutoshikiEngine.mistakes(puzzle, state))

        // Clear the mistake
        val clearMoves = FutoshikiBoardInput.clearMoves(state, Cell(0))
        state = clearMoves.fold(state, FutoshikiState::withMove)

        assertEquals(emptyList<Cell>(), FutoshikiEngine.mistakes(puzzle, state))
        assertEquals(null, state.digits[0])
    }

    @Test
    fun brokenInequalitiesDetectedWhenIncorrectDigitsEntered() {
        val puzzle = handFixture5x5()
        var state = FutoshikiState.initial(puzzle)

        // In handFixture5x5, between (0, 0) and (0, 1), horizontal sign is LESS (<)
        // If we enter 5 in (0, 0) and 1 in (0, 1), it violates 5 < 1
        val move1 = FutoshikiBoardInput.digitMoves(state, Cell(0), 5, autoNotesEnabled = false)
        state = move1.fold(state, FutoshikiState::withMove)

        val move2 = FutoshikiBoardInput.digitMoves(state, Cell(1), 1, autoNotesEnabled = false)
        state = move2.fold(state, FutoshikiState::withMove)

        val sign = puzzle.horizontalSign(0, 0)
        assertTrue(FutoshikiBoardInput.isHorizontalBroken(sign, state.digits[0], state.digits[1]))

        // Now fix cell 0 to 1 and cell 1 to 2 -> 1 < 2 is valid
        val fix1 = FutoshikiBoardInput.digitMoves(state, Cell(0), 1, autoNotesEnabled = false)
        state = fix1.fold(state, FutoshikiState::withMove)
        val fix2 = FutoshikiBoardInput.digitMoves(state, Cell(1), 2, autoNotesEnabled = false)
        state = fix2.fold(state, FutoshikiState::withMove)

        assertFalse(FutoshikiBoardInput.isHorizontalBroken(sign, state.digits[0], state.digits[1]))
    }
}
