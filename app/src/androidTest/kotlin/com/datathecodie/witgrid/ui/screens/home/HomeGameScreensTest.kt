package com.datathecodie.witgrid.ui.screens.home

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.ui.layout.TopBarAction
import com.datathecodie.witgrid.ui.layout.WitgridScreenFrame
import com.datathecodie.witgrid.ui.layout.WitgridTopBar
import com.datathecodie.witgrid.ui.screens.game.GameScreen
import com.datathecodie.witgrid.ui.screens.game.GameUiState
import com.datathecodie.witgrid.ui.screens.game.PackProgress
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeGameScreensTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val ids = listOf("crowns", "binary", "nonogram", "minisudoku")
    private fun level(game: String, number: Long = 14, pack: Pack = Pack.EASY) =
        LevelRef(game, "preview-$pack", pack, levelNumber = number)

    @Test
    fun homeHandlesOddGameCountAndStartsFirstUnfinishedDaily() {
        var daily: String? = null
        var selected: String? = null
        composeRule.setContent {
            WitgridTheme {
                HomeScreen(HomeUiState((ids + "future").map { HomeGameProgress(it, level(it)) }, false),
                    completedDailyGameIds = setOf("crowns", "nonogram"), onSelectGame = { selected = it },
                    onPlayDaily = { daily = it })
            }
        }
        composeRule.onNodeWithText("Play daily Binary").performClick()
        assertEquals("binary", daily)
        composeRule.onNodeWithText("Game").performScrollTo().performClick()
        assertEquals("future", selected)
    }

    @Test
    fun gamePackActionsStayIndependentAndRewardsAreBehindSheet() {
        var selected: LevelRef? = null
        var dailyOpens = 0
        var hints = 0
        val easy = level("crowns", 613)
        val medium = level("crowns", 27, Pack.MEDIUM)
        composeRule.setContent {
            WitgridTheme {
                GameScreen(GameUiState("crowns", listOf(PackProgress(Pack.EASY, easy, 6..6),
                    PackProgress(Pack.MEDIUM, medium, 7..7)), medium, false),
                    {}, { selected = it }, { dailyOpens++ }, dailyCompletedToday = true, hintCredits = 3, checkCredits = 1,
                    onEarnHintCredit = { hints++; com.datathecodie.witgrid.ads.AdShowResult.RewardGranted })
            }
        }
        composeRule.onNodeWithText("Daily Crowns").performScrollTo().performClick()
        assertEquals(1, dailyOpens)
        composeRule.onNodeWithText("Next: level 613").performScrollTo().performClick()
        assertEquals(easy, selected)
        composeRule.onNodeWithText("3 hints and 1 check saved").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Get more", substring = true).performScrollTo().performClick()
        composeRule.onNodeWithText("Watch an ad to earn one hint credit").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, hints)
    }

    @Test
    fun captureLightAndDarkAtDefaultAndLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "u6-screens").apply { mkdirs() }
        val bankSource = AssetBankSource(context.assets)
        val puzzleRef = CrownsGameModule.levelRefs(Pack.EASY, bankSource).first { !it.isIntro }
        val puzzleLevel = CrownsGameModule.loadLevel(puzzleRef, bankSource)
        val boardState = CrownsState(
            puzzleLevel.puzzle,
            List(puzzleLevel.size * puzzleLevel.size) { index ->
                if (index == 0 || index == puzzleLevel.size + 2) Mark.FILLED else Mark.EMPTY
            },
        )
        val continueRef = level("crowns", 14)
        val continueProgress = LevelProgress(
            gameId = "crowns",
            levelId = continueRef.levelId,
            stateBytes = CrownsGameModule.encodeState(boardState),
            elapsedTimeMillis = 102_000,
            hintsUsed = 1,
            completed = false,
            bestTimeMillis = null,
            attemptNumber = 14,
        )
        var dark by mutableStateOf(false)
        var game by mutableStateOf(false)
        var scale by mutableStateOf(1f)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                WitgridTheme(darkTheme = dark) {
                    Surface(Modifier.requiredSize(390.dp, 844.dp).testTag("sample"),
                        color = WitgridTheme.colors.paper, contentColor = WitgridTheme.colors.ink) {
                        WitgridScreenFrame(topBar = {
                            WitgridTopBar(if (game) "Crowns" else "Witgrid", onBack = if (game) ({}) else null,
                                showLogo = !game, actions = if (game) listOf(TopBarAction(R.drawable.ic_info, "Rules", {})) else listOf(
                                    TopBarAction(R.drawable.ic_calendar, "Calendar", {}),
                                    TopBarAction(R.drawable.ic_statistics, "Statistics", {}),
                                    TopBarAction(R.drawable.ic_settings, "Settings", {})))
                        }) {
                            if (game) {
                                GameScreen(GameUiState("crowns", Pack.entries.mapIndexed { index, pack ->
                                    val sizeRange = if (pack == Pack.EXPERT) 9..10 else (6 + index)..(6 + index)
                                    PackProgress(pack, level("crowns", if (index == 0) 14 else 1, pack), sizeRange)
                                }, continueRef, false, continueProgress = continueProgress,
                                    continueState = boardState), {}, {}, {}, dailyCompletedToday = true,
                                    hintCredits = 3, checkCredits = 1)
                            } else {
                                HomeScreen(HomeUiState(ids.map { HomeGameProgress(it, level(it)) }, false),
                                    StreakData(currentDays = 3), setOf("crowns", "nonogram"), {}, {},
                                    todayLabel = "Tuesday, 17 November")
                            }
                        }
                    }
                }
            }
        }
        listOf(1f, 1.3f).forEach { font -> listOf(false, true).forEach { theme -> listOf(false, true).forEach { screen ->
            composeRule.runOnIdle { dark = theme; game = screen; scale = font }
            composeRule.waitForIdle()
            composeRule.waitUntil(10_000) { composeRule.activity.hasWindowFocus() }
            val bitmap = composeRule.onNodeWithTag("sample").captureToImage().asAndroidBitmap()
            File(directory, "${if (screen) "game" else "home"}-${if (theme) "dark" else "light"}-$font.png")
                .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        } } }
    }
}
