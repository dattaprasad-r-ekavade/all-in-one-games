package com.datathecodie.witgrid.games.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState

/** Builds absolute digit and note updates so one keypad action is one history entry. */
internal object SkyscrapersBoardInput {

    fun digitMoves(
        state: SkyscrapersState,
        cell: Cell,
        digit: Int,
        autoNotesEnabled: Boolean,
    ): List<Move> {
        val size = state.puzzle.size
        require(cell.index in state.digits.indices) { "Digit cell outside Skyscrapers board" }
        require(digit in 0..size) { "Skyscrapers digit must be zero through $size" }
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

    fun noteMoves(state: SkyscrapersState, cell: Cell, digit: Int): List<Move> {
        val size = state.puzzle.size
        require(cell.index in state.digits.indices) { "Note cell outside Skyscrapers board" }
        require(digit in 1..size) { "Skyscrapers note digit must be from one to $size" }
        if (state.puzzle.givens[cell.index] != null || state.digits[cell.index] != null) return emptyList()

        val digitBit = 1 shl (digit - 1)
        val updatedMask = state.notes[cell.index] xor digitBit
        return listOf(Move.SetNotes(cell, updatedMask))
    }

    fun clearMoves(state: SkyscrapersState, cell: Cell): List<Move> {
        require(cell.index in state.digits.indices) { "Clear cell outside Skyscrapers board" }
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
}
