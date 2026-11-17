package com.datathecodie.witgrid.ui.screens.result

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletion
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.ReminderSettings
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.data.settings.ThemeMode
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.domain.stats.GameStatistics
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.reminder.ReminderEnableResult
import com.datathecodie.witgrid.ui.layout.WitgridScreenFrame
import com.datathecodie.witgrid.ui.layout.WitgridTopBar
import com.datathecodie.witgrid.ui.screens.calendar.CalendarScreen
import com.datathecodie.witgrid.ui.screens.failed.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.settings.SettingsScreen
import com.datathecodie.witgrid.ui.screens.stats.StatisticsScreen
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.io.File
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilityScreensTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val level = LevelRef("crowns", "preview", Pack.EASY, levelNumber = 14)

    @Test
    fun resultKeepsNextSingleUseAndReturnsToPacks() {
        var next = 0
        composeRule.setContent {
            WitgridTheme { ResultScreen(168_000, 168_000, { next++ }, {}, level, 1) }
        }
        composeRule.onNodeWithText("Crowns · Easy · Level 14").assertIsDisplayed()
        composeRule.onNodeWithText("Personal best").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next level").performScrollTo().performClick()
        composeRule.onNodeWithText("Next level").performClick()
        assertEquals(1, next)
    }

    @Test
    fun premiumContinueKeepsBoardAndNeverRequestsAnAd() {
        var continues = 0
        var ads = 0
        composeRule.setContent {
            WitgridTheme {
                LevelFailedScreen({ continues++ }, {}, {}, levelRef = level, premiumEnabled = true,
                    onRewardedContinue = { ads++; com.datathecodie.witgrid.ads.AdShowResult.Unavailable })
            }
        }
        val continueButton = composeRule.onNodeWithText("Continue with Premium and one extra mistake")
        continueButton.performScrollTo().assertIsDisplayed()
        val minimumHeight = with(composeRule.density) { 54.dp.toPx() }
        assertTrue(continueButton.fetchSemanticsNode().boundsInRoot.height >= minimumHeight)
        continueButton.performClick()
        composeRule.waitForIdle()
        assertEquals(1, continues)
        assertEquals(0, ads)
    }

    @Test
    fun settingsPersistsThemeAndColourwayAndOpensReminder() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            WitgridTheme {
                SettingsScreen(settings, ReminderSettings(), null, false, { settings = it },
                    { ReminderEnableResult.Enabled }, {}, { _, _ -> }, {})
            }
        }
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.DARK, settings.themeMode)
        composeRule.onNodeWithContentDescription("Forest").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("forest", settings.colourwayId)
        composeRule.onNodeWithText("Daily reminder").performScrollTo().performClick()
        composeRule.onNodeWithText("Remind me each day").assertIsDisplayed()
    }

    @Test
    fun reminderDialogFitsAtSystemFontScale() {
        val configuration = composeRule.activity.resources.configuration
        val deviceWidthDp = configuration.screenWidthDp
        assertTrue(configuration.fontScale >= 1.8f)
        assertTrue(deviceWidthDp == 360 || deviceWidthDp == 480)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permissionMessage = context.getString(R.string.settings_reminder_permission_denied)
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "u7-reminder-dialog").apply {
            mkdirs()
        }
        var dark by mutableStateOf(false)
        composeRule.setContent {
            WitgridTheme(darkTheme = dark) {
                WitgridScreenFrame({ WitgridTopBar("Settings", onBack = {}) }) {
                    SettingsScreen(
                        AppSettings(),
                        ReminderSettings(true, 20, 0),
                        R.string.settings_reminder_permission_denied,
                        true,
                        {},
                        { ReminderEnableResult.Enabled },
                        {},
                        { _, _ -> },
                        {},
                    )
                }
            }
        }

        listOf(false, true).forEach { darkTheme ->
            composeRule.runOnIdle { dark = darkTheme }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Daily reminder").performScrollTo().performClick()
            composeRule.onNodeWithText("Remind me each day").assertIsDisplayed()
            composeRule.onNodeWithText("Reminder time").assertIsDisplayed()
            composeRule.onAllNodesWithText(permissionMessage).assertCountEquals(2)
            val themeName = if (darkTheme) "dark" else "light"
            captureDeviceScreenshot(directory, "settings-reminder-$themeName-${deviceWidthDp}dp-font2.0")
            composeRule.onNodeWithText("Close").performClick()
        }
    }

    @Test
    fun captureFiveScreensInLightDarkAndLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssetBankSource(context.assets)
        val ref = CrownsGameModule.levelRefs(Pack.EASY, bank).first { !it.isIntro }
        val puzzle = CrownsGameModule.loadLevel(ref, bank).puzzle
        val solvedState = CrownsState(puzzle, List(puzzle.size * puzzle.size) { index ->
            if (index % puzzle.size == puzzle.solutionColumns[index / puzzle.size]) Mark.FILLED else Mark.EMPTY
        })
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "u7-screens").apply { mkdirs() }
        var screen by mutableStateOf("result")
        var dark by mutableStateOf(false)
        var font by mutableStateOf(1f)
        var width by mutableStateOf(390)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font)) {
                WitgridTheme(darkTheme = dark) {
                    Surface(Modifier.requiredSize(width.dp, 844.dp).testTag("sample"),
                        color = WitgridTheme.colors.paper, contentColor = WitgridTheme.colors.ink) {
                        when (screen) {
                            "result" -> ResultScreen(168_000, 168_000, {}, {}, level, 1, solvedState)
                            "failed" -> LevelFailedScreen({}, {}, {}, level, solvedState, premiumEnabled = true)
                            "calendar" -> WitgridScreenFrame({ WitgridTopBar("Calendar", onBack = {}) }) {
                                CalendarScreen(20774, TimeZone.getTimeZone("UTC"), Locale.US, 20758, 20774,
                                    StreakData(currentDays = 3, freezesAvailable = 1), false, false, false,
                                    com.datathecodie.witgrid.billing.BillingUiState(), {},
                                    listOf(DailyPuzzleCompletion(20774, "crowns", "sample"),
                                        DailyPuzzleCompletion(20773, "binary", "previous")),
                                    listOf("crowns", "binary", "nonogram", "minisudoku"), false, {}, {}, {}, {},
                                    { com.datathecodie.witgrid.ads.AdShowResult.Unavailable })
                            }
                            "statistics" -> WitgridScreenFrame({ WitgridTopBar("Statistics", onBack = {}) }) {
                                StatisticsScreen(false, listOf("crowns", "binary", "nonogram", "minisudoku").map {
                                    GameStatistics(it, 24, 3, 74_000, 108_000)
                                })
                            }
                            else -> WitgridScreenFrame({ WitgridTopBar("Settings", onBack = {}) }) {
                                SettingsScreen(AppSettings(crownsAutoXEnabled = true, nonogramAutoCrossEnabled = true),
                                    ReminderSettings(true, 20, 0), null, true, {},
                                    { ReminderEnableResult.Enabled }, {}, { _, _ -> }, {},
                                    billingState = com.datathecodie.witgrid.billing.BillingUiState(productPrice = "$7.99"))
                            }
                        }
                    }
                }
            }
        }
        listOf(390 to 1f, 390 to 1.3f, 360 to 2f, 480 to 2f).forEach { (phoneWidth, scale) ->
            listOf(false, true).forEach { theme ->
                listOf("result", "failed", "calendar", "statistics", "settings").forEach { name ->
                    composeRule.runOnIdle { screen = name; dark = theme; font = scale; width = phoneWidth }
                    composeRule.waitForIdle()
                    val suffix = "${if (theme) "dark" else "light"}-$scale-${phoneWidth}dp"
                    capture(directory, "$name-$suffix")
                    if (name == "calendar" && scale == 2f && phoneWidth == 360) {
                        composeRule.onNodeWithContentDescription("Sunday").assertIsDisplayed()
                    }
                    if (name == "settings") {
                        composeRule.onNodeWithText("Open-source licences").performScrollTo().assertIsDisplayed()
                        capture(directory, "settings-more-$suffix")
                    } else if (scale == 2f) {
                        val bottom = when (name) {
                            "result" -> "Back to packs"
                            "failed" -> "Restart"
                            "calendar" -> "Mini Sudoku"
                            else -> "Mini Sudoku"
                        }
                        composeRule.onNodeWithText(bottom).performScrollTo().assertIsDisplayed()
                        capture(directory, "$name-bottom-$suffix")
                    }
                }
            }
        }
    }

    private fun capture(directory: File, name: String) {
        val bitmap = composeRule.onNodeWithTag("sample").captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun captureDeviceScreenshot(directory: File, name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?: error("Unable to capture reminder dialog screenshot")
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
