package com.datathecodie.witgrid.ui.board

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardHighlightTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun highlightIsVisibleOverOpaqueCellAndAnnouncedToAccessibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            WitgridTheme {
                CompositionLocalProvider(LocalBoardHighlightCells provides setOf(Cell(0))) {
                    BoardCanvas(
                        gridSize = 1,
                        cellDescription = { "test cell" },
                        drawCell = { _, bounds -> drawRect(Color.Red, bounds.topLeft, bounds.size) },
                        onCellTap = {},
                        onDragPaint = {},
                        onLongPress = {},
                        onUndo = {},
                    )
                }
            }
        }

        val highlightedCell = composeRule.onNodeWithContentDescription(
            context.getString(R.string.board_highlighted_cell_accessibility, "test cell"),
        )
        highlightedCell.assertExists()
        val pixels = highlightedCell.captureToImage().toPixelMap()
        assertNotEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 2])
    }
}
