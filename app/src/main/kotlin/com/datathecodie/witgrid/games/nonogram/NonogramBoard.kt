package com.datathecodie.witgrid.games.nonogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramRules
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.board.MIN_BOARD_CELL_SIZE
import com.datathecodie.witgrid.ui.board.PannableBoardViewport
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun NonogramBoard(
    state: NonogramState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: NonogramBoardOptions = LocalNonogramBoardOptions.current,
    modifier: Modifier = Modifier,
    layout: (@Composable (board: @Composable () -> Unit, controls: @Composable () -> Unit) -> Unit)? = null,
) {
    var mode by remember(state.puzzle.id) { mutableStateOf(NonogramMarkMode.FILL) }
    var activeCell by remember(state.puzzle.id) { mutableStateOf<Cell?>(null) }
    val puzzle = state.puzzle
    val colors = WitgridTheme.colors
    val filledValues = remember(state) {
        state.marks.map { mark -> if (mark == Mark.FILLED) 1 else 0 }
    }
    val currentClues = remember(state) { NonogramRules.cluesForSolution(puzzle.size, filledValues) }
    val completedRows = currentClues.first.mapIndexed { index, clues ->
        clues == puzzle.rowClues[index]
    }
    val completedColumns = currentClues.second.mapIndexed { index, clues ->
        clues == puzzle.columnClues[index]
    }

    val controlsContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = mode == NonogramMarkMode.FILL,
                onClick = { mode = NonogramMarkMode.FILL },
                label = { Text(stringResource(R.string.nonogram_mode_fill)) },
            )
            Spacer(Modifier.width(Spacing.sm))
            FilterChip(
                selected = mode == NonogramMarkMode.CROSS,
                onClick = { mode = NonogramMarkMode.CROSS },
                label = { Text(stringResource(R.string.nonogram_mode_cross)) },
            )
        }

    }
    val boardContent: @Composable () -> Unit = {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val viewportMode = LocalBoardViewportMode.current
            val maxClueCount = maxOf(
                puzzle.rowClues.maxOf { clues -> clues.size },
                puzzle.columnClues.maxOf { clues -> clues.size },
                MIN_CLUE_COUNT,
            )
            val margin = if (viewportMode.embedded) 0.dp else Spacing.screenMargin
            val availableSide = if (maxHeight == Dp.Infinity) maxWidth else minOf(maxWidth, maxHeight)
            val fitCellSize = ((availableSide - margin * 2) / (puzzle.size + maxClueCount))
                .coerceAtLeast(1.dp)
            val cellSize = if (viewportMode.enlarged) {
                maxOf(fitCellSize, MIN_BOARD_CELL_SIZE)
            } else {
                fitCellSize
            }
            val clueBandWidth = cellSize * maxClueCount
            val boardSlotWidth = cellSize * puzzle.size + margin * 2
            val contentWidth = cellSize * (puzzle.size + maxClueCount) + margin * 2
            val contentHeight = cellSize * (puzzle.size + maxClueCount)
            val viewportWidth = minOf(maxWidth, contentWidth)
            val viewportHeight = if (maxHeight == Dp.Infinity) {
                contentHeight
            } else {
                minOf(maxHeight, contentHeight)
            }
            val canPan = viewportMode.enlarged &&
                (contentWidth > viewportWidth || contentHeight > viewportHeight)

            val cluesAndGrid: @Composable () -> Unit = {
                Column(modifier = Modifier.width(contentWidth).height(contentHeight)) {
                    Row(modifier = Modifier.fillMaxWidth().height(clueBandWidth)) {
                        Spacer(Modifier.width(clueBandWidth))
                        ColumnClues(
                            puzzle = puzzle,
                            cellSize = cellSize,
                            maxClueCount = maxClueCount,
                            boardSlotWidth = boardSlotWidth,
                            completedColumns = completedColumns,
                            margin = margin,
                            colors = colors.ink,
                            completedColor = colors.mutedInk.copy(alpha = 0.5f),
                            activeIndex = activeCell?.index?.rem(puzzle.size),
                            activeColor = colors.accentSoft,
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RowClues(
                            puzzle = puzzle,
                            cellSize = cellSize,
                            clueBandWidth = clueBandWidth,
                            completedRows = completedRows,
                            colors = colors.ink,
                            completedColor = colors.mutedInk.copy(alpha = 0.5f),
                            activeIndex = activeCell?.index?.div(puzzle.size),
                            activeColor = colors.accentSoft,
                        )
                        BoardCanvas(
                            gridSize = puzzle.size,
                            cellDescription = { cell -> describeCell(state, cell) },
                            modifier = Modifier.width(boardSlotWidth),
                            solved = NonogramRules.isSolved(puzzle, state),
                            reduceMotion = options.reduceMotion,
                            hapticsEnabled = options.hapticsEnabled,
                            panningEnabled = canPan,
                            drawCell = { cell, bounds ->
                                drawRect(colors.card, bounds.topLeft, bounds.size)
                                val active = activeCell
                                if (active != null && (cell.index / puzzle.size == active.index / puzzle.size ||
                                        cell.index % puzzle.size == active.index % puzzle.size)
                                ) {
                                    drawRect(colors.accentSoft, bounds.topLeft, bounds.size)
                                }
                                drawMark(state.marks[cell.index], bounds, colors.ink, colors.mutedInk)
                                drawGuideLines(cell, puzzle.size, bounds, colors.ink)
                            },
                            onCellTap = { cell ->
                                activeCell = cell
                                NonogramBoardInput.markMoves(
                                    state,
                                    listOf(cell),
                                    mode,
                                    options.autoCrossCompletedLines,
                                ).takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                            },
                            onDragPaint = { path ->
                                activeCell = path.lastOrNull()
                                NonogramBoardInput.markMoves(
                                    state,
                                    path,
                                    mode,
                                    options.autoCrossCompletedLines,
                                ).takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                            },
                            onLongPress = { cell ->
                                activeCell = cell
                                NonogramBoardInput.clearMoves(state, cell)
                                    .takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                            },
                            onUndo = onUndo,
                        )
                    }
                }
            }

            if (canPan) {
                PannableBoardViewport(
                    modifier = Modifier.width(viewportWidth).height(viewportHeight),
                    contentWidth = contentWidth,
                    contentHeight = contentHeight,
                    enabled = true,
                    content = cluesAndGrid,
                )
            } else {
                cluesAndGrid()
            }
        }
    }
    if (layout == null) {
        Column(modifier = modifier.fillMaxWidth()) {
            controlsContent()
            boardContent()
        }
    } else {
        layout(boardContent, controlsContent)
    }
}

@Composable
private fun ColumnClues(
    puzzle: NonogramPuzzle,
    cellSize: Dp,
    maxClueCount: Int,
    boardSlotWidth: Dp,
    completedColumns: List<Boolean>,
    margin: Dp,
    colors: Color,
    completedColor: Color,
    activeIndex: Int?,
    activeColor: Color,
) {
    val clueHeight = cellSize * maxClueCount
    Row(
        modifier = Modifier.width(boardSlotWidth).height(clueHeight).padding(horizontal = margin),
    ) {
        puzzle.columnClues.forEachIndexed { index, clues ->
            val clueDescription = clueDescription(
                R.string.nonogram_column_clues_accessibility,
                index + 1,
                clues,
            )
            Column(
                modifier = Modifier
                    .width(cellSize)
                    .height(clueHeight)
                    .background(if (activeIndex == index) activeColor else Color.Transparent)
                    .semantics(mergeDescendants = true) { contentDescription = clueDescription },
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val labels = clueLabels(clues)
                repeat(maxClueCount - labels.size) { Spacer(Modifier.height(cellSize)) }
                labels.forEach { label ->
                    ClueLabel(
                        label = label,
                        cellSize = cellSize,
                        color = if (completedColumns[index]) completedColor else colors,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowClues(
    puzzle: NonogramPuzzle,
    cellSize: Dp,
    clueBandWidth: Dp,
    completedRows: List<Boolean>,
    colors: Color,
    completedColor: Color,
    activeIndex: Int?,
    activeColor: Color,
) {
    Column(modifier = Modifier.width(clueBandWidth).height(cellSize * puzzle.size)) {
        puzzle.rowClues.forEachIndexed { index, clues ->
            val clueDescription = clueDescription(
                R.string.nonogram_row_clues_accessibility,
                index + 1,
                clues,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellSize)
                    .background(if (activeIndex == index) activeColor else Color.Transparent)
                    .semantics(mergeDescendants = true) { contentDescription = clueDescription },
                horizontalArrangement = Arrangement.End,
            ) {
                clueLabels(clues).forEach { label ->
                    ClueLabel(
                        label = label,
                        cellSize = cellSize,
                        color = if (completedRows[index]) completedColor else colors,
                    )
                }
            }
        }
    }
}

@Composable
private fun clueDescription(resourceId: Int, line: Int, clues: List<Int>): String {
    val clueText = clues.joinToString(", ").ifEmpty {
        stringResource(R.string.nonogram_no_run_clues)
    }
    return stringResource(resourceId, line, clueText)
}

@Composable
private fun clueLabels(clues: List<Int>): List<String> = if (clues.isEmpty()) {
    listOf(stringResource(R.string.nonogram_no_run_marker))
} else {
    clues.map(Int::toString)
}

@Composable
private fun ClueLabel(label: String, cellSize: Dp, color: Color) {
    Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = clueStyle(cellSize, color),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun clueStyle(cellSize: Dp, color: Color): TextStyle {
    // Headers share the grid's fixed cells, including when system font scaling is large.
    val maximumFontSize = with(LocalDensity.current) { (cellSize * CLUE_FONT_SIZE_FRACTION).toSp() }
    val fontSize = minOf(NumberStyles.clue.fontSize.value, maximumFontSize.value).sp
    return NumberStyles.clue.copy(
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * CLUE_LINE_HEIGHT_FRACTION,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}

@Composable
private fun describeCell(state: NonogramState, cell: Cell): String {
    val markDescription = stringResource(
        when (state.marks[cell.index]) {
            Mark.EMPTY -> R.string.nonogram_mark_empty
            Mark.FILLED -> R.string.nonogram_mark_filled
            Mark.CROSSED -> R.string.nonogram_mark_crossed
        },
    )
    return stringResource(
        R.string.nonogram_cell_accessibility_label,
        cell.index / state.puzzle.size + 1,
        cell.index % state.puzzle.size + 1,
        markDescription,
    )
}

private fun DrawScope.drawMark(mark: Mark, bounds: Rect, fillColor: Color, crossColor: Color) {
    when (mark) {
        Mark.EMPTY -> Unit
        Mark.FILLED -> drawRect(fillColor, topLeft = bounds.topLeft, size = bounds.size)
        Mark.CROSSED -> drawCross(bounds, crossColor)
    }
}

private fun DrawScope.drawCross(bounds: Rect, color: Color) {
    val radius = minOf(bounds.width, bounds.height) * CROSS_RADIUS_FRACTION
    val strokeWidth = minOf(bounds.width, bounds.height) * CROSS_STROKE_FRACTION
    drawLine(
        color,
        Offset(bounds.center.x - radius, bounds.center.y - radius),
        Offset(bounds.center.x + radius, bounds.center.y + radius),
        strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(bounds.center.x + radius, bounds.center.y - radius),
        Offset(bounds.center.x - radius, bounds.center.y + radius),
        strokeWidth,
        cap = StrokeCap.Round,
    )
}

private const val CLUE_FONT_SIZE_FRACTION = 0.72f
private const val CLUE_LINE_HEIGHT_FRACTION = 1.1f
private const val CROSS_RADIUS_FRACTION = 0.24f
private const val CROSS_STROKE_FRACTION = 0.075f
private const val MIN_CLUE_COUNT = 1

private fun DrawScope.drawGuideLines(cell: Cell, gridSize: Int, bounds: Rect, color: Color) {
    val row = cell.index / gridSize
    val column = cell.index % gridSize
    val width = 2.dp.toPx()
    if (column % 5 == 0) drawLine(color, bounds.topLeft, bounds.bottomLeft, width)
    if (row % 5 == 0) drawLine(color, bounds.topLeft, bounds.topRight, width)
    if (column == gridSize - 1) drawLine(color, bounds.topRight, bounds.bottomRight, width)
    if (row == gridSize - 1) drawLine(color, bounds.bottomLeft, bounds.bottomRight, width)
}
