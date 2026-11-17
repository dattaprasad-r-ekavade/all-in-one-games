package com.datathecodie.witgrid.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

typealias DrawBoardCell = DrawScope.(cell: Cell, bounds: Rect) -> Unit

@Composable
fun BoardCanvas(
    gridSize: Int,
    cellDescription: @Composable (Cell) -> String,
    modifier: Modifier = Modifier,
    solved: Boolean = false,
    reduceMotion: Boolean = false,
    hapticsEnabled: Boolean = true,
    dragPaintEnabled: Boolean = true,
    panningEnabled: Boolean = false,
    drawCell: DrawBoardCell = { _, _ -> },
    onCellTap: (Cell) -> Unit,
    onDragPaint: (List<Cell>) -> Unit,
    onLongPress: (Cell) -> Unit,
    onUndo: () -> Unit,
) {
    require(gridSize in 1..MAX_RENDERED_GRID_SIZE) {
        "Board size must be between 1 and $MAX_RENDERED_GRID_SIZE"
    }

    val colors = WitgridTheme.colors
    val highlightedCells = LocalBoardHighlightCells.current
    val viewportMode = LocalBoardViewportMode.current
    val haptics = rememberBoardHaptics(hapticsEnabled)
    var dragPreviewCells by remember { mutableStateOf(emptySet<Cell>()) }
    val latestOnCellTap = rememberUpdatedState(onCellTap)
    val latestOnLongPress = rememberUpdatedState(onLongPress)
    val latestOnUndo = rememberUpdatedState(onUndo)
    val highlightedCellDescription: @Composable (Cell) -> String = { cell ->
        val description = cellDescription(cell)
        if (cell in highlightedCells) {
            stringResource(R.string.board_highlighted_cell_accessibility, description)
        } else {
            description
        }
    }
    val latestCellDescription = rememberUpdatedState(highlightedCellDescription)
    val clearActionLabel = stringResource(R.string.board_cell_clear_action)
    val undoActionLabel = stringResource(R.string.board_undo_action)
    val waveProgress = remember { Animatable(1f) }

    LaunchedEffect(solved, reduceMotion) {
        if (solved && !reduceMotion) {
            waveProgress.snapTo(0f)
            waveProgress.animateTo(1f, animationSpec = tween(durationMillis = WAVE_DURATION_MILLIS))
        } else {
            waveProgress.snapTo(1f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenMargin)
            .aspectRatio(1f),
    ) {
        val viewportSide = maxWidth
        val contentSide = if (viewportMode.enlarged) {
            maxOf(viewportSide, MIN_BOARD_CELL_SIZE * gridSize)
        } else {
            viewportSide
        }
        val panEnabled = panningEnabled || (viewportMode.enlarged && contentSide > viewportSide)
        val activeGestureModifier = Modifier.boardGestureInput(
            gridSize = gridSize,
            haptics = haptics,
            dragPaintEnabled = dragPaintEnabled && !panEnabled,
            panEnabled = panEnabled,
            onCellTap = onCellTap,
            onDragPaint = onDragPaint,
            onDragPreview = { cells -> dragPreviewCells = cells.toSet() },
            onLongPress = onLongPress,
            onUndo = onUndo,
        )

        PannableBoardViewport(
            modifier = Modifier.fillMaxSize(),
            contentWidth = contentSide,
            contentHeight = contentSide,
            enabled = panEnabled,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize().then(activeGestureModifier)) {
                    drawRect(colors.paper)
                    val cellWidth = size.width / gridSize
                    val cellHeight = size.height / gridSize
                    for (row in 0 until gridSize) {
                        for (column in 0 until gridSize) {
                            val cell = Cell(row * gridSize + column)
                            val bounds = Rect(
                                left = column * cellWidth,
                                top = row * cellHeight,
                                right = (column + 1) * cellWidth,
                                bottom = (row + 1) * cellHeight,
                            )
                            drawCell(cell, bounds)
                            if (cell in highlightedCells) {
                                drawRect(
                                    color = colors.accent.copy(alpha = HINT_HIGHLIGHT_ALPHA),
                                    topLeft = bounds.topLeft,
                                    size = bounds.size,
                                )
                                val inset = HINT_HIGHLIGHT_STROKE_DP.dp.toPx() / 2f
                                drawRect(
                                    color = colors.accent,
                                    topLeft = bounds.topLeft + Offset(inset, inset),
                                    size = Size(bounds.width - 2f * inset, bounds.height - 2f * inset),
                                    style = Stroke(width = 2f * inset),
                                )
                            }
                            if (cell in dragPreviewCells) {
                                drawRect(
                                    color = colors.accent.copy(alpha = DRAG_PREVIEW_ALPHA),
                                    topLeft = bounds.topLeft,
                                    size = bounds.size,
                                )
                            }
                        }
                    }

                    val lineWidth = GRID_LINE_WIDTH_DP.dp.toPx()
                    val lineColor = colors.ink.copy(alpha = GRID_LINE_ALPHA)
                    for (line in 0..gridSize) {
                        val x = line * cellWidth
                        val y = line * cellHeight
                        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), lineWidth)
                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), lineWidth)
                    }

                    if (solved && !reduceMotion && waveProgress.value < 1f) {
                        val waveX = waveProgress.value * size.width
                        val waveWidth = WAVE_WIDTH_DP.dp.toPx()
                        drawRect(
                            color = colors.accent.copy(alpha = WAVE_ALPHA),
                            topLeft = Offset((waveX - waveWidth / 2f).coerceAtLeast(0f), 0f),
                            size = Size(width = waveWidth, height = size.height),
                        )
                        drawLine(
                            color = colors.accent,
                            start = Offset(waveX, 0f),
                            end = Offset(waveX, size.height),
                            strokeWidth = WAVE_EDGE_WIDTH_DP.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                AccessibilityCells(
                    gridSize = gridSize,
                    cellDescription = latestCellDescription,
                    onCellTap = latestOnCellTap,
                    onLongPress = latestOnLongPress,
                    onUndo = latestOnUndo,
                    haptics = haptics,
                    clearActionLabel = clearActionLabel,
                    undoActionLabel = undoActionLabel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private const val GRID_LINE_WIDTH_DP = 1f
private const val GRID_LINE_ALPHA = 0.45f
private const val DRAG_PREVIEW_ALPHA = 0.15f
private const val MAX_RENDERED_GRID_SIZE = 32
private const val WAVE_ALPHA = 0.18f
private const val WAVE_DURATION_MILLIS = 850
private const val WAVE_EDGE_WIDTH_DP = 4f
private const val WAVE_WIDTH_DP = 28f
private const val HINT_HIGHLIGHT_ALPHA = 0.28f
private const val HINT_HIGHLIGHT_STROKE_DP = 3f
