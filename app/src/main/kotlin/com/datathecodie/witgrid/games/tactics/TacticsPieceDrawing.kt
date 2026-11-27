package com.datathecodie.witgrid.games.tactics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.datathecodie.witgrid.engine.tactics.Piece
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.PieceType
import com.datathecodie.witgrid.ui.theme.WitgridColors

internal val tacticsDrawnPieceTypes = PieceType.entries.toSet()

internal fun DrawScope.drawTacticsPiece(piece: Piece?, bounds: Rect, colors: WitgridColors) {
    if (piece == null) return
    val padX = bounds.width * 0.22f
    val padY = bounds.height * 0.16f
    val left = bounds.left + padX
    val right = bounds.right - padX
    val top = bounds.top + padY
    val bottom = bounds.bottom - padY
    val mid = bounds.center.x
    val base = Path().apply {
        moveTo(left, bottom)
        lineTo(left + bounds.width * 0.08f, bottom - bounds.height * 0.12f)
        lineTo(right - bounds.width * 0.08f, bottom - bounds.height * 0.12f)
        lineTo(right, bottom)
        close()
    }
    val head = Path().apply {
        when (piece.type) {
            PieceType.PAWN -> addOval(Rect(mid - bounds.width * .13f, top + bounds.height * .28f, mid + bounds.width * .13f, top + bounds.height * .54f))
            PieceType.ROOK -> {
                moveTo(left + bounds.width * .04f, top + bounds.height * .38f)
                lineTo(left + bounds.width * .04f, top)
                lineTo(mid - bounds.width * .07f, top)
                lineTo(mid - bounds.width * .07f, top + bounds.height * .12f)
                lineTo(mid + bounds.width * .07f, top + bounds.height * .12f)
                lineTo(mid + bounds.width * .07f, top)
                lineTo(right - bounds.width * .04f, top)
                lineTo(right - bounds.width * .04f, top + bounds.height * .38f)
                close()
            }
            PieceType.KNIGHT -> {
                moveTo(left + bounds.width * .04f, top + bounds.height * .5f)
                lineTo(left + bounds.width * .1f, top + bounds.height * .14f)
                lineTo(mid + bounds.width * .02f, top)
                lineTo(right - bounds.width * .04f, top + bounds.height * .24f)
                lineTo(right - bounds.width * .18f, top + bounds.height * .54f)
                close()
            }
            PieceType.BISHOP -> {
                moveTo(mid, top)
                lineTo(right - bounds.width * .07f, top + bounds.height * .25f)
                lineTo(mid + bounds.width * .1f, top + bounds.height * .52f)
                lineTo(mid - bounds.width * .1f, top + bounds.height * .52f)
                lineTo(left + bounds.width * .07f, top + bounds.height * .25f)
                close()
            }
            PieceType.QUEEN, PieceType.KING -> {
                moveTo(left + bounds.width * .04f, top + bounds.height * .48f)
                lineTo(left + bounds.width * .1f, top + bounds.height * .12f)
                lineTo(mid, top + bounds.height * .3f)
                lineTo(right - bounds.width * .1f, top + bounds.height * .12f)
                lineTo(right - bounds.width * .04f, top + bounds.height * .48f)
                close()
            }
        }
    }
    val fill = if (piece.color == PieceColor.WHITE) colors.card else colors.mutedInk
    val stroke = if (piece.color == PieceColor.WHITE) colors.ink else colors.ink
    drawPath(base, fill)
    drawPath(head, fill)
    val lineWidth = bounds.width * 0.045f
    drawPath(base, stroke, style = Stroke(lineWidth))
    drawPath(head, stroke, style = Stroke(lineWidth))
    if (piece.type == PieceType.KING || piece.type == PieceType.QUEEN) {
        drawLine(stroke, Offset(mid, top - bounds.height * .04f), Offset(mid, top + bounds.height * .12f), lineWidth)
        if (piece.type == PieceType.KING) drawLine(stroke, Offset(mid - bounds.width * .07f, top + bounds.height * .04f), Offset(mid + bounds.width * .07f, top + bounds.height * .04f), lineWidth)
    }
}
