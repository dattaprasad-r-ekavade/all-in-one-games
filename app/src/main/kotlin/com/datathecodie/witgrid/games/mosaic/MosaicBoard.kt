package com.datathecodie.witgrid.games.mosaic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicRules
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun MosaicBoard(
    state: MosaicState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: MosaicBoardOptions = LocalMosaicBoardOptions.current,
    modifier: Modifier = Modifier,
    layout: (@Composable (board: @Composable () -> Unit, controls: @Composable () -> Unit) -> Unit)? = null,
) {
    var mode by remember(state.puzzle.id) { mutableStateOf(MosaicMarkMode.FILL) }
    val puzzle = state.puzzle
    val colors = WitgridTheme.colors
    val errorBackground = MaterialTheme.colorScheme.error
    val errorText = MaterialTheme.colorScheme.onError
    val textMeasurer = rememberTextMeasurer()
    val digitStyle = NumberStyles.cell
    val haptics = rememberBoardHaptics(options.hapticsEnabled)

    val clueStatuses = remember(state) {
        Array(puzzle.grid.cellCount) { index ->
            val clue = puzzle.clues[index]
            if (clue != null) {
                MosaicBoardInput.clueStatus(puzzle.grid, clue, Cell(index), state.marks)
            } else {
                null
            }
        }
    }

    val controlsContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = mode == MosaicMarkMode.FILL,
                onClick = { mode = MosaicMarkMode.FILL },
                label = { Text(stringResource(R.string.nonogram_mode_fill)) },
            )
            Spacer(Modifier.width(Spacing.sm))
            FilterChip(
                selected = mode == MosaicMarkMode.CROSS,
                onClick = { mode = MosaicMarkMode.CROSS },
                label = { Text(stringResource(R.string.nonogram_mode_cross)) },
            )
        }
    }

    val boardContent: @Composable () -> Unit = {
        BoardCanvas(
            gridSize = puzzle.size,
            cellDescription = { cell -> describeMosaicCell(state, cell, clueStatuses[cell.index]) },
            solved = MosaicRules.isSolved(puzzle, state),
            reduceMotion = options.reduceMotion,
            hapticsEnabled = options.hapticsEnabled,
            dragPaintEnabled = true,
            drawCell = { cell, bounds ->
                val mark = state.marks[cell.index]
                val isFilled = mark == Mark.FILLED
                val background = if (isFilled) colors.ink else colors.card
                val foreground = if (isFilled) colors.card else colors.ink
                drawRect(background, topLeft = bounds.topLeft, size = bounds.size)

                if (mark == Mark.CROSSED) {
                    drawCross(bounds, colors.mutedInk.copy(alpha = 0.45f))
                }

                val clue = puzzle.clues[cell.index]
                if (clue != null) {
                    val status = clueStatuses[cell.index] ?: MosaicClueStatus.NORMAL
                    val textColor = when (status) {
                        MosaicClueStatus.ERROR -> errorText
                        MosaicClueStatus.SATISFIED -> {
                            if (isFilled) colors.card.copy(alpha = 0.5f)
                            else colors.mutedInk.copy(alpha = 0.45f)
                        }
                        MosaicClueStatus.NORMAL -> foreground
                    }
                    val textLayout = textMeasurer.measure(
                        AnnotatedString(clue.toString()),
                        style = digitStyle.copy(color = textColor),
                    )
                    if (status == MosaicClueStatus.ERROR) {
                        drawCircle(
                            color = errorBackground,
                            radius = minOf(bounds.width, bounds.height) * ERROR_BADGE_RADIUS_FRACTION,
                            center = bounds.center,
                        )
                    }
                    drawCenteredLabel(textLayout, bounds)
                }

                drawGuideLines(cell, puzzle.size, bounds, foreground)
            },
            onCellTap = { cell ->
                val moves = MosaicBoardInput.markMoves(state, listOf(cell), mode)
                if (moves.isNotEmpty()) {
                    haptics.placement()
                    onMoves(moves)
                }
            },
            onDragPaint = { path ->
                val moves = MosaicBoardInput.markMoves(state, path, mode)
                if (moves.isNotEmpty()) {
                    haptics.placement()
                    onMoves(moves)
                }
            },
            onLongPress = { cell ->
                val moves = MosaicBoardInput.clearMoves(state, cell)
                if (moves.isNotEmpty()) {
                    haptics.placement()
                    onMoves(moves)
                }
            },
            onUndo = onUndo,
        )
    }

    if (layout == null) {
        MosaicBoardWithControls(modifier, boardContent, controlsContent)
    } else {
        layout(boardContent, controlsContent)
    }
}

@Composable
private fun describeMosaicCell(state: MosaicState, cell: Cell, status: MosaicClueStatus?): String {
    val row = cell.index / state.puzzle.size + 1
    val column = cell.index % state.puzzle.size + 1
    val markText = stringResource(
        when (state.marks[cell.index]) {
            Mark.EMPTY -> R.string.nonogram_mark_empty
            Mark.FILLED -> R.string.nonogram_mark_filled
            Mark.CROSSED -> R.string.nonogram_mark_crossed
        },
    )
    val clue = state.puzzle.clues[cell.index]
    return if (clue != null) {
        stringResource(
            when (status) {
                MosaicClueStatus.SATISFIED -> R.string.mosaic_clue_satisfied_accessibility_label
                MosaicClueStatus.ERROR -> R.string.mosaic_clue_error_accessibility_label
                MosaicClueStatus.NORMAL, null -> R.string.mosaic_clue_accessibility_label
            },
            row,
            column,
            markText,
            clue,
        )
    } else {
        stringResource(
            R.string.nonogram_cell_accessibility_label,
            row,
            column,
            markText,
        )
    }
}

private fun DrawScope.drawCross(bounds: Rect, color: Color) {
    val radius = minOf(bounds.width, bounds.height) * CROSS_RADIUS_FRACTION
    val strokeWidth = minOf(bounds.width, bounds.height) * CROSS_STROKE_FRACTION
    drawLine(
        color = color,
        start = Offset(bounds.center.x - radius, bounds.center.y - radius),
        end = Offset(bounds.center.x + radius, bounds.center.y + radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(bounds.center.x + radius, bounds.center.y - radius),
        end = Offset(bounds.center.x - radius, bounds.center.y + radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawGuideLines(cell: Cell, gridSize: Int, bounds: Rect, color: Color) {
    val row = cell.index / gridSize
    val column = cell.index % gridSize
    val width = minOf(1.5.dp.toPx(), minOf(bounds.width, bounds.height) / 4f)
    // Keep each half of a guide inside its cell so the next cell's background cannot cover it.
    val vertical = Size(width, bounds.height)
    val horizontal = Size(bounds.width, width)
    if (column % 5 == 0) drawRect(color, bounds.topLeft, vertical)
    if (row % 5 == 0) drawRect(color, bounds.topLeft, horizontal)
    if (column % 5 == 4 || column == gridSize - 1) {
        drawRect(color, Offset(bounds.right - width, bounds.top), vertical)
    }
    if (row % 5 == 4 || row == gridSize - 1) {
        drawRect(color, Offset(bounds.left, bounds.bottom - width), horizontal)
    }
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

private const val CROSS_RADIUS_FRACTION = 0.24f
private const val CROSS_STROKE_FRACTION = 0.075f
private const val ERROR_BADGE_RADIUS_FRACTION = 0.40f
private const val LABEL_SIZE_FRACTION = 0.70f
