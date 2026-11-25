package com.datathecodie.witgrid.games.coloursort

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortPuzzle
import com.datathecodie.witgrid.engine.coloursort.ColourSortRules
import com.datathecodie.witgrid.engine.coloursort.ColourSortState
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.HatchPattern
import com.datathecodie.witgrid.ui.theme.RegionStyle
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridColors
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun ColourSortBoard(
    state: ColourSortState,
    onAction: (ColourSortAction) -> Unit,
    onUndo: () -> Unit,
    options: ColourSortBoardOptions = LocalColourSortBoardOptions.current,
    modifier: Modifier = Modifier,
) {
    var selectedTube by remember(state.puzzle.bankId) { mutableStateOf<Int?>(null) }
    val haptics = rememberBoardHaptics(options.hapticsEnabled)
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()

    // Clear stale selection if undo, restart, or Continue alters the selected tube out from under it.
    LaunchedEffect(state.tubes) {
        val current = selectedTube
        if (current != null && (current !in state.tubes.indices || state.tubes[current].isEmpty())) {
            selectedTube = null
        }
    }

    val containerModifier = modifier
        .fillMaxWidth()
        .twoFingerUndoGesture(hapticsEnabled = options.hapticsEnabled, onUndo = onUndo)

    BoxWithConstraints(
        modifier = containerModifier,
        contentAlignment = Alignment.Center,
    ) {
        val boundedHeight = constraints.hasBoundedHeight
        val layoutWidthDp = maxWidth.value
        val layoutHeightDp = if (boundedHeight) maxHeight.value else maxWidth.value

        val metrics = remember(state.tubes.size, layoutWidthDp, layoutHeightDp) {
            ColourSortBoardLayout.calculate(
                tubeCount = state.tubes.size,
                availableWidthDp = layoutWidthDp,
                availableHeightDp = layoutHeightDp,
            )
        }

        val boardBoxModifier = if (boundedHeight) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().aspectRatio(1f)
        }

        Box(
            modifier = boardBoxModifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(metrics.verticalGapDp.dp),
            ) {
                metrics.rows.forEach { rowIndices ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalGapDp.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        rowIndices.forEach { tubeIndex ->
                            val tube = state.tubes[tubeIndex]
                            val isSelected = selectedTube == tubeIndex
                            val isLegalDest = ColourSortBoardInput.isLegalDestination(state, selectedTube, tubeIndex)
                            val description = describeTube(tubeIndex, tube, isSelected)

                            TubeItem(
                                tubeIndex = tubeIndex,
                                tube = tube,
                                isSelected = isSelected,
                                isLegalDestination = isLegalDest,
                                metrics = metrics,
                                options = options,
                                colors = colors,
                                textMeasurer = textMeasurer,
                                contentDescription = description,
                                onClick = {
                                    val result = ColourSortBoardInput.tapTube(state, selectedTube, tubeIndex)
                                    selectedTube = result.selectedTube
                                    when (result.feedback) {
                                        TapFeedback.SELECTED, TapFeedback.DESELECTED -> haptics.mark()
                                        TapFeedback.POURED -> {
                                            haptics.placement()
                                            result.action?.let(onAction)
                                        }
                                        TapFeedback.NONE -> Unit
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TubeItem(
    tubeIndex: Int,
    tube: List<Int>,
    isSelected: Boolean,
    isLegalDestination: Boolean,
    metrics: TubeGridMetrics,
    options: ColourSortBoardOptions,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val targetLift = if (isSelected) -metrics.selectionLiftDp.dp else 0.dp
    val liftOffset by animateDpAsState(
        targetValue = targetLift,
        animationSpec = tween(durationMillis = if (options.reduceMotion) 0 else LIFT_ANIMATION_MILLIS),
        label = "tube-lift",
    )
    val isSortedFull = ColourSortBoardInput.isTubeSortedAndFull(tube)
    val labelStyle = TextStyle(
        color = colors.ink,
        fontSize = COLOUR_LABEL_SP.sp,
        fontWeight = FontWeight.SemiBold,
    )

    Box(
        modifier = Modifier
            .testTag("coloursort-tube-$tubeIndex")
            .width(metrics.tubeWidthDp.dp)
            .height((metrics.tubeHeightDp + metrics.selectionLiftDp).dp)
            .sizeIn(minWidth = Spacing.minTouchTarget, minHeight = Spacing.minTouchTarget)
            .semantics {
                this.selected = isSelected
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(
            modifier = Modifier
                .offset(y = liftOffset)
                .size(width = metrics.tubeWidthDp.dp, height = metrics.tubeHeightDp.dp),
        ) {
            drawTube(
                tube = tube,
                isSelected = isSelected,
                isLegalDestination = isLegalDestination,
                isSortedFull = isSortedFull,
                showColourAids = options.showColourAids,
                colors = colors,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
            )
        }
    }
}

@Composable
private fun describeTube(tubeIndex: Int, tube: List<Int>, isSelected: Boolean): String {
    val number = tubeIndex + 1
    val base = if (tube.isEmpty()) {
        stringResource(R.string.coloursort_tube_empty_accessibility, number)
    } else {
        // Bottom-to-top order matches how pours work: only the top run moves.
        val colours = tube.joinToString(separator = " ") { colour ->
            ColourSortBoardInput.colourLetter(colour).toString()
        }
        stringResource(R.string.coloursort_tube_accessibility, number, colours)
    }
    return if (isSelected) stringResource(R.string.coloursort_tube_selected_accessibility, base) else base
}

private fun DrawScope.drawTube(
    tube: List<Int>,
    isSelected: Boolean,
    isLegalDestination: Boolean,
    isSortedFull: Boolean,
    showColourAids: Boolean,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val rimOverhang = LIP_OVERHANG_DP.dp.toPx()
    val indicatorReserve = INDICATOR_SPACE_DP.dp.toPx()
    val bodyLeft = rimOverhang
    val bodyRight = size.width - rimOverhang
    val bodyWidth = (bodyRight - bodyLeft).coerceAtLeast(1f)
    val bodyTop = indicatorReserve
    val bodyBottom = size.height - 2.dp.toPx()
    val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(1f)
    val bottomRadius = bodyWidth * BOTTOM_RADIUS_FRACTION

    val tubePath = Path().apply {
        addRoundRect(
            RoundRect(
                left = bodyLeft,
                top = bodyTop,
                right = bodyRight,
                bottom = bodyBottom,
                topLeftCornerRadius = CornerRadius(TOP_CORNER_RADIUS_DP.dp.toPx()),
                topRightCornerRadius = CornerRadius(TOP_CORNER_RADIUS_DP.dp.toPx()),
                bottomLeftCornerRadius = CornerRadius(bottomRadius),
                bottomRightCornerRadius = CornerRadius(bottomRadius),
            ),
        )
    }

    drawPath(path = tubePath, color = colors.card)

    val headroom = bodyHeight * HEADROOM_FRACTION
    val usableHeight = (bodyHeight - headroom).coerceAtLeast(1f)
    val slotHeight = usableHeight / ColourSortRules.TUBE_CAPACITY

    clipPath(tubePath) {
        for (slot in tube.indices) {
            val colour = tube[slot]
            val style = styleForColour(colour, colors)
            val slotBottom = bodyBottom - slot * slotHeight
            val slotTop = slotBottom - slotHeight
            val slotRect = Rect(bodyLeft, slotTop, bodyRight, slotBottom)

            drawRect(color = style.color, topLeft = slotRect.topLeft, size = slotRect.size)

            if (showColourAids) {
                drawHatchPattern(style.hatch, slotRect, colors.ink)
                val letter = ColourSortBoardInput.colourLetter(colour).toString()
                val textLayout = textMeasurer.measure(AnnotatedString(letter), style = labelStyle)
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        x = slotRect.center.x - textLayout.size.width / 2f,
                        y = slotRect.center.y - textLayout.size.height / 2f,
                    ),
                )
            }

            if (slot > 0) {
                val sameAsBelow = tube[slot - 1] == colour
                val dividerAlpha = if (sameAsBelow) SAME_COLOUR_DIVIDER_ALPHA else DIFF_COLOUR_DIVIDER_ALPHA
                val dividerWidth = if (sameAsBelow) 0.75.dp.toPx() else 1.25.dp.toPx()
                drawLine(
                    color = colors.ink.copy(alpha = dividerAlpha),
                    start = Offset(bodyLeft, slotBottom),
                    end = Offset(bodyRight, slotBottom),
                    strokeWidth = dividerWidth,
                )
            }
        }
    }

    val outlineColor = when {
        isSelected -> colors.accent
        isLegalDestination -> colors.accent.copy(alpha = DESTINATION_OUTLINE_ALPHA)
        isSortedFull -> colors.ink
        else -> colors.ink.copy(alpha = IDLE_OUTLINE_ALPHA)
    }
    val outlineWidth = when {
        isSelected -> SELECTED_STROKE_DP.dp.toPx()
        isLegalDestination || isSortedFull -> EMPHASIS_STROKE_DP.dp.toPx()
        else -> IDLE_STROKE_DP.dp.toPx()
    }

    drawPath(
        path = tubePath,
        color = outlineColor,
        style = Stroke(width = outlineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Draw the paper-and-ink lip across the tube mouth.
    drawLine(
        color = outlineColor,
        start = Offset(bodyLeft - rimOverhang, bodyTop),
        end = Offset(bodyRight + rimOverhang, bodyTop),
        strokeWidth = outlineWidth,
        cap = StrokeCap.Round,
    )

    if (isLegalDestination) {
        val markerRadius = DESTINATION_DOT_RADIUS_DP.dp.toPx()
        drawCircle(
            color = colors.accent,
            radius = markerRadius,
            center = Offset(size.width / 2f, indicatorReserve * 0.45f),
        )
    } else if (isSortedFull) {
        drawSortedCheckmark(
            center = Offset(size.width / 2f, indicatorReserve * 0.45f),
            radius = SORTED_MARK_RADIUS_DP.dp.toPx(),
            color = colors.ink,
        )
    }
}

private fun DrawScope.drawSortedCheckmark(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - radius * 0.8f, center.y)
        lineTo(center.x - radius * 0.2f, center.y + radius * 0.6f)
        lineTo(center.x + radius * 0.85f, center.y - radius * 0.65f)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun styleForColour(colour: Int, colors: WitgridColors): RegionStyle {
    val index = colour - 1
    return if (index in colors.regions.indices) {
        colors.regions[index]
    } else {
        RegionStyle(
            color = colors.accent,
            letter = ColourSortBoardInput.colourLetter(colour),
            hatch = HatchPattern.entries[index % HatchPattern.entries.size],
        )
    }
}

private fun DrawScope.drawHatchPattern(pattern: HatchPattern, bounds: Rect, ink: Color) {
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

private fun DrawScope.drawHorizontalHatch(bounds: Rect, spacing: Float, color: Color, strokeWidth: Float) {
    var y = bounds.top + spacing / 2f
    while (y < bounds.bottom) {
        drawLine(color, Offset(bounds.left, y), Offset(bounds.right, y), strokeWidth)
        y += spacing
    }
}

private fun DrawScope.drawVerticalHatch(bounds: Rect, spacing: Float, color: Color, strokeWidth: Float) {
    var x = bounds.left + spacing / 2f
    while (x < bounds.right) {
        drawLine(color, Offset(x, bounds.top), Offset(x, bounds.bottom), strokeWidth)
        x += spacing
    }
}

private fun DrawScope.drawDotHatch(bounds: Rect, spacing: Float, color: Color) {
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

private fun DrawScope.drawZigzagHatch(bounds: Rect, spacing: Float, color: Color, strokeWidth: Float) {
    var rowTop = bounds.top + spacing / 2f
    while (rowTop < bounds.bottom) {
        val path = Path().apply {
            moveTo(bounds.left, rowTop)
            var x = bounds.left
            var rises = true
            while (x < bounds.right) {
                x = (x + spacing / 2f).coerceAtMost(bounds.right)
                val y = if (rises) rowTop - spacing / 4f else rowTop + spacing / 4f
                lineTo(x, y)
                rises = !rises
            }
        }
        drawPath(path, color, style = Stroke(strokeWidth))
        rowTop += spacing
    }
}

@Composable
private fun Modifier.twoFingerUndoGesture(
    hapticsEnabled: Boolean,
    onUndo: () -> Unit,
): Modifier {
    val haptics = rememberBoardHaptics(hapticsEnabled)
    val viewConfiguration = LocalViewConfiguration.current
    val latestOnUndo = rememberUpdatedState(onUndo)

    return pointerInput(haptics, viewConfiguration) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downTime = firstDown.uptimeMillis
            val initialPositions = mutableMapOf(firstDown.id to firstDown.position)
            var sawMultiTouch = false
            var moved = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val active = event.changes.filter { it.pressed }
                event.changes.forEach { change ->
                    val start = initialPositions.getOrPut(change.id) { change.position }
                    if ((change.position - start).getDistance() > viewConfiguration.touchSlop) {
                        moved = true
                    }
                }
                if (active.size >= 2 && !moved) {
                    sawMultiTouch = true
                }
                if (sawMultiTouch) {
                    event.changes.forEach(PointerInputChange::consume)
                }
                if (active.isEmpty()) {
                    val elapsed = (event.changes.maxOfOrNull { it.uptimeMillis } ?: downTime) - downTime
                    if (sawMultiTouch && !moved && elapsed <= viewConfiguration.longPressTimeoutMillis) {
                        haptics.undo()
                        latestOnUndo.value()
                    }
                    break
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun ColourSortBoardLightPreview() {
    WitgridTheme(darkTheme = false) {
        ColourSortBoard(
            state = previewState(),
            onAction = {},
            onUndo = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun ColourSortBoardDarkAidsPreview() {
    WitgridTheme(darkTheme = true) {
        ColourSortBoard(
            state = previewState(),
            onAction = {},
            onUndo = {},
            options = ColourSortBoardOptions(showColourAids = true),
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

private fun previewState(): ColourSortState {
    val tubes = listOf(
        listOf(1, 2, 3, 4),
        listOf(2, 3, 4, 1),
        listOf(3, 4, 1, 2),
        listOf(4, 1, 2, 3),
        listOf(1, 1, 1, 1),
        emptyList(),
    )
    return ColourSortState(
        puzzle = ColourSortPuzzle("cs-preview", Pack.MEDIUM, "d2", tubes),
        tubes = tubes,
        pourCount = 1,
    )
}

private const val LIFT_ANIMATION_MILLIS = 140
private const val COLOUR_LABEL_SP = 12
private const val LIP_OVERHANG_DP = 3f
private const val INDICATOR_SPACE_DP = 10f
private const val TOP_CORNER_RADIUS_DP = 3f
private const val BOTTOM_RADIUS_FRACTION = 0.42f
private const val HEADROOM_FRACTION = 0.08f
private const val SAME_COLOUR_DIVIDER_ALPHA = 0.14f
private const val DIFF_COLOUR_DIVIDER_ALPHA = 0.35f
private const val DESTINATION_OUTLINE_ALPHA = 0.72f
private const val IDLE_OUTLINE_ALPHA = 0.58f
private const val SELECTED_STROKE_DP = 2.5f
private const val EMPHASIS_STROKE_DP = 2f
private const val IDLE_STROKE_DP = 1.5f
private const val DESTINATION_DOT_RADIUS_DP = 3.5f
private const val SORTED_MARK_RADIUS_DP = 4f
private const val HATCH_ALPHA = 0.22f
private const val HATCH_SPACING_FRACTION = 0.36f
private const val HATCH_STROKE_FRACTION = 0.055f
private const val HATCH_DOT_RADIUS_FRACTION = 0.045f
