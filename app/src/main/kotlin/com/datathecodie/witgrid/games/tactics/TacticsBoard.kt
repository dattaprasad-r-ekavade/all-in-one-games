package com.datathecodie.witgrid.games.tactics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.tactics.Piece
import com.datathecodie.witgrid.engine.tactics.TacticsBoard as TacticsEngineBoard
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.TacticsRules
import com.datathecodie.witgrid.engine.tactics.TacticsState
import com.datathecodie.witgrid.engine.tactics.cellToSquare
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.WitgridColors
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

@Composable
fun TacticsBoard(
    state: TacticsState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: TacticsBoardOptions = LocalTacticsBoardOptions.current,
    modifier: Modifier = Modifier,
) {
    val puzzle = state.puzzle
    val board = state.currentBoard
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val whitePieceStyle = NumberStyles.cell.copy(
        color = colors.ink,
        fontWeight = FontWeight.Bold,
        fontSize = PIECE_LABEL_SIZE_SP.sp,
    )
    val blackPieceStyle = NumberStyles.cell.copy(
        color = colors.mutedInk,
        fontWeight = FontWeight.SemiBold,
        fontSize = PIECE_LABEL_SIZE_SP.sp,
    )
    val haptics = rememberBoardHaptics(options.hapticsEnabled)
    val mistakeCells = remember(state) {
        TacticsRules.mistakes(puzzle, state).map { it.index }.toSet()
    }
    val errorFill = MaterialTheme.colorScheme.error.copy(alpha = MISTAKE_FILL_ALPHA)

    BoardCanvas(
        gridSize = TACTICS_BOARD_SIZE,
        cellDescription = { cell -> describeTacticsCell(board, cell) },
        modifier = modifier,
        solved = TacticsRules.isSolved(puzzle, state),
        reduceMotion = options.reduceMotion,
        hapticsEnabled = options.hapticsEnabled,
        dragPaintEnabled = false,
        drawCell = { cell, bounds ->
            val row = cell.index / TACTICS_BOARD_SIZE
            val column = cell.index % TACTICS_BOARD_SIZE
            val lightSquare = (row + column) % 2 == 0
            val squareColor = if (lightSquare) colors.card else colors.paper
            drawRect(squareColor, topLeft = bounds.topLeft, size = bounds.size)

            highlightSquare(bounds, state, cell.index, colors, errorFill, mistakeCells)
            drawPiece(
                piece = board.pieceAt(cell),
                bounds = bounds,
                textMeasurer = textMeasurer,
                whiteStyle = whitePieceStyle,
                blackStyle = blackPieceStyle,
            )
        },
        onCellTap = { cell ->
            val moves = TacticsBoardInput.tapMoves(state, cell)
            val willPlace = state.pendingFrom != null && state.pendingFrom != cell
            if (willPlace || (state.pendingFrom == null && board.pieceAt(cell)?.color == puzzle.sideToMove)) {
                haptics.placement()
            }
            onMoves(moves)
        },
        onDragPaint = {},
        onLongPress = {},
        onUndo = onUndo,
    )
}

private fun DrawScope.highlightSquare(
    bounds: Rect,
    state: TacticsState,
    index: Int,
    colors: WitgridColors,
    errorFill: Color,
    mistakeCells: Set<Int>,
) {
    if (index in mistakeCells) {
        drawRect(errorFill, topLeft = bounds.topLeft, size = bounds.size)
    }
    val opponentMove = state.lastOpponentMove
    if (opponentMove != null && (index == opponentMove.from.index || index == opponentMove.to.index)) {
        drawRect(
            colors.mutedInk.copy(alpha = OPPONENT_MOVE_FILL_ALPHA),
            topLeft = bounds.topLeft,
            size = bounds.size,
        )
    }
    val lastMove = state.lastMove
    if (lastMove != null && lastMove != opponentMove) {
        if (index == lastMove.from.index || index == lastMove.to.index) {
            drawRect(
                colors.accentSoft,
                topLeft = bounds.topLeft,
                size = bounds.size,
            )
        }
    }
    val pending = state.pendingFrom
    if (pending != null && index == pending.index) {
        drawRect(
            colors.accent.copy(alpha = SELECTION_FILL_ALPHA),
            topLeft = bounds.topLeft,
            size = bounds.size,
        )
    }
}

private fun DrawScope.drawPiece(
    piece: Piece?,
    bounds: Rect,
    textMeasurer: TextMeasurer,
    whiteStyle: TextStyle,
    blackStyle: TextStyle,
) {
    if (piece == null) return
    val label = piece.toChar().toString()
    val style = if (piece.color == PieceColor.WHITE) whiteStyle else blackStyle
    val text = textMeasurer.measure(AnnotatedString(label), style = style)
    drawCenteredLabel(text, bounds)
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

@Composable
private fun describeTacticsCell(
    board: TacticsEngineBoard,
    cell: Cell,
): String {
    val row = cell.index / TACTICS_BOARD_SIZE + 1
    val column = cell.index % TACTICS_BOARD_SIZE + 1
    val square = cellToSquare(cell)
    val pieceLabel = board.pieceAt(cell)?.toChar()?.toString() ?: "-"
    return stringResource(R.string.board_cell_accessibility_label, row, column) +
        ", $square, $pieceLabel"
}

private const val TACTICS_BOARD_SIZE = 8
private const val PIECE_LABEL_SIZE_SP = 22
private const val LABEL_SIZE_FRACTION = 0.72f
private const val SELECTION_FILL_ALPHA = 0.22f
private const val OPPONENT_MOVE_FILL_ALPHA = 0.14f
private const val MISTAKE_FILL_ALPHA = 0.28f