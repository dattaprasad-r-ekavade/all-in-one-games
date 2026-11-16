package com.datathecodie.witgrid.games.crowns

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.crowns.CrownsRules
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.HatchPattern
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun CrownsBoard(
    state: CrownsState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: CrownsBoardOptions = LocalCrownsBoardOptions.current,
    modifier: Modifier = Modifier,
) {
    val puzzle = state.puzzle
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val regionLabelStyle = TextStyle(
        color = colors.ink.copy(alpha = REGION_LABEL_ALPHA),
        fontSize = REGION_LABEL_SIZE_SP.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val haptics = rememberBoardHaptics(options.hapticsEnabled)

    BoardCanvas(
        gridSize = puzzle.size,
        cellDescription = { cell ->
            val markDescription = stringResource(
                when (state.marks[cell.index]) {
                    Mark.EMPTY -> R.string.crowns_cell_empty
                    Mark.CROSSED -> R.string.crowns_cell_crossed
                    Mark.FILLED -> R.string.crowns_cell_filled
                },
            )
            stringResource(
                R.string.crowns_cell_accessibility_label,
                cell.index / puzzle.size + 1,
                cell.index % puzzle.size + 1,
                ('A'.code + puzzle.regions[cell.index]).toChar().toString(),
                markDescription,
            )
        },
        modifier = modifier,
        solved = CrownsRules.isSolved(puzzle, state),
        reduceMotion = options.reduceMotion,
        hapticsEnabled = options.hapticsEnabled,
        drawCell = { cell, bounds ->
            val regionId = puzzle.regions[cell.index]
            val region = colors.regions[regionId % colors.regions.size]
            drawRect(region.color, topLeft = bounds.topLeft, size = bounds.size)

            if (options.showRegionAids) {
                drawRegionHatch(region.hatch, bounds, colors.ink)
                val letter = ('A'.code + regionId).toChar().toString()
                val text = textMeasurer.measure(AnnotatedString(letter), style = regionLabelStyle)
                drawText(
                    text,
                    topLeft = Offset(
                        x = bounds.center.x - text.size.width / 2f,
                        y = bounds.center.y - text.size.height / 2f,
                    ),
                )
            }

            when (state.marks[cell.index]) {
                Mark.EMPTY -> Unit
                Mark.CROSSED -> drawCross(bounds, colors.ink)
                Mark.FILLED -> drawCrown(bounds, colors.ink)
            }

            // Larger boards reuse region hues, so borders must identify regions without colour aids.
            val borderWidth = REGION_BORDER_WIDTH_DP.dp.toPx()
            val row = cell.index / puzzle.size
            val column = cell.index % puzzle.size
            if (column > 0 && puzzle.regions[cell.index - 1] != regionId) {
                drawLine(colors.ink, bounds.topLeft, bounds.bottomLeft, borderWidth)
            }
            if (row > 0 && puzzle.regions[cell.index - puzzle.size] != regionId) {
                drawLine(colors.ink, bounds.topLeft, bounds.topRight, borderWidth)
            }
        },
        onCellTap = { cell ->
            val nextMark = CrownsBoardInput.nextMark(state.marks[cell.index])
            val moves = CrownsBoardInput.tapMoves(state, cell, options.autoXEnabled)
            if (nextMark == Mark.FILLED) haptics.placement()
            onMoves(moves)
        },
        onDragPaint = { path ->
            val moves = CrownsBoardInput.dragMoves(state, path)
            if (moves.isNotEmpty()) onMoves(moves)
        },
        onLongPress = { cell ->
            CrownsBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
        },
        onUndo = onUndo,
    )
}

private fun DrawScope.drawCross(bounds: Rect, color: Color) {
    val radius = minOf(bounds.width, bounds.height) * MARK_RADIUS_FRACTION
    val strokeWidth = minOf(bounds.width, bounds.height) * MARK_STROKE_FRACTION
    drawLine(
        color = color,
        start = Offset(bounds.center.x - radius, bounds.center.y - radius),
        end = Offset(bounds.center.x + radius, bounds.center.y + radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(bounds.center.x + radius, bounds.center.y - radius),
        end = Offset(bounds.center.x - radius, bounds.center.y + radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawCrown(bounds: Rect, color: Color) {
    val path = Path().apply {
        moveTo(bounds.left + bounds.width * 0.13f, bounds.bottom - bounds.height * 0.19f)
        lineTo(bounds.left + bounds.width * 0.05f, bounds.top + bounds.height * 0.25f)
        lineTo(bounds.left + bounds.width * 0.35f, bounds.top + bounds.height * 0.49f)
        lineTo(bounds.center.x, bounds.top + bounds.height * 0.12f)
        lineTo(bounds.left + bounds.width * 0.65f, bounds.top + bounds.height * 0.49f)
        lineTo(bounds.right - bounds.width * 0.05f, bounds.top + bounds.height * 0.25f)
        lineTo(bounds.right - bounds.width * 0.13f, bounds.bottom - bounds.height * 0.19f)
        close()
    }
    drawPath(path, color)
    drawLine(
        color = color,
        start = Offset(bounds.left + bounds.width * 0.18f, bounds.bottom - bounds.height * 0.13f),
        end = Offset(bounds.right - bounds.width * 0.18f, bounds.bottom - bounds.height * 0.13f),
        strokeWidth = minOf(bounds.width, bounds.height) * CROWN_BASE_STROKE_FRACTION,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawRegionHatch(
    pattern: HatchPattern,
    bounds: Rect,
    ink: Color,
) {
    val spacing = minOf(bounds.width, bounds.height) * HATCH_SPACING_FRACTION
    val color = ink.copy(alpha = HATCH_ALPHA)
    val strokeWidth = minOf(bounds.width, bounds.height) * HATCH_STROKE_FRACTION

    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
        when (pattern) {
            HatchPattern.DIAGONAL_UP -> drawDiagonalHatch(bounds, spacing, color, strokeWidth, up = true)
            HatchPattern.DIAGONAL_DOWN -> drawDiagonalHatch(bounds, spacing, color, strokeWidth, up = false)
            HatchPattern.HORIZONTAL -> drawHorizontalHatch(bounds, spacing, color, strokeWidth)
            HatchPattern.VERTICAL -> drawVerticalHatch(bounds, spacing, color, strokeWidth)
            HatchPattern.GRID -> {
                drawHorizontalHatch(bounds, spacing, color, strokeWidth)
                drawVerticalHatch(bounds, spacing, color, strokeWidth)
            }
            HatchPattern.DOTS -> drawDotHatch(bounds, spacing, color)
            HatchPattern.CROSS_DIAGONAL -> {
                drawDiagonalHatch(bounds, spacing, color, strokeWidth, up = true)
                drawDiagonalHatch(bounds, spacing, color, strokeWidth, up = false)
            }
            HatchPattern.ZIGZAG -> drawZigzagHatch(bounds, spacing, color, strokeWidth)
        }
    }
}

private fun DrawScope.drawDiagonalHatch(
    bounds: Rect,
    spacing: Float,
    color: Color,
    strokeWidth: Float,
    up: Boolean,
) {
    var startX = bounds.left - bounds.height
    while (startX < bounds.right) {
        val start = if (up) Offset(startX, bounds.bottom) else Offset(startX, bounds.top)
        val end = if (up) Offset(startX + bounds.height, bounds.top) else Offset(startX + bounds.height, bounds.bottom)
        drawLine(color, start, end, strokeWidth)
        startX += spacing
    }
}

private fun DrawScope.drawHorizontalHatch(
    bounds: Rect,
    spacing: Float,
    color: Color,
    strokeWidth: Float,
) {
    var y = bounds.top + spacing / 2f
    while (y < bounds.bottom) {
        drawLine(color, Offset(bounds.left, y), Offset(bounds.right, y), strokeWidth)
        y += spacing
    }
}

private fun DrawScope.drawVerticalHatch(
    bounds: Rect,
    spacing: Float,
    color: Color,
    strokeWidth: Float,
) {
    var x = bounds.left + spacing / 2f
    while (x < bounds.right) {
        drawLine(color, Offset(x, bounds.top), Offset(x, bounds.bottom), strokeWidth)
        x += spacing
    }
}

private fun DrawScope.drawDotHatch(
    bounds: Rect,
    spacing: Float,
    color: Color,
) {
    val radius = minOf(bounds.width, bounds.height) * HATCH_DOT_RADIUS_FRACTION
    var y = bounds.top + spacing / 2f
    while (y < bounds.bottom) {
        var x = bounds.left + spacing / 2f
        while (x < bounds.right) {
            drawCircle(color, radius, Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}

private fun DrawScope.drawZigzagHatch(
    bounds: Rect,
    spacing: Float,
    color: Color,
    strokeWidth: Float,
) {
    var rowTop = bounds.top + spacing / 2f
    while (rowTop < bounds.bottom) {
        val path = Path().apply {
            moveTo(bounds.left, rowTop)
            var x = bounds.left
            var y = rowTop
            var rises = true
            while (x < bounds.right) {
                x = (x + spacing / 2f).coerceAtMost(bounds.right)
                y = if (rises) rowTop - spacing / 4f else rowTop + spacing / 4f
                lineTo(x, y)
                rises = !rises
            }
        }
        drawPath(path, color, style = Stroke(strokeWidth))
        rowTop += spacing
    }
}

private const val REGION_LABEL_ALPHA = 0.42f
private const val REGION_BORDER_WIDTH_DP = 3f
private const val REGION_LABEL_SIZE_SP = 13
private const val HATCH_ALPHA = 0.20f
private const val HATCH_SPACING_FRACTION = 0.34f
private const val HATCH_STROKE_FRACTION = 0.035f
private const val HATCH_DOT_RADIUS_FRACTION = 0.025f
private const val MARK_RADIUS_FRACTION = 0.24f
private const val MARK_STROKE_FRACTION = 0.075f
private const val CROWN_BASE_STROKE_FRACTION = 0.08f
