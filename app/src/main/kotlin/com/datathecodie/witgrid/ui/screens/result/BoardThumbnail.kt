package com.datathecodie.witgrid.ui.screens.result

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.ui.theme.WitgridTheme

/** Draws saved marks without adding playable targets to the result screen. */
@Composable
internal fun BoardThumbnail(state: Any?, modifier: Modifier = Modifier) {
    val colors = WitgridTheme.colors
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        val count = when (state) {
            is CrownsState -> state.puzzle.size
            is BinaryState -> state.puzzle.size
            is NonogramState -> state.puzzle.size
            is MiniSudokuState -> 6
            else -> 5
        }
        val cell = size.minDimension / count
        val regions = colors.regions.map { it.color }
        drawRoundRect(colors.card, cornerRadius = CornerRadius(cell / 3))
        repeat(count * count) { index ->
            val row = index / count
            val column = index % count
            val position = Offset(column * cell, row * cell)
            val center = position + Offset(cell / 2, cell / 2)
            when (state) {
                is CrownsState -> {
                    drawRect(regions[state.puzzle.regions[index] % regions.size], position, Size(cell, cell))
                    if (state.marks[index] == Mark.FILLED) {
                        val crown = Path().apply {
                            moveTo(position.x + cell * 0.2f, position.y + cell * 0.3f)
                            lineTo(position.x + cell * 0.38f, position.y + cell * 0.48f)
                            lineTo(center.x, position.y + cell * 0.2f)
                            lineTo(position.x + cell * 0.63f, position.y + cell * 0.48f)
                            lineTo(position.x + cell * 0.8f, position.y + cell * 0.3f)
                            lineTo(position.x + cell * 0.7f, position.y + cell * 0.72f)
                            lineTo(position.x + cell * 0.3f, position.y + cell * 0.72f)
                            close()
                        }
                        drawPath(crown, colors.ink)
                    }
                }
                is BinaryState -> {
                    when (state.symbols[index]) {
                        BinarySymbol.A -> drawCircle(colors.accent, cell * 0.28f, center)
                        BinarySymbol.B -> drawCircle(Color(0xFFC2622F), cell * 0.26f, center, style = Stroke(cell * 0.09f))
                        else -> Unit
                    }
                }
                is NonogramState -> if (state.marks[index] == Mark.FILLED) drawRect(colors.ink, position, Size(cell, cell))
                is MiniSudokuState -> state.digits[index]?.let { digit ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(
                            (colors.ink.red * 255).toInt(), (colors.ink.green * 255).toInt(), (colors.ink.blue * 255).toInt())
                        textSize = cell * 0.65f
                        textAlign = Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(digit.toString(), center.x,
                        center.y - (paint.ascent() + paint.descent()) / 2, paint)
                }
            }
            if (state !is CrownsState) drawRect(colors.cardEdge, position, Size(cell, cell), style = Stroke(1f))
        }
        drawRect(colors.ink, style = Stroke(2f))
    }
}
