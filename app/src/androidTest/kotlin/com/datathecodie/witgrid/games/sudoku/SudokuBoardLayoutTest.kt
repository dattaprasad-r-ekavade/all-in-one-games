package com.datathecodie.witgrid.games.sudoku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SudokuBoardLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nineDigitPadShowsEveryRemainingCountAtDoubleFontScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val linearDensity = object : Density {
            override val density = 1f
            override val fontScale = 2f

            override fun Dp.toSp(): TextUnit = (value / fontScale).sp
            override fun TextUnit.toDp(): Dp = (value * fontScale).dp
        }
        composeRule.setContent { BoardSample(height = 480, width = 360, density = linearDensity) }

        val countText = context.getString(R.string.play_digit_left, 9)
        val labels = composeRule.onAllNodes(hasText(countText), useUnmergedTree = true)
        assertEquals(9, labels.fetchSemanticsNodes().size)
        for (index in 0 until 9) {
            val label = labels[index].assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("Key ${index + 1} must show the full count", layout.hasVisualOverflow)
            assertTrue(label.fetchSemanticsNode().boundsInRoot.height >= layout.size.height - 1f)
        }
    }

    @Composable
    private fun BoardSample(
        height: Int,
        direction: LayoutDirection = LayoutDirection.Ltr,
        width: Int = 360,
        density: Density = Density(1f, 1.3f),
    ) {
        val state = remember { SudokuState.initial(fixture()) }
        WitgridTheme {
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalLayoutDirection provides direction,
                LocalBoardViewportMode provides BoardViewportMode(embedded = true),
            ) {
                Box(Modifier.requiredSize(width.dp, height.dp)) {
                    SudokuGameModule.Board(state, onMoves = {}, onUndo = {})
                }
            }
        }
    }

    private fun fixture(): SudokuPuzzle {
        val solution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) }

        return SudokuPuzzle(
            id = "SU-TEST-LAYOUT",
            givens = List(81) { null },
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "fixture",
        )
    }
}
