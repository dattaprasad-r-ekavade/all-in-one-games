package com.datathecodie.witgrid.games.path

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.path.PathRules
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun PathBoard(
    state: PathState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: PathBoardOptions = LocalPathBoardOptions.current,
    modifier: Modifier = Modifier,
) {
    val puzzle = state.puzzle
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val givenStyle = NumberStyles.cell.copy(color = colors.ink)
    val playerStyle = NumberStyles.cell.copy(color = colors.accent)
    val haptics = rememberBoardHaptics(options.hapticsEnabled)

    BoardCanvas(
        gridSize = puzzle.size,
        cellDescription = { cell -> describePathCell(state, cell) },
        modifier = modifier,
        solved = PathRules.isSolved(puzzle, state),
        reduceMotion = options.reduceMotion,
        hapticsEnabled = options.hapticsEnabled,
        drawCell = { cell, bounds ->
            drawRect(colors.card, topLeft = bounds.topLeft, size = bounds.size)
            val isWall = puzzle.walls[cell.index]
            val isEndpoint = puzzle.endpoints[cell.index] != null
            if (isWall || isEndpoint) {
                drawRect(
                    color = colors.ink.copy(alpha = GIVEN_CELL_ALPHA),
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                )
            }
            if (isWall) {
                drawWallMark(bounds, colors.ink)
            } else {
                val digit = state.digits[cell.index]
                if (digit != null) {
                    val style = if (isEndpoint) givenStyle else playerStyle
                    val label = pairLabel(digit)
                    val text = textMeasurer.measure(AnnotatedString(label), style = style)
                    drawCenteredLabel(text, bounds)
                }
            }
        },
        onCellTap = {},
        onDragPaint = { path ->
            val moves = PathBoardInput.dragMoves(state, path)
            if (moves.isNotEmpty()) {
                haptics.placement()
                onMoves(moves)
            }
        },
        onLongPress = { cell ->
            PathBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
        },
        onUndo = onUndo,
    )
}

@Composable
private fun describePathCell(state: PathState, cell: Cell): String {
    val row = cell.index / state.puzzle.size + 1
    val column = cell.index % state.puzzle.size + 1
    return stringResource(R.string.board_cell_accessibility_label, row, column)
}

private fun DrawScope.drawWallMark(bounds: Rect, ink: Color) {
    val inset = minOf(bounds.width, bounds.height) * WALL_INSET_FRACTION
    val stroke = minOf(bounds.width, bounds.height) * WALL_STROKE_FRACTION
    drawRect(
        color = ink.copy(alpha = WALL_FILL_ALPHA),
        topLeft = Offset(bounds.left + inset, bounds.top + inset),
        size = Size(
            width = bounds.width - inset * 2f,
            height = bounds.height - inset * 2f,
        ),
    )
    drawLine(
        color = ink.copy(alpha = WALL_LINE_ALPHA),
        start = Offset(bounds.left + inset, bounds.top + inset),
        end = Offset(bounds.right - inset, bounds.bottom - inset),
        strokeWidth = stroke,
    )
    drawLine(
        color = ink.copy(alpha = WALL_LINE_ALPHA),
        start = Offset(bounds.right - inset, bounds.top + inset),
        end = Offset(bounds.left + inset, bounds.bottom - inset),
        strokeWidth = stroke,
    )
}

private fun DrawScope.drawCenteredLabel(
    text: TextLayoutResult,
    bounds: Rect,
) {
    val labelScale = minOf(
        1f,
        bounds.width * LABEL_SIZE_FRACTION / text.size.width.coerceAtLeast(1),
        bounds.height * LABEL_SIZE_FRACTION / text.size.height.coerceAtLeast(1),
    )
    scale(labelScale, pivot = bounds.center) {
        drawText(
            text,
            topLeft = Offset(
                x = bounds.center.x - text.size.width / 2f,
                y = bounds.center.y - text.size.height / 2f,
            ),
        )
    }
}

private fun pairLabel(pair: Int): String = when (pair) {
    in 1..9 -> pair.toString()
    in 10..35 -> ('A' + (pair - 10)).toString()
    else -> pair.toString()
}

private const val GIVEN_CELL_ALPHA = 0.06f
private const val WALL_INSET_FRACTION = 0.18f
private const val WALL_STROKE_FRACTION = 0.06f
private const val WALL_FILL_ALPHA = 0.12f
private const val WALL_LINE_ALPHA = 0.35f
private const val LABEL_SIZE_FRACTION = 0.78f