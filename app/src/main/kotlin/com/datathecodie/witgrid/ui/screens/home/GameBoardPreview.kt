package com.datathecodie.witgrid.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.datathecodie.witgrid.engine.tactics.TacticsState
import com.datathecodie.witgrid.ui.preview.GameMotif
import com.datathecodie.witgrid.ui.preview.GameMotifKind
import com.datathecodie.witgrid.ui.theme.WitgridColors
import com.datathecodie.witgrid.ui.theme.WitgridTheme

/** A decorative board motif; game controls and accessibility belong to the playable board. */
@Composable
internal fun GameBoardPreview(gameId: String, modifier: Modifier = Modifier, boardState: Any? = null) {
    val colors = WitgridTheme.colors
    Canvas(modifier.size(64.dp)) {
        val gap = size.minDimension / 38
        val regions = listOf(Color(0xFFCBC0EE), Color(0xFFA7DCC2), Color(0xFFA8CFEA), Color(0xFFF1C798))
        val palette = listOf(colors.accent, Color(0xFFC2622F), Color(0xFF70A98B), Color(0xFF698EB5), Color(0xFF9A78B5))
        drawRoundRect(colors.paper, cornerRadius = CornerRadius(8.dp.toPx()))
        when (boardState) {
            is ColourSortState -> drawColourSortContinue(boardState, gap, palette, colors.cardEdge)
            is KlondikeState -> drawKlondikeContinue(boardState, gap, colors.cardEdge, colors.accent)
            else -> {
                val count = continuePreviewCellCount(boardState)
                val cell = (size.minDimension - gap * (count + 1)) / count
                val cellSize = Size(cell, cell)
                repeat(count * count) { index ->
                    val row = index / count
                    val column = index % count
                    val position = Offset(gap + column * (cell + gap), gap + row * (cell + gap))
                    if (boardState != null) {
                        drawContinueCell(boardState, index, row, column, count, position, cell, cellSize, gap, regions, colors.ink, colors.accent, colors.cardEdge, colors.paper)
                    } else {
                        drawMotifCell(GameMotif.kindFor(gameId), row, column, count, position, cell, cellSize, gap, regions, colors)
                    }
                }
            }
        }
        drawRoundRect(colors.ink, cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    }
}

internal fun continuePreviewCellCount(boardState: Any?): Int = when (boardState) {
    is CrownsState -> boardState.puzzle.size
    is BinaryState -> boardState.puzzle.size
    is NonogramState -> boardState.puzzle.size
    is MiniSudokuState -> 6
    is SudokuState, is KillerSudokuState -> 9
    is FutoshikiState -> boardState.puzzle.size
    is SkyscrapersState -> boardState.puzzle.size
    is PathState -> boardState.puzzle.size
    is MosaicState -> boardState.puzzle.size
    is ArrowsState -> boardState.puzzle.size
    is TacticsState -> 8
    else -> 5
}

private fun DrawScope.drawContinueCell(
    boardState: Any,
    index: Int,
    row: Int,
    column: Int,
    count: Int,
    position: Offset,
    cell: Float,
    cellSize: Size,
    gap: Float,
    regions: List<Color>,
    ink: Color,
    accent: Color,
    cardEdge: Color,
    paper: Color,
) {
    val center = position + Offset(cell / 2, cell / 2)
    when (boardState) {
        is CrownsState -> {
            drawRoundRect(regions[boardState.puzzle.regions[index] % regions.size], position, cellSize, CornerRadius(gap))
            if (boardState.marks[index] == Mark.FILLED) drawCircle(ink, cell / 4, center)
        }
        is BinaryState -> when (boardState.symbols[index]) {
            BinarySymbol.A -> drawCircle(accent, cell / 2.4f, center)
            BinarySymbol.B -> drawCircle(Color(0xFFC2622F), cell / 2.7f, center, style = Stroke(cell / 5))
            else -> Unit
        }
        is NonogramState -> if (boardState.marks[index] == Mark.FILLED) drawRect(ink, position, cellSize)
        is MiniSudokuState -> if (boardState.digits[index] != null) drawRoundRect(accent, position, cellSize, CornerRadius(gap))
        is SudokuState -> if (boardState.digits[index] != null) drawCircle(ink, cell * .22f, center)
        is KillerSudokuState -> {
            drawRoundRect(cardEdge, position, cellSize, CornerRadius(gap), style = Stroke(cell * .08f))
            if (boardState.digits[index] != null) drawCircle(ink, cell * .18f, center)
            if (boardState.puzzle.cages.any { it.cells.firstOrNull()?.index == index }) {
                drawCircle(accent, cell * .1f, position + Offset(cell * .22f, cell * .22f))
            }
        }
        is FutoshikiState -> {
            drawRoundRect(cardEdge, position, cellSize, CornerRadius(gap))
            if (boardState.digits[index] != null) drawCircle(accent, cell * .22f, center)
            val rightSign = boardState.puzzle.horizontalSigns.getOrNull(row * (count - 1) + column)
            if (column < count - 1 && rightSign != null && rightSign != FutoshikiSign.NONE) {
                drawLine(ink, position + Offset(cell * .82f, cell * .35f), position + Offset(cell * .82f, cell * .65f), cell * .1f)
            }
        }
        is SkyscrapersState -> {
            val digit = boardState.digits[index]
            val height = cell * (0.14f + (digit ?: 0) * 0.12f)
            drawRect(accent, Offset(position.x + cell * .22f, position.y + cell - height), Size(cell * .56f, height))
        }
        is PathState -> {
            when {
                boardState.puzzle.walls[index] -> drawRect(ink, position, cellSize)
                boardState.digits[index] != null -> drawCircle(accent, cell * .28f, center)
                else -> drawRoundRect(cardEdge, position, cellSize, CornerRadius(gap), style = Stroke(cell * .06f))
            }
        }
        is MosaicState -> when (boardState.marks[index]) {
            Mark.FILLED -> drawRect(ink, position, cellSize)
            Mark.CROSSED -> {
                drawLine(accent, position + Offset(cell * .2f, cell * .2f), position + Offset(cell * .8f, cell * .8f), cell * .09f)
                drawLine(accent, position + Offset(cell * .8f, cell * .2f), position + Offset(cell * .2f, cell * .8f), cell * .09f)
            }
            Mark.EMPTY -> if (boardState.puzzle.clues[index] != null) {
                drawCircle(cardEdge, cell * .22f, center, style = Stroke(cell * .08f))
            }
        }
        is TacticsState -> {
            drawRect(if ((row + column) % 2 == 0) ink else cardEdge, position, cellSize)
            boardState.currentBoard.pieceAt(row, column)?.let { piece ->
                if (piece.color == PieceColor.WHITE) drawCircle(paper, cell * .28f, center)
                else drawCircle(accent, cell * .28f, center)
            }
        }
        is ArrowsState -> {
            drawRoundRect(cardEdge, position, cellSize, CornerRadius(gap), style = Stroke(cell * .06f))
            boardState.arrows[index]?.let { arrow ->
                val path = Path().apply {
                    moveTo(center.x - arrow.columnDelta * cell * .25f, center.y - arrow.rowDelta * cell * .25f)
                    lineTo(center.x + arrow.columnDelta * cell * .22f, center.y + arrow.rowDelta * cell * .22f)
                }
                drawPath(path, ink, style = Stroke(cell * .1f))
            }
        }
    }
}

private fun DrawScope.drawMotifCell(
    kind: GameMotifKind,
    row: Int,
    column: Int,
    count: Int,
    position: Offset,
    cell: Float,
    cellSize: Size,
    gap: Float,
    regions: List<Color>,
    colors: WitgridColors,
) {
    when (kind) {
        GameMotifKind.CROWNS -> drawRoundRect(regions[if (column > 2) 1 else if (row < 2) 0 else if (column < 2) 2 else 3], position, cellSize, CornerRadius(gap))
        GameMotifKind.BINARY -> if ((row + column) % 3 != 1) {
            if ((row + column) % 2 == 0) drawCircle(colors.accent, cell / 2.3f, position + Offset(cell / 2, cell / 2))
            else drawCircle(Color(0xFFC2622F), cell / 2.7f, position + Offset(cell / 2, cell / 2), style = Stroke(cell / 5))
        }
        GameMotifKind.NONOGRAM -> if ((row * 3 + column) % 4 != 0) drawRect(colors.ink, position, cellSize)
        GameMotifKind.MOSAIC -> {
            if ((row + column * 2) % 3 == 0) drawRoundRect(colors.ink, position, cellSize, CornerRadius(gap))
            else if ((row * 2 + column) % 3 == 0) {
                drawLine(colors.accent, position + Offset(cell * .2f, cell * .2f), position + Offset(cell * .8f, cell * .8f), cell * .09f)
                drawLine(colors.accent, position + Offset(cell * .8f, cell * .2f), position + Offset(cell * .2f, cell * .8f), cell * .09f)
            }
        }
        GameMotifKind.MINI_SUDOKU -> {
            drawRoundRect(colors.cardEdge, position, cellSize, CornerRadius(gap))
            if ((row * 2 + column) % 3 == 0) drawCircle(colors.accent, cell / 3, position + Offset(cell / 2, cell / 2))
        }
        GameMotifKind.SUDOKU -> {
            if (column % 3 == 0) drawLine(colors.accent, position, position + Offset(0f, cell), cell * .12f)
            if (row % 3 == 0) drawLine(colors.accent, position, position + Offset(cell, 0f), cell * .12f)
            if ((row * 3 + column) % 4 == 0) drawCircle(colors.ink, cell * .2f, position + Offset(cell / 2, cell / 2))
        }
        GameMotifKind.KILLER_SUDOKU -> {
            drawRoundRect(colors.cardEdge, position, cellSize, CornerRadius(gap), style = Stroke(cell * .08f))
            if ((row + column) % 2 == 0) drawCircle(colors.accent, cell * .16f, position + Offset(cell * .25f, cell * .25f))
            if ((row + column * 2) % 3 == 0) drawLine(colors.ink, position + Offset(cell * .5f, 0f), position + Offset(cell, cell * .5f), cell * .06f)
        }
        GameMotifKind.SKYSCRAPERS -> {
            val height = cell * (0.25f + ((row + column * 2) % 4) * 0.16f)
            drawRect(colors.accent, Offset(position.x + cell * 0.22f, position.y + cell - height), Size(cell * 0.56f, height))
        }
        GameMotifKind.FUTOSHIKI -> {
            drawRoundRect(colors.cardEdge, position, cellSize, CornerRadius(gap))
            if ((row + column) % 2 == 0) drawLine(colors.accent, position + Offset(cell * .3f, cell * .7f), position + Offset(cell * .7f, cell * .3f), cell * .12f)
        }
        GameMotifKind.PATH -> if ((row == column) || (row + column == count - 1)) drawCircle(colors.accent, cell * .28f, position + Offset(cell / 2, cell / 2))
        GameMotifKind.TACTICS -> {
            drawRect(if ((row + column) % 2 == 0) colors.ink else colors.cardEdge, position, cellSize)
            if ((row * 3 + column) % 4 == 0) drawCircle(colors.paper, cell * .25f, position + Offset(cell / 2, cell / 2))
        }
        GameMotifKind.COLOUR_SORT -> {
            drawRoundRect(colors.cardEdge, Offset(position.x + cell * .25f, position.y + cell * .15f), Size(cell * .5f, cell * .7f), CornerRadius(cell * .2f), style = Stroke(cell * .08f))
            if ((row + column) % 2 == 0) drawRect(colors.accent, Offset(position.x + cell * .3f, position.y + cell * .55f), Size(cell * .4f, cell * .22f))
        }
        GameMotifKind.ARROWS -> {
            drawRoundRect(colors.cardEdge, position, cellSize, CornerRadius(gap))
            if ((row + column) % 2 == 0) drawLine(colors.accent, position + Offset(cell * .25f, cell * .5f), position + Offset(cell * .75f, cell * .5f), cell * .12f)
        }
        GameMotifKind.KLONDIKE -> {
            drawRoundRect(colors.cardEdge, Offset(position.x + cell * .12f, position.y + cell * .12f), Size(cell * .76f, cell * .76f), CornerRadius(cell * .18f))
            if ((row + column) % 2 == 0) drawCircle(colors.accent, cell * .2f, position + Offset(cell / 2, cell / 2))
        }
        GameMotifKind.UNKNOWN -> if ((row + column) % 2 == 0) drawRoundRect(colors.cardEdge, position, cellSize, CornerRadius(gap))
    }
}

private fun DrawScope.drawColourSortContinue(
    boardState: ColourSortState,
    gap: Float,
    palette: List<Color>,
    cardEdge: Color,
) {
    val tubeCount = boardState.tubes.size.coerceAtLeast(1)
    val tubeWidth = (size.minDimension - gap * (tubeCount + 1)) / tubeCount
    val tubeHeight = size.minDimension * .78f
    val top = (size.minDimension - tubeHeight) / 2
    val capacity = boardState.tubes.maxOf { it.size }.coerceAtLeast(4)
    boardState.tubes.forEachIndexed { index, stack ->
        val x = gap + index * (tubeWidth + gap)
        drawRoundRect(cardEdge, Offset(x, top), Size(tubeWidth, tubeHeight), CornerRadius(tubeWidth / 3), style = Stroke(tubeWidth * .12f))
        val layerHeight = tubeHeight / capacity
        stack.forEachIndexed { layer, colorIndex ->
            drawRect(
                palette[colorIndex.mod(palette.size)],
                Offset(x, top + tubeHeight - (layer + 1) * layerHeight),
                Size(tubeWidth, layerHeight),
            )
        }
    }
}

private fun DrawScope.drawKlondikeContinue(
    boardState: KlondikeState,
    gap: Float,
    cardEdge: Color,
    accent: Color,
) {
    val columns = boardState.tableau.size.coerceAtLeast(1)
    val columnWidth = (size.minDimension - gap * (columns + 1)) / columns
    val cardHeight = size.minDimension * .22f
    boardState.tableau.forEachIndexed { index, pile ->
        val x = gap + index * (columnWidth + gap)
        if (pile.isEmpty()) {
            drawRoundRect(cardEdge, Offset(x, gap * 2), Size(columnWidth, cardHeight), CornerRadius(columnWidth * .2f), style = Stroke(columnWidth * .1f))
        } else {
            pile.take(5).forEachIndexed { layer, card ->
                val y = gap * 2 + layer * (cardHeight * .28f)
                drawRoundRect(if (card.faceUp) accent else cardEdge, Offset(x, y), Size(columnWidth, cardHeight), CornerRadius(columnWidth * .18f))
            }
        }
    }
}
