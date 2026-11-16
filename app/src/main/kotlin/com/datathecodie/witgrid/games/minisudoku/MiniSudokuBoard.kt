package com.datathecodie.witgrid.games.minisudoku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuRules
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun MiniSudokuBoard(
    state: MiniSudokuState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: MiniSudokuBoardOptions = LocalMiniSudokuBoardOptions.current,
    modifier: Modifier = Modifier,
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
    val noteStyle = NumberStyles.clue.copy(color = colors.ink)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoardCanvas(
            gridSize = puzzle.size,
            cellDescription = { cell ->
                describeMiniSudokuCell(state, cell, selectedCellIndex == cell.index)
            },
            solved = MiniSudokuRules.isSolved(puzzle, state),
            reduceMotion = options.reduceMotion,
            hapticsEnabled = options.hapticsEnabled,
            dragPaintEnabled = false,
            drawCell = { cell, bounds ->
                val isGiven = puzzle.givens[cell.index] != null
                if (isGiven) drawRect(colors.ink.copy(alpha = GIVEN_CELL_ALPHA), bounds.topLeft, bounds.size)
                if (selectedCellIndex == cell.index) {
                    drawRect(colors.accent.copy(alpha = SELECTION_FILL_ALPHA), bounds.topLeft, bounds.size)
                }

                val digit = state.digits[cell.index]
                if (digit != null) {
                    val style = if (isGiven) givenStyle else digitStyle
                    val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
                    drawCenteredLabel(text, bounds, bounds.width, bounds.height)
                } else {
                    drawNotes(state.notes[cell.index], bounds, textMeasurer, noteStyle)
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
                MiniSudokuBoardInput.clearMoves(state, cell).takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
            },
            onUndo = onUndo,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !notesMode,
                onClick = { notesMode = false },
                label = { Text(stringResource(R.string.minisudoku_mode_digits)) },
            )
            Spacer(Modifier.width(Spacing.xs))
            FilterChip(
                selected = notesMode,
                onClick = { notesMode = true },
                label = { Text(stringResource(R.string.minisudoku_mode_notes)) },
            )
            Spacer(Modifier.width(Spacing.sm))
            TextButton(
                enabled = selectedCellIsEditable,
                onClick = {
                    selectedCell?.let { cell ->
                        MiniSudokuBoardInput.clearMoves(state, cell)
                            .takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                    }
                },
            ) {
                Text(stringResource(R.string.minisudoku_clear_cell))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..MINI_SUDOKU_SIZE).forEach { digit ->
                val buttonDescription = stringResource(
                    if (notesMode) {
                        R.string.minisudoku_note_button_accessibility
                    } else {
                        R.string.minisudoku_digit_button_accessibility
                    },
                    digit,
                )
                IconButton(
                    modifier = Modifier.weight(1f),
                    enabled = selectedCellIsEditable,
                    onClick = {
                        selectedCell?.let { cell ->
                            val moves = if (notesMode) {
                                MiniSudokuBoardInput.noteMoves(state, cell, digit)
                            } else {
                                MiniSudokuBoardInput.digitMoves(state, cell, digit, options.autoNotesEnabled)
                            }
                            moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                        }
                    },
                ) {
                    Text(
                        text = digit.toString(),
                        style = NumberStyles.cell,
                        modifier = Modifier.semantics {
                            contentDescription = buttonDescription
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun describeMiniSudokuCell(state: MiniSudokuState, cell: Cell, selected: Boolean): String {
    val digit = state.digits[cell.index]?.toString() ?: stringResource(R.string.minisudoku_cell_empty)
    val noteDigits = (1..MINI_SUDOKU_SIZE)
        .filter { value -> state.notes[cell.index] and (1 shl (value - 1)) != 0 }
        .joinToString(", ")
    val notes = if (noteDigits.isEmpty()) {
        stringResource(R.string.minisudoku_no_notes)
    } else {
        stringResource(R.string.minisudoku_cell_notes_accessibility, noteDigits)
    }
    val editability = stringResource(
        if (state.puzzle.givens[cell.index] == null) {
            R.string.minisudoku_cell_editable
        } else {
            R.string.minisudoku_cell_given
        },
    )
    val selection = stringResource(
        if (selected) R.string.minisudoku_cell_selected else R.string.minisudoku_cell_not_selected,
    )
    return stringResource(
        R.string.minisudoku_cell_accessibility_label,
        cell.index / state.puzzle.size + 1,
        cell.index % state.puzzle.size + 1,
        digit,
        notes,
        editability,
        selection,
    )
}

// The shared kit draws uniform cell lines, so the game supplies its 2-by-3 box boundaries.
private fun DrawScope.drawBoxEdges(cell: Cell, bounds: Rect, color: Color) {
    val row = cell.index / MINI_SUDOKU_SIZE
    val column = cell.index % MINI_SUDOKU_SIZE
    val strokeWidth = BOX_BORDER_WIDTH_DP.dp.toPx()
    if (row % BOX_HEIGHT == 0) {
        drawLine(color, bounds.topLeft, bounds.topRight, strokeWidth, StrokeCap.Square)
    }
    if (row == MINI_SUDOKU_SIZE - 1) {
        drawLine(color, bounds.bottomLeft, bounds.bottomRight, strokeWidth, StrokeCap.Square)
    }
    if (column % BOX_WIDTH == 0) {
        drawLine(color, bounds.topLeft, bounds.bottomLeft, strokeWidth, StrokeCap.Square)
    }
    if (column == MINI_SUDOKU_SIZE - 1) {
        drawLine(color, bounds.topRight, bounds.bottomRight, strokeWidth, StrokeCap.Square)
    }
}

private fun DrawScope.drawNotes(noteMask: Int, bounds: Rect, textMeasurer: TextMeasurer, style: TextStyle) {
    for (digit in 1..MINI_SUDOKU_SIZE) {
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

private const val MINI_SUDOKU_SIZE = 6
private const val BOX_HEIGHT = 2
private const val BOX_WIDTH = 3
private const val NOTES_COLUMNS = 3
private const val NOTES_ROWS = 2
private const val BOX_BORDER_WIDTH_DP = 3f
private const val GIVEN_CELL_ALPHA = 0.06f
private const val SELECTION_FILL_ALPHA = 0.12f
private const val SELECTION_BORDER_WIDTH_DP = 2f
private const val LABEL_SIZE_FRACTION = 0.78f
