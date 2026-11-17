package com.datathecodie.witgrid.ui.screens.play

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameModule
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayLayoutMatrixTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun everyGameFitsPhoneSizesThemesAndFontScales() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val directory = File(context.cacheDir, "u4-play").apply { mkdirs() }
        var sample by mutableStateOf(Sample())
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, sample.fontScale)) {
                WitgridTheme(darkTheme = sample.dark) {
                    Box(Modifier.requiredSize(sample.width.dp, sample.height.dp).testTag("play-sample")) {
                        SamplePlay(sample, bank)
                    }
                }
            }
        }
        val clippedLabels = mutableListOf<String>()
        val toolLabels = listOf(R.string.button_undo, R.string.button_redo, R.string.button_restart,
            R.string.button_hint, R.string.button_check, R.string.minisudoku_mode_notes, R.string.play_erase)
            .map(context::getString)
        listOf(360 to 780, 432 to 936).forEach { (width, height) ->
            listOf(false, true).forEach { dark ->
                listOf(1f, 1.3f).forEach { scale ->
                    GameRegistry.games.forEach { module ->
                        composeRule.runOnIdle { sample = Sample(module.id, width, height, dark, scale) }
                        composeRule.waitForIdle()
                        val bitmap = composeRule.onNodeWithTag("play-sample").captureToImage().asAndroidBitmap()
                        assertEquals(width, bitmap.width)
                        assertEquals(height, bitmap.height)
                        val name = "${module.id}-$width-${if (dark) "dark" else "light"}-$scale.png"
                        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                        val root = composeRule.onNodeWithTag("play-sample").fetchSemanticsNode().boundsInRoot
                        val board = composeRule.onNodeWithTag("play-board").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                        val tools = composeRule.onNodeWithTag("play-toolbar").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                        assertTrue("Board must fit width", board.left >= root.left && board.right <= root.right)
                        assertTrue("Board must fit above tools", board.bottom <= tools.top)
                        assertEquals("Board slot is square", board.width, board.height, 1f)
                        composeRule.onNodeWithText(context.getString(R.string.button_check)).assertIsDisplayed()
                        composeRule.onNodeWithText(context.getString(R.string.button_hint)).assertIsDisplayed()
                        if (module.id == "minisudoku") {
                            val digits = composeRule.onNodeWithTag("play-digit-pad").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                            assertTrue("Digit pad fits above tools", digits.bottom <= tools.top)
                            composeRule.onNodeWithText(context.getString(R.string.play_erase)).assertIsDisplayed()
                        }
                        // All labels, including every row and column clue, must fit their measured boxes.
                        composeRule.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
                            val results = mutableListOf<TextLayoutResult>()
                            if (node.config.contains(SemanticsActions.GetTextLayoutResult)) node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                            results.forEach { result ->
                                // Compose rounds measured text boxes to integer pixels; allow that single pixel.
                                val fits = (0 until result.lineCount).all { line ->
                                    result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1f &&
                                        result.getLineBottom(line) <= result.size.height + 1f &&
                                        !result.isLineEllipsized(line)
                                }
                                if (!fits || (result.layoutInput.text.text in toolLabels && result.lineCount != 1)) clippedLabels.add("$sample: ${result.layoutInput.text}; size=${result.size}")
                            }
                        }

                    }
                }
            }
        }
        assertEquals(32, directory.listFiles()?.count { it.extension == "png" })
        assertTrue("Clipped labels: $clippedLabels", clippedLabels.isEmpty())
    }

    @Test
    fun timerRulesAndRestartActionsWork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var restarts = 0
        composeRule.setContent {
            WitgridTheme {
                PlayScreenLayout(LevelRef("crowns", "test", Pack.EASY, levelNumber = 613), 123_000,
                    true, true, false, null, null, false, board = {}, gameControls = {},
                    onUndo = {}, onRedo = {}, onRequestHint = {}, onCheck = {}, onBack = {},
                    onRestart = { restarts++ }, hintCredits = "3")
            }
        }
        composeRule.onNodeWithText("Easy " + '\u00b7' + " Level 613").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.button_hide_timer)).performClick()
        composeRule.onNodeWithText("2:03").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(context.getString(R.string.button_show_timer)).performClick()
        composeRule.onNodeWithText("2:03").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.play_rules)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.play_rules_crowns)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.button_restart)).performClick()
        composeRule.onAllNodes(hasText(context.getString(R.string.button_restart))).fetchSemanticsNodes().last().let {
            composeRule.runOnIdle { it.config[SemanticsActions.OnClick].action?.invoke() }
        }
        composeRule.runOnIdle { assertEquals(1, restarts) }
    }
}

private data class Sample(val gameId: String = "crowns", val width: Int = 360, val height: Int = 780,
    val dark: Boolean = false, val fontScale: Float = 1f)

@Composable
private fun SamplePlay(sample: Sample, bank: AssetBankSource) {
    @Suppress("UNCHECKED_CAST")
    val module = requireNotNull(GameRegistry.find(sample.gameId)) as GameModule<Any, Any>
    val ref = LevelRef.forLevel(613, module.levelRefs(Pack.EXPERT, bank))
    val state = module.newState(module.loadLevel(ref, bank))
    val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit, (@Composable () -> Unit)?) -> Unit = { board, controls, tools ->
        PlayScreenLayout(ref, 123_000, true, false, false, null, playPuzzleStatus(state), false,
            board = board, gameControls = controls, onUndo = {}, onRedo = {}, onRequestHint = {},
            onCheck = {}, onBack = {}, hintCredits = "3", modeTools = tools)
    }
    CompositionLocalProvider(LocalBoardViewportMode provides BoardViewportMode(embedded = true)) {
        when (state) {
            is MiniSudokuState -> MiniSudokuBoard(state, {}, {}, playLayout = { board, actions ->
                layout(board, { PlaySudokuDigits(state, actions) }, { PlaySudokuTools(actions) })
            })
            is NonogramState -> NonogramBoard(state, {}, {}, layout = { board, controls -> layout(board, controls, null) })
            else -> layout({ module.Board(state, {}, {}) }, {}, null)
        }
    }
}
