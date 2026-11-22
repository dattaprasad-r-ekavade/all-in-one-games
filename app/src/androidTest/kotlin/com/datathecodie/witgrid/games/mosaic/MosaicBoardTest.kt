package com.datathecodie.witgrid.games.mosaic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.mosaic.MosaicPuzzle
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.screens.play.PlayScreenLayout
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MosaicBoardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun expertFitsAboveBothControlsAndSupportsDragAndZoomInLargeTextPortrait() {
        var state by mutableStateOf(MosaicState.initial(expertMosaicFixture()))
        var enlarged by mutableStateOf(false)
        val gestures = mutableListOf<List<Move>>()
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, fontScale = 2f),
                LocalBoardViewportMode provides BoardViewportMode(enlarged, embedded = true),
            ) {
                WitgridTheme {
                    Box(Modifier.requiredSize(360.dp, 780.dp)) {
                        PlayScreenLayout(
                            levelRef = LevelRef("mosaic", state.puzzle.id, Pack.EXPERT, levelNumber = 1),
                            elapsedTimeMillis = 0L,
                            canUndo = false,
                            canRedo = false,
                            saveFailed = false,
                            introCoachLine = null,
                            statusMessage = null,
                            hintOperationPending = false,
                            enlargedBoardMode = enlarged,
                            onToggleEnlargedBoard = { enlarged = !enlarged },
                            board = {
                                MosaicGameModule.Board(state, onMoves = { moves ->
                                    gestures.add(moves)
                                    state = moves.fold(state, MosaicState::withMove)
                                }, onUndo = {})
                            },
                            gameControls = {},
                            onUndo = {},
                            onRedo = {},
                            onRequestHint = {},
                            onCheck = {},
                            onBack = {},
                        )
                    }
                }
            }
        }

        val fill = composeRule.onNodeWithText(mosaicTestString(R.string.nonogram_mode_fill)).assertIsDisplayed()
        val cross = composeRule.onNodeWithText(mosaicTestString(R.string.nonogram_mode_cross)).assertIsDisplayed()
        val slot = composeRule.onNodeWithTag("play-board")
        val slotBounds = slot.fetchSemanticsNode().boundsInRoot
        val firstCell = cellBounds(2)
        val topLeftCell = composeRule.onNodeWithContentDescription(
            mosaicCellLabel(0, label = R.string.mosaic_clue_accessibility_label),
        ).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val lastCell = cellBounds(224)
        val controlsTop = minOf(fill.fetchSemanticsNode().boundsInRoot.top, cross.fetchSemanticsNode().boundsInRoot.top)
        assertEquals(328f, slotBounds.width, 1f)
        assertTrue("The grid must end above both mode controls", lastCell.bottom <= controlsTop)
        assertEquals("Fit cells must be square", firstCell.width, firstCell.height, 1f)
        assertTrue("The grid must start inside the Play slot", topLeftCell.top >= slotBounds.top)
        assertEquals(
            "The complete grid must fit as a square",
            lastCell.right - topLeftCell.left,
            lastCell.bottom - topLeftCell.top,
            1f,
        )

        cross.performTouchInput { click() }.assertIsSelected()
        drag(32, 34)
        fill.performTouchInput { click() }.assertIsSelected()
        drag(47, 49)
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    (32..34).map { Move.SetMark(Cell(it), Mark.CROSSED) },
                    (47..49).map { Move.SetMark(Cell(it), Mark.FILLED) },
                ),
                gestures,
            )
        }

        composeRule.onNodeWithContentDescription(mosaicTestString(R.string.button_board_enlarge))
            .performTouchInput { click() }
        assertEquals(48f, cellBounds(2).width, 1f)
        assertEquals(48f, cellBounds(2).height, 1f)
        repeat(3) { slot.performTouchInput { swipeLeft() } }
        composeRule.onNodeWithContentDescription(mosaicCellLabel(14)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("Panning must not paint", 2, gestures.size) }

        composeRule.onNodeWithContentDescription(mosaicTestString(R.string.button_board_fit))
            .performTouchInput { click() }
        assertEquals(firstCell.width, cellBounds(2).width, 1f)
        fill.assertIsSelected()
    }

    @Test
    fun clueLabelsAnnounceNormalErrorAndSatisfiedAreas() {
        var state by mutableStateOf(MosaicState.initial(expertMosaicFixture()))
        composeRule.setContent {
            MosaicBoardSample {
                MosaicBoard(
                    state,
                    onMoves = { moves -> state = moves.fold(state, MosaicState::withMove) },
                    onUndo = {},
                    options = MosaicBoardOptions(hapticsEnabled = false, reduceMotion = true),
                )
            }
        }

        val normal = mosaicCellLabel(0, label = R.string.mosaic_clue_accessibility_label)
        composeRule.onNodeWithContentDescription(normal).assertIsDisplayed().performTouchInput { click() }
        val error = mosaicCellLabel(0, Mark.FILLED, R.string.mosaic_clue_error_accessibility_label)
        composeRule.onNodeWithContentDescription(error).assertIsDisplayed()

        composeRule.onNodeWithText(mosaicTestString(R.string.nonogram_mode_cross)).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(error).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(mosaicCellLabel(1, label = R.string.mosaic_clue_accessibility_label))
            .performTouchInput { click() }
        composeRule.onNodeWithContentDescription(mosaicCellLabel(15)).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(mosaicCellLabel(16, label = R.string.mosaic_clue_accessibility_label))
            .performTouchInput { click() }
        val satisfied = mosaicCellLabel(0, Mark.CROSSED, R.string.mosaic_clue_satisfied_accessibility_label)
        composeRule.onNodeWithContentDescription(satisfied).assertIsDisplayed()
    }

    private fun cellBounds(index: Int): Rect =
        composeRule.onNodeWithContentDescription(mosaicCellLabel(index)).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot

    private fun drag(first: Int, last: Int) {
        val start = cellBounds(first).center
        val end = cellBounds(last).center
        composeRule.onRoot().performTouchInput { swipe(start, end, durationMillis = 300) }
    }
}

@Composable
internal fun MosaicBoardSample(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDensity provides Density(1f),
        LocalBoardViewportMode provides BoardViewportMode(embedded = true),
    ) {
        WitgridTheme(darkTheme = dark) {
            Box(Modifier.requiredSize(300.dp, 380.dp)) { content() }
        }
    }
}

internal fun expertMosaicFixture(): MosaicPuzzle {
    // Each tile's center clue forces all nine cells to be empty.
    val tileCenters = 1..13 step 3
    val clues = List(225) { index ->
        if (index in 0..1 || (index / 15 in tileCenters && index % 15 in tileCenters)) 0 else null
    }
    return MosaicPuzzle("MO-EXPERT-UI", 15, clues, List(225) { 0 }, Pack.EXPERT, "fixture")
}

internal fun mosaicTestString(resource: Int, vararg arguments: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resource, *arguments)

internal fun mosaicCellLabel(
    index: Int,
    mark: Mark = Mark.EMPTY,
    label: Int = R.string.nonogram_cell_accessibility_label,
): String {
    val markLabel = mosaicTestString(
        when (mark) {
            Mark.EMPTY -> R.string.nonogram_mark_empty
            Mark.FILLED -> R.string.nonogram_mark_filled
            Mark.CROSSED -> R.string.nonogram_mark_crossed
        },
    )
    return if (label == R.string.nonogram_cell_accessibility_label) {
        mosaicTestString(label, index / 15 + 1, index % 15 + 1, markLabel)
    } else {
        mosaicTestString(label, index / 15 + 1, index % 15 + 1, markLabel, 0)
    }
}
