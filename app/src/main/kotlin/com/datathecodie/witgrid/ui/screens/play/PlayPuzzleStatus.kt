package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState

@Composable
internal fun playPuzzleStatus(state: Any): String? = when (state) {
    is CrownsState -> stringResource(R.string.play_crowns_count, state.marks.count { it == Mark.FILLED }, state.puzzle.size)
    is BinaryState -> stringResource(R.string.play_cells_count, state.symbols.count { it != BinarySymbol.EMPTY }, state.symbols.size)
    is MiniSudokuState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is NonogramState -> stringResource(R.string.play_marked_count, state.marks.count { it != Mark.EMPTY }, state.marks.size)
    else -> null
}
