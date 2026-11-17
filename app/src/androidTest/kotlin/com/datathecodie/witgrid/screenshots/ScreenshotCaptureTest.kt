package com.datathecodie.witgrid.screenshots

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.MainActivity
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.billing.BillingNotice
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameModule
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.binary.BinaryGameModule
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.games.nonogram.NonogramGameModule
import com.datathecodie.witgrid.ui.screens.game.GameScreen
import com.datathecodie.witgrid.ui.screens.game.GameUiState
import com.datathecodie.witgrid.ui.screens.game.PackProgress
import com.datathecodie.witgrid.ui.screens.home.HomeGameProgress
import com.datathecodie.witgrid.ui.screens.home.HomeScreen
import com.datathecodie.witgrid.ui.screens.home.HomeUiState
import com.datathecodie.witgrid.ui.screens.play.PlayScreenLayout
import com.datathecodie.witgrid.ui.screens.result.ResultScreen
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capturesEightStoreScreens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        deleteExistingScreenshots(context)

        showHome()
        captureScreenshot("home", context)

        showHome(darkTheme = true)
        captureScreenshot("dark_mode", context)

        val bankSource = AssetBankSource(context.assets)
        captureBoard("crowns_board", CrownsGameModule, bankSource, context)
        captureBoard("binary_board", BinaryGameModule, bankSource, context)
        captureBoard("nonogram_board", NonogramGameModule, bankSource, context)
        captureBoard("mini_sudoku_board", MiniSudokuGameModule, bankSource, context)

        showResult()
        captureScreenshot("result", context)

        showOfflineMessage(context)
        captureScreenshot("offline_message", context)
    }

    private fun showHome(darkTheme: Boolean = false) {
        setScreenContent(darkTheme) {
            HomeScreen(
                state = HomeUiState(
                    games = listOf(
                        HomeGameProgress("crowns", completedLevels = 6, totalLevels = 64),
                        HomeGameProgress("binary", completedLevels = 12, totalLevels = 72),
                        HomeGameProgress("nonogram", completedLevels = 4, totalLevels = 80),
                        HomeGameProgress("minisudoku", completedLevels = 9, totalLevels = 60),
                    ),
                    isLoading = false,
                ),
                streak = StreakData(currentDays = 5, freezesAvailable = 1),
                completedDailyGamesToday = 2,
                onSelectGame = {},
                onOpenCalendar = {},
                onOpenStatistics = {},
                onOpenSettings = {},
            )
        }
        waitForText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.screen_home))
        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.screen_home),
        ).performScrollTo()
    }

    private fun <P : Any, S : Any> captureBoard(
        name: String,
        module: GameModule<P, S>,
        bankSource: AssetBankSource,
        context: android.content.Context,
    ) {
        val levelRef = module.levelRefs(Pack.EASY, bankSource)
            .firstOrNull { ref -> !ref.isIntro }
            ?: module.firstLevelRef(Pack.EASY, bankSource)
        val level = module.loadLevel(levelRef, bankSource)
        val state = module.newState(level)
        setScreenContent {
            BoardCaptureContent(module = module, levelRef = levelRef, state = state)
        }
        composeRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodes(
                hasContentDescription(ROW_ONE_CELL_DESCRIPTION, substring = true),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        captureScreenshot(name, context)
    }

    private fun showResult() {
        setScreenContent {
            ResultScreen(
                elapsedTimeMillis = 74_000L,
                bestTimeMillis = 74_000L,
                onNext = {},
                onHome = {},
            )
        }
        waitForText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.screen_result))
    }

    private fun showOfflineMessage(context: android.content.Context) {
        val levelRef = CrownsGameModule.firstLevelRef(
            Pack.EASY,
            AssetBankSource(context.assets),
        )
        val offlineMessage = context.getString(R.string.premium_store_unavailable)
        setScreenContent {
            Box {
                GameScreen(
                    state = GameUiState(
                        gameId = levelRef.gameId,
                        packs = Pack.entries.map { pack ->
                            PackProgress(
                                pack = pack,
                                completedLevels = 0,
                                totalLevels = 16,
                                nextLevelRef = levelRef.copy(pack = pack),
                            )
                        },
                        continueLevelRef = levelRef,
                        isLoading = false,
                    ),
                    onContinue = {},
                    onSelectPack = {},
                    onOpenDaily = {},
                    onHome = {},
                )
                PremiumOfferDialog(
                    premiumEnabled = false,
                    billingState = BillingUiState(notice = BillingNotice.STORE_UNAVAILABLE),
                    onPurchasePremium = {},
                    onDismiss = {},
                )
            }
        }
        waitForText(offlineMessage)
    }

    private fun captureScreenshot(name: String, context: android.content.Context) {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SCREENSHOT_SETTLE_MILLIS)
        SystemClock.sleep(SCREENSHOT_SETTLE_MILLIS)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val screenshotUri = requireNotNull(
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "$PICTURES_DIRECTORY/$SCREENSHOT_DIRECTORY_NAME",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                },
            ),
        )
        try {
            checkNotNull(context.contentResolver.openOutputStream(screenshotUri)).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            check(
                context.contentResolver.update(
                    screenshotUri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                ) == 1,
            )
        } catch (exception: Exception) {
            context.contentResolver.delete(screenshotUri, null, null)
            throw exception
        } finally {
            bitmap.recycle()
        }
    }

    private fun deleteExistingScreenshots(context: android.content.Context) {
        SCREENSHOT_NAMES.forEach { name ->
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf("$name.png", "$PICTURES_DIRECTORY/$SCREENSHOT_DIRECTORY_NAME/"),
            )
        }
    }

    private fun setScreenContent(darkTheme: Boolean = false, content: @Composable () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            composeRule.activity.setContent {
                WitgridTheme(darkTheme = darkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                        ) {
                            content()
                        }
                    }
                }
            }
            val systemBars = WindowCompat.getInsetsController(
                composeRule.activity.window,
                composeRule.activity.window.decorView,
            )
            systemBars.isAppearanceLightStatusBars = !darkTheme
            systemBars.isAppearanceLightNavigationBars = !darkTheme
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun <P : Any, S : Any> BoardCaptureContent(
        module: GameModule<P, S>,
        levelRef: LevelRef,
        state: S,
    ) {
        val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit) -> Unit =
            { board, controls ->
                PlayScreenLayout(
                    levelRef = levelRef,
                    elapsedTimeMillis = 0L,
                    canUndo = false,
                    canRedo = false,
                    saveFailed = false,
                    introCoachLine = null,
                    statusMessage = null,
                    hintOperationPending = false,
                    board = board,
                    gameControls = controls,
                    onUndo = {},
                    onRedo = {},
                    onRequestHint = {},
                    onCheck = {},
                    onBack = {},
                )
            }

        when (module.id) {
            "minisudoku" -> MiniSudokuBoard(
                state = state as MiniSudokuState,
                onMoves = {},
                onUndo = {},
                layout = layout,
            )

            "nonogram" -> NonogramBoard(
                state = state as NonogramState,
                onMoves = {},
                onUndo = {},
                layout = layout,
            )

            else -> layout(
                { module.Board(state, onMoves = {}, onUndo = {}) },
                {},
            )
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runCatching { composeRule.onNodeWithText(text).assertIsDisplayed() }.isSuccess
        }
    }

    private companion object {
        const val SCREENSHOT_DIRECTORY_NAME = "WitgridCapture"
        val PICTURES_DIRECTORY = Environment.DIRECTORY_PICTURES
        const val ROW_ONE_CELL_DESCRIPTION = "Row 1, column"
        const val WAIT_TIMEOUT_MILLIS = 30_000L
        const val SCREENSHOT_SETTLE_MILLIS = 1_000L
        val SCREENSHOT_NAMES = listOf(
            "home",
            "dark_mode",
            "crowns_board",
            "binary_board",
            "nonogram_board",
            "mini_sudoku_board",
            "result",
            "offline_message",
        )
    }
}
