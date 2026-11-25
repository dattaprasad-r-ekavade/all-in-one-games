package com.datathecodie.witgrid.games.coloursort

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColourSortBoardLayoutTest {
    @Test
    fun standardPacksMaintainMinimumTouchTargetsOnPhoneLandscapeAndTablet() {
        val viewports = listOf(
            328f to 328f, // 360 dp portrait Play board
            280f to 280f, // 840x360 landscape Play board
            520f to 520f, // 840 dp tablet Play board
        )

        for (tubeCount in listOf(4, 6, 8, 10)) {
            for ((width, height) in viewports) {
                val metrics = ColourSortBoardLayout.calculate(tubeCount, width, height)

                assertEquals((0 until tubeCount).toList(), metrics.rows.flatten())
                assertTrue(
                    metrics.tubeWidthDp >= ColourSortBoardLayout.MIN_TOUCH_TARGET_DP,
                    "Tube width ${metrics.tubeWidthDp} < 48 dp for $tubeCount tubes on ${width}x$height",
                )
                assertTrue(
                    metrics.tubeHeightDp >= ColourSortBoardLayout.MIN_TOUCH_TARGET_DP,
                    "Tube height ${metrics.tubeHeightDp} < 48 dp for $tubeCount tubes on ${width}x$height",
                )

                val maxColumns = metrics.rows.maxOf { it.size }
                val totalRowWidth = maxColumns * metrics.tubeWidthDp +
                    (maxColumns - 1).coerceAtLeast(0) * metrics.horizontalGapDp
                assertTrue(
                    totalRowWidth <= width + 0.5f,
                    "Row width $totalRowWidth exceeds $width for $tubeCount tubes",
                )

                val totalHeight = metrics.rows.size * (metrics.tubeHeightDp + metrics.selectionLiftDp) +
                    (metrics.rows.size - 1).coerceAtLeast(0) * metrics.verticalGapDp
                assertTrue(
                    totalHeight <= height + 0.5f,
                    "Total height $totalHeight exceeds $height for $tubeCount tubes",
                )
            }
        }
    }

    @Test
    fun extraContinueTubesReflowCleanlyWithoutClipping() {
        for (tubeCount in listOf(5, 7, 9, 11, 12)) {
            val phoneMetrics = ColourSortBoardLayout.calculate(tubeCount, 328f, 328f)
            assertEquals((0 until tubeCount).toList(), phoneMetrics.rows.flatten())
            assertTrue(phoneMetrics.tubeWidthDp >= ColourSortBoardLayout.MIN_TOUCH_TARGET_DP)

            val landscapeMetrics = ColourSortBoardLayout.calculate(tubeCount, 280f, 280f)
            assertEquals((0 until tubeCount).toList(), landscapeMetrics.rows.flatten())
            assertTrue(landscapeMetrics.tubeWidthDp >= ColourSortBoardLayout.MIN_TOUCH_TARGET_DP)
        }
    }

    @Test
    fun zeroTubeCountThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ColourSortBoardLayout.calculate(0, 328f, 328f)
        }
    }
}
