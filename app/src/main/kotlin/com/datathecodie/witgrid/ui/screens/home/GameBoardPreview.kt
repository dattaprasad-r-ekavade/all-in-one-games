package com.datathecodie.witgrid.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState

/** A decorative board motif; game controls and accessibility belong to the playable board. */
@Composable
internal fun GameBoardPreview(gameId: String, modifier: Modifier = Modifier, boardState: Any? = null) {
    val colors = WitgridTheme.colors
    Canvas(modifier.size(64.dp)) {
        val count = when (boardState) {
            is CrownsState -> boardState.puzzle.size
            is BinaryState -> boardState.puzzle.size
            is NonogramState -> boardState.puzzle.size
            is MiniSudokuState -> 6
            else -> 5
        }
        val gap = size.minDimension / 38
        val cell = (size.minDimension - gap * (count + 1)) / count
        val regions = listOf(Color(0xFFCBC0EE), Color(0xFFA7DCC2), Color(0xFFA8CFEA), Color(0xFFF1C798))
        drawRoundRect(colors.paper, cornerRadius = CornerRadius(8.dp.toPx()))
        repeat(count * count) { index ->
            val row = index / count
            val column = index % count
            val position = Offset(gap + column * (cell + gap), gap + row * (cell + gap))
            val cellSize = Size(cell, cell)
            if (boardState != null) {
                val center = position + Offset(cell / 2, cell / 2)
                when (boardState) {
                    is CrownsState -> {
                        drawRoundRect(regions[boardState.puzzle.regions[index] % regions.size], position, cellSize, CornerRadius(gap))
                        if (boardState.marks[index] == Mark.FILLED) drawCircle(colors.ink, cell / 4, center)
                    }
                    is BinaryState -> when (boardState.symbols[index]) {
                        BinarySymbol.A -> drawCircle(colors.accent, cell / 2.4f, center)
                        BinarySymbol.B -> drawCircle(Color(0xFFC2622F), cell / 2.7f, center, style = Stroke(cell / 5))
                        else -> Unit
                    }
                    is NonogramState -> if (boardState.marks[index] == Mark.FILLED) drawRect(colors.ink, position, cellSize)
                    is MiniSudokuState -> if (boardState.digits[index] != null) drawRoundRect(colors.accent, position, cellSize, CornerRadius(gap))
                }
            } else when (gameId) {
                "crowns" -> drawRoundRect(regions[if (column > 2) 1 else if (row < 2) 0 else if (column < 2) 2 else 3], position, cellSize, CornerRadius(gap))
                "binary" -> if ((row + column) % 3 != 1) {
                    if ((row + column) % 2 == 0) drawCircle(colors.accent, cell / 2.3f, position + Offset(cell / 2, cell / 2))
                    else drawCircle(Color(0xFFC2622F), cell / 2.7f, position + Offset(cell / 2, cell / 2), style = Stroke(cell / 5))
                }
                "nonogram" -> if ((row * 3 + column) % 4 != 0) drawRoundRect(colors.ink, position, cellSize, CornerRadius(gap))
                else -> if ((row + column) % 2 == 0) drawRoundRect(if (index == 12) colors.accent else colors.cardEdge, position, cellSize, CornerRadius(gap))
            }
        }
        drawRoundRect(colors.ink, cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    }
}
