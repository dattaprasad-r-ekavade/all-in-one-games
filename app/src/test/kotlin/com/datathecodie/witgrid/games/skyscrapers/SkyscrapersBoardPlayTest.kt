package com.datathecodie.witgrid.games.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersEngine
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections

class SkyscrapersBoardPlayTest {

    private fun handFixture4x4(): SkyscrapersPuzzle {
        return SkyscrapersPuzzle(
            id = "SK-4-HAND-FIXTURE",
            size = 4,
            topClues = listOf(4, 3, 2, 1),
            bottomClues = listOf(1, 2, 2, 2),
            leftClues = listOf(4, 3, 2, 1),
            rightClues = listOf(1, 2, 2, 2),
            solution = listOf(
                1, 2, 3, 4,
                2, 3, 4, 1,
                3, 4, 1, 2,
                4, 1, 2, 3,
            ),
            pack = Pack.EASY,
            storedGrade = "none:0",
            givens = Collections.nCopies(16, null),
        )
    }

    @Test
    fun playsHandFixturePuzzleToSolvedThroughBoardInput() {
        val puzzle = handFixture4x4()
        var state = SkyscrapersState.initial(puzzle)

        assertFalse(SkyscrapersEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), SkyscrapersEngine.mistakes(puzzle, state))

        // Check initial clue satisfaction (none satisfied initially)
        (0 until puzzle.size).forEach { index ->
            val topDown = (0 until puzzle.size).map { r -> state.digits[r * puzzle.size + index] }
            assertFalse(isClueSatisfied(topDown, puzzle.topClues[index], puzzle.size))
        }

        // Play each cell from solution
        for (index in 0 until puzzle.grid.cellCount) {
            val cell = Cell(index)
            val digit = puzzle.solution[index]
            val moves = SkyscrapersBoardInput.digitMoves(state, cell, digit, autoNotesEnabled = false)
            assertEquals(1, moves.size)
            state = moves.fold(state, SkyscrapersState::withMove)
        }

        assertTrue(SkyscrapersEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), SkyscrapersEngine.mistakes(puzzle, state))

        // Check all clues satisfied at solved state
        (0 until puzzle.size).forEach { col ->
            val topDown = (0 until puzzle.size).map { r -> state.digits[r * puzzle.size + col] }
            assertTrue(isClueSatisfied(topDown, puzzle.topClues[col], puzzle.size))

            val botUp = (puzzle.size - 1 downTo 0).map { r -> state.digits[r * puzzle.size + col] }
            assertTrue(isClueSatisfied(botUp, puzzle.bottomClues[col], puzzle.size))
        }
        (0 until puzzle.size).forEach { row ->
            val rowRight = (0 until puzzle.size).map { c -> state.digits[row * puzzle.size + c] }
            assertTrue(isClueSatisfied(rowRight, puzzle.leftClues[row], puzzle.size))

            val rowLeft = (puzzle.size - 1 downTo 0).map { c -> state.digits[row * puzzle.size + c] }
            assertTrue(isClueSatisfied(rowLeft, puzzle.rightClues[row], puzzle.size))
        }
    }

    @Test
    fun playsGeneratedEasyPuzzleToSolvedThroughBoardInput() {
        val rng = SplitMix64(42L)
        val puzzle = SkyscrapersEngine.generate(rng, Pack.EASY)
        var state = SkyscrapersState.initial(puzzle)

        assertFalse(SkyscrapersEngine.isSolved(puzzle, state))

        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.givens[index] == null) {
                val cell = Cell(index)
                val digit = puzzle.solution[index]
                val moves = SkyscrapersBoardInput.digitMoves(state, cell, digit, autoNotesEnabled = true)
                state = moves.fold(state, SkyscrapersState::withMove)
            }
        }

        assertTrue(SkyscrapersEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), SkyscrapersEngine.mistakes(puzzle, state))
    }

    @Test
    fun mistakesAreIdentifiedAndCleared() {
        val puzzle = handFixture4x4()
        var state = SkyscrapersState.initial(puzzle)

        // Enter wrong digit at cell 0 (solution is 1, enter 4)
        val moves = SkyscrapersBoardInput.digitMoves(state, Cell(0), 4, autoNotesEnabled = false)
        state = moves.fold(state, SkyscrapersState::withMove)

        assertEquals(listOf(Cell(0)), SkyscrapersEngine.mistakes(puzzle, state))

        // Clear the mistake
        val clearMoves = SkyscrapersBoardInput.clearMoves(state, Cell(0))
        state = clearMoves.fold(state, SkyscrapersState::withMove)

        assertEquals(emptyList<Cell>(), SkyscrapersEngine.mistakes(puzzle, state))
        assertEquals(null, state.digits[0])
    }
}
