package com.datathecodie.witgrid.ui.screens.play

import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.engine.path.PathPuzzle
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import com.datathecodie.witgrid.engine.tactics.ChessMove
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.TacticsBoard
import com.datathecodie.witgrid.engine.tactics.TacticsPuzzle
import com.datathecodie.witgrid.engine.tactics.TacticsState
import org.junit.Assert.assertEquals
import org.junit.Test

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

    @Test
    fun skyscrapersDuplicateHeightInRowIsFlagged() {
        val puzzle = SkyscrapersPuzzle(
            "test", 4, List(4) { null }, List(4) { null }, List(4) { null }, List(4) { null },
            List(16) { it / 4 + 1 }, Pack.EASY, "test",
        )
        val digits = MutableList<Int?>(16) { null }
        digits[0] = 2
        digits[2] = 2
        assertEquals(setOf(Cell(0), Cell(2)), RuleConflicts.cells(SkyscrapersState(puzzle, digits, List(16) { 0 })))
    }

    @Test
    fun pathAdjacentDifferentDigitsAreFlagged() {
        val endpoints = MutableList<Int?>(25) { null }
        endpoints[0] = 1
        endpoints[24] = 1
        endpoints[4] = 2
        endpoints[20] = 2
        val solution = List(25) { index -> endpoints[index] ?: 1 }
        val puzzle = PathPuzzle(
            "test", 5, endpoints, List(25) { false }, solution, Pack.EASY, "test",
        )
        val digits = endpoints.toMutableList()
        digits[6] = 1
        digits[7] = 2
        assertEquals(setOf(Cell(6), Cell(7)), RuleConflicts.cells(PathState(puzzle, digits)))
    }

    @Test
    fun nonogramOverfilledRowFlagsFilledCells() {
        val clues = listOf(listOf(1), emptyList(), emptyList(), emptyList(), emptyList())
        val puzzle = NonogramPuzzle(
            "test", 5, clues, clues, List(25) { 0 }, Pack.EASY, "test",
        )
        val marks = MutableList(25) { Mark.EMPTY }
        marks[0] = Mark.FILLED
        marks[1] = Mark.FILLED
        assertEquals(setOf(Cell(0), Cell(1)), RuleConflicts.cells(NonogramState(puzzle, marks)))
    }

    @Test
    fun tacticsPositionIsNotFlagged() {
        val puzzle = TacticsPuzzle(
            "test",
            TacticsBoard.fromFen("7k/3Q4/5K2/8/8/8/8/8 w - - 0 1"),
            PieceColor.WHITE,
            1,
            listOf(ChessMove(Cell(11), Cell(14))),
            Pack.EASY,
            "test",
        )
        assertEquals(emptySet<Cell>(), RuleConflicts.cells(TacticsState.initial(puzzle)))
    }
}
