package com.datathecodie.witgrid.games.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.klondike.Card
import com.datathecodie.witgrid.engine.klondike.KlondikePuzzle
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.klondike.Suit
import com.datathecodie.witgrid.ui.theme.Colourway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class KlondikeBoardLayoutTest {
    @Test
    fun sevenColumnsFitOn360DpPortraitPhone() {
        val phone = KlondikeBoardLayout.calculate(
            availableWidthDp = KlondikeBoardLayout.PHONE_PORTRAIT_WIDTH_DP,
            availableHeightDp = 640f,
            tallestTableau = 7,
        )
        assertFits(phone, KlondikeBoardLayout.PHONE_PORTRAIT_WIDTH_DP)
        assertTrue(phone.cardWidthDp >= 47f, "Card width ${phone.cardWidthDp} is too narrow on 360 dp")
        assertTrue(phone.faceUpPeekDp >= 12f, "Face-up peek ${phone.faceUpPeekDp} hides the rank")

        val playArea = KlondikeBoardLayout.calculate(
            availableWidthDp = KlondikeBoardLayout.PLAY_BOARD_WIDTH_DP,
            availableHeightDp = 520f,
            tallestTableau = 7,
        )
        assertFits(playArea, KlondikeBoardLayout.PLAY_BOARD_WIDTH_DP)
        assertTrue(playArea.cardWidthDp >= 40f, "Card width ${playArea.cardWidthDp} is too narrow in the play area")
    }

    @Test
    fun overlapHitSelectsTheUpperCardAndATightPileStaysInBand() {
        val pile = List(7) { index -> Card(rank = index + 1, suit = Suit.CLUBS, faceUp = index >= 3) }
        val geometry = KlondikeBoardLayout.columnGeometry(
            pile = pile,
            cardHeightDp = 64f,
            bandHeightDp = 400f,
            faceUpPeekDp = 20f,
            faceDownPeekDp = 10f,
        )

        assertEquals(7, geometry.cardTopsDp.size)
        assertEquals(0f, geometry.cardTopsDp.first())
        assertNear(10f, geometry.cardTopsDp[1] - geometry.cardTopsDp[0])
        assertNear(20f, geometry.cardTopsDp[4] - geometry.cardTopsDp[3])
        assertEquals(0, KlondikeBoardLayout.cardIndexAt(geometry, geometry.cardTopsDp[1] - 0.5f))
        assertEquals(1, KlondikeBoardLayout.cardIndexAt(geometry, geometry.cardTopsDp[1] + 1f))
        assertEquals(6, KlondikeBoardLayout.cardIndexAt(geometry, geometry.cardTopsDp.last() + 1f))

        val many = List(20) { Card(rank = 1, suit = Suit.CLUBS, faceUp = true) }
        val tight = KlondikeBoardLayout.columnGeometry(
            pile = many,
            cardHeightDp = 50f,
            bandHeightDp = 180f,
            faceUpPeekDp = 20f,
            faceDownPeekDp = 10f,
        )
        assertTrue(tight.columnHeightDp <= 180.05f, "Column height ${tight.columnHeightDp} exceeds the band")

        val empty = KlondikeBoardLayout.columnGeometry(emptyList(), 50f, 180f, 20f, 10f)
        assertTrue(empty.cardTopsDp.isEmpty())
        assertEquals(50f, empty.columnHeightDp)
        assertNull(KlondikeBoardLayout.cardIndexAt(empty, 10f))
        assertTrue(KlondikeBoardLayout.tapInsideColumn(empty, 10f))
    }

    @Test
    fun hitAtMapsTopRowAndTableauCoordinates() {
        val metrics = KlondikeBoardLayout.calculate(360f, 640f, tallestTableau = 2)
        val deal = KlondikeState(
            puzzle = KlondikePuzzle(
                bankId = "kl-hit",
                pack = Pack.EASY,
                difficulty = "d1",
                seed = 1L,
            ),
            tableau = List(7) { emptyList() },
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = emptyList(),
        )
        val stock = KlondikeBoardLayout.hitAt(metrics.cardWidthDp / 2f, metrics.cardHeightDp / 2f, deal, metrics, false)
        assertEquals(KlondikeHit.Stock, stock)
        val wasteLeft = KlondikeBoardLayout.columnLeftDp(1, metrics) + 1f
        assertEquals(KlondikeHit.Waste, KlondikeBoardLayout.hitAt(wasteLeft, 1f, deal, metrics, false))
        val clubLeft = KlondikeBoardLayout.columnLeftDp(3, metrics) + 1f
        assertEquals(KlondikeHit.Foundation(Suit.CLUBS), KlondikeBoardLayout.hitAt(clubLeft, 1f, deal, metrics, false))
        val tableauY = metrics.cardHeightDp + metrics.rowGapDp + 1f
        assertEquals(KlondikeHit.Tableau(0, null), KlondikeBoardLayout.hitAt(1f, tableauY, deal, metrics, false))
    }

    @Test
    fun redSuitsUseTheRustColourwayAndMovesDoNotAnimate() {
        assertEquals(Colourway.RUST.lightAccent, klondikeRedSuitRgb(darkTheme = false))
        assertEquals(Colourway.RUST.darkAccent, klondikeRedSuitRgb(darkTheme = true))
        assertEquals(0, klondikeMoveDurationMillis(reduceMotion = true))
        assertEquals(180, klondikeMoveDurationMillis(reduceMotion = false))
    }

    @Test
    fun zeroSizeThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeBoardLayout.calculate(0f, 640f, 7)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeBoardLayout.calculate(360f, 640f, 0)
        }
    }

    private fun assertFits(metrics: KlondikeBoardMetrics, availableWidthDp: Float) {
        assertEquals(KlondikeBoardLayout.COLUMN_COUNT, 7)
        assertTrue(
            metrics.contentWidthDp <= availableWidthDp + 0.05f,
            "Content width ${metrics.contentWidthDp} exceeds $availableWidthDp",
        )
        val rightEdge = KlondikeBoardLayout.columnLeftDp(6, metrics) + metrics.cardWidthDp
        assertTrue(rightEdge <= availableWidthDp + 0.05f, "Column 7 ends at $rightEdge, past $availableWidthDp")
        assertTrue(metrics.tableauBandHeightDp >= metrics.cardHeightDp)
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(abs(actual - expected) < 0.05f, "Expected $expected, got $actual")
    }
}
