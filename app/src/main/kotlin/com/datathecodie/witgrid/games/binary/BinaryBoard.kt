package com.datathecodie.witgrid.games.binary

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.binary.BinaryRules
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun BinaryBoard(
    state: BinaryState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: BinaryBoardOptions = LocalBinaryBoardOptions.current,
    modifier: Modifier = Modifier,
) {
    val puzzle = state.puzzle
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val symbolA = stringResource(R.string.binary_symbol_a)
    val symbolB = stringResource(R.string.binary_symbol_b)

    BoardCanvas(
        gridSize = puzzle.size,
        cellDescription = { cell -> describeBinaryCell(state, cell) },
        modifier = modifier,
        solved = BinaryRules.isSolved(puzzle, state),
        reduceMotion = options.reduceMotion,
        hapticsEnabled = options.hapticsEnabled,
        dragPaintEnabled = false,
        drawCell = { cell, bounds ->
            val isGiven = puzzle.givens[cell.index] != null
            if (isGiven) {
                drawRect(
                    color = colors.ink.copy(alpha = GIVEN_CELL_ALPHA),
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                )
            }

            val label = when (state.symbols[cell.index]) {
                BinarySymbol.EMPTY -> null
                BinarySymbol.A -> symbolA
                BinarySymbol.B -> symbolB
            }
            if (label != null) {
                val text = textMeasurer.measure(
                    AnnotatedString(label),
                    style = NumberStyles.cell.copy(color = if (isGiven) colors.ink else colors.accent),
                )
                // Dense boards must keep symbols inside their cells at larger font scales.
                val symbolScale = minOf(
                    1f,
                    bounds.width * SYMBOL_SIZE_FRACTION / text.size.width.coerceAtLeast(1),
                    bounds.height * SYMBOL_SIZE_FRACTION / text.size.height.coerceAtLeast(1),
                )
                scale(symbolScale, pivot = bounds.center) {
                    drawText(
                        text,
                        topLeft = Offset(
                            x = bounds.center.x - text.size.width / 2f,
                            y = bounds.center.y - text.size.height / 2f,
                        ),
                    )
                }
            }
        },
        onCellTap = { cell ->
            BinaryBoardInput.tapMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
        },
        onDragPaint = {},
        onLongPress = { cell ->
            BinaryBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
        },
        onUndo = onUndo,
    )
}

@Composable
private fun describeBinaryCell(state: BinaryState, cell: Cell): String {
    val mark = stringResource(
        when (state.symbols[cell.index]) {
            BinarySymbol.EMPTY -> R.string.binary_mark_empty
            BinarySymbol.A -> R.string.binary_symbol_a
            BinarySymbol.B -> R.string.binary_symbol_b
        },
    )
    val status = stringResource(
        if (state.puzzle.givens[cell.index] == null) {
            R.string.binary_cell_editable
        } else {
            R.string.binary_cell_given
        },
    )
    return stringResource(
        R.string.binary_cell_accessibility_label,
        cell.index / state.puzzle.size + 1,
        cell.index % state.puzzle.size + 1,
        mark,
        status,
    )
}

private const val GIVEN_CELL_ALPHA = 0.06f
private const val SYMBOL_SIZE_FRACTION = 0.75f
