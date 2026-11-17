package com.datathecodie.witgrid.games.binary

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.binary.BinaryRules
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.ui.board.BoardCanvas
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
    val ringColor = if (colors.isDark) Color(0xFFE9A27A) else Color(0xFFC2622F)

    BoardCanvas(
        gridSize = puzzle.size,
        cellDescription = { cell -> describeBinaryCell(state, cell) },
        modifier = modifier,
        solved = BinaryRules.isSolved(puzzle, state),
        reduceMotion = options.reduceMotion,
        hapticsEnabled = options.hapticsEnabled,
        dragPaintEnabled = false,
        drawCell = { cell, bounds ->
            drawRect(colors.card, topLeft = bounds.topLeft, size = bounds.size)
            val isGiven = puzzle.givens[cell.index] != null
            if (isGiven) {
                drawRect(
                    color = colors.ink.copy(alpha = GIVEN_CELL_ALPHA),
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                )
            }

            val radius = minOf(bounds.width, bounds.height) * SYMBOL_RADIUS_FRACTION
            when (state.symbols[cell.index]) {
                BinarySymbol.EMPTY -> Unit
                BinarySymbol.A -> drawCircle(colors.accent, radius, bounds.center)
                BinarySymbol.B -> drawCircle(
                    ringColor,
                    radius - radius * RING_STROKE_FRACTION / 2f,
                    bounds.center,
                    style = Stroke(width = radius * RING_STROKE_FRACTION),
                )
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
private const val SYMBOL_RADIUS_FRACTION = 0.26f
private const val RING_STROKE_FRACTION = 0.40f
