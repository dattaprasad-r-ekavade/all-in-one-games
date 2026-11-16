package com.datathecodie.witgrid.games.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CrownsBoardInputTest {
    @Test
    fun tapCyclesFromXToCrownToClear() {
        val state = testState()
        val cell = Cell(5)

        assertEquals(Mark.CROSSED, CrownsBoardInput.nextMark(Mark.EMPTY))
        assertEquals(Mark.FILLED, CrownsBoardInput.nextMark(Mark.CROSSED))
        assertEquals(Mark.EMPTY, CrownsBoardInput.nextMark(Mark.FILLED))
        assertEquals(listOf(Move.SetMark(cell, Mark.CROSSED)), CrownsBoardInput.tapMoves(state, cell, false))
    }

    @Test
    fun autoXAddsOnlyEmptyNeighboursToTheSameGesture() {
        val state = testState()
        val crownCell = Cell(16)
        val moves = CrownsBoardInput.tapMoves(state, crownCell, autoXEnabled = true)

        assertEquals(Move.SetMark(crownCell, Mark.CROSSED), moves.first())
        val crownState = state.withMove(moves.first())
        val crownMoves = CrownsBoardInput.tapMoves(crownState, crownCell, autoXEnabled = true)
        assertEquals(Move.SetMark(crownCell, Mark.FILLED), crownMoves.first())
        assertEquals(
            crownState.puzzle.grid.neighbours(crownCell).map { Move.SetMark(it, Mark.CROSSED) },
            crownMoves.drop(1),
        )
    }

    @Test
    fun autoXKeepsExistingMarksAndCrowns() {
        val cell = Cell(0)
        val state = testState()
            .withMove(Move.SetMark(cell, Mark.CROSSED))
            .withMove(Move.SetMark(Cell(1), Mark.FILLED))
            .withMove(Move.SetMark(Cell(6), Mark.CROSSED))

        assertEquals(
            listOf(Move.SetMark(cell, Mark.FILLED), Move.SetMark(Cell(7), Mark.CROSSED)),
            CrownsBoardInput.tapMoves(state, cell, autoXEnabled = true),
        )
    }

    @Test
    fun dragSetsOnlyEmptyCellsAndRemovesRepeatedCells() {
        val state = testState()
            .withMove(Move.SetMark(Cell(3), Mark.FILLED))
            .withMove(Move.SetMark(Cell(4), Mark.CROSSED))

        assertEquals(
            listOf(Move.SetMark(Cell(2), Mark.CROSSED)),
            CrownsBoardInput.dragMoves(state, listOf(Cell(2), Cell(3), Cell(4), Cell(2))),
        )
    }

    @Test
    fun longPressClearsOnlyMarkedCells() {
        val state = testState().withMove(Move.SetMark(Cell(3), Mark.CROSSED))

        assertEquals(listOf(Move.SetMark(Cell(3), Mark.EMPTY)), CrownsBoardInput.clearMoves(state, Cell(3)))
        assertEquals(emptyList<Move>(), CrownsBoardInput.clearMoves(state, Cell(4)))
    }

    private fun testState(): CrownsState = CrownsState.initial(testPuzzle())

    private fun testPuzzle() = CrownsPuzzle(
        id = "CR-TEST-1",
        size = 6,
        regions = (0 until 6).flatMap { region -> List(6) { region } },
        solutionColumns = listOf(0, 2, 4, 1, 3, 5),
        pack = Pack.EASY,
        storedGrade = "none:0",
    )
}
