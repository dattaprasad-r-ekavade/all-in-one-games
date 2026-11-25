package com.datathecodie.witgrid.engine.mosaic.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicRules
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.engine.mosaic.MosaicTechniqueSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicTechniqueSolverSpecTest {
    @Test
    fun uniqueHandMadePuzzleSolvesUsingOnlyClueDeductions() {
        val puzzle = MosaicSpecFixtures.allEmptySmallPuzzle()

        assertEquals(1, MosaicRules.countSolutions(puzzle, limit = 2))
        var state = MosaicState.initial(puzzle)
        while (!MosaicRules.isSolved(puzzle, state)) {
            val step = requireNotNull(MosaicTechniqueSolver.nextStep(puzzle, state))
            assertTrue(step.moves.isNotEmpty())
            state = step.moves.fold(state, MosaicState::withMove)
        }
        assertTrue(MosaicRules.isSolved(puzzle, state))
    }

    @Test
    fun hintsAreLegalAndHaveOneSentenceExplanations() {
        val puzzle = MosaicSpecFixtures.allEmptySmallPuzzle()
        val state = MosaicState.initial(puzzle)
        val step = MosaicTechniqueSolver.nextStep(puzzle, state)

        assertNotNull(step)
        step!!
        assertFalse(step.explanation.isBlank())
        assertEquals(1, step.explanation.count { it == '.' })
        assertTrue(step.moves.all { move ->
            move is Move.SetMark && state.marks[move.cell.index] == Mark.EMPTY &&
                move.mark == Mark.CROSSED && move.cell in MosaicRules.areaCells(puzzle.grid, Cell(4))
        })
        assertEquals(step.moves.size, step.moves.map { it.cell }.toSet().size)
    }
}
