package com.datathecodie.witgrid.games.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.sudoku.SudokuState

/** Builds absolute digit and note updates so one keypad action is one history entry. */
internal object SudokuBoardInput {
    fun digitMoves(
        state: SudokuState,
        cell: Cell,
        digit: Int,
        autoNotesEnabled: Boolean,
    ): List<Move> {
        require(cell.index in state.digits.indices) { "Digit cell outside Sudoku board" }
        require(digit in 0..BOARD_SIZE) { "Sudoku digit must be zero through nine" }
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

    fun noteMoves(state: SudokuState, cell: Cell, digit: Int): List<Move> {
        require(cell.index in state.digits.indices) { "Note cell outside Sudoku board" }
        require(digit in 1..BOARD_SIZE) { "Sudoku note digit must be from one to nine" }
        if (state.puzzle.givens[cell.index] != null || state.digits[cell.index] != null) return emptyList()

        val digitBit = 1 shl (digit - 1)
        val updatedMask = state.notes[cell.index] xor digitBit
        return listOf(Move.SetNotes(cell, updatedMask))
    }

    fun clearMoves(state: SudokuState, cell: Cell): List<Move> {
        require(cell.index in state.digits.indices) { "Clear cell outside Sudoku board" }
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
            (firstRow / BOX_SIZE == secondRow / BOX_SIZE && firstColumn / BOX_SIZE == secondColumn / BOX_SIZE)
    }

    private const val BOARD_SIZE = 9
    private const val BOX_SIZE = 3
}
