package com.datathecodie.witgrid.games.minisudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState

/** Builds absolute digit and note updates so one keypad action is one history entry. */
internal object MiniSudokuBoardInput {
    fun digitMoves(
        state: MiniSudokuState,
        cell: Cell,
        digit: Int,
        autoNotesEnabled: Boolean,
    ): List<Move> {
        require(cell.index in state.digits.indices) { "Digit cell outside Mini Sudoku board" }
        require(digit in 0..BOARD_SIZE) { "Mini Sudoku digit must be zero through six" }
        if (state.puzzle.givens[cell.index] != null || state.digits[cell.index] == digit.takeIf { it != 0 }) {
            return emptyList()
        }

        val moves = mutableListOf<Move>()
        var updatedState = state
        val digitMove = Move.SetDigit(cell, digit)
        moves += digitMove
        updatedState = updatedState.withMove(digitMove)

        if (autoNotesEnabled && digit != 0) {
            val digitBit = 1 shl (digit - 1)
            for (peerIndex in updatedState.digits.indices) {
                if (
                    peerIndex != cell.index &&
                    updatedState.digits[peerIndex] == null &&
                    isPeer(cell.index, peerIndex, state.puzzle.size)
                ) {
                    val oldMask = updatedState.notes[peerIndex]
                    val newMask = oldMask and digitBit.inv()
                    if (newMask != oldMask) {
                        val noteMove = Move.SetNotes(Cell(peerIndex), newMask)
                        moves += noteMove
                        updatedState = updatedState.withMove(noteMove)
                    }
                }
            }
        }

        return moves
    }

    fun noteMoves(state: MiniSudokuState, cell: Cell, digit: Int): List<Move> {
        require(cell.index in state.digits.indices) { "Note cell outside Mini Sudoku board" }
        require(digit in 1..BOARD_SIZE) { "Mini Sudoku note digit must be from one to six" }
        if (state.puzzle.givens[cell.index] != null || state.digits[cell.index] != null) return emptyList()

        val digitBit = 1 shl (digit - 1)
        val updatedMask = state.notes[cell.index] xor digitBit
        return listOf(Move.SetNotes(cell, updatedMask))
    }

    fun clearMoves(state: MiniSudokuState, cell: Cell): List<Move> {
        require(cell.index in state.digits.indices) { "Clear cell outside Mini Sudoku board" }
        if (state.puzzle.givens[cell.index] != null) return emptyList()
        return when {
            state.digits[cell.index] != null -> listOf(Move.SetDigit(cell, 0))
            state.notes[cell.index] != 0 -> listOf(Move.SetNotes(cell, 0))
            else -> emptyList()
        }
    }

    private fun isPeer(firstIndex: Int, secondIndex: Int, size: Int): Boolean {
        val firstRow = firstIndex / size
        val firstColumn = firstIndex % size
        val secondRow = secondIndex / size
        val secondColumn = secondIndex % size
        return firstRow == secondRow ||
            firstColumn == secondColumn ||
            firstRow / BOX_HEIGHT == secondRow / BOX_HEIGHT && firstColumn / BOX_WIDTH == secondColumn / BOX_WIDTH
    }

    private const val BOARD_SIZE = 6
    private const val BOX_HEIGHT = 2
    private const val BOX_WIDTH = 3
}
