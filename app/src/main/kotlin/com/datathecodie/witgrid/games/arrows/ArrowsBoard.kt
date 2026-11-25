package com.datathecodie.witgrid.games.arrows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsCodec
import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsPuzzle
import com.datathecodie.witgrid.engine.arrows.ArrowsRules
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun ArrowsBoard(
    state: ArrowsState,
    onAction: (ArrowsAction) -> Unit,
    onUndo: () -> Unit,
    options: ArrowsBoardOptions = LocalArrowsBoardOptions.current,
    modifier: Modifier = Modifier,
    playLayout: (@Composable (board: @Composable () -> Unit, controls: @Composable () -> Unit) -> Unit)? = null,
) {
    val colors = WitgridTheme.colors
    val haptics = rememberBoardHaptics(options.hapticsEnabled)
    val livesLeft = ArrowsBoardInput.livesLeft(state)
    val livesTotal = ArrowsRules.MAX_MISTAKES

    // A slide removes the arrow in one step. There is no travel animation, so reduce-motion
    // and full motion show the same board change. The solved wave still follows reduce motion.
    val boardContent: @Composable () -> Unit = {
        BoardCanvas(
            gridSize = state.puzzle.size,
            cellDescription = { cell -> describeArrowCell(state, cell) },
            solved = ArrowsRules.isWon(state.arrows),
            reduceMotion = options.reduceMotion,
            hapticsEnabled = options.hapticsEnabled,
            dragPaintEnabled = false,
            drawCell = { cell, bounds ->
                drawRect(colors.card, topLeft = bounds.topLeft, size = bounds.size)
                val direction = state.arrows[cell.index]
                val canSlide = ArrowsBoardInput.canSlideNow(state, cell.index)
                if (canSlide) {
                    drawRect(
                        color = colors.accent.copy(alpha = SLIDEABLE_FILL_ALPHA),
                        topLeft = bounds.topLeft,
                        size = bounds.size,
                    )
                }
                if (direction != null) {
                    drawArrow(
                        direction = direction,
                        bounds = bounds,
                        color = if (canSlide) colors.accent else colors.ink,
                    )
                }
            },
            onCellTap = { cell ->
                val result = ArrowsBoardInput.tapCell(state, cell.index)
                when (result.feedback) {
                    ArrowTapFeedback.SLID -> {
                        haptics.placement()
                        result.action?.let(onAction)
                    }
                    ArrowTapFeedback.BLOCKED -> {
                        haptics.mark()
                        result.action?.let(onAction)
                    }
                    ArrowTapFeedback.NONE -> Unit
                }
            },
            onDragPaint = {},
            onLongPress = {},
            onUndo = onUndo,
        )
    }
    val controlsContent: @Composable () -> Unit = {
        val livesDescription = stringResource(R.string.arrows_lives_accessibility, livesLeft, livesTotal)
        Text(
            text = stringResource(R.string.arrows_lives_left, livesLeft, livesTotal),
            modifier = Modifier
                .testTag("arrows-lives")
                .padding(bottom = Spacing.sm)
                .clearAndSetSemantics { contentDescription = livesDescription },
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    if (playLayout != null) {
        playLayout(boardContent, controlsContent)
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            controlsContent()
            boardContent()
        }
    }
}

@Composable
private fun describeArrowCell(state: ArrowsState, cell: Cell): String {
    val label = ArrowsBoardInput.cellLabel(state, cell.index)
    val direction = label.direction
    return if (direction == null) {
        stringResource(R.string.arrows_cell_empty_accessibility, label.row, label.column)
    } else {
        stringResource(
            R.string.arrows_cell_arrow_accessibility,
            label.row,
            label.column,
            stringResource(arrowsDirectionLabelResource(direction)),
            stringResource(arrowsSlideLabelResource(label.canSlide)),
        )
    }
}

internal fun arrowsDirectionLabelResource(direction: ArrowsDirection): Int = when (direction) {
    ArrowsDirection.UP -> R.string.arrows_direction_up
    ArrowsDirection.DOWN -> R.string.arrows_direction_down
    ArrowsDirection.LEFT -> R.string.arrows_direction_left
    ArrowsDirection.RIGHT -> R.string.arrows_direction_right
}

internal fun arrowsSlideLabelResource(canSlide: Boolean): Int =
    if (canSlide) R.string.arrows_can_slide else R.string.arrows_cannot_slide

private fun DrawScope.drawArrow(direction: ArrowsDirection, bounds: Rect, color: Color) {
    val minSide = minOf(bounds.width, bounds.height)
    val shaft = minSide * ARROW_SHAFT_FRACTION
    val stroke = minSide * ARROW_STROKE_FRACTION
    val center = bounds.center
    val forwardX = direction.columnDelta.toFloat()
    val forwardY = direction.rowDelta.toFloat()
    val head = Offset(center.x + forwardX * shaft, center.y + forwardY * shaft)
    val tail = Offset(center.x - forwardX * shaft, center.y - forwardY * shaft)
    drawLine(color = color, start = tail, end = head, strokeWidth = stroke, cap = StrokeCap.Round)
    val wing = shaft * ARROW_WING_FRACTION
    val sideX = -forwardY
    val sideY = forwardX
    val neck = Offset(head.x - forwardX * wing, head.y - forwardY * wing)
    drawLine(
        color = color,
        start = head,
        end = Offset(neck.x + sideX * wing, neck.y + sideY * wing),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = head,
        end = Offset(neck.x - sideX * wing, neck.y - sideY * wing),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun ArrowsBoardPreview() {
    WitgridTheme(darkTheme = false) {
        ArrowsBoard(
            state = previewState(),
            onAction = {},
            onUndo = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

private fun previewState(): ArrowsState {
    val puzzle = ArrowsPuzzle(
        bankId = "ar-preview",
        size = 5,
        pack = Pack.EASY,
        difficulty = "d1",
        initialArrows = ArrowsCodec.decodeLayout(PREVIEW_LAYOUT, 5),
    )
    return ArrowsEngine.initialState(puzzle)
}

private const val PREVIEW_LAYOUT =
    "..^.." +
        "..^.." +
        "..>.v" +
        "....." +
        "....."

private const val ARROW_SHAFT_FRACTION = 0.26f
private const val ARROW_STROKE_FRACTION = 0.07f
private const val ARROW_WING_FRACTION = 0.62f
private const val SLIDEABLE_FILL_ALPHA = 0.14f
