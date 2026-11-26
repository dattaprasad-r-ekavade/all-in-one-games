package com.datathecodie.witgrid.games.klondike

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.klondike.Card
import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikePuzzle
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.klondike.Suit
import com.datathecodie.witgrid.ui.board.rememberBoardHaptics
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridColors
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun KlondikeBoard(
    state: KlondikeState,
    onAction: (CardMove) -> Unit,
    onUndo: () -> Unit,
    options: KlondikeBoardOptions = LocalKlondikeBoardOptions.current,
    modifier: Modifier = Modifier,
) {
    var selection by remember(state.puzzle.bankId, state.puzzle.seed) { mutableStateOf<KlondikeSelection?>(null) }
    val haptics = rememberBoardHaptics(options.hapticsEnabled)
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()

    // Undo, restart, and Continue replace the piles under the selection.
    LaunchedEffect(state.tableau, state.waste, state.foundations, state.stock, state.moveCount) {
        selection = KlondikeBoardInput.activeSelection(state, selection)
    }

    val onHit: (KlondikeHit) -> Unit = { hit ->
        val result = KlondikeBoardInput.tap(state, selection, hit)
        selection = result.selection
        when (result.feedback) {
            KlondikeTapFeedback.SELECTED, KlondikeTapFeedback.DESELECTED -> haptics.mark()
            KlondikeTapFeedback.MOVED -> {
                haptics.placement()
                result.action?.let(onAction)
            }
            KlondikeTapFeedback.NONE -> Unit
        }
    }

    // Card moves replace the board in one step. There is no travel animation,
    // so reduce motion and full motion draw the same end position.
    val container = modifier
        .fillMaxWidth()
        .twoFingerUndoGesture(hapticsEnabled = options.hapticsEnabled, onUndo = onUndo)

    BoxWithConstraints(modifier = container, contentAlignment = Alignment.TopCenter) {
        val boundedHeight = constraints.hasBoundedHeight
        val layoutWidthDp = maxWidth.value
        val layoutHeightDp = if (boundedHeight) maxHeight.value else maxWidth.value * PORTRAIT_HEIGHT_RATIO
        val tallest = state.tableau.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        val moveDurationMillis = klondikeMoveDurationMillis(options.reduceMotion)
        val metrics = remember(layoutWidthDp, layoutHeightDp, tallest, moveDurationMillis) {
            KlondikeBoardLayout.calculate(layoutWidthDp, layoutHeightDp, tallest)
        }
        val boxModifier = if (boundedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()

        Box(modifier = boxModifier, contentAlignment = Alignment.TopCenter) {
            Column(verticalArrangement = Arrangement.spacedBy(metrics.rowGapDp.dp)) {
                TopRow(
                    state = state,
                    selection = selection,
                    metrics = metrics,
                    colors = colors,
                    textMeasurer = textMeasurer,
                    onHit = onHit,
                )
                TableauRow(
                    state = state,
                    selection = selection,
                    metrics = metrics,
                    colors = colors,
                    textMeasurer = textMeasurer,
                    onHit = onHit,
                )
            }
        }
    }
}

@Composable
private fun TopRow(
    state: KlondikeState,
    selection: KlondikeSelection?,
    metrics: KlondikeBoardMetrics,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
    onHit: (KlondikeHit) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(metrics.columnGapDp.dp)) {
        PileSlot(
            pile = KlondikePile.Stock,
            state = state,
            selection = selection,
            metrics = metrics,
            colors = colors,
            textMeasurer = textMeasurer,
            testTag = "klondike-stock",
            onHit = onHit,
        ) { legalDest ->
            val top = state.stock.lastOrNull()
            if (top == null) {
                drawEmptySlot(colors, suit = null, legalDest = legalDest)
            } else {
                drawCardFace(top, selected = false, legalDest = legalDest, colors = colors, textMeasurer = textMeasurer)
            }
        }
        PileSlot(
            pile = KlondikePile.Waste,
            state = state,
            selection = selection,
            metrics = metrics,
            colors = colors,
            textMeasurer = textMeasurer,
            testTag = "klondike-waste",
            onHit = onHit,
        ) { legalDest ->
            val top = state.waste.lastOrNull()
            val selectedWaste = selection is KlondikeSelection.Waste
            if (top == null) {
                drawEmptySlot(colors, suit = null, legalDest = legalDest)
            } else {
                drawCardFace(top, selectedWaste, legalDest, colors, textMeasurer)
            }
        }
        Spacer(modifier = Modifier.size(width = metrics.cardWidthDp.dp, height = metrics.cardHeightDp.dp))
        for (suit in Suit.entries) {
            PileSlot(
                pile = KlondikePile.Foundation(suit),
                state = state,
                selection = selection,
                metrics = metrics,
                colors = colors,
                textMeasurer = textMeasurer,
                testTag = "klondike-foundation-${suit.name.lowercase()}",
                onHit = onHit,
            ) { legalDest ->
                val top = state.foundations[suit].orEmpty().lastOrNull()
                val selectedSuit = selection is KlondikeSelection.Foundation && selection.suit == suit
                if (top == null) {
                    drawEmptySlot(colors, suit = suit, legalDest = legalDest)
                } else {
                    drawCardFace(top, selectedSuit, legalDest, colors, textMeasurer)
                }
            }
        }
    }
}

@Composable
private fun TableauRow(
    state: KlondikeState,
    selection: KlondikeSelection?,
    metrics: KlondikeBoardMetrics,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
    onHit: (KlondikeHit) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.columnGapDp.dp),
        verticalAlignment = Alignment.Top,
    ) {
        for (column in 0 until KlondikeBoardLayout.COLUMN_COUNT) {
            TableauColumn(
                column = column,
                state = state,
                selection = selection,
                metrics = metrics,
                colors = colors,
                textMeasurer = textMeasurer,
                onHit = onHit,
            )
        }
    }
}

@Composable
private fun PileSlot(
    pile: KlondikePile,
    state: KlondikeState,
    selection: KlondikeSelection?,
    metrics: KlondikeBoardMetrics,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
    testTag: String,
    onHit: (KlondikeHit) -> Unit,
    draw: DrawScope.(legalDest: Boolean) -> Unit,
) {
    val description = describePile(state, pile, selection)
    val label = KlondikeBoardInput.pileLabel(state, pile, selection)
    val legalDest = KlondikeBoardInput.isLegalDestination(state, selection, KlondikeBoardInput.accessibilityHit(state, pile))
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(width = metrics.cardWidthDp.dp, height = metrics.cardHeightDp.dp)
            .semantics {
                selected = label.selected
                contentDescription = description
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = { onHit(KlondikeBoardInput.accessibilityHit(state, pile)) },
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            draw(legalDest)
        }
    }
}

@Composable
private fun TableauColumn(
    column: Int,
    state: KlondikeState,
    selection: KlondikeSelection?,
    metrics: KlondikeBoardMetrics,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
    onHit: (KlondikeHit) -> Unit,
) {
    val pile = state.tableau[column]
    val geometry = KlondikeBoardLayout.columnGeometry(pile, metrics)
    val pileId = KlondikePile.Tableau(column)
    val description = describePile(state, pileId, selection)
    val label = KlondikeBoardInput.pileLabel(state, pileId, selection)
    val legalDest = KlondikeBoardInput.isLegalDestination(
        state,
        selection,
        KlondikeHit.Tableau(column, null),
    )
    val run = selection as? KlondikeSelection.Tableau
    Box(
        modifier = Modifier
            .testTag("klondike-column-$column")
            .width(metrics.cardWidthDp.dp)
            .height(geometry.columnHeightDp.dp)
            .semantics {
                role = Role.Button
                selected = label.selected
                contentDescription = description
                onClick {
                    onHit(KlondikeBoardInput.accessibilityHit(state, pileId))
                    true
                }
            }
            .pointerInput(state, selection, geometry) {
                detectTapGestures { offset ->
                    val yDp = offset.y.toDp().value
                    if (!KlondikeBoardLayout.tapInsideColumn(geometry, yDp)) return@detectTapGestures
                    val index = if (pile.isEmpty()) null else KlondikeBoardLayout.cardIndexAt(geometry, yDp)
                    onHit(KlondikeHit.Tableau(column, index))
                }
            },
    ) {
        if (pile.isEmpty()) {
            Canvas(modifier = Modifier.size(metrics.cardWidthDp.dp, metrics.cardHeightDp.dp)) {
                drawEmptySlot(colors, suit = null, legalDest = legalDest)
            }
        } else {
            pile.forEachIndexed { index, card ->
                val inRun = run != null && run.column == column && index >= run.cardIndex
                val showDest = legalDest && index == pile.lastIndex
                Canvas(
                    modifier = Modifier
                        .offset(y = geometry.cardTopsDp[index].dp)
                        .size(metrics.cardWidthDp.dp, metrics.cardHeightDp.dp),
                ) {
                    drawCardFace(card, selected = inRun, legalDest = showDest, colors = colors, textMeasurer = textMeasurer)
                }
            }
        }
    }
}

@Composable
private fun describePile(state: KlondikeState, pile: KlondikePile, selection: KlondikeSelection?): String {
    val label = KlondikeBoardInput.pileLabel(state, pile, selection)
    val top = describeTop(label.topCard)
    val base = when (pile) {
        KlondikePile.Stock -> stringResource(R.string.klondike_stock_accessibility, top, label.faceDownCount)
        KlondikePile.Waste -> stringResource(R.string.klondike_waste_accessibility, top, label.faceDownCount)
        is KlondikePile.Foundation -> stringResource(foundationLabelRes(pile.suit), top, label.faceDownCount)
        is KlondikePile.Tableau -> stringResource(
            R.string.klondike_column_accessibility,
            pile.column + 1,
            top,
            label.faceDownCount,
        )
    }
    return if (label.selected) stringResource(R.string.klondike_pile_selected_accessibility, base) else base
}

@Composable
private fun describeTop(card: Card?): String = when (val speech = KlondikeBoardInput.topCardSpeech(card)) {
    KlondikeTopSpeech.Empty -> stringResource(R.string.klondike_pile_empty)
    KlondikeTopSpeech.FaceDown -> stringResource(R.string.klondike_card_face_down)
    is KlondikeTopSpeech.FaceUp -> stringResource(
        R.string.klondike_card_accessibility,
        stringResource(rankLabelRes(speech.card.rank)),
        stringResource(suitLabelRes(speech.card.suit)),
    )
}

internal fun foundationLabelRes(suit: Suit): Int = when (suit) {
    Suit.CLUBS -> R.string.klondike_foundation_clubs_accessibility
    Suit.DIAMONDS -> R.string.klondike_foundation_diamonds_accessibility
    Suit.HEARTS -> R.string.klondike_foundation_hearts_accessibility
    Suit.SPADES -> R.string.klondike_foundation_spades_accessibility
}

internal fun suitLabelRes(suit: Suit): Int = when (suit) {
    Suit.CLUBS -> R.string.klondike_suit_clubs
    Suit.DIAMONDS -> R.string.klondike_suit_diamonds
    Suit.HEARTS -> R.string.klondike_suit_hearts
    Suit.SPADES -> R.string.klondike_suit_spades
}

internal fun rankLabelRes(rank: Int): Int = when (rank) {
    1 -> R.string.klondike_rank_ace
    2 -> R.string.klondike_rank_2
    3 -> R.string.klondike_rank_3
    4 -> R.string.klondike_rank_4
    5 -> R.string.klondike_rank_5
    6 -> R.string.klondike_rank_6
    7 -> R.string.klondike_rank_7
    8 -> R.string.klondike_rank_8
    9 -> R.string.klondike_rank_9
    10 -> R.string.klondike_rank_10
    11 -> R.string.klondike_rank_jack
    12 -> R.string.klondike_rank_queen
    13 -> R.string.klondike_rank_king
    else -> throw IllegalArgumentException("Rank must be 1..13, got $rank")
}

private fun DrawScope.drawCardFace(
    card: Card,
    selected: Boolean,
    legalDest: Boolean,
    colors: WitgridColors,
    textMeasurer: TextMeasurer,
) {
    if (!card.faceUp) {
        drawFaceDown(colors, selected, legalDest)
        return
    }
    val radius = 4.dp.toPx()
    val bounds = Path().apply {
        addRoundRect(
            RoundRect(
                0f,
                0f,
                size.width,
                size.height,
                CornerRadius(radius),
            ),
        )
    }
    drawPath(bounds, colors.card)
    val ink = suitInk(card, colors)
    val rank = card.rankEnum.symbol
    val fontPx = size.width * if (rank.length == 1) 0.42f else 0.32f
    val style = NumberStyles.cell.copy(
        color = ink,
        fontSize = fontPx.toSp(),
        lineHeight = fontPx.toSp(),
        fontFeatureSettings = TABULAR_NUMERALS,
    )
    val text = textMeasurer.measure(AnnotatedString(rank), style = style)
    val pad = 3.dp.toPx()
    drawText(text, topLeft = Offset(pad, pad))
    val cornerMark = minOf(size.width, size.height) * 0.09f
    drawSuitMark(
        suit = card.suit,
        center = Offset(pad + text.size.width / 2f, pad + text.size.height + cornerMark),
        radius = cornerMark,
        color = ink,
    )
    val centerRadius = minOf(size.width, size.height) * 0.16f
    val center = Offset(size.width * 0.62f, size.height * 0.62f)
    if (center.y - centerRadius > pad + text.size.height + cornerMark * 2f) {
        drawSuitMark(card.suit, center, centerRadius, ink)
    }
    drawCardOutline(bounds, selected, legalDest, colors)
}

private fun DrawScope.drawFaceDown(colors: WitgridColors, selected: Boolean, legalDest: Boolean) {
    val radius = 4.dp.toPx()
    val bounds = Path().apply {
        addRoundRect(
            RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)),
        )
    }
    drawPath(bounds, colors.card)
    val inset = minOf(size.width, size.height) * 0.12f
    val inner = Path().apply {
        addRoundRect(
            RoundRect(
                inset,
                inset,
                size.width - inset,
                size.height - inset,
                CornerRadius(radius * 0.6f),
            ),
        )
    }
    val hatch = colors.mutedInk.copy(alpha = FACE_DOWN_HATCH_ALPHA)
    val stroke = 1.dp.toPx()
    clipPath(inner) {
        var start = -size.height
        val step = minOf(size.width, size.height) * 0.28f
        while (start < size.width) {
            drawLine(hatch, Offset(start, size.height), Offset(start + size.height, 0f), stroke)
            start += step
        }
    }
    drawPath(inner, colors.mutedInk, style = Stroke(width = stroke))
    drawCardOutline(bounds, selected, legalDest, colors)
}

private fun DrawScope.drawEmptySlot(colors: WitgridColors, suit: Suit?, legalDest: Boolean) {
    val radius = 4.dp.toPx()
    val color = if (legalDest) colors.accent else colors.cardEdge
    drawRoundRect(
        color = color,
        style = Stroke(width = if (legalDest) 2.dp.toPx() else 1.25.dp.toPx()),
        cornerRadius = CornerRadius(radius),
    )
    if (suit != null) {
        drawSuitMark(
            suit = suit,
            center = Offset(size.width / 2f, size.height / 2f),
            radius = minOf(size.width, size.height) * 0.14f,
            color = colors.mutedInk,
        )
    }
}

private fun DrawScope.drawCardOutline(bounds: Path, selected: Boolean, legalDest: Boolean, colors: WitgridColors) {
    val color = when {
        selected -> colors.accent
        legalDest -> colors.accent.copy(alpha = DESTINATION_OUTLINE_ALPHA)
        else -> colors.cardEdge
    }
    val width = when {
        selected -> 2.5.dp.toPx()
        legalDest -> 2.dp.toPx()
        else -> 1.dp.toPx()
    }
    drawPath(bounds, color, style = Stroke(width = width))
}

private fun suitInk(card: Card, colors: WitgridColors): Color {
    if (!card.isRed) return colors.ink
    val rgb = klondikeRedSuitRgb(colors.isDark)
    return Color(0xFF000000.toInt() or rgb)
}

private fun DrawScope.drawSuitMark(suit: Suit, center: Offset, radius: Float, color: Color) {
    when (suit) {
        Suit.HEARTS -> drawHeart(center, radius, color)
        Suit.DIAMONDS -> drawDiamond(center, radius, color)
        Suit.CLUBS -> drawClub(center, radius, color)
        Suit.SPADES -> drawSpade(center, radius, color)
    }
}

private fun DrawScope.drawDiamond(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.72f, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.72f, center.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawHeart(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + radius * 0.9f)
        cubicTo(
            center.x - radius * 1.5f,
            center.y + radius * 0.15f,
            center.x - radius * 0.95f,
            center.y - radius,
            center.x,
            center.y - radius * 0.28f,
        )
        cubicTo(
            center.x + radius * 0.95f,
            center.y - radius,
            center.x + radius * 1.5f,
            center.y + radius * 0.15f,
            center.x,
            center.y + radius * 0.9f,
        )
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawClub(center: Offset, radius: Float, color: Color) {
    val lobe = radius * 0.48f
    drawCircle(color, lobe, Offset(center.x, center.y - lobe * 0.85f))
    drawCircle(color, lobe, Offset(center.x - lobe * 0.9f, center.y + lobe * 0.35f))
    drawCircle(color, lobe, Offset(center.x + lobe * 0.9f, center.y + lobe * 0.35f))
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - radius * 0.12f, center.y + radius * 0.35f),
        size = Size(radius * 0.24f, radius * 0.55f),
        cornerRadius = CornerRadius(radius * 0.08f),
    )
}

private fun DrawScope.drawSpade(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(
            center.x - radius * 1.4f,
            center.y - radius * 0.1f,
            center.x - radius * 1.15f,
            center.y + radius * 0.85f,
            center.x,
            center.y + radius * 0.2f,
        )
        cubicTo(
            center.x + radius * 1.15f,
            center.y + radius * 0.85f,
            center.x + radius * 1.4f,
            center.y - radius * 0.1f,
            center.x,
            center.y - radius,
        )
        close()
    }
    drawPath(path, color)
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - radius * 0.12f, center.y + radius * 0.15f),
        size = Size(radius * 0.24f, radius * 0.6f),
        cornerRadius = CornerRadius(radius * 0.08f),
    )
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun KlondikeBoardPreview() {
    WitgridTheme(darkTheme = false) {
        KlondikeBoard(
            state = previewState(),
            onAction = {},
            onUndo = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

private fun previewState(): KlondikeState {
    fun card(rank: Int, suit: Suit, faceUp: Boolean = true) = Card(rank, suit, faceUp)
    val foundations = Suit.entries.associateWith { emptyList<Card>() }.toMutableMap()
    foundations[Suit.HEARTS] = listOf(card(1, Suit.HEARTS))
    return KlondikeState(
        puzzle = KlondikePuzzle(bankId = "kl-preview", pack = Pack.EASY, difficulty = "d1", seed = 1L),
        tableau = listOf(
            listOf(card(13, Suit.SPADES, false), card(12, Suit.HEARTS), card(11, Suit.CLUBS)),
            listOf(card(1, Suit.DIAMONDS)),
            listOf(card(5, Suit.CLUBS, false), card(4, Suit.HEARTS)),
            emptyList(),
            listOf(card(9, Suit.SPADES)),
            listOf(card(7, Suit.DIAMONDS, false), card(6, Suit.CLUBS)),
            listOf(card(3, Suit.HEARTS)),
        ),
        foundations = foundations,
        stock = listOf(card(2, Suit.CLUBS, false), card(8, Suit.SPADES, false)),
        waste = listOf(card(9, Suit.HEARTS)),
    )
}

private const val PORTRAIT_HEIGHT_RATIO = 1.7f
private const val TABULAR_NUMERALS = "tnum"
private const val DESTINATION_OUTLINE_ALPHA = 0.72f
private const val FACE_DOWN_HATCH_ALPHA = 0.45f
