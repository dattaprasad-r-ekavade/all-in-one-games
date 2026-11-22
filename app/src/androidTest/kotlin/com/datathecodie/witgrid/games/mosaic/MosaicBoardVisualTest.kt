package com.datathecodie.witgrid.games.mosaic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MosaicBoardVisualTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun bothFiveCellGuidesStayVisibleBetweenFilledCells() {
        val state = filledCells(34, 35, 62, 77)
        var dark by mutableStateOf(false)
        composeRule.setContent {
            MosaicBoardSample(dark) { MosaicBoard(state, {}, {}) }
        }
        for (darkTheme in listOf(false, true)) {
            composeRule.runOnIdle { dark = darkTheme }
            val left = pixels(mosaicCellLabel(34, Mark.FILLED))
            val right = pixels(mosaicCellLabel(35, Mark.FILLED))
            val above = pixels(mosaicCellLabel(62, Mark.FILLED))
            val below = pixels(mosaicCellLabel(77, Mark.FILLED))
            val guideContrasts = listOf(
                left.edgeContrast { offset -> left[left.width - 1 - offset, left.height / 2] },
                right.edgeContrast { offset -> right[offset, right.height / 2] },
                above.edgeContrast { offset -> above[above.width / 2, above.height - 1 - offset] },
                below.edgeContrast { offset -> below[below.width / 2, offset] },
            )
            assertTrue("Five-cell guide contrasts in dark theme $darkTheme were $guideContrasts",
                guideContrasts.all { it >= 3f })
        }
    }

    @Test
    fun renderedErrorDigitsHaveReadableContrastOnFilledAndEmptyCells() {
        val state = filledCells(0)
        var dark by mutableStateOf(false)
        composeRule.setContent {
            MosaicBoardSample(dark) { MosaicBoard(state, {}, {}) }
        }
        for (darkTheme in listOf(false, true)) {
            composeRule.runOnIdle { dark = darkTheme }
            for ((index, mark) in listOf(0 to Mark.FILLED, 1 to Mark.EMPTY)) {
                val image = pixels(mosaicCellLabel(index, mark, R.string.mosaic_clue_error_accessibility_label))
                val centerX = image.width / 2
                val centerY = image.height / 2
                val background = image[centerX + image.width * 3 / 10, centerY]
                // Sample the digit interior, allowing for antialiasing at its edges.
                var digitContrast = 1f
                for (x in centerX - image.width / 5..centerX + image.width / 5) {
                    for (y in centerY - image.height / 5..centerY + image.height / 5) {
                        digitContrast = maxOf(digitContrast, contrast(image[x, y], background))
                    }
                }
                assertTrue("Error digit contrast for $mark in dark theme $darkTheme was $digitContrast",
                    digitContrast >= 4.5f)
            }
        }
    }

    private fun pixels(label: String): PixelMap =
        composeRule.onNodeWithContentDescription(label).captureToImage().toPixelMap()

    private fun filledCells(vararg indices: Int): MosaicState =
        indices.fold(MosaicState.initial(expertMosaicFixture())) { state, index ->
            state.withMove(Move.SetMark(Cell(index), Mark.FILLED))
        }

    private fun PixelMap.edgeContrast(edgePixel: (Int) -> Color): Float {
        val fill = this[width / 2, height / 2]
        return (0..1).maxOf { contrast(fill, edgePixel(it)) }
    }

    private fun contrast(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (maxOf(firstLuminance, secondLuminance) + 0.05f) /
            (minOf(firstLuminance, secondLuminance) + 0.05f)
    }
}
