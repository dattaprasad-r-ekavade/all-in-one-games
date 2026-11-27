package com.datathecodie.witgrid.ui.screens.play

import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RuleConflictsTest {
    @Test
    fun crownsTouchingEachOtherAreFlaggedAndUniqueCrownIsNot() {
        val puzzle = CrownsPuzzle("test", 5, List(25) { it % 5 }, listOf(0, 2, 4, 1, 3), Pack.EASY, "test")
        val state = CrownsState.initial(puzzle)
            .withMove(com.datathecodie.witgrid.engine.core.Move.SetMark(Cell(0), Mark.FILLED))
        assertEquals(emptySet<Cell>(), RuleConflicts.cells(state))
        val touching = state.withMove(com.datathecodie.witgrid.engine.core.Move.SetMark(Cell(6), Mark.FILLED))
        assertEquals(setOf(Cell(0), Cell(6)), RuleConflicts.cells(touching))
    }

    @Test
    fun binaryTripleIsFlagged() {
        val puzzle = BinaryPuzzle("test", 6, List(36) { null }, List(36) { it % 2 }, Pack.EASY, "test")
        val symbols = MutableList(36) { BinarySymbol.EMPTY }
        repeat(3) { symbols[it] = BinarySymbol.A }
        assertEquals(setOf(Cell(0), Cell(1), Cell(2)), RuleConflicts.cells(BinaryState(puzzle, symbols)))
    }

    @Test
    fun sudokuDuplicateIsFlagged() {
        val puzzle = SudokuPuzzle("test", List(81) { null }, List(81) { it % 9 + 1 }, Pack.EASY, "test")
        val digits = MutableList<Int?>(81) { null }
        digits[0] = 4
        digits[1] = 4
        assertEquals(setOf(Cell(0), Cell(1)), RuleConflicts.cells(SudokuState(puzzle, digits, List(81) { 0 })))
    }
}
