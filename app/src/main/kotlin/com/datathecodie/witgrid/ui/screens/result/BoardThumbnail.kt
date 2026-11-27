package com.datathecodie.witgrid.ui.screens.result

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.coloursort.ColourSortState
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.PieceType
import com.datathecodie.witgrid.engine.tactics.TacticsState
import com.datathecodie.witgrid.ui.theme.WitgridTheme

/** Draws a compact, non-interactive picture of the saved game state. */
@Composable
internal fun BoardThumbnail(state: Any?, modifier: Modifier = Modifier) {
    val colors = WitgridTheme.colors
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        val count = when (state) {
            is CrownsState -> state.puzzle.size
            is BinaryState -> state.puzzle.size
            is NonogramState -> state.puzzle.size
            is MiniSudokuState -> 6
            is SudokuState, is KillerSudokuState -> 9
            is FutoshikiState -> state.puzzle.size
            is SkyscrapersState -> state.puzzle.size
            is PathState -> state.puzzle.size
            is MosaicState -> state.puzzle.size
            is ArrowsState -> state.puzzle.size
            is TacticsState -> 8
            else -> 5
        }
        val cell = size.minDimension / count
        val regions = colors.regions.map { it.color }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb((colors.ink.red * 255).toInt(), (colors.ink.green * 255).toInt(), (colors.ink.blue * 255).toInt())
            textAlign = Paint.Align.CENTER
        }
        fun label(value: String, center: Offset, fontSize: Float = cell * .55f) {
            textPaint.textSize = fontSize
            drawContext.canvas.nativeCanvas.drawText(value, center.x, center.y - (textPaint.ascent() + textPaint.descent()) / 2, textPaint)
        }
        drawRoundRect(colors.card, cornerRadius = CornerRadius(cell / 3))
        repeat(count * count) { index ->
            val row = index / count
            val column = index % count
            val position = Offset(column * cell, row * cell)
            val center = position + Offset(cell / 2, cell / 2)
            when (state) {
                is CrownsState -> {
                    drawRect(regions[state.puzzle.regions[index] % regions.size], position, Size(cell, cell))
                    if (state.marks[index] == Mark.FILLED) {
                        val crown = Path().apply {
                            moveTo(position.x + cell * .2f, position.y + cell * .3f)
                            lineTo(position.x + cell * .38f, position.y + cell * .48f)
                            lineTo(center.x, position.y + cell * .2f)
                            lineTo(position.x + cell * .63f, position.y + cell * .48f)
                            lineTo(position.x + cell * .8f, position.y + cell * .3f)
                            lineTo(position.x + cell * .7f, position.y + cell * .72f)
                            lineTo(position.x + cell * .3f, position.y + cell * .72f)
                            close()
                        }
                        drawPath(crown, colors.ink)
                    }
                }
                is BinaryState -> when (state.symbols[index]) {
                    BinarySymbol.A -> drawCircle(colors.accent, cell * .28f, center)
                    BinarySymbol.B -> drawCircle(Color(0xFFC2622F), cell * .26f, center, style = Stroke(cell * .09f))
                    else -> Unit
                }
                is NonogramState -> if (state.marks[index] == Mark.FILLED) drawRect(colors.ink, position, Size(cell, cell))
                is MiniSudokuState -> state.digits[index]?.let { label(it.toString(), center) }
                is SudokuState -> state.digits[index]?.let { label(it.toString(), center) }
                is KillerSudokuState -> {
                    state.digits[index]?.let { label(it.toString(), center) }
                    val cage = state.puzzle.cages.firstOrNull { it.cells.firstOrNull()?.index == index }
                    if (cage != null) {
                        drawCircle(colors.accent, cell * .055f, position + Offset(cell * .17f, cell * .17f))
                        label(cage.sum.toString(), position + Offset(cell * .36f, cell * .2f), cell * .2f)
                    }
                    drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
                }
                is SkyscrapersState -> {
                    drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
                    state.digits[index]?.let { label(it.toString(), center) }
                    if (row == 0) state.puzzle.topClues[column]?.let { label(it.toString(), Offset(center.x, cell * .18f), cell * .2f) }
                }
                is FutoshikiState -> {
                    drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
                    state.digits[index]?.let { label(it.toString(), center) }
                    val rightSign = state.puzzle.horizontalSigns.getOrNull(row * (count - 1) + column)
                    if (column < count - 1 && rightSign != null && rightSign != FutoshikiSign.NONE) {
                        label(rightSign.horizontalSymbol.toString(), position + Offset(cell * .96f, cell * .5f), cell * .25f)
                    }
                }
                is PathState -> {
                    if (state.puzzle.walls[index]) drawRect(colors.ink, position, Size(cell, cell))
                    else state.digits[index]?.let { label(it.toString(), center) }
                    drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
                }
                is MosaicState -> {
                    when (state.marks[index]) {
                        Mark.FILLED -> drawRect(colors.ink, position, Size(cell, cell))
                        Mark.CROSSED -> {
                            drawLine(colors.cardEdge, position + Offset(cell * .25f, cell * .25f), position + Offset(cell * .75f, cell * .75f), cell * .08f)
                            drawLine(colors.cardEdge, position + Offset(cell * .75f, cell * .25f), position + Offset(cell * .25f, cell * .75f), cell * .08f)
                        }
                        Mark.EMPTY -> state.puzzle.clues[index]?.let { label(it.toString(), center) }
                    }
                    drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
                }
                is TacticsState -> {
                    drawRect(if ((row + column) % 2 == 0) colors.paper else colors.cardEdge, position, Size(cell, cell))
                    state.currentBoard.pieceAt(row, column)?.let { piece ->
                        val typeMark = when (piece.type) {
                            PieceType.KING -> "♔"
                            PieceType.QUEEN -> "♕"
                            PieceType.ROOK -> "♖"
                            PieceType.BISHOP -> "♗"
                            PieceType.KNIGHT -> "♘"
                            PieceType.PAWN -> "♙"
                        }
                        drawCircle(if (piece.color == PieceColor.WHITE) colors.ink else colors.card, cell * .34f, center)
                        label(typeMark, center, cell * .45f)
                    }
                }
                is ColourSortState -> {
                    val tubeCount = state.tubes.size.coerceAtLeast(1)
                    val tubeWidth = cell * .54f
                    val tubeHeight = cell * .86f
                    drawRoundRect(colors.cardEdge, Offset(center.x - tubeWidth / 2, center.y - tubeHeight / 2), Size(tubeWidth, tubeHeight), CornerRadius(tubeWidth / 3), style = Stroke(cell * .07f))
                    state.tubes.getOrNull(index)?.forEachIndexed { layer, colorIndex ->
                        val palette = listOf(colors.accent, Color(0xFFC2622F), Color(0xFF70A98B), Color(0xFF698EB5), Color(0xFF9A78B5))
                        val layerHeight = tubeHeight / 4
                        drawRect(palette[colorIndex.mod(palette.size)], Offset(center.x - tubeWidth / 2, center.y + tubeHeight / 2 - (layer + 1) * layerHeight), Size(tubeWidth, layerHeight))
                    }
                }
                is ArrowsState -> {
                    drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
                    state.arrows[index]?.let { arrow ->
                        val path = Path().apply {
                            moveTo(center.x - arrow.columnDelta * cell * .25f, center.y - arrow.rowDelta * cell * .25f)
                            lineTo(center.x + arrow.columnDelta * cell * .22f, center.y + arrow.rowDelta * cell * .22f)
                            lineTo(center.x + arrow.columnDelta * cell * .06f - arrow.rowDelta * cell * .1f, center.y + arrow.rowDelta * cell * .06f + arrow.columnDelta * cell * .1f)
                            moveTo(center.x + arrow.columnDelta * cell * .22f, center.y + arrow.rowDelta * cell * .22f)
                            lineTo(center.x + arrow.columnDelta * cell * .06f + arrow.rowDelta * cell * .1f, center.y + arrow.rowDelta * cell * .06f - arrow.columnDelta * cell * .1f)
                        }
                        drawPath(path, colors.ink, style = Stroke(cell * .08f))
                    }
                }
                is KlondikeState -> {
                    drawRoundRect(colors.cardEdge, Offset(position.x + cell * .12f, position.y + cell * .08f), Size(cell * .76f, cell * .84f), CornerRadius(cell * .12f), style = Stroke(cell * .06f))
                    val pile = state.tableau.getOrNull(column)
                    if (row == 0 && pile != null) label(pile.size.toString(), center, cell * .28f)
                    if (row == count - 1 && column < state.foundations.size) drawCircle(colors.accent, cell * .2f, center, style = Stroke(cell * .07f))
                }
                else -> if ((row + column) % 2 == 0) drawRect(colors.cardEdge, position, Size(cell, cell))
            }
            if (state !is CrownsState && state !is ColourSortState && state !is TacticsState) {
                drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
            }
        }
        drawRect(colors.ink, style = Stroke(2f))
    }
}
