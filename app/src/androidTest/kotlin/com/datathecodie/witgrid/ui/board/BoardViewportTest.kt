package com.datathecodie.witgrid.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardViewportTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun enlargedCellsStay48DpAndTheViewportPansWithoutPainting() {
        var taps = 0
        var paints = 0
        composeRule.setContent {
            WitgridTheme {
                CompositionLocalProvider(LocalBoardViewportMode provides BoardViewportMode(enlarged = true, embedded = true)) {
                    Box(Modifier.size(192.dp).testTag("viewport")) {
                        BoardCanvas(10, cellDescription = { "cell ${it.index}" },
                            onCellTap = { taps++ }, onDragPaint = { paints++ }, onLongPress = {}, onUndo = {})
                    }
                }
            }
        }
        val cell = composeRule.onNodeWithContentDescription("cell 0").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val minimum = with(composeRule.density) { 48.dp.toPx() }
        assertEquals(minimum, cell.width, 1f)
        assertEquals(minimum, cell.height, 1f)
        val viewport = composeRule.onNodeWithTag("viewport")
        repeat(3) { viewport.performTouchInput { swipeLeft() } }
        repeat(3) { viewport.performTouchInput { swipeUp() } }
        composeRule.runOnIdle { assertEquals(0, taps); assertEquals(0, paints) }
        val lastCell = composeRule.onNodeWithContentDescription("cell 99").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val bounds = viewport.fetchSemanticsNode().boundsInRoot
        assertTrue(lastCell.right <= bounds.right + 1f && lastCell.bottom <= bounds.bottom + 1f)
    }
}
