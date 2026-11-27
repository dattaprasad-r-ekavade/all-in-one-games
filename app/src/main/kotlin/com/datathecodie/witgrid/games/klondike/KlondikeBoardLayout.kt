package com.datathecodie.witgrid.games.klondike

import com.datathecodie.witgrid.engine.klondike.Card
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.klondike.Suit

internal data class KlondikeBoardMetrics(
    val cardWidthDp: Float,
    val cardHeightDp: Float,
    val columnGapDp: Float,
    val rowGapDp: Float,
    val faceUpPeekDp: Float,
    val faceDownPeekDp: Float,
    val contentWidthDp: Float,
    val topRowHeightDp: Float,
    val tableauBandHeightDp: Float,
    val contentHeightDp: Float,
)

/** Vertical placement of one tableau column. An empty column has no card tops and one empty slot. */
internal data class TableauColumnGeometry(
    val cardTopsDp: List<Float>,
    val cardHeightDp: Float,
    val columnHeightDp: Float,
)

/**
 * Sizes a 7-column Klondike table so the row fits the width it is given.
 * A 360 dp portrait phone, and the 328 dp play area inside its 16 dp margins, both keep all 7 columns.
 */
internal object KlondikeBoardLayout {
    const val COLUMN_COUNT = 7
    const val PHONE_PORTRAIT_WIDTH_DP = 360f

    /** Play board width on a 360 dp phone after the 16 dp screen margins. */
    const val PLAY_BOARD_WIDTH_DP = 328f

    private const val CARD_ASPECT = 1.45f
    private const val PREFERRED_GAP_DP = 4f
    private const val MIN_GAP_DP = 2f
    private const val FACE_UP_PEEK_FRACTION = 0.34f
    private const val FACE_DOWN_PEEK_FRACTION = 0.16f

    fun calculate(
        availableWidthDp: Float,
        availableHeightDp: Float,
        tallestTableau: Int,
    ): KlondikeBoardMetrics {
        require(availableWidthDp > 0f) { "Width must be positive" }
        require(availableHeightDp > 0f) { "Height must be positive" }
        require(tallestTableau >= 1) { "Tableau band needs at least one card slot" }

        val gaps = COLUMN_COUNT - 1
        val preferredRow = COLUMN_COUNT * 40f + gaps * PREFERRED_GAP_DP
        val gap = if (availableWidthDp >= preferredRow) PREFERRED_GAP_DP else MIN_GAP_DP
        val cardWidth = ((availableWidthDp - gaps * gap) / COLUMN_COUNT).coerceAtLeast(1f)
        val idealHeight = cardWidth * CARD_ASPECT
        // Top row plus the tableau peeks share the height. The factor counts two card faces
        // (stock row and the top tableau card) and one peek for each buried card.
        val factor = 2f + FACE_UP_PEEK_FRACTION * (tallestTableau - 1)
        val fittedHeight = ((availableHeightDp - gap) / factor).coerceAtLeast(1f)
        val cardHeight = minOf(idealHeight, fittedHeight)
        val tableauBand = (availableHeightDp - cardHeight - gap).coerceAtLeast(cardHeight)
        return KlondikeBoardMetrics(
            cardWidthDp = cardWidth,
            cardHeightDp = cardHeight,
            columnGapDp = gap,
            rowGapDp = gap,
            faceUpPeekDp = cardHeight * FACE_UP_PEEK_FRACTION,
            faceDownPeekDp = cardHeight * FACE_DOWN_PEEK_FRACTION,
            contentWidthDp = COLUMN_COUNT * cardWidth + gaps * gap,
            topRowHeightDp = cardHeight,
            tableauBandHeightDp = tableauBand,
            contentHeightDp = cardHeight + gap + tableauBand,
        )
    }

    fun columnLeftDp(column: Int, metrics: KlondikeBoardMetrics): Float {
        require(column in 0 until COLUMN_COUNT) { "Column $column outside board" }
        return column * (metrics.cardWidthDp + metrics.columnGapDp)
    }

    fun columnGeometry(pile: List<Card>, metrics: KlondikeBoardMetrics): TableauColumnGeometry =
        columnGeometry(
            pile = pile,
            cardHeightDp = metrics.cardHeightDp,
            bandHeightDp = metrics.tableauBandHeightDp,
            faceUpPeekDp = metrics.faceUpPeekDp,
            faceDownPeekDp = metrics.faceDownPeekDp,
        )

    fun columnGeometry(
        pile: List<Card>,
        cardHeightDp: Float,
        bandHeightDp: Float,
        faceUpPeekDp: Float,
        faceDownPeekDp: Float,
    ): TableauColumnGeometry {
        require(cardHeightDp > 0f) { "Card height must be positive" }
        if (pile.isEmpty()) {
            return TableauColumnGeometry(emptyList(), cardHeightDp, cardHeightDp)
        }
        val gaps = FloatArray(pile.lastIndex) { index ->
            if (pile[index].faceUp) faceUpPeekDp else faceDownPeekDp
        }
        val preferred = gaps.sum()
        val room = (bandHeightDp - cardHeightDp).coerceAtLeast(0f)
        if (preferred > room && preferred > 0f) {
            val scale = room / preferred
            for (index in gaps.indices) {
                gaps[index] *= scale
            }
        }
        val tops = ArrayList<Float>(pile.size)
        var cursor = 0f
        for (index in pile.indices) {
            tops += cursor
            if (index < gaps.size) cursor += gaps[index]
        }
        return TableauColumnGeometry(tops, cardHeightDp, cursor + cardHeightDp)
    }

    /** The card drawn on top wins, so a tap on an overlap selects the upper card. */
    fun cardIndexAt(geometry: TableauColumnGeometry, yDp: Float): Int? {
        val height = geometry.cardHeightDp
        for (index in geometry.cardTopsDp.indices.reversed()) {
            val top = geometry.cardTopsDp[index]
            if (yDp >= top && yDp < top + height) return index
        }
        return null
    }

    fun tapInsideColumn(geometry: TableauColumnGeometry, yDp: Float): Boolean =
        yDp >= 0f && yDp < geometry.columnHeightDp

    fun columnIndexAt(xDp: Float, metrics: KlondikeBoardMetrics, snapGaps: Boolean): Int? {
        if (xDp < 0f || xDp > metrics.contentWidthDp) return null
        val stride = metrics.cardWidthDp + metrics.columnGapDp
        val column = (xDp / stride).toInt().coerceIn(0, COLUMN_COUNT - 1)
        if (!snapGaps) {
            val left = columnLeftDp(column, metrics)
            if (xDp < left || xDp > left + metrics.cardWidthDp) return null
        }
        return column
    }

    fun hitAt(
        xDp: Float,
        yDp: Float,
        state: KlondikeState,
        metrics: KlondikeBoardMetrics,
        snapForDrop: Boolean,
    ): KlondikeHit? {
        val column = columnIndexAt(xDp, metrics, snapGaps = snapForDrop) ?: return null
        if (yDp >= 0f && yDp < metrics.cardHeightDp) {
            return when (column) {
                0 -> KlondikeHit.Stock
                1 -> KlondikeHit.Waste
                2 -> null
                else -> KlondikeHit.Foundation(Suit.entries[column - 3])
            }
        }
        val tableauTop = metrics.cardHeightDp + metrics.rowGapDp
        if (yDp < tableauTop) {
            return if (snapForDrop) KlondikeHit.Tableau(column, null) else null
        }
        val localY = yDp - tableauTop
        val pile = state.tableau[column]
        val geometry = columnGeometry(pile, metrics)
        if (snapForDrop) return KlondikeHit.Tableau(column, null)
        if (!tapInsideColumn(geometry, localY)) return null
        val index = if (pile.isEmpty()) null else cardIndexAt(geometry, localY)
        return KlondikeHit.Tableau(column, index)
    }
}
