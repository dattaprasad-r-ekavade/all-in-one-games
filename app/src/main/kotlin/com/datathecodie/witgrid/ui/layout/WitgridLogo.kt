package com.datathecodie.witgrid.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.datathecodie.witgrid.ui.theme.WitgridTheme

/**
 * The Witgrid mark: a rounded frame around a small grid of filled cells, the same idea as the
 * launcher icon. It is drawn, not loaded, so it follows the theme ink and accent colours.
 */
@Composable
fun WitgridLogo(modifier: Modifier = Modifier, pattern: List<String> = TOP_BAR_PATTERN) {
    val colors = WitgridTheme.colors
    Canvas(modifier = modifier) {
        val side = size.minDimension
        val frame = side * FRAME_FRACTION
        drawRoundRect(
            color = colors.ink,
            topLeft = Offset(frame / 2f, frame / 2f),
            size = Size(side - frame, side - frame),
            cornerRadius = CornerRadius(side * CORNER_FRACTION),
            style = Stroke(width = frame),
        )
        val count = pattern.size
        val inset = frame + side * PADDING_FRACTION
        val gap = side * GAP_FRACTION
        val cell = (side - 2f * inset - gap * (count - 1)) / count
        pattern.forEachIndexed { row, line ->
            line.forEachIndexed { column, mark ->
                val color = when (mark) {
                    INK_MARK -> colors.ink
                    ACCENT_MARK -> colors.accent
                    else -> null
                }
                if (color != null) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(inset + column * (cell + gap), inset + row * (cell + gap)),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * CELL_CORNER_FRACTION),
                    )
                }
            }
        }
    }
}

/** The small 3 × 3 mark for the top bar. */
val TOP_BAR_PATTERN = listOf("1.1", "121", ".1.")

/** The full 5 × 5 "W" from the launcher icon, for large sizes. */
val LARGE_PATTERN = listOf("1...1", "1...1", "1.2.1", "1.2.1", ".1.1.")

private const val INK_MARK = '1'
private const val ACCENT_MARK = '2'
private const val FRAME_FRACTION = 0.08f
private const val CORNER_FRACTION = 0.22f
private const val PADDING_FRACTION = 0.06f
private const val GAP_FRACTION = 0.04f
private const val CELL_CORNER_FRACTION = 0.2f
