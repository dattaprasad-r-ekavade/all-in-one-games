package com.datathecodie.witgrid.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

data class BoardDragPreview(
    val strokes: List<Pair<Offset, Offset>>,
    val strokeWidth: Float,
) {
    companion object {
        fun forCell(bounds: Rect): BoardDragPreview {
            val radius = minOf(bounds.width, bounds.height) * MARK_RADIUS_FRACTION
            val strokeWidth = minOf(bounds.width, bounds.height) * MARK_STROKE_FRACTION
            val center = bounds.center
            return BoardDragPreview(
                strokes = listOf(
                    (Offset(center.x - radius, center.y - radius) to
                        Offset(center.x + radius, center.y + radius)),
                    (Offset(center.x + radius, center.y - radius) to
                        Offset(center.x - radius, center.y + radius)),
                ),
                strokeWidth = strokeWidth,
            )
        }
    }
}

private const val MARK_RADIUS_FRACTION = 0.28f
private const val MARK_STROKE_FRACTION = 0.12f
