package com.datathecodie.witgrid.games.coloursort

import kotlin.math.ceil
import kotlin.math.floor

internal data class TubeGridMetrics(
    val rows: List<List<Int>>,
    val tubeWidthDp: Float,
    val tubeHeightDp: Float,
    val horizontalGapDp: Float,
    val verticalGapDp: Float,
    val selectionLiftDp: Float,
)

/** Computes balanced tube rows and sizes that keep touch targets at least 48 dp whenever width allows. */
internal object ColourSortBoardLayout {
    const val MIN_TOUCH_TARGET_DP = 48f
    private const val MAX_TUBE_WIDTH_DP = 84f
    private const val MAX_TUBE_HEIGHT_DP = 236f
    private const val PREFERRED_ASPECT_RATIO = 2.6f
    private const val MIN_ASPECT_RATIO = 1.6f
    private const val PREFERRED_HORIZONTAL_GAP_DP = 14f
    private const val MIN_HORIZONTAL_GAP_DP = 8f
    private const val PREFERRED_VERTICAL_GAP_DP = 14f
    private const val MIN_VERTICAL_GAP_DP = 8f
    private const val SELECTION_LIFT_DP = 10f

    fun calculate(
        tubeCount: Int,
        availableWidthDp: Float,
        availableHeightDp: Float,
    ): TubeGridMetrics {
        require(tubeCount >= 1) { "Colour Sort requires at least one tube" }

        val safeWidth = availableWidthDp.coerceAtLeast(MIN_TOUCH_TARGET_DP)
        val safeHeight = availableHeightDp.coerceAtLeast(MIN_TOUCH_TARGET_DP * 2f)

        val rowCount = chooseRowCount(tubeCount, safeWidth, safeHeight)
        val rows = splitIntoRows(tubeCount, rowCount)
        val maxColumns = rows.maxOf { it.size }

        val horizontalGap = if (maxColumns <= 1) {
            0f
        } else {
            val minTotalWidth = maxColumns * MIN_TOUCH_TARGET_DP + (maxColumns - 1) * MIN_HORIZONTAL_GAP_DP
            if (safeWidth >= minTotalWidth + (maxColumns - 1) * (PREFERRED_HORIZONTAL_GAP_DP - MIN_HORIZONTAL_GAP_DP)) {
                PREFERRED_HORIZONTAL_GAP_DP
            } else {
                MIN_HORIZONTAL_GAP_DP
            }
        }

        val maxTubeWidthForRow = if (maxColumns == 1) {
            MAX_TUBE_WIDTH_DP
        } else {
            ((safeWidth - (maxColumns - 1) * horizontalGap) / maxColumns).coerceAtLeast(1f)
        }

        val verticalGap = if (rowCount <= 1) 0f else {
            if (safeHeight >= rowCount * 120f) PREFERRED_VERTICAL_GAP_DP else MIN_VERTICAL_GAP_DP
        }
        val lift = SELECTION_LIFT_DP
        val perRowHeight = ((safeHeight - (rowCount - 1) * verticalGap) / rowCount).coerceAtLeast(MIN_TOUCH_TARGET_DP)
        val maxTubeHeight = (perRowHeight - lift).coerceAtLeast(MIN_TOUCH_TARGET_DP)

        val desiredHeightFromWidth = minOf(maxTubeWidthForRow, MAX_TUBE_WIDTH_DP) * PREFERRED_ASPECT_RATIO
        val tubeHeight = minOf(maxTubeHeight, MAX_TUBE_HEIGHT_DP, desiredHeightFromWidth)
            .coerceAtLeast(MIN_TOUCH_TARGET_DP)

        val tubeWidthFromHeight = tubeHeight / MIN_ASPECT_RATIO
        val rawWidth = minOf(maxTubeWidthForRow, MAX_TUBE_WIDTH_DP, maxOf(tubeHeight / PREFERRED_ASPECT_RATIO, MIN_TOUCH_TARGET_DP))
        val tubeWidth = if (maxTubeWidthForRow >= MIN_TOUCH_TARGET_DP) {
            rawWidth.coerceIn(MIN_TOUCH_TARGET_DP, minOf(maxTubeWidthForRow, tubeWidthFromHeight.coerceAtLeast(MIN_TOUCH_TARGET_DP)))
        } else {
            maxTubeWidthForRow
        }

        return TubeGridMetrics(
            rows = rows,
            tubeWidthDp = tubeWidth,
            tubeHeightDp = tubeHeight,
            horizontalGapDp = horizontalGap,
            verticalGapDp = verticalGap,
            selectionLiftDp = lift,
        )
    }

    private fun chooseRowCount(tubeCount: Int, availableWidthDp: Float, availableHeightDp: Float): Int {
        val maxFitInOneRow = maxColumnsThatFit(availableWidthDp)
        if (tubeCount <= 4 && maxFitInOneRow >= tubeCount) {
            return 1
        }
        if (tubeCount == 5 && maxFitInOneRow >= 5 && availableWidthDp / availableHeightDp >= 1.25f) {
            return 1
        }

        for (rows in 2..tubeCount) {
            val maxCols = ceil(tubeCount.toDouble() / rows).toInt()
            if (maxCols <= maxFitInOneRow) {
                return rows
            }
        }
        return tubeCount
    }

    private fun maxColumnsThatFit(availableWidthDp: Float): Int {
        val fit = floor(((availableWidthDp + MIN_HORIZONTAL_GAP_DP) / (MIN_TOUCH_TARGET_DP + MIN_HORIZONTAL_GAP_DP)).toDouble()).toInt()
        return fit.coerceAtLeast(1)
    }

    private fun splitIntoRows(tubeCount: Int, rowCount: Int): List<List<Int>> {
        val baseSize = tubeCount / rowCount
        val extra = tubeCount % rowCount
        val rows = ArrayList<List<Int>>(rowCount)
        var cursor = 0
        for (row in 0 until rowCount) {
            val count = baseSize + if (row < extra) 1 else 0
            rows += (cursor until cursor + count).toList()
            cursor += count
        }
        return rows
    }
}
