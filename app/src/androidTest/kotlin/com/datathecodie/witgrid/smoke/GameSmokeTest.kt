package com.datathecodie.witgrid.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.MainActivity
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.WitgridDatabase
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuPuzzle
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.binary.BinaryGameModule
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
import com.datathecodie.witgrid.games.nonogram.NonogramGameModule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun crownsCanBePlayedFromHomeToResult() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearGameProgress(context, "crowns")
        openIntro(context, R.string.game_crowns, R.string.intro_coach_crowns)

        val bank = AssetBankSource(context.assets)
        val level = CrownsGameModule.loadLevel(CrownsGameModule.firstLevelRef(Pack.EASY, bank), bank)
        level.puzzle.solutionColumns.forEachIndexed { row, column ->
            val regionLetter = ('A'.code + level.puzzle.regions[row * level.size + column]).toChar().toString()
            listOf(R.string.crowns_cell_empty, R.string.crowns_cell_crossed).forEach { markResource ->
                val description = context.getString(
                    R.string.crowns_cell_accessibility_label,
                    row + 1,
                    column + 1,
                    regionLetter,
                    context.getString(markResource),
                )
                clickCell(description)
            }
        }

        assertResult(context)
    }

    @Test
    fun binaryCanBePlayedFromHomeToResult() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearGameProgress(context, "binary")
        openIntro(context, R.string.game_binary, R.string.intro_coach_binary)

        val bank = AssetBankSource(context.assets)
        val level = BinaryGameModule.loadLevel(BinaryGameModule.firstLevelRef(Pack.EASY, bank), bank)
        level.puzzle.solution.forEachIndexed { index, value ->
            if (level.puzzle.givens[index] == null) {
                val symbolsBeforeTap = if (value == 0) {
                    listOf(BinarySymbol.EMPTY)
                } else {
                    listOf(BinarySymbol.EMPTY, BinarySymbol.A)
                }
                symbolsBeforeTap.forEach { symbol ->
                    clickCell(binaryCellDescription(context, level.puzzle, index, symbol))
                }
            }
        }

        assertResult(context)
    }

    @Test
    fun nonogramCanBePlayedFromHomeToResult() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearGameProgress(context, "nonogram")
        openIntro(context, R.string.game_nonogram, R.string.intro_coach_nonogram)

        val bank = AssetBankSource(context.assets)
        val level = NonogramGameModule.loadLevel(NonogramGameModule.firstLevelRef(Pack.EASY, bank), bank)
        level.puzzle.solution.forEachIndexed { index, value ->
            val modeResource = if (value == 1) R.string.nonogram_mode_fill else R.string.nonogram_mode_cross
            clickText(context.getString(modeResource))
            val description = context.getString(
                R.string.nonogram_cell_accessibility_label,
                index / level.size + 1,
                index % level.size + 1,
                context.getString(R.string.nonogram_mark_empty),
            )
            clickCell(description)
        }

        assertResult(context)
    }

    @Test
    fun miniSudokuCanBePlayedFromHomeToResult() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearGameProgress(context, "minisudoku")
        openIntro(context, R.string.game_minisudoku, R.string.intro_coach_minisudoku)

        val bank = AssetBankSource(context.assets)
        val level = MiniSudokuGameModule.loadLevel(MiniSudokuGameModule.firstLevelRef(Pack.EASY, bank), bank)
        var state = MiniSudokuGameModule.engine.initialState(level.puzzle)
        level.puzzle.solution.forEachIndexed { index, digit ->
            if (level.puzzle.givens[index] == null) {
                clickCell(miniSudokuCellDescription(context, level.puzzle, state, index))
                clickContentDescription(context.getString(R.string.minisudoku_digit_button_accessibility, digit))
                state = state.withMove(Move.SetDigit(Cell(index), digit))
            }
        }

        assertResult(context)
    }

    private fun openIntro(context: android.content.Context, gameNameResource: Int, coachLineResource: Int) {
        waitForText(context.getString(gameNameResource))
        clickText(context.getString(gameNameResource))

        val continueText = context.getString(R.string.button_continue)
        waitForEnabledText(continueText)
        clickText(continueText)
        waitForText(context.getString(coachLineResource))
    }

    private fun assertResult(context: android.content.Context) {
        val resultTitle = context.getString(R.string.result_solved)
        waitForText(resultTitle)
        composeRule.onNodeWithText(resultTitle).assertIsDisplayed()
    }

    private fun clickCell(description: String) {
        val cell = composeRule.onNodeWithContentDescription(description, useUnmergedTree = true)
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runCatching { cell.fetchSemanticsNode() }.isSuccess
        }
        if (runCatching { cell.assertIsDisplayed() }.isFailure) cell.performScrollTo()
        cell.assertIsDisplayed().performClick()
    }

    private fun clickContentDescription(description: String) {
        composeRule.onNodeWithContentDescription(description)
            .performScrollTo()
            .performClick()
    }

    private fun clickText(text: String) {
        composeRule.onNodeWithText(text)
            .performScrollTo()
            .performClick()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runCatching { composeRule.onNodeWithText(text).assertIsDisplayed() }.isSuccess
        }
    }

    private fun waitForEnabledText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runCatching { composeRule.onNodeWithText(text).assertIsEnabled() }.isSuccess
        }
    }

    private fun clearGameProgress(context: android.content.Context, gameId: String) {
        val database = WitgridDatabase.create(context)
        try {
            runBlocking {
                database.levelProgressDao().getForGame(gameId).forEach { progress ->
                    database.levelProgressDao().delete(gameId, progress.levelId)
                }
            }
        } finally {
            database.close()
        }
    }

    private fun binaryCellDescription(
        context: android.content.Context,
        puzzle: BinaryPuzzle,
        index: Int,
        symbol: BinarySymbol,
    ): String {
        val symbolResource = when (symbol) {
            BinarySymbol.EMPTY -> R.string.binary_mark_empty
            BinarySymbol.A -> R.string.binary_symbol_a
            BinarySymbol.B -> R.string.binary_symbol_b
        }
        val statusResource = if (puzzle.givens[index] == null) {
            R.string.binary_cell_editable
        } else {
            R.string.binary_cell_given
        }
        return context.getString(
            R.string.binary_cell_accessibility_label,
            index / puzzle.size + 1,
            index % puzzle.size + 1,
            context.getString(symbolResource),
            context.getString(statusResource),
        )
    }

    private fun miniSudokuCellDescription(
        context: android.content.Context,
        puzzle: MiniSudokuPuzzle,
        state: MiniSudokuState,
        index: Int,
    ): String {
        val digit = state.digits[index]?.toString() ?: context.getString(R.string.minisudoku_cell_empty)
        val noteDigits = (1..puzzle.size)
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
        val selection = context.getString(R.string.minisudoku_cell_not_selected)
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

    private companion object {
        const val WAIT_TIMEOUT_MILLIS = 20_000L
    }
}
