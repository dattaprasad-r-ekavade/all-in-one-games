package com.datathecodie.witgrid.games.futoshiki

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiRules
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.SudokuPlayActions
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun FutoshikiBoard(
    state: FutoshikiState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: FutoshikiBoardOptions = LocalFutoshikiBoardOptions.current,
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
    val errorColor = MaterialTheme.colorScheme.error
    val textMeasurer = rememberTextMeasurer()
    val digitStyle = NumberStyles.cell.copy(color = colors.accent)
    val givenStyle = NumberStyles.cell.copy(color = colors.ink)
    val noteStyle = NumberStyles.clue.copy(color = colors.mutedInk, fontSize = 11.sp)

    val boardContent: @Composable () -> Unit = {
        BoardCanvas(
            gridSize = puzzle.size,
            cellDescription = { cell ->
                describeFutoshikiCell(state, cell, selectedCellIndex == cell.index)
            },
            solved = FutoshikiRules.isSolved(puzzle, state),
            reduceMotion = options.reduceMotion,
            hapticsEnabled = options.hapticsEnabled,
            dragPaintEnabled = false,
            drawCell = { cell, bounds ->
                val row = cell.index / puzzle.size
                val column = cell.index % puzzle.size
                val isGiven = puzzle.givens[cell.index] != null

                drawRect(colors.card, bounds.topLeft, bounds.size)

                val selected = selectedCellIndex
                if (selected != null) {
                    val selectedRow = selected / puzzle.size
                    val selectedColumn = selected % puzzle.size
                    if (row == selectedRow || column == selectedColumn) {
                        drawRect(colors.accentSoft, bounds.topLeft, bounds.size)
                    }
                }
                if (selectedCellIndex == cell.index) {
                    drawRect(
                        colors.accent.copy(alpha = SELECTION_FILL_ALPHA),
                        bounds.topLeft,
                        bounds.size,
                    )
                }

                val digit = state.digits[cell.index]
                if (digit != null) {
                    val style = if (isGiven) givenStyle else digitStyle
                    val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
                    drawCenteredLabel(text, bounds, bounds.width, bounds.height)
                } else {
                    drawNotes(state.notes[cell.index], bounds, textMeasurer, noteStyle, puzzle.size)
                }

                if (selectedCellIndex == cell.index) {
                    drawRect(
                        color = colors.accent,
                        topLeft = bounds.topLeft,
                        size = bounds.size,
                        style = Stroke(width = SELECTION_BORDER_WIDTH_DP.dp.toPx()),
                    )
                }

                // Horizontal sign on the left edge of this cell
                if (column > 0) {
                    val hSign = puzzle.horizontalSign(row, column - 1)
                    if (hSign != FutoshikiSign.NONE) {
                        val leftVal = state.digits[row * puzzle.size + column - 1]
                        val rightVal = state.digits[cell.index]
                        val isBroken = FutoshikiBoardInput.isHorizontalBroken(hSign, leftVal, rightVal)
                        val signColor = if (isBroken) errorColor else colors.ink
                        val isLeftTinted = selected != null &&
                            (selected / puzzle.size == row || selected % puzzle.size == column - 1)
                        val isRightTinted = selected != null &&
                            (selected / puzzle.size == row || selected % puzzle.size == column)
                        val haloColor = if (isLeftTinted || isRightTinted) colors.accentSoft else colors.card
                        drawHorizontalChevron(
                            sign = hSign,
                            edgeCenter = Offset(bounds.left, bounds.center.y),
                            cellSize = bounds.width,
                            color = signColor,
                            haloColor = haloColor,
                        )
                    }
                }

                // Vertical sign on the top edge of this cell
                if (row > 0) {
                    val vSign = puzzle.verticalSign(row - 1, column)
                    if (vSign != FutoshikiSign.NONE) {
                        val topVal = state.digits[(row - 1) * puzzle.size + column]
                        val bottomVal = state.digits[cell.index]
                        val isBroken = FutoshikiBoardInput.isVerticalBroken(vSign, topVal, bottomVal)
                        val signColor = if (isBroken) errorColor else colors.ink
                        val isTopTinted = selected != null &&
                            (selected / puzzle.size == row - 1 || selected % puzzle.size == column)
                        val isBottomTinted = selected != null &&
                            (selected / puzzle.size == row || selected % puzzle.size == column)
                        val haloColor = if (isTopTinted || isBottomTinted) colors.accentSoft else colors.card
                        drawVerticalChevron(
                            sign = vSign,
                            edgeCenter = Offset(bounds.center.x, bounds.top),
                            cellSize = bounds.height,
                            color = signColor,
                            haloColor = haloColor,
                        )
                    }
                }
            },
            onCellTap = { cell -> selectedCellIndex = cell.index },
            onDragPaint = {},
            onLongPress = { cell ->
                selectedCellIndex = cell.index
                FutoshikiBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
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
                            FutoshikiBoardInput.clearMoves(state, cell)
                                .takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                        }
                    },
                ) {
                    Text(stringResource(R.string.minisudoku_clear_cell))
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..puzzle.size).forEach { digit ->
                    val remaining = (puzzle.size - state.digits.count { it == digit }).coerceAtLeast(0)
                    val buttonDescription = stringResource(
                        if (notesMode) {
                            R.string.minisudoku_note_button_accessibility
                        } else {
                            R.string.minisudoku_digit_button_accessibility
                        },
                        digit,
                    )
                    Surface(
                        onClick = {
                            selectedCell?.let { cell ->
                                val moves = if (notesMode) {
                                    FutoshikiBoardInput.noteMoves(state, cell, digit)
                                } else {
                                    FutoshikiBoardInput.digitMoves(state, cell, digit, options.autoNotesEnabled)
                                }
                                moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                            }
                        },
                        enabled = selectedCellIsEditable,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = buttonDescription },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        contentColor = colors.accent,
                        border = BorderStroke(1.dp, colors.cardEdge),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
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
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            }
        }
    }

    if (playLayout != null) {
        playLayout(boardContent, SudokuPlayActions(
            notesMode = notesMode,
            canEdit = selectedCellIsEditable,
            onToggleNotes = { notesMode = !notesMode },
            onErase = {
                selectedCell?.let { cell ->
                    FutoshikiBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
                }
            },
            onDigit = { digit ->
                selectedCell?.let { cell ->
                    val moves = if (notesMode) {
                        FutoshikiBoardInput.noteMoves(state, cell, digit)
                    } else {
                        FutoshikiBoardInput.digitMoves(state, cell, digit, options.autoNotesEnabled)
                    }
                    moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                }
            },
        ))
    } else if (layout == null) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            boardContent()
            Spacer(Modifier.height(Spacing.sm))
            controlsContent()
        }
    } else {
        layout(boardContent, controlsContent)
    }
}

private fun DrawScope.drawHorizontalChevron(
    sign: FutoshikiSign,
    edgeCenter: Offset,
    cellSize: Float,
    color: Color,
    haloColor: Color,
) {
    val depth = (cellSize * 0.09f).coerceIn(4.dp.toPx(), 7.dp.toPx())
    val halfSpread = (cellSize * 0.13f).coerceIn(5.dp.toPx(), 9.dp.toPx())
    val strokeWidth = 2.5f.dp.toPx()
    val haloWidth = strokeWidth + 3.dp.toPx()

    val path = Path()
    when (sign) {
        FutoshikiSign.LESS -> {
            path.moveTo(edgeCenter.x + depth, edgeCenter.y - halfSpread)
            path.lineTo(edgeCenter.x - depth, edgeCenter.y)
            path.lineTo(edgeCenter.x + depth, edgeCenter.y + halfSpread)
        }
        FutoshikiSign.GREATER -> {
            path.moveTo(edgeCenter.x - depth, edgeCenter.y - halfSpread)
            path.lineTo(edgeCenter.x + depth, edgeCenter.y)
            path.lineTo(edgeCenter.x - depth, edgeCenter.y + halfSpread)
        }
        FutoshikiSign.NONE -> return
    }

    drawPath(
        path = path,
        color = haloColor,
        style = Stroke(width = haloWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawVerticalChevron(
    sign: FutoshikiSign,
    edgeCenter: Offset,
    cellSize: Float,
    color: Color,
    haloColor: Color,
) {
    val halfSpread = (cellSize * 0.13f).coerceIn(5.dp.toPx(), 9.dp.toPx())
    val depth = (cellSize * 0.09f).coerceIn(4.dp.toPx(), 7.dp.toPx())
    val strokeWidth = 2.5f.dp.toPx()
    val haloWidth = strokeWidth + 3.dp.toPx()

    val path = Path()
    when (sign) {
        FutoshikiSign.LESS -> {
            path.moveTo(edgeCenter.x - halfSpread, edgeCenter.y + depth)
            path.lineTo(edgeCenter.x, edgeCenter.y - depth)
            path.lineTo(edgeCenter.x + halfSpread, edgeCenter.y + depth)
        }
        FutoshikiSign.GREATER -> {
            path.moveTo(edgeCenter.x - halfSpread, edgeCenter.y - depth)
            path.lineTo(edgeCenter.x, edgeCenter.y + depth)
            path.lineTo(edgeCenter.x + halfSpread, edgeCenter.y - depth)
        }
        FutoshikiSign.NONE -> return
    }

    drawPath(
        path = path,
        color = haloColor,
        style = Stroke(width = haloWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Composable
private fun describeFutoshikiCell(state: FutoshikiState, cell: Cell, selected: Boolean): String {
    val digit = state.digits[cell.index]?.toString() ?: stringResource(R.string.minisudoku_cell_empty)
    val noteDigits = (1..state.puzzle.size)
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

private fun DrawScope.drawNotes(
    noteMask: Int,
    bounds: Rect,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    size: Int,
) {
    val noteCols = 3
    val noteRows = if (size <= 6) 2 else 3
    for (digit in 1..size) {
        if (noteMask and (1 shl (digit - 1)) == 0) continue
        val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
        val column = (digit - 1) % noteCols
        val row = (digit - 1) / noteCols
        val slotWidth = bounds.width / noteCols
        val slotHeight = bounds.height / noteRows
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

private const val SELECTION_BORDER_WIDTH_DP = 2
private const val SELECTION_FILL_ALPHA = 0.12f
private const val LABEL_SIZE_FRACTION = 0.78f
