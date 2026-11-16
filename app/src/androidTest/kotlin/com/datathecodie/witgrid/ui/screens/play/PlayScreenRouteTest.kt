package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayScreenRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun miniSudokuModesAndSolveWorkThroughPlayRouteWithControlsInBottomThird() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val module = MiniSudokuGameModule
        val levelRef = module.firstLevelRef(Pack.EASY, bank)
        val level = module.loadLevel(levelRef, bank)
        val repository = MemoryProgressRepository()
        val persistence = PlayPersistenceScope(Dispatchers.Main.immediate)
        val solved = AtomicBoolean(false)
        composeRule.setContent {
            WitgridTheme {
                PlayScreenRoute(
                    levelRef = levelRef,
                    gameModule = module,
                    bankSource = bank,
                    progressRepository = repository,
                    persistence = persistence,
                    restartRequestId = null,
                    onRestartRequestConsumed = {},
                    onSolved = { solved.set(true) },
                    onBack = {},
                )
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription("Row 1, column 1,", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val notes = composeRule.onNodeWithText(context.getString(R.string.minisudoku_mode_notes))
        assertTrue(notes.fetchSemanticsNode().boundsInRoot.top >= root.top + root.height * 2f / 3f - 1f)
        val firstCellBounds = composeRule.onNodeWithContentDescription(
            "Row 1, column 1,",
            substring = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(firstCellBounds.top >= root.top + root.height * 0.15f - 1f)

        val editable = level.puzzle.givens.indices.filter { level.puzzle.givens[it] == null }
        val first = editable.first()
        selectCell(first)
        notes.performClick()
        val digit = level.puzzle.solution[first]
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.minisudoku_note_button_accessibility, digit),
        ).performClick()
        composeRule.waitUntil(5_000) { repository.savedProgress != null }
        assertEquals(1 shl (digit - 1), module.decodeState(requireNotNull(repository.savedProgress).stateBytes).notes[first])
        composeRule.onNodeWithText(context.getString(R.string.minisudoku_mode_digits)).performClick()

        editable.forEach { index ->
            selectCell(index)
            val solution = level.puzzle.solution[index]
            composeRule.onNodeWithContentDescription(
                context.getString(R.string.minisudoku_digit_button_accessibility, solution),
            ).performClick()
        }
        composeRule.waitUntil(10_000) { solved.get() }
        assertTrue(repository.savedProgress?.completed == true)
        composeRule.runOnIdle { persistence.close {} }
    }

    private fun selectCell(index: Int) {
        composeRule.onNodeWithContentDescription(
            "Row ${index / 6 + 1}, column ${index % 6 + 1},",
            substring = true,
        ).performClick()
    }

    private class MemoryProgressRepository : ProgressRepository {
        @Volatile
        var savedProgress: LevelProgress? = null

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> =
            flowOf(savedProgress)

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? = savedProgress

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            savedProgress = progress
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            savedProgress = null
        }
    }
}
