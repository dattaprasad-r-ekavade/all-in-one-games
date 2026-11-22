package com.datathecodie.witgrid.games.skyscrapers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPackBands
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
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
class SkyscrapersBoardLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun constrainedBoardKeepsTheDigitPadAndModeControlsVisibleAtLargeFontScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var size by mutableIntStateOf(4)
        composeRule.setContent { BoardSample(size, height = 252) }

        for (boardSize in 4..7) {
            composeRule.runOnIdle { size = boardSize }
            val clear = composeRule.onNodeWithText(context.getString(R.string.minisudoku_clear_cell))
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            composeRule.onNodeWithText(context.getString(R.string.minisudoku_mode_notes)).assertIsDisplayed()
            composeRule.onNodeWithText(context.getString(R.string.minisudoku_mode_digits)).assertIsDisplayed()
            val cell = composeRule.onNodeWithContentDescription(cellDescription(context, boardSize, 1))
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(cell.bottom <= clear.top)
            for (digit in 1..boardSize) {
                composeRule.onNodeWithContentDescription(
                    context.getString(R.string.minisudoku_digit_button_accessibility, digit),
                ).assertIsDisplayed()
            }
            composeRule.onAllNodes(hasText("", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes().forEach { node ->
                    if (node.config.contains(SemanticsActions.GetTextLayoutResult)) {
                        val results = mutableListOf<TextLayoutResult>()
                        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                        results.forEach { assertFalse(it.layoutInput.text.text, it.hasVisualOverflow) }
                    }
                }
        }
    }

    @Test
    fun cluesAlignWithTheirCellsAndKeepTheirPhysicalSidesInBothLayoutDirections() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val puzzle = fixture(7)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        composeRule.setContent { BoardSample(7, height = 490, direction = direction) }

        for (layoutDirection in LayoutDirection.entries) {
            composeRule.runOnIdle { direction = layoutDirection }
            for (line in 1..puzzle.size) {
                val column = composeRule.onNodeWithContentDescription(cellDescription(context, 1, line))
                    .fetchSemanticsNode().boundsInRoot
                val row = composeRule.onNodeWithContentDescription(cellDescription(context, line, 1))
                    .fetchSemanticsNode().boundsInRoot
                val top = clueNode(context, R.string.skyscrapers_clue_top, puzzle.topClues, line)
                val bottom = clueNode(context, R.string.skyscrapers_clue_bottom, puzzle.bottomClues, line)
                val left = clueNode(context, R.string.skyscrapers_clue_left, puzzle.leftClues, line)
                val right = clueNode(context, R.string.skyscrapers_clue_right, puzzle.rightClues, line)

                assertEquals(column.center.x, top.center.x, 1f)
                assertEquals(column.center.x, bottom.center.x, 1f)
                assertEquals(row.center.y, left.center.y, 1f)
                assertEquals(row.center.y, right.center.y, 1f)
                assertTrue(top.bottom <= column.top)
                assertTrue(bottom.top >= column.bottom)
                assertTrue(left.right <= row.left)
                assertTrue(right.left >= row.right)
            }
        }
    }

    @Test
    fun clueDescriptionsAnnounceTheValueLineAndViewingSide() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val puzzle = fixture(4)
        composeRule.setContent { BoardSample(4, height = 252) }

        clueNode(context, R.string.skyscrapers_clue_top, puzzle.topClues, line = 2)
        clueNode(context, R.string.skyscrapers_clue_bottom, puzzle.bottomClues, line = 3)
        clueNode(context, R.string.skyscrapers_clue_left, puzzle.leftClues, line = 4)
        clueNode(context, R.string.skyscrapers_clue_right, puzzle.rightClues, line = 1)
    }

    private fun clueNode(context: Context, label: Int, clues: List<Int?>, line: Int) =
        composeRule.onNodeWithContentDescription(context.getString(label, clues[line - 1], line))
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun cellDescription(context: Context, row: Int, column: Int) = context.getString(
        R.string.minisudoku_cell_accessibility_label,
        row,
        column,
        context.getString(R.string.minisudoku_cell_empty),
        context.getString(R.string.minisudoku_no_notes),
        context.getString(R.string.minisudoku_cell_editable),
        context.getString(R.string.minisudoku_cell_not_selected),
    )

    @Composable
    private fun BoardSample(size: Int, height: Int, direction: LayoutDirection = LayoutDirection.Ltr) {
        val state = remember(size) { SkyscrapersState.initial(fixture(size)) }
        WitgridTheme {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.3f),
                LocalLayoutDirection provides direction,
                LocalBoardViewportMode provides BoardViewportMode(embedded = true),
            ) {
                Box(Modifier.requiredSize(328.dp, height.dp)) {
                    SkyscrapersGameModule.Board(state, onMoves = {}, onUndo = {})
                }
            }
        }
    }

    private fun fixture(size: Int): SkyscrapersPuzzle {
        val solution = List(size * size) { index -> (index / size + index % size) % size + 1 }
        val rows = solution.chunked(size)
        val columns = List(size) { column -> rows.map { it[column] } }
        return SkyscrapersPuzzle(
            id = "SK-LAYOUT-$size",
            size = size,
            topClues = columns.map(SkyscrapersRules::countVisible),
            bottomClues = columns.map { SkyscrapersRules.countVisible(it.asReversed()) },
            leftClues = rows.map(SkyscrapersRules::countVisible),
            rightClues = rows.map { SkyscrapersRules.countVisible(it.asReversed()) },
            solution = solution,
            pack = SkyscrapersPackBands.packForSize(size),
            storedGrade = "none:0",
        )
    }
}
