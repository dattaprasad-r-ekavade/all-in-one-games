package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter

internal enum class PlayIcon { UNDO, REDO, RESTART, HINT, CHECK, ZOOM, FIT, NOTES, ERASE }

@Composable
internal fun playIcon(icon: PlayIcon): Painter = remember(icon) { PlayToolPainter(icon) }

private class PlayToolPainter(private val icon: PlayIcon) : Painter() {
    override val intrinsicSize = Size(24f, 24f)
    override fun DrawScope.onDraw() {
        val width = size.width / 24f
        val height = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * width, y * height)
        fun line(x: Float, y: Float, endX: Float, endY: Float) = drawLine(
            Color.Black, point(x, y), point(endX, endY), 1.8f * width, StrokeCap.Round)
        fun path(vararg points: Pair<Float, Float>) {
            val path = Path()
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) path.moveTo(x * width, y * height)
                else path.lineTo(x * width, y * height)
            }
            drawPath(path, Color.Black, style = Stroke(1.8f * width, cap = StrokeCap.Round))
        }
        when (icon) {
            PlayIcon.UNDO, PlayIcon.REDO, PlayIcon.RESTART -> {
                val redo = icon == PlayIcon.REDO
                val path = Path().apply {
                    if (redo) {
                        moveTo(19f * width, 7f * height)
                        lineTo(9f * width, 7f * height)
                        cubicTo(1f * width, 7f * height, 1f * width, 19f * height, 10f * width, 19f * height)
                    } else {
                        moveTo(5f * width, 7f * height)
                        lineTo(15f * width, 7f * height)
                        cubicTo(23f * width, 7f * height, 23f * width, 19f * height, 14f * width, 19f * height)
                    }
                }
                drawPath(path, Color.Black, style = Stroke(1.8f * width, cap = StrokeCap.Round))
                if (redo) path(15f to 3f, 20f to 7f, 15f to 11f) else path(9f to 3f, 4f to 7f, 9f to 11f)
            }
            PlayIcon.CHECK -> path(4f to 12f, 10f to 18f, 21f to 6f)
            PlayIcon.ZOOM, PlayIcon.FIT -> {
                drawCircle(Color.Black, 7f * width, point(10f, 10f), style = Stroke(1.8f * width))
                line(15f, 15f, 21f, 21f)
                line(6f, 10f, 14f, 10f)
                if (icon == PlayIcon.ZOOM) line(10f, 6f, 10f, 14f)
            }
            PlayIcon.HINT -> {
                drawCircle(Color.Black, 7f * width, point(12f, 9f), style = Stroke(1.8f * width))
                line(8f, 15f, 9f, 18f)
                line(16f, 15f, 15f, 18f)
                line(9f, 19f, 15f, 19f)
                line(10f, 22f, 14f, 22f)
            }
            PlayIcon.NOTES -> {
                path(4f to 16f, 16f to 4f, 20f to 8f, 8f to 20f, 4f to 20f, 4f to 16f)
                line(14f, 6f, 18f, 10f)
            }
            PlayIcon.ERASE -> {
                path(3f to 15f, 13f to 4f, 21f to 11f, 12f to 21f, 8f to 21f, 3f to 15f)
                line(8f, 10f, 16f, 17f)
                line(8f, 21f, 21f, 21f)
            }
        }
    }
}
