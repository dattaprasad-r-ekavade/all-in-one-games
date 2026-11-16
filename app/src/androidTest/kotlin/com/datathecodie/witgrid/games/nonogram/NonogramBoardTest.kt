package com.datathecodie.witgrid.games.nonogram

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramRules
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.play.MonotonicClock
import com.datathecodie.witgrid.play.PlayController
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class NonogramBoardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstBankPuzzleCanBeSolvedThroughNonogramBoard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val levelRef = NonogramGameModule.firstLevelRef(Pack.EASY, bank)
        val level = NonogramGameModule.loadLevel(levelRef, bank)
        val repository = InMemoryProgressRepository()
        val controller = runBlocking {
            PlayController.open(
                gameModule = NonogramGameModule,
                level = level,
                progressRepository = repository,
                clock = MonotonicClock { 1_000L },
            )
        }
        val solvedEventObserved = AtomicBoolean(false)

        composeRule.setContent {
            val snapshot by controller.snapshot.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(controller) {
                controller.solvedEvents.collect { solvedEventObserved.set(true) }
            }
            WitgridTheme {
                CompositionLocalProvider(
                    LocalNonogramBoardOptions provides NonogramBoardOptions(hapticsEnabled = false),
                ) {
                    NonogramGameModule.Board(
                        state = snapshot.state,
                        onMoves = { moves -> coroutineScope.launch { controller.applyGesture(moves) } },
                        onUndo = { coroutineScope.launch { controller.undo() } },
                    )
                }
            }
        }

        level.puzzle.solution.forEachIndexed { index, solution ->
            val modeResource = if (solution == 1) R.string.nonogram_mode_fill else R.string.nonogram_mode_cross
            composeRule.onNodeWithText(context.getString(modeResource)).performTouchInput { click() }
            val state = controller.snapshot.value.state
            val cellDescription = cellDescription(context, level.puzzle, state, index)
            composeRule.onNodeWithContentDescription(cellDescription).performTouchInput { click() }
        }

        composeRule.waitUntil(timeoutMillis = SOLVED_EVENT_TIMEOUT_MILLIS) {
            solvedEventObserved.get()
        }
        composeRule.runOnIdle {
            assertTrue(controller.snapshot.value.solved)
            assertTrue(repository.savedProgress?.completed == true)
        }
    }

    @Test
    fun dragPaintsTheSelectedModeAcrossCells() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val level = NonogramGameModule.loadLevel(NonogramGameModule.firstLevelRef(Pack.EASY, bank), bank)
        val state = NonogramState.initial(level.puzzle)
        val emittedMoves = mutableListOf<List<Move>>()

        composeRule.setContent {
            WitgridTheme {
                NonogramBoard(
                    state = state,
                    onMoves = emittedMoves::add,
                    onUndo = {},
                    options = NonogramBoardOptions(hapticsEnabled = false),
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.nonogram_mode_cross)).performTouchInput { click() }

        val start = composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state, 0),
        ).fetchSemanticsNode().boundsInRoot.center
        val end = composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state, 1),
        ).fetchSemanticsNode().boundsInRoot.center
        composeRule.onRoot().performTouchInput {
            down(start)
            moveTo(end, delayMillis = 200)
            up()
        }

        composeRule.runOnIdle {
            assertEquals(1, emittedMoves.size)
            assertEquals(listOf(Mark.CROSSED, Mark.CROSSED), emittedMoves.single().map { (it as Move.SetMark).mark })
            assertEquals(listOf(0, 1), emittedMoves.single().map { it.cell.index })
        }
    }

    @Test
    fun denseExpertCluesFitTheirCellsWithLargeSystemText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val size = 15
        val solution = List(size * size) { index -> if (index % size % 2 == 0) 1 else 0 }
        val (rows, columns) = NonogramRules.cluesForSolution(size, solution)
        val puzzle = NonogramPuzzle("NO-DENSE", size, rows, columns, solution, Pack.EXPERT, "fixture")
        val state = NonogramState.initial(puzzle)
        var tolerancePixels = 0f
        composeRule.setContent {
            val density = LocalDensity.current
            tolerancePixels = with(density) { 3.dp.toPx() }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WitgridTheme {
                    Box(Modifier.width(320.dp)) {
                        NonogramBoard(state, {}, {}, options = NonogramBoardOptions(hapticsEnabled = false))
                    }
                }
            }
        }

        composeRule.onAllNodesWithText(context.getString(R.string.nonogram_no_run_marker), useUnmergedTree = true)
            .assertCountEquals(7)
        listOf("1", "15", context.getString(R.string.nonogram_no_run_marker)).forEach { label ->
            val labels = composeRule.onAllNodesWithText(label, useUnmergedTree = true)
            repeat(labels.fetchSemanticsNodes().size) { index ->
                val layouts = mutableListOf<TextLayoutResult>()
                labels[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue(layouts.isNotEmpty())
                val layout = layouts.single()
                assertFalse(
                    "Clue $label must fit its allocated cell: size=${layout.size}, " +
                        "font=${layout.layoutInput.style.fontSize}, height=${layout.getLineBottom(0)}, " +
                        "width=${layout.getLineRight(0)}, density=${layout.layoutInput.density}",
                    layout.hasVisualOverflow,
                )
            }
        }
        listOf(0, 7, 14).forEach { line ->
            val row = composeRule.onNodeWithContentDescription(
                context.getString(R.string.nonogram_row_clues_accessibility, line + 1, rows[line].joinToString(", ")),
            ).fetchSemanticsNode().boundsInRoot
            val columnText = columns[line].joinToString(", ").ifEmpty {
                context.getString(R.string.nonogram_no_run_clues)
            }
            val column = composeRule.onNodeWithContentDescription(
                context.getString(R.string.nonogram_column_clues_accessibility, line + 1, columnText),
            ).fetchSemanticsNode().boundsInRoot
            val firstInRow = composeRule.onNodeWithContentDescription(cellDescription(context, puzzle, state, line * size))
                .fetchSemanticsNode().boundsInRoot
            val firstInColumn = composeRule.onNodeWithContentDescription(cellDescription(context, puzzle, state, line))
                .fetchSemanticsNode().boundsInRoot
            assertEquals(firstInRow.center.y, row.center.y, tolerancePixels)
            assertEquals(firstInColumn.center.x, column.center.x, tolerancePixels)
        }
    }

    private fun cellDescription(
        context: Context,
        puzzle: NonogramPuzzle,
        state: NonogramState,
        index: Int,
    ): String = context.getString(
        R.string.nonogram_cell_accessibility_label,
        index / puzzle.size + 1,
        index % puzzle.size + 1,
        context.getString(
            when (state.marks[index]) {
                Mark.EMPTY -> R.string.nonogram_mark_empty
                Mark.FILLED -> R.string.nonogram_mark_filled
                Mark.CROSSED -> R.string.nonogram_mark_crossed
            },
        ),
    )

    private class InMemoryProgressRepository : ProgressRepository {
        var savedProgress: LevelProgress? = null
            private set

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> = flowOf(null)

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? = null

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            savedProgress = progress
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            savedProgress = null
        }
    }

    private companion object {
        const val SOLVED_EVENT_TIMEOUT_MILLIS = 10_000L
    }
}
