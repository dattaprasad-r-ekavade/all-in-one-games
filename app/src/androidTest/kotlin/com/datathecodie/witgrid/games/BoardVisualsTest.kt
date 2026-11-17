package com.datathecodie.witgrid.games

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.crowns.CrownsBoardOptions
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.screens.play.PlayScreenLayout
import com.datathecodie.witgrid.ui.screens.play.PlaySudokuDigits
import com.datathecodie.witgrid.ui.screens.play.PlaySudokuTools
import com.datathecodie.witgrid.ui.screens.play.playPuzzleStatus
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardVisualsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun markedBoardsInBothThemes() {
        assertFalse(AppSettings().crownsShowRegionAids)
        assertFalse(CrownsBoardOptions().showRegionAids)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val directory = File(context.cacheDir, "u5-boards").apply { mkdirs() }
        var gameId by mutableStateOf("crowns")
        var dark by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                WitgridTheme(darkTheme = dark) {
                    Box(Modifier.requiredSize(390.dp, 844.dp).testTag("sample")) {
                        VisualPlay(gameId, bank)
                    }
                }
            }
        }
        SystemClock.sleep(2_000)
        listOf(false, true).forEach { darkTheme ->
            GameRegistry.games.forEach { module ->
                composeRule.runOnIdle { gameId = module.id; dark = darkTheme }
                composeRule.waitForIdle()
                if (module.id == "minisudoku" || module.id == "nonogram") {
                    composeRule.onNodeWithContentDescription("Row 3, column 4", substring = true).performClick()
                }
                composeRule.waitUntil(timeoutMillis = 10_000) { composeRule.activity.hasWindowFocus() }
                composeRule.onNodeWithTag("play-board").assertIsDisplayed()
                composeRule.onNodeWithTag("play-toolbar").assertIsDisplayed()
                // UiAutomation captures the display after Compose has submitted its frame.
                SystemClock.sleep(1_000)
                val bounds = composeRule.onNodeWithTag("sample").fetchSemanticsNode().boundsInWindow
                val screen = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                val bitmap = Bitmap.createBitmap(screen, bounds.left.toInt(), bounds.top.toInt(),
                    bounds.width.toInt(), bounds.height.toInt())
                File(directory, "${module.id}-${if (darkTheme) "dark" else "light"}.png")
                    .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                screen.recycle()
            }
        }
    }
}
@Composable
private fun VisualPlay(gameId: String, bank: AssetBankSource) {
    @Suppress("UNCHECKED_CAST")
    val module = requireNotNull(GameRegistry.find(gameId)) as GameModule<Any, Any>
    val ref = module.firstLevelRef(if (gameId == "nonogram") Pack.MEDIUM else Pack.EASY, bank)
    val initial = module.newState(module.loadLevel(ref, bank))
    val state = when (initial) {
        is CrownsState -> {
            val crowns = initial.puzzle.solutionColumns.mapIndexed { row, column -> row * initial.puzzle.size + column }.take(2)
            CrownsState(initial.puzzle, initial.marks.mapIndexed { index, _ ->
                when { index in crowns -> Mark.FILLED; index % 3 == 0 -> Mark.CROSSED; else -> Mark.EMPTY }
            })
        }
        is NonogramState -> NonogramState(initial.puzzle, initial.marks.mapIndexed { index, _ ->
            when { index < initial.puzzle.size * 3 && initial.puzzle.solution[index] == 1 -> Mark.FILLED
                index < initial.puzzle.size * 3 -> Mark.CROSSED
                else -> Mark.EMPTY }
        })
        is MiniSudokuState -> {
            var marked: MiniSudokuState = initial
            initial.puzzle.givens.indices.filter { initial.puzzle.givens[it] == null }.take(6)
                .forEachIndexed { offset, index ->
                    marked = if (offset < 3) marked.withMove(Move.SetDigit(Cell(index), initial.puzzle.solution[index]))
                    else marked.withMove(Move.SetNotes(Cell(index), 0b110101))
                }
            marked
        }
        else -> initial
    }
    val frame: @Composable (@Composable () -> Unit, @Composable () -> Unit, (@Composable () -> Unit)?) -> Unit = { board, controls, tools ->
        PlayScreenLayout(ref, 123_000, true, false, false, null, playPuzzleStatus(state), false,
            board = board, gameControls = controls, onUndo = {}, onRedo = {}, onRequestHint = {},
            onCheck = {}, onBack = {}, hintCredits = "3", modeTools = tools)
    }
    CompositionLocalProvider(LocalBoardViewportMode provides BoardViewportMode(embedded = true)) {
        when (state) {
            is MiniSudokuState -> MiniSudokuBoard(state, {}, {}, playLayout = { board, actions ->
                frame(board, { PlaySudokuDigits(state, actions) }, { PlaySudokuTools(actions) })
            })
            is NonogramState -> NonogramBoard(state, {}, {}, layout = { board, controls -> frame(board, controls, null) })
            else -> frame({ module.Board(state, {}, {}) }, {}, null)
        }
    }
}
