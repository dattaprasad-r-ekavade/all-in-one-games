package com.datathecodie.witgrid.games.minisudoku

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuPuzzle
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class MiniSudokuBoardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstBankPuzzleCanBeSolvedAfterSelectingCellsAndDigits() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val levelRef = MiniSudokuGameModule.firstLevelRef(Pack.EASY, bank)
        val level = MiniSudokuGameModule.loadLevel(levelRef, bank)
        val repository = InMemoryProgressRepository()
        val controller = runBlocking {
            PlayController.open(
                gameModule = MiniSudokuGameModule,
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
                    LocalMiniSudokuBoardOptions provides MiniSudokuBoardOptions(hapticsEnabled = false),
                ) {
                    MiniSudokuGameModule.Board(
                        state = snapshot.state,
                        onMoves = { moves -> coroutineScope.launch { controller.applyGesture(moves) } },
                        onUndo = { coroutineScope.launch { controller.undo() } },
                    )
                }
            }
        }

        val firstEditableIndex = level.puzzle.givens.indexOfFirst { given -> given == null }
        assertTrue(firstEditableIndex >= 0)
        val firstEditableState = controller.snapshot.value.state
        val noteDigit = level.puzzle.solution[firstEditableIndex]
        composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, firstEditableState, firstEditableIndex),
        ).performTouchInput { click() }
        composeRule.onNodeWithText(context.getString(R.string.minisudoku_mode_notes)).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.minisudoku_note_button_accessibility, noteDigit),
        ).performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(1 shl (noteDigit - 1), controller.snapshot.value.state.notes[firstEditableIndex])
        }
        composeRule.onNodeWithText(context.getString(R.string.minisudoku_mode_digits)).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(digitDescription(context, noteDigit)).performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(noteDigit, controller.snapshot.value.state.digits[firstEditableIndex])
            assertEquals(0, controller.snapshot.value.state.notes[firstEditableIndex])
        }

        level.puzzle.solution.forEachIndexed { index, solutionDigit ->
            if (level.puzzle.givens[index] == null && index != firstEditableIndex) {
                val state = controller.snapshot.value.state
                composeRule.onNodeWithContentDescription(
                    cellDescription(context, level.puzzle, state, index),
                ).performTouchInput { click() }
                composeRule.onNodeWithContentDescription(digitDescription(context, solutionDigit))
                    .performTouchInput { click() }
            }
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
    fun givensCanBeSelectedButDigitButtonsStayDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val level = MiniSudokuGameModule.loadLevel(MiniSudokuGameModule.firstLevelRef(Pack.EASY, bank), bank)
        val state = MiniSudokuState.initial(level.puzzle)
        val givenIndex = level.puzzle.givens.indexOfFirst { given -> given != null }
        assertTrue(givenIndex >= 0)

        composeRule.setContent {
            WitgridTheme {
                MiniSudokuBoard(
                    state = state,
                    onMoves = {},
                    onUndo = {},
                    options = MiniSudokuBoardOptions(hapticsEnabled = false),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state, givenIndex),
        ).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(digitDescription(context, 1)).assertIsNotEnabled()
    }

    @Test
    fun longPressAndAccessibilityClearRemoveEditableValuesInOneGesture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val level = MiniSudokuGameModule.loadLevel(MiniSudokuGameModule.firstLevelRef(Pack.EASY, bank), bank)
        val index = level.puzzle.givens.indexOfFirst { it == null }
        val state = mutableStateOf(MiniSudokuState.initial(level.puzzle).withMove(Move.SetDigit(Cell(index), 2)))
        val gestures = mutableListOf<List<Move>>()

        composeRule.setContent {
            WitgridTheme {
                MiniSudokuBoard(
                    state = state.value,
                    onMoves = { moves ->
                        gestures += moves
                        state.value = moves.fold(state.value, MiniSudokuState::withMove)
                    },
                    onUndo = {},
                    options = MiniSudokuBoardOptions(hapticsEnabled = false),
                )
            }
        }

        composeRule.onNodeWithContentDescription(cellDescription(context, level.puzzle, state.value, index))
            .performTouchInput { longClick() }
        composeRule.runOnIdle {
            assertEquals(listOf(listOf(Move.SetDigit(Cell(index), 0))), gestures)
            assertEquals(null, state.value.digits[index])
            state.value = state.value.withMove(Move.SetNotes(Cell(index), 3))
        }
        val actions = composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state.value, index, selected = true),
        ).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle {
            val clearAction = actions.single { it.label == context.getString(R.string.board_cell_clear_action) }
            assertTrue(clearAction.action())
        }
        composeRule.runOnIdle {
            assertEquals(0, state.value.notes[index])
            assertEquals(listOf(Move.SetNotes(Cell(index), 0)), gestures.last())
            assertEquals(2, gestures.size)
        }
    }

    private fun cellDescription(
        context: Context,
        puzzle: MiniSudokuPuzzle,
        state: MiniSudokuState,
        index: Int,
        selected: Boolean = false,
    ): String {
        val digit = state.digits[index]?.toString() ?: context.getString(R.string.minisudoku_cell_empty)
        val noteDigits = (1..BOARD_SIZE)
            .filter { value -> state.notes[index] and (1 shl (value - 1)) != 0 }
            .joinToString(", ")
        val notes = if (noteDigits.isEmpty()) {
            context.getString(R.string.minisudoku_no_notes)
        } else {
            context.getString(R.string.minisudoku_cell_notes_accessibility, noteDigits)
        }
        val editability = context.getString(
            if (puzzle.givens[index] == null) R.string.minisudoku_cell_editable else R.string.minisudoku_cell_given,
        )
        val selection = context.getString(
            if (selected) R.string.minisudoku_cell_selected else R.string.minisudoku_cell_not_selected,
        )
        return context.getString(
            R.string.minisudoku_cell_accessibility_label,
            index / puzzle.size + 1,
            index % puzzle.size + 1,
            digit,
            notes,
            editability,
            selection,
        )
    }

    private fun digitDescription(context: Context, digit: Int): String =
        context.getString(R.string.minisudoku_digit_button_accessibility, digit)

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
        const val BOARD_SIZE = 6
        const val SOLVED_EVENT_TIMEOUT_MILLIS = 10_000L
    }
}
