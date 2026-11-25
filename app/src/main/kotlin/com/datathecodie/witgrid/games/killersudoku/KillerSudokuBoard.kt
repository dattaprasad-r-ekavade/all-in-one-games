package com.datathecodie.witgrid.games.killersudoku

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuRules
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.SudokuPlayActions
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun KillerSudokuBoard(
    state: KillerSudokuState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: KillerSudokuBoardOptions = LocalKillerSudokuBoardOptions.current,
    modifier: Modifier = Modifier,
    playLayout: (@Composable (board: @Composable () -> Unit, actions: SudokuPlayActions) -> Unit)? = null,
    layout: (@Composable (board: @Composable () -> Unit, controls: @Composable () -> Unit) -> Unit)? = null,
) {
    var selectedCellIndex by remember(state.puzzle.id) { mutableStateOf<Int?>(null) }
    var notesMode by remember(state.puzzle.id) { mutableStateOf(false) }
    val puzzle = state.puzzle
    val selectedCell = selectedCellIndex?.let(::Cell)
    val selectedCellIsEditable = selectedCellIndex?.let { index -> puzzle.givens[index] == null } == true
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val digitStyle = NumberStyles.cell.copy(color = colors.accent)
    val givenStyle = NumberStyles.cell.copy(color = colors.ink)
    val noteStyle = NumberStyles.clue.copy(color = colors.mutedInk, fontSize = 11.sp)
    val cageSumStyle = NumberStyles.clue.copy(color = colors.mutedInk, lineHeight = 14.sp)

    val boardContent: @Composable () -> Unit = {
        BoardCanvas(
            gridSize = puzzle.size,
            cellDescription = { cell ->
                describeKillerSudokuCell(state, cell, selectedCellIndex == cell.index)
            },
            solved = KillerSudokuRules.isSolved(puzzle, state),
            reduceMotion = options.reduceMotion,
            hapticsEnabled = options.hapticsEnabled,
            dragPaintEnabled = false,
            drawCell = { cell, bounds ->
                val isGiven = puzzle.givens[cell.index] != null
                val cage = puzzle.cageOf(cell)
                // Engine sorts cage cells, so the first is the top-left (lowest index) and the last
                // is drawn after every other cell in the cage.
                val showsCageSum = cage.cells.first().index == cell.index
                val drawsCageOutline = cage.cells.last().index == cell.index
                drawRect(colors.card, bounds.topLeft, bounds.size)
                val selected = selectedCellIndex
                if (selected != null) {
                    val row = cell.index / KILLER_SUDOKU_SIZE
                    val column = cell.index % KILLER_SUDOKU_SIZE
                    val selectedRow = selected / KILLER_SUDOKU_SIZE
                    val selectedColumn = selected % KILLER_SUDOKU_SIZE
                    if (row == selectedRow || column == selectedColumn ||
                        (row / BOX_SIZE == selectedRow / BOX_SIZE &&
                            column / BOX_SIZE == selectedColumn / BOX_SIZE)
                    ) {
                        drawRect(colors.accentSoft, bounds.topLeft, bounds.size)
                    }
                }
                if (selectedCellIndex == cell.index) {
                    drawRect(colors.accent.copy(alpha = SELECTION_FILL_ALPHA), bounds.topLeft, bounds.size)
                }

                val inset = cageInset(bounds)
                // Keep digits and notes below the sum so the sum stays readable and does not cover notes.
                val contentBounds = if (showsCageSum) {
                    Rect(
                        left = bounds.left,
                        top = bounds.top + bounds.height * CAGE_SUM_BAND_FRACTION,
                        right = bounds.right,
                        bottom = bounds.bottom,
                    )
                } else {
                    bounds
                }
                val digit = state.digits[cell.index]
                if (digit != null) {
                    val style = if (isGiven) givenStyle else digitStyle
                    val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
                    drawCenteredLabel(text, contentBounds, contentBounds.width, contentBounds.height)
                } else {
                    drawNotes(state.notes[cell.index], contentBounds, textMeasurer, noteStyle)
                }

                if (drawsCageOutline) {
                    drawCageOutline(cage.cells.map { it.index }.toSet(), bounds, inset, colors.ink)
                }
                if (showsCageSum) {
                    drawCageSum(cage.sum, bounds, inset, textMeasurer, cageSumStyle)
                }

                drawBoxEdges(cell, bounds, colors.ink)

                if (selectedCellIndex == cell.index) {
                    drawRect(
                        color = colors.accent,
                        topLeft = bounds.topLeft,
                        size = bounds.size,
                        style = Stroke(width = SELECTION_BORDER_WIDTH_DP.dp.toPx()),
                    )
                }
            },
            onCellTap = { cell -> selectedCellIndex = cell.index },
            onDragPaint = {},
            onLongPress = { cell ->
                selectedCellIndex = cell.index
                KillerSudokuBoardInput.clearMoves(state, cell).takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
            },
            onUndo = onUndo,
        )
    }

    val controlsContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !notesMode,
                    onClick = { notesMode = false },
                    label = { Text(stringResource(R.string.killersudoku_mode_digits)) },
                )
                Spacer(Modifier.width(Spacing.xs))
                FilterChip(
                    selected = notesMode,
                    onClick = { notesMode = true },
                    label = { Text(stringResource(R.string.killersudoku_mode_notes)) },
                )
                Spacer(Modifier.width(Spacing.sm))
                TextButton(
                    enabled = selectedCellIsEditable,
                    onClick = {
                        selectedCell?.let { cell ->
                            KillerSudokuBoardInput.clearMoves(state, cell)
                                .takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                        }
                    },
                ) {
                    Text(stringResource(R.string.killersudoku_clear_cell))
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..KILLER_SUDOKU_SIZE).forEach { digit ->
                    val remaining = (KILLER_SUDOKU_SIZE - state.digits.count { it == digit }).coerceAtLeast(0)
                    val buttonDescription = stringResource(
                        if (notesMode) {
                            R.string.killersudoku_note_button_accessibility
                        } else {
                            R.string.killersudoku_digit_button_accessibility
                        },
                        digit,
                    )
                    Surface(
                        onClick = {
                            selectedCell?.let { cell ->
                                val moves = if (notesMode) {
                                    KillerSudokuBoardInput.noteMoves(state, cell, digit)
                                } else {
                                    KillerSudokuBoardInput.digitMoves(
                                        state,
                                        cell,
                                        digit,
                                        options.autoNotesEnabled,
                                    )
                                }
                                moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                            }
                        },
                        enabled = selectedCellIsEditable,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = buttonDescription },
                        shape = RoundedCornerShape(10.dp),
                        color = colors.card,
                        contentColor = colors.accent,
                        border = BorderStroke(1.dp, colors.cardEdge),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = digit.toString(),
                                style = NumberStyles.cell.copy(fontSize = 18.sp),
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = stringResource(R.string.play_digit_left, remaining),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colors.mutedInk,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    if (playLayout != null) {
        playLayout(
            boardContent,
            SudokuPlayActions(
                notesMode = notesMode,
                canEdit = selectedCellIsEditable,
                onToggleNotes = { notesMode = !notesMode },
                onErase = {
                    selectedCell?.let { cell ->
                        KillerSudokuBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
                    }
                },
                onDigit = { digit ->
                    selectedCell?.let { cell ->
                        val moves = if (notesMode) {
                            KillerSudokuBoardInput.noteMoves(state, cell, digit)
                        } else {
                            KillerSudokuBoardInput.digitMoves(state, cell, digit, options.autoNotesEnabled)
                        }
                        moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                    }
                },
            ),
        )
    } else if (layout == null) {
        KillerSudokuBoardWithControls(modifier, boardContent, controlsContent)
    } else {
        layout(boardContent, controlsContent)
    }
}

@Composable
private fun describeKillerSudokuCell(state: KillerSudokuState, cell: Cell, selected: Boolean): String {
    val digit = state.digits[cell.index]?.toString() ?: stringResource(R.string.killersudoku_cell_empty)
    val noteDigits = (1..KILLER_SUDOKU_SIZE)
        .filter { value -> state.notes[cell.index] and (1 shl (value - 1)) != 0 }
        .joinToString(", ")
    val notes = if (noteDigits.isEmpty()) {
        stringResource(R.string.killersudoku_no_notes)
    } else {
        stringResource(R.string.killersudoku_cell_notes_accessibility, noteDigits)
    }
    val editability = stringResource(
        if (state.puzzle.givens[cell.index] == null) {
            R.string.killersudoku_cell_editable
        } else {
            R.string.killersudoku_cell_given
        },
    )
    val selection = stringResource(
        if (selected) R.string.killersudoku_cell_selected else R.string.killersudoku_cell_not_selected,
    )
    val cage = state.puzzle.cageOf(cell)
    return stringResource(
        R.string.killersudoku_cell_accessibility_label,
        cell.index / state.puzzle.size + 1,
        cell.index % state.puzzle.size + 1,
        digit,
        notes,
        editability,
        selection,
        cage.sum,
        cage.cells.size,
    )
}

// The shared kit draws uniform cell lines, so the game supplies its 3-by-3 box boundaries.
private fun DrawScope.drawBoxEdges(cell: Cell, bounds: Rect, color: Color) {
    val row = cell.index / KILLER_SUDOKU_SIZE
    val column = cell.index % KILLER_SUDOKU_SIZE
    val strokeWidth = BOX_BORDER_WIDTH_DP.dp.toPx()
    if (row % BOX_SIZE == 0) {
        drawLine(color, bounds.topLeft, bounds.topRight, strokeWidth, StrokeCap.Square)
    }
    if (row == KILLER_SUDOKU_SIZE - 1) {
        drawLine(color, bounds.bottomLeft, bounds.bottomRight, strokeWidth, StrokeCap.Square)
    }
    if (column % BOX_SIZE == 0) {
        drawLine(color, bounds.topLeft, bounds.bottomLeft, strokeWidth, StrokeCap.Square)
    }
    if (column == KILLER_SUDOKU_SIZE - 1) {
        drawLine(color, bounds.topRight, bounds.bottomRight, strokeWidth, StrokeCap.Square)
    }
}

private fun DrawScope.drawCageOutline(
    cellIndices: Set<Int>,
    bounds: Rect,
    inset: Float,
    color: Color,
) {
    val side = minOf(bounds.width, bounds.height)
    val strokeWidth = minOf(1.25.dp.toPx(), inset * 0.45f).coerceAtLeast(1f)
    val dash = (side * 0.10f).coerceAtLeast(strokeWidth * 2f)
    val gap = (side * 0.07f).coerceAtLeast(strokeWidth * 1.5f)
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
    val segments = KillerSudokuCageGeometry.segments(
        cellIndices = cellIndices,
        cellWidth = bounds.width,
        cellHeight = bounds.height,
        inset = inset,
        boardSize = KILLER_SUDOKU_SIZE,
    )
    for (segment in segments) {
        drawLine(
            color = color,
            start = Offset(segment.startX, segment.startY),
            end = Offset(segment.endX, segment.endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
            pathEffect = effect,
        )
    }
}

private fun DrawScope.drawCageSum(
    sum: Int,
    bounds: Rect,
    inset: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    val text = textMeasurer.measure(AnnotatedString(sum.toString()), style = style)
    val bandBottom = bounds.top + bounds.height * CAGE_SUM_BAND_FRACTION
    // Sit inside the top-left corner. The sum cell always owns that corner, and the gap keeps the dashes off the glyphs.
    val gap = 2.dp.toPx()
    val textLeft = bounds.left + inset + gap
    val textTop = bounds.top + inset + gap
    val availableWidth = (bounds.width * 0.55f).coerceAtLeast(1f)
    val availableHeight = (bandBottom - textTop).coerceAtLeast(1f)
    val labelScale = minOf(
        1f,
        availableWidth / text.size.width.coerceAtLeast(1),
        availableHeight / text.size.height.coerceAtLeast(1),
    )
    val pivot = Offset(textLeft, textTop)
    scale(labelScale, pivot = pivot) {
        drawText(text, topLeft = pivot)
    }
}

private fun DrawScope.cageInset(bounds: Rect): Float {
    val side = minOf(bounds.width, bounds.height)
    val maxInset = side * 0.22f
    if (maxInset <= 0f) return 0f
    val minInset = 3.dp.toPx()
    val preferred = side * CAGE_INSET_FRACTION
    return if (minInset >= maxInset) maxInset else preferred.coerceIn(minInset, maxInset)
}

private fun DrawScope.drawNotes(noteMask: Int, bounds: Rect, textMeasurer: TextMeasurer, style: TextStyle) {
    for (digit in 1..KILLER_SUDOKU_SIZE) {
        if (noteMask and (1 shl (digit - 1)) == 0) continue
        val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
        val column = (digit - 1) % NOTES_COLUMNS
        val row = (digit - 1) / NOTES_COLUMNS
        val slotWidth = bounds.width / NOTES_COLUMNS
        val slotHeight = bounds.height / NOTES_ROWS
        val center = Offset(
            x = bounds.left + (column + 0.5f) * slotWidth,
            y = bounds.top + (row + 0.5f) * slotHeight,
        )
        drawCenteredLabel(text, bounds, slotWidth, slotHeight, center)
    }
}

private fun DrawScope.drawCenteredLabel(
    text: TextLayoutResult,
    bounds: Rect,
    availableWidth: Float,
    availableHeight: Float,
    center: Offset = bounds.center,
) {
    val labelScale = minOf(
        1f,
        availableWidth * LABEL_SIZE_FRACTION / text.size.width.coerceAtLeast(1),
        availableHeight * LABEL_SIZE_FRACTION / text.size.height.coerceAtLeast(1),
    )
    scale(labelScale, pivot = center) {
        drawText(
            text,
            topLeft = Offset(
                x = center.x - text.size.width / 2f,
                y = center.y - text.size.height / 2f,
            ),
        )
    }
}

private const val KILLER_SUDOKU_SIZE = 9
private const val BOX_SIZE = 3
private const val NOTES_COLUMNS = 3
private const val NOTES_ROWS = 3
private const val BOX_BORDER_WIDTH_DP = 3f
private const val SELECTION_FILL_ALPHA = 0.12f
private const val SELECTION_BORDER_WIDTH_DP = 2f
private const val LABEL_SIZE_FRACTION = 0.78f
private const val CAGE_INSET_FRACTION = 0.10f
private const val CAGE_SUM_BAND_FRACTION = 0.38f
