package com.datathecodie.witgrid.ui.screens.play

import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.engine.sudoku.SudokuState

/** Player entries that visibly break a puzzle's local rules. Never inspects stored solutions. */
object RuleConflicts {
    fun cells(state: Any): Set<Cell> = when (state) {
        is CrownsState -> crowns(state)
        is BinaryState -> binary(state)
        is MiniSudokuState -> sudoku(state.puzzle.size, state.digits)
        is SudokuState -> sudoku(state.puzzle.size, state.digits)
        is KillerSudokuState -> sudoku(state.puzzle.size, state.digits)
        is FutoshikiState -> futoshiki(state)
        is MosaicState -> mosaic(state)
        else -> emptySet()
    }

    private fun crowns(state: CrownsState): Set<Cell> {
        val size = state.puzzle.size
        val crowns = state.marks.indices.filter { state.marks[it] == Mark.FILLED }
        val conflicts = mutableSetOf<Cell>()
        for (left in crowns.indices) for (right in left + 1 until crowns.size) {
            val first = crowns[left]
            val second = crowns[right]
            val sameRow = first / size == second / size
            val sameColumn = first % size == second % size
            val sameRegion = state.puzzle.regions[first] == state.puzzle.regions[second]
            val touching = kotlin.math.abs(first / size - second / size) <= 1 &&
                kotlin.math.abs(first % size - second % size) <= 1
            if (sameRow || sameColumn || sameRegion || touching) {
                conflicts += Cell(first)
                conflicts += Cell(second)
            }
        }
        return conflicts
    }

    private fun binary(state: BinaryState): Set<Cell> {
        val size = state.puzzle.size
        val conflicts = mutableSetOf<Cell>()
        for (row in 0 until size) for (start in 0 until size - 2) {
            val indices = (start..start + 2).map { row * size + it }
            val values = indices.map { state.symbols[it] }
            if (values[0] != BinarySymbol.EMPTY && values.distinct().size == 1) indices.forEach { conflicts += Cell(it) }
        }
        for (column in 0 until size) for (start in 0 until size - 2) {
            val indices = (start..start + 2).map { it * size + column }
            val values = indices.map { state.symbols[it] }
            if (values[0] != BinarySymbol.EMPTY && values.distinct().size == 1) indices.forEach { conflicts += Cell(it) }
        }
        return conflicts
    }

    private fun sudoku(size: Int, digits: List<Int?>): Set<Cell> {
        val conflicts = mutableSetOf<Cell>()
        fun markDuplicates(indices: List<Int>) {
            indices.groupBy { digits[it] }.filter { (digit, cells) -> digit != null && cells.size > 1 }
                .values.flatten().forEach { conflicts += Cell(it) }
        }
        for (row in 0 until size) markDuplicates((0 until size).map { row * size + it })
        for (column in 0 until size) markDuplicates((0 until size).map { it * size + column })
        val boxHeight = if (size == 6) 2 else 3
        val boxWidth = size / boxHeight
        for (row in 0 until size step boxHeight) for (column in 0 until size step boxWidth) {
            markDuplicates((row until row + boxHeight).flatMap { r ->
                (column until column + boxWidth).map { c -> r * size + c }
            })
        }
        return conflicts
    }

    private fun futoshiki(state: FutoshikiState): Set<Cell> = state.puzzle.inequalities
        .filter { inequality ->
            val lesser = state.digits[inequality.lesser.index]
            val greater = state.digits[inequality.greater.index]
            lesser != null && greater != null && lesser >= greater
        }
        .flatMapTo(mutableSetOf()) { listOf(it.lesser, it.greater) }

    private fun mosaic(state: MosaicState): Set<Cell> {
        val size = state.puzzle.size
        val conflicts = mutableSetOf<Cell>()
        state.puzzle.clues.forEachIndexed { index, clue ->
            if (clue == null) return@forEachIndexed
            val row = index / size
            val column = index % size
            val filled = (maxOf(0, row - 1)..minOf(size - 1, row + 1)).flatMap { r ->
                (maxOf(0, column - 1)..minOf(size - 1, column + 1)).map { c -> r * size + c }
            }.filter { state.marks[it] == Mark.FILLED }
            if (filled.size > clue) filled.forEach { conflicts += Cell(it) }
        }
        return conflicts
    }
}
