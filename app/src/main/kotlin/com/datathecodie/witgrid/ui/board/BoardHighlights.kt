package com.datathecodie.witgrid.ui.board

import androidx.compose.runtime.compositionLocalOf
import com.datathecodie.witgrid.engine.core.Cell

val LocalBoardHighlightCells = compositionLocalOf<Set<Cell>> { emptySet() }
