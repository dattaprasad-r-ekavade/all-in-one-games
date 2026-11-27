package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.coloursort.ColourSortState
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import com.datathecodie.witgrid.engine.tactics.TacticsState

@Composable
internal fun playPuzzleStatus(state: Any): String? = when (state) {
    is CrownsState -> stringResource(R.string.play_crowns_count, state.marks.count { it == Mark.FILLED }, state.puzzle.size)
    is BinaryState -> stringResource(R.string.play_cells_count, state.symbols.count { it != BinarySymbol.EMPTY }, state.symbols.size)
    is MiniSudokuState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is NonogramState -> stringResource(R.string.play_marked_count, state.marks.count { it != Mark.EMPTY }, state.marks.size)
    is SkyscrapersState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is KillerSudokuState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is SudokuState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is FutoshikiState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is PathState -> stringResource(R.string.play_cells_count, state.digits.count { it != null }, state.digits.size)
    is ArrowsState -> stringResource(R.string.play_cells_count, state.arrows.count { it == null }, state.arrows.size)
    is ColourSortState -> stringResource(R.string.play_cells_count,
        state.tubes.count { tube -> tube.isNotEmpty() && tube.all { it == tube.first() } }, state.tubes.size)
    is KlondikeState -> stringResource(R.string.play_cells_count, state.totalFoundationCards, 52)
    is MosaicState -> stringResource(R.string.play_marked_count, state.marks.count { it != Mark.EMPTY }, state.marks.size)
    is TacticsState -> stringResource(R.string.play_cells_count,
        state.completedPlayerMoves.size, state.puzzle.mateIn)
    else -> null
}
