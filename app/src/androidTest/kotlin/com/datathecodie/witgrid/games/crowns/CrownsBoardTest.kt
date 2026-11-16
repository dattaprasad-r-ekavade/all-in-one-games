package com.datathecodie.witgrid.games.crowns

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class CrownsBoardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstBankPuzzleCanBeSolvedThroughCrownsBoard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val levelRef = CrownsGameModule.firstLevelRef(Pack.EASY, bank)
        val level = CrownsGameModule.loadLevel(levelRef, bank)
        val repository = InMemoryProgressRepository()
        val controller = runBlocking {
            PlayController.open(
                gameModule = CrownsGameModule,
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
                    LocalCrownsBoardOptions provides CrownsBoardOptions(hapticsEnabled = false),
                ) {
                    CrownsGameModule.Board(
                        state = snapshot.state,
                        onMoves = { moves -> coroutineScope.launch { controller.applyGesture(moves) } },
                        onUndo = { coroutineScope.launch { controller.undo() } },
                    )
                }
            }
        }

        level.puzzle.solutionColumns.forEachIndexed { row, column ->
            listOf(R.string.crowns_cell_empty, R.string.crowns_cell_crossed).forEach { markLabel ->
                val cellDescription = context.getString(
                    R.string.crowns_cell_accessibility_label,
                    row + 1,
                    column + 1,
                    ('A'.code + level.puzzle.regions[row * level.size + column]).toChar().toString(),
                    context.getString(markLabel),
                )
                composeRule.onNodeWithContentDescription(cellDescription).performTouchInput { click() }
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
