package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HintRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun checkAndMistakeHintFlagCellsThenLogicalHintApplies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val module = MiniSudokuGameModule
        val bank = AssetBankSource(context.assets)
        val ref = module.firstLevelRef(Pack.EASY, bank)
        val level = module.loadLevel(ref, bank)
        val repository = MemoryProgressRepository()
        val persistence = PlayPersistenceScope(Dispatchers.Main.immediate)
        composeRule.setContent {
            WitgridTheme {
                PlayScreenRoute(ref, module, bank, repository, persistence, null, {}, {}, {})
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription("Row 1, column 1,", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val index = level.puzzle.givens.indices.first { level.puzzle.givens[it] == null }
        val cellPrefix = "Row ${index / 6 + 1}, column ${index % 6 + 1},"
        composeRule.onNodeWithContentDescription(cellPrefix, substring = true).performClick()
        val wrongDigit = level.puzzle.solution[index] % 6 + 1
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.minisudoku_digit_button_accessibility, wrongDigit),
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.button_check)).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("highlighted", substring = true).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.button_hint)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.hint_mistake_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.button_dismiss_hint)).performClick()
        composeRule.runOnIdle { assertEquals(1, repository.savedProgress?.hintsUsed) }
        composeRule.onNodeWithText(context.getString(R.string.button_undo)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.button_hint)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.button_apply_hint)).performClick()
        composeRule.waitUntil(5_000) { repository.savedProgress?.hintsUsed == 2 }
        composeRule.runOnIdle { persistence.close {} }
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
