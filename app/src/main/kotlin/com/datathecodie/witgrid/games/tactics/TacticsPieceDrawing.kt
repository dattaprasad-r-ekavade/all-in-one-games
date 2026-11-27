package com.datathecodie.witgrid.games.tactics

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.datathecodie.witgrid.engine.tactics.Piece
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.PieceType
import com.datathecodie.witgrid.ui.theme.WitgridColors

internal fun tacticsPieceSilhouette(type: PieceType): Path = when (type) {
    PieceType.KING -> Path().apply {
        moveTo(43f, 12f); lineTo(57f, 12f); lineTo(57f, 24f); lineTo(69f, 24f)
        lineTo(69f, 36f); lineTo(61f, 42f); lineTo(72f, 84f); quadraticTo(50f, 91f, 28f, 84f)
        lineTo(39f, 42f); lineTo(31f, 36f); lineTo(31f, 24f); lineTo(43f, 24f); close()
    }
    PieceType.QUEEN -> Path().apply {
        moveTo(28f, 38f); lineTo(20f, 18f); lineTo(38f, 31f); lineTo(50f, 13f)
        lineTo(62f, 31f); lineTo(80f, 18f); lineTo(72f, 38f); lineTo(66f, 78f)
        quadraticTo(50f, 84f, 34f, 78f); close()
    }
    PieceType.ROOK -> Path().apply {
        moveTo(27f, 18f); lineTo(40f, 18f); lineTo(40f, 29f); lineTo(48f, 29f)
        lineTo(48f, 18f); lineTo(60f, 18f); lineTo(60f, 29f); lineTo(73f, 29f)
        lineTo(69f, 43f); lineTo(63f, 78f); lineTo(72f, 86f); lineTo(28f, 86f)
        lineTo(37f, 78f); lineTo(31f, 43f); close()
    }
    PieceType.BISHOP -> Path().apply {
        moveTo(50f, 13f); quadraticTo(76f, 38f, 61f, 54f); lineTo(70f, 80f)
        quadraticTo(50f, 88f, 30f, 80f); lineTo(39f, 54f); quadraticTo(24f, 38f, 50f, 13f); close()
        moveTo(45f, 38f); lineTo(54f, 47f)
    }
    PieceType.KNIGHT -> Path().apply {
        moveTo(27f, 82f); lineTo(34f, 65f); lineTo(25f, 48f); lineTo(41f, 27f)
        lineTo(56f, 17f); lineTo(76f, 26f); lineTo(67f, 39f); lineTo(59f, 36f)
        lineTo(55f, 49f); lineTo(69f, 64f); lineTo(73f, 82f); close()
    }
    PieceType.PAWN -> Path().apply {
        moveTo(50f, 17f); cubicTo(31f, 17f, 31f, 43f, 46f, 48f); lineTo(35f, 70f)
        lineTo(27f, 83f); quadraticTo(50f, 90f, 73f, 83f); lineTo(65f, 70f); lineTo(54f, 48f)
        cubicTo(69f, 43f, 69f, 17f, 50f, 17f); close()
    }
}

internal val tacticsPieceTypesWithSilhouettes = setOf(
    PieceType.KING,
    PieceType.QUEEN,
    PieceType.ROOK,
    PieceType.BISHOP,
    PieceType.KNIGHT,
    PieceType.PAWN,
)

internal fun DrawScope.drawTacticsPiece(piece: Piece?, bounds: Rect, colors: WitgridColors) {
    if (piece == null) return
    val silhouette = tacticsPieceSilhouette(piece.type)
    val white = piece.color == PieceColor.WHITE
    val fill = if (white) colors.paper else colors.mutedInk
    val stroke = if (white) colors.ink else colors.ink
    val scaleX = bounds.width * PIECE_PADDING_FRACTION / SILHOUETTE_SIZE
    val scaleY = bounds.height * PIECE_PADDING_FRACTION / SILHOUETTE_SIZE
    val silhouetteScale = minOf(scaleX, scaleY)
    val left = bounds.center.x - SILHOUETTE_SIZE * silhouetteScale / 2f
    val top = bounds.center.y - SILHOUETTE_SIZE * silhouetteScale / 2f
    withTransform({
        translate(left, top)
        scale(silhouetteScale, silhouetteScale, pivot = Offset.Zero)
    }) {
        drawPath(silhouette, fill)
        drawPath(silhouette, stroke, style = Stroke(width = PIECE_STROKE_WIDTH))
    }
}

private const val SILHOUETTE_SIZE = 100f
private const val PIECE_PADDING_FRACTION = 0.68f
private const val PIECE_STROKE_WIDTH = 3f
