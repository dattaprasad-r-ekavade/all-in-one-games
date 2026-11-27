package com.datathecodie.witgrid.games.tactics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.tactics.Piece
import com.datathecodie.witgrid.engine.tactics.TacticsBoard as TacticsEngineBoard
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.PieceType
import com.datathecodie.witgrid.engine.tactics.TacticsRules
import com.datathecodie.witgrid.engine.tactics.TacticsState
import com.datathecodie.witgrid.engine.tactics.cellToSquare
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.WitgridColors
import com.datathecodie.witgrid.ui.theme.WitgridTheme

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
    val coordinateStyle = NumberStyles.cell.copy(color = colors.ink, fontSize = 10.sp)
    val isBlackToMove = puzzle.sideToMove == PieceColor.BLACK
    val haptics = rememberBoardHaptics(options.hapticsEnabled)
    val mistakeCells = remember(state) {
        TacticsRules.mistakes(puzzle, state).map { it.index }.toSet()
    }
    val legalTargets = remember(state) {
        state.pendingFrom?.let { from -> board.legalMoves().filter { it.from == from }.map { it.to }.toSet() }.orEmpty()
    }
    val errorFill = MaterialTheme.colorScheme.error.copy(alpha = MISTAKE_FILL_ALPHA)

    BoardCanvas(
        gridSize = TACTICS_BOARD_SIZE,
        cellDescription = { displayCell -> describeTacticsCell(board, TacticsBoardInput.displayCellToEngineCell(isBlackToMove, displayCell)) },
        modifier = modifier,
        solved = TacticsRules.isSolved(puzzle, state),
        reduceMotion = options.reduceMotion,
        hapticsEnabled = options.hapticsEnabled,
        dragPaintEnabled = false,
        drawCell = { cell, bounds ->
            val displayRow = cell.index / TACTICS_BOARD_SIZE
            val column = cell.index % TACTICS_BOARD_SIZE
            val rankFromWhite = TacticsBoardInput.displayRowToRankFromWhite(isBlackToMove, displayRow)
            val row = 7 - rankFromWhite
            val engineCell = Cell(row * TACTICS_BOARD_SIZE + column)
            val lightSquare = (row + column) % 2 == 0
            val squareColor = if (lightSquare) colors.card else colors.paper
            drawRect(squareColor, topLeft = bounds.topLeft, size = bounds.size)

            highlightSquare(bounds, state, engineCell.index, colors, errorFill, mistakeCells)
            drawPiece(
                piece = board.pieceAt(engineCell),
                bounds = bounds,
                colors = colors,
            )
            if (column == 0) {
                drawText(textMeasurer.measure((rankFromWhite + 1).toString(), coordinateStyle), topLeft = bounds.topLeft + androidx.compose.ui.geometry.Offset(2f, 1f))
            }
            if (displayRow == 7) {
                drawText(textMeasurer.measure(('a' + column).toString(), coordinateStyle), topLeft = bounds.topLeft + androidx.compose.ui.geometry.Offset(bounds.width - 10f, bounds.height - 13f))
            }
            if (engineCell in legalTargets) {
                    drawCircle(colors.accent.copy(alpha = 0.72f), radius = bounds.width * if (board.pieceAt(engineCell) == null) 0.13f else 0.34f, center = bounds.center,
                        style = if (board.pieceAt(engineCell) == null) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(bounds.width * 0.07f))
            }
        },
        onCellTap = { cell ->
            val engineCell = TacticsBoardInput.displayCellToEngineCell(isBlackToMove, cell)
            val moves = TacticsBoardInput.tapMoves(state, cell)
            val willPlace = state.pendingFrom != null && state.pendingFrom != engineCell
            if (willPlace || (state.pendingFrom == null && board.pieceAt(engineCell)?.color == puzzle.sideToMove)) {
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

private fun DrawScope.drawPiece(piece: Piece?, bounds: Rect, colors: WitgridColors) = drawTacticsPiece(piece, bounds, colors)

@Composable
private fun describeTacticsCell(
    board: TacticsEngineBoard,
    cell: Cell,
): String {
    val row = cell.index / TACTICS_BOARD_SIZE + 1
    val column = cell.index % TACTICS_BOARD_SIZE + 1
    val square = cellToSquare(cell)
    return stringResource(
        R.string.tactics_cell_accessibility_label,
        row,
        column,
        square,
        tacticsPieceLabel(board.pieceAt(cell)),
    )
}

@Composable
private fun tacticsPieceLabel(piece: Piece?): String {
    if (piece == null) return stringResource(R.string.tactics_square_empty)
    val resource = when (piece.type) {
        PieceType.KING -> if (piece.color == PieceColor.WHITE) R.string.tactics_white_king else R.string.tactics_black_king
        PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) R.string.tactics_white_queen else R.string.tactics_black_queen
        PieceType.ROOK -> if (piece.color == PieceColor.WHITE) R.string.tactics_white_rook else R.string.tactics_black_rook
        PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) R.string.tactics_white_bishop else R.string.tactics_black_bishop
        PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) R.string.tactics_white_knight else R.string.tactics_black_knight
        PieceType.PAWN -> if (piece.color == PieceColor.WHITE) R.string.tactics_white_pawn else R.string.tactics_black_pawn
    }
    return stringResource(resource)
}

private const val TACTICS_BOARD_SIZE = 8
private const val SELECTION_FILL_ALPHA = 0.22f
private const val OPPONENT_MOVE_FILL_ALPHA = 0.14f
private const val MISTAKE_FILL_ALPHA = 0.28f
