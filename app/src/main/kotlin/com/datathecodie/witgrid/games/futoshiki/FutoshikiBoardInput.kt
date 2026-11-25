package com.datathecodie.witgrid.games.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState

/** Builds absolute digit and note updates so one keypad action is one history entry. */
internal object FutoshikiBoardInput {

    fun digitMoves(
        state: FutoshikiState,
        cell: Cell,
        digit: Int,
        autoNotesEnabled: Boolean,
    ): List<Move> {
        val size = state.puzzle.size
        require(cell.index in state.digits.indices) { "Digit cell outside Futoshiki board" }
        require(digit in 0..size) { "Futoshiki digit must be zero through $size" }
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
                    isPeer(cell.index, peerIndex, size)
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

    fun noteMoves(state: FutoshikiState, cell: Cell, digit: Int): List<Move> {
        val size = state.puzzle.size
        require(cell.index in state.digits.indices) { "Note cell outside Futoshiki board" }
        require(digit in 1..size) { "Futoshiki note digit must be from one to $size" }
        if (state.puzzle.givens[cell.index] != null || state.digits[cell.index] != null) return emptyList()

        val digitBit = 1 shl (digit - 1)
        val updatedMask = state.notes[cell.index] xor digitBit
        return listOf(Move.SetNotes(cell, updatedMask))
    }

    fun clearMoves(state: FutoshikiState, cell: Cell): List<Move> {
        require(cell.index in state.digits.indices) { "Clear cell outside Futoshiki board" }
        if (state.puzzle.givens[cell.index] != null) return emptyList()
        return when {
            state.digits[cell.index] != null -> listOf(Move.SetDigit(cell, 0))
            state.notes[cell.index] != 0 -> listOf(Move.SetNotes(cell, 0))
            else -> emptyList()
        }
    }

    fun isPeer(firstIndex: Int, secondIndex: Int, size: Int): Boolean {
        val firstRow = firstIndex / size
        val firstColumn = firstIndex % size
        val secondRow = secondIndex / size
        val secondColumn = secondIndex % size
        return firstRow == secondRow || firstColumn == secondColumn
    }

    fun isHorizontalBroken(sign: FutoshikiSign, left: Int?, right: Int?): Boolean {
        if (left == null || right == null) return false
        return when (sign) {
            FutoshikiSign.LESS -> left >= right
            FutoshikiSign.GREATER -> left <= right
            FutoshikiSign.NONE -> false
        }
    }

    fun isVerticalBroken(sign: FutoshikiSign, top: Int?, bottom: Int?): Boolean {
        if (top == null || bottom == null) return false
        return when (sign) {
            FutoshikiSign.LESS -> top >= bottom
            FutoshikiSign.GREATER -> top <= bottom
            FutoshikiSign.NONE -> false
        }
    }
}
