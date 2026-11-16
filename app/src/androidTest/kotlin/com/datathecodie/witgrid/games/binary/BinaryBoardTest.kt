package com.datathecodie.witgrid.games.binary

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
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
class BinaryBoardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstBankPuzzleCanBeSolvedThroughBinaryBoard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val levelRef = BinaryGameModule.firstLevelRef(Pack.EASY, bank)
        val level = BinaryGameModule.loadLevel(levelRef, bank)
        val repository = InMemoryProgressRepository()
        val controller = runBlocking {
            PlayController.open(
                gameModule = BinaryGameModule,
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
                    LocalBinaryBoardOptions provides BinaryBoardOptions(hapticsEnabled = false),
                ) {
                    BinaryGameModule.Board(
                        state = snapshot.state,
                        onMoves = { moves -> coroutineScope.launch { controller.applyGesture(moves) } },
                        onUndo = { coroutineScope.launch { controller.undo() } },
                    )
                }
            }
        }

        level.puzzle.solution.forEachIndexed { index, solution ->
            if (level.puzzle.givens[index] == null) {
                val tapCount = if (solution == 0) 1 else 2
                repeat(tapCount) {
                    val cellDescription = cellDescription(
                        context = context,
                        puzzle = level.puzzle,
                        state = controller.snapshot.value.state,
                        index = index,
                    )
                    composeRule.onNodeWithContentDescription(cellDescription).performTouchInput { click() }
                }
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
    fun tapCyclesSymbolsAndLongPressClearsWithoutChangingGivens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val level = BinaryGameModule.loadLevel(BinaryGameModule.firstLevelRef(Pack.EASY, bank), bank)
        var state by mutableStateOf(BinaryState.initial(level.puzzle))
        val editableIndex = level.puzzle.givens.indexOfFirst { it == null }
        val givenIndex = level.puzzle.givens.indexOfFirst { it != null }
        assertTrue(editableIndex >= 0 && givenIndex >= 0)
        var moveCount = 0

        composeRule.setContent {
            WitgridTheme {
                BinaryBoard(
                    state = state,
                    onMoves = { moves ->
                        moveCount += moves.size
                        moves.forEach { state = state.withMove(it) }
                    },
                    onUndo = {},
                    options = BinaryBoardOptions(hapticsEnabled = false),
                )
            }
        }

        listOf(BinarySymbol.A, BinarySymbol.B, BinarySymbol.EMPTY, BinarySymbol.A).forEach { expected ->
            composeRule.onNodeWithContentDescription(
                cellDescription(context, level.puzzle, state, editableIndex),
            ).performTouchInput { click() }
            composeRule.runOnIdle { assertEquals(expected, state.symbols[editableIndex]) }
        }
        composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state, editableIndex),
        ).performTouchInput { longClick() }
        composeRule.runOnIdle { assertEquals(BinarySymbol.EMPTY, state.symbols[editableIndex]) }

        val givenDescription = cellDescription(context, level.puzzle, state, givenIndex)
        composeRule.onNodeWithContentDescription(givenDescription).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(givenDescription).performTouchInput { longClick() }
        composeRule.runOnIdle {
            assertEquals(5, moveCount)
            assertEquals(BinaryState.initial(level.puzzle).symbols[givenIndex], state.symbols[givenIndex])
        }
    }

    @Test
    fun dragDoesNotPaintBinaryCells() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val levelRef = BinaryGameModule.firstLevelRef(Pack.EASY, bank)
        val level = BinaryGameModule.loadLevel(levelRef, bank)
        val state = BinaryState.initial(level.puzzle)
        val emittedMoves = mutableListOf<List<Move>>()

        composeRule.setContent {
            WitgridTheme {
                CompositionLocalProvider(
                    LocalBinaryBoardOptions provides BinaryBoardOptions(hapticsEnabled = false),
                ) {
                    BinaryGameModule.Board(
                        state = state,
                        onMoves = emittedMoves::add,
                        onUndo = {},
                    )
                }
            }
        }

        val emptyCells = level.puzzle.givens.indices.filter { level.puzzle.givens[it] == null }.take(2)
        assertTrue(emptyCells.size == 2)
        val start = composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state, emptyCells[0]),
        ).fetchSemanticsNode().boundsInRoot.center
        val end = composeRule.onNodeWithContentDescription(
            cellDescription(context, level.puzzle, state, emptyCells[1]),
        ).fetchSemanticsNode().boundsInRoot.center

        composeRule.onRoot().performTouchInput {
            down(start)
            moveTo(end, delayMillis = 200)
            up()
        }
        composeRule.runOnIdle { assertTrue(emittedMoves.isEmpty()) }
    }

    private fun cellDescription(
        context: android.content.Context,
        puzzle: BinaryPuzzle,
        state: BinaryState,
        index: Int,
    ): String {
        val symbolLabel = context.getString(
            when (state.symbols[index]) {
                BinarySymbol.EMPTY -> R.string.binary_mark_empty
                BinarySymbol.A -> R.string.binary_symbol_a
                BinarySymbol.B -> R.string.binary_symbol_b
            },
        )
        val statusLabel = context.getString(
            if (puzzle.givens[index] == null) R.string.binary_cell_editable else R.string.binary_cell_given,
        )
        return context.getString(
            R.string.binary_cell_accessibility_label,
            index / puzzle.size + 1,
            index % puzzle.size + 1,
            symbolLabel,
            statusLabel,
        )
    }

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
