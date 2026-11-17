package com.datathecodie.witgrid.screenshots

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.billing.BillingNotice
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletion
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.ReminderSettings
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.data.settings.ThemeMode
import com.datathecodie.witgrid.domain.stats.GameStatistics
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsState
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
import com.datathecodie.witgrid.reminder.ReminderEnableResult
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.layout.TopBarAction
import com.datathecodie.witgrid.ui.layout.WitgridScreenFrame
import com.datathecodie.witgrid.ui.layout.WitgridTopBar
import com.datathecodie.witgrid.ui.screens.calendar.CalendarScreen
import com.datathecodie.witgrid.ui.screens.failed.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.game.GameScreen
import com.datathecodie.witgrid.ui.screens.game.GameUiState
import com.datathecodie.witgrid.ui.screens.game.PackProgress
import com.datathecodie.witgrid.ui.screens.home.HomeGameProgress
import com.datathecodie.witgrid.ui.screens.home.HomeScreen
import com.datathecodie.witgrid.ui.screens.home.HomeUiState
import com.datathecodie.witgrid.ui.screens.play.PlayScreenLayout
import com.datathecodie.witgrid.ui.screens.play.PlaySudokuDigits
import com.datathecodie.witgrid.ui.screens.play.PlaySudokuTools
import com.datathecodie.witgrid.ui.screens.play.playPuzzleStatus
import com.datathecodie.witgrid.ui.screens.result.ResultScreen
import com.datathecodie.witgrid.ui.screens.settings.SettingsScreen
import com.datathecodie.witgrid.ui.screens.stats.StatisticsScreen
import com.datathecodie.witgrid.ui.screens.welcome.WelcomeScreen
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val arguments = InstrumentationRegistry.getArguments()
    private val darkTheme = arguments.getString("captureTheme", "light") == "dark"
    private val sampleLevel = LevelRef("crowns", "preview", Pack.EASY, levelNumber = 7)
    private val gameIds = listOf("crowns", "binary", "nonogram", "minisudoku")

    @Test
    fun capturesScreenProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        deleteExistingScreenshots(context)

        arguments.getString("captureFontScale")?.let { expected ->
            assertEquals(expected.toFloat(), Settings.System.getFloat(context.contentResolver,
                Settings.System.FONT_SCALE), 0.01f)
            assertEquals(expected.toFloat(), context.resources.configuration.fontScale, 0.01f)
        }
        arguments.getString("captureWidthDp")?.let { expected ->
            assertEquals(expected.toInt(), context.resources.configuration.screenWidthDp)
        }
        if (arguments.getString("captureBoardsOnly") == "true") {
            val bank = AssetBankSource(context.assets)
            captureBoard("crowns_board", CrownsGameModule, bank, context)
            captureBoard("binary_board", BinaryGameModule, bank, context)
            captureBoard("nonogram_board", NonogramGameModule, bank, context)
            captureBoard("mini_sudoku_board", MiniSudokuGameModule, bank, context)
            return
        }
        setScreenContent { WelcomeScreen(onFinish = {}) }
        captureScreenshot("welcome", context)
        showHome()
        captureScreenshot("home", context)
        setScreenContent { CaptureFrame(R.string.game_crowns) { SampleGame() } }
        captureScreenshot("game", context)

        val bankSource = AssetBankSource(context.assets)
        captureBoard("crowns_board", CrownsGameModule, bankSource, context)
        captureBoard("binary_board", BinaryGameModule, bankSource, context)
        captureBoard("nonogram_board", NonogramGameModule, bankSource, context)
        captureBoard("mini_sudoku_board", MiniSudokuGameModule, bankSource, context)

        showResult()
        captureScreenshot("result", context)

        setScreenContent { LevelFailedScreen({}, {}, {}, sampleLevel, sampleBoard(context, solved = false)) }
        captureScreenshot("level_failed", context)
        setScreenContent {
            CaptureFrame(R.string.screen_calendar) {
                CalendarScreen(20774, TimeZone.getTimeZone("UTC"), Locale.US, 20758, 20774,
                    StreakData(currentDays = 5, freezesAvailable = 1), false, false, false,
                    BillingUiState(), {}, listOf(DailyPuzzleCompletion(20774, "crowns", "sample"),
                        DailyPuzzleCompletion(20773, "binary", "previous")), gameIds, false,
                    {}, {}, {}, {}, { AdShowResult.Unavailable })
            }
        }
        captureScreenshot("calendar", context)
        setScreenContent {
            CaptureFrame(R.string.screen_statistics) {
                StatisticsScreen(false, gameIds.map { GameStatistics(it, 24, 3, 74_000, 108_000) })
            }
        }
        captureScreenshot("statistics", context)
        setScreenContent {
            CaptureFrame(R.string.screen_settings) {
                SettingsScreen(AppSettings(themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
                    crownsAutoXEnabled = true, nonogramAutoCrossEnabled = true),
                    ReminderSettings(true, 20, 0), null, false, {},
                    { ReminderEnableResult.Enabled }, {}, { _, _ -> }, {})
            }
        }
        captureScreenshot("settings", context)
        if (arguments.getString("captureOffline", "true") == "true") {
            showOfflineMessage(context)
            captureScreenshot("offline_message", context)
        }
    }

    private fun showHome() {
        setScreenContent {
            WitgridScreenFrame(topBar = {
                WitgridTopBar(contextString(R.string.app_name), showLogo = true, actions = listOf(
                    TopBarAction(R.drawable.ic_calendar, contextString(R.string.action_calendar)) {},
                    TopBarAction(R.drawable.ic_statistics, contextString(R.string.action_statistics)) {},
                    TopBarAction(R.drawable.ic_settings, contextString(R.string.action_settings)) {},
                ))
            }) { HomeScreen(
                state = HomeUiState(
                    games = listOf(
                        HomeGameProgress("crowns", LevelRef("crowns", "preview", Pack.EASY, levelNumber = 7)),
                        HomeGameProgress("binary", LevelRef("binary", "preview", Pack.EASY, levelNumber = 13)),
                        HomeGameProgress("nonogram", LevelRef("nonogram", "preview", Pack.EASY, levelNumber = 5)),
                        HomeGameProgress("minisudoku", LevelRef("minisudoku", "preview", Pack.EASY, levelNumber = 10)),
                    ),
                    isLoading = false,
                ),
                streak = StreakData(currentDays = 5, freezesAvailable = 1),
                completedDailyGameIds = setOf("crowns", "nonogram"),
                onSelectGame = {},
                onPlayDaily = {},
            ) }
        }
        waitForText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.home_today))
        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.home_today),
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
        assertToolbarLabels()
        if (module.id == "minisudoku") {
            val board = composeRule.onAllNodesWithTag("play-board").fetchSemanticsNodes().single().boundsInRoot
            println("Mini Sudoku board: ${board.width / composeRule.density.density}dp x ${board.height / composeRule.density.density}dp")
            val minimum = 118 * composeRule.density.density
            assertTrue("Mini Sudoku must retain at least a 118dp board", board.width >= minimum && board.height >= minimum)
        }
    }

    private fun assertToolbarLabels() {
        val nodes = composeRule.onAllNodes(hasAnyAncestor(hasTestTag("play-toolbar")), useUnmergedTree = true)
            .fetchSemanticsNodes()
        nodes.forEach { node ->
            if (node.config.contains(SemanticsActions.GetTextLayoutResult)) {
                val action = node.config[SemanticsActions.GetTextLayoutResult]
                val layouts = mutableListOf<TextLayoutResult>()
                action.action?.invoke(layouts)
                layouts.forEach { layout ->
                    assertTrue("Toolbar label must fit on one line: ${layout.layoutInput.text}, lines=${layout.lineCount}, width=${node.boundsInRoot.width}, lineWidth=${layout.getLineRight(0) - layout.getLineLeft(0)}",
                        layout.lineCount == 1 && layout.getLineRight(0) - layout.getLineLeft(0) <= node.boundsInRoot.width + 1f)
                }
            }
            if (node.config.contains(SemanticsActions.OnClick)) {
                val minimum = 48 * composeRule.density.density
                assertTrue("Toolbar target must remain at least 48dp", node.boundsInRoot.width >= minimum &&
                    node.boundsInRoot.height >= minimum)
            }
        }
    }

    private fun showResult() {
        setScreenContent {
            ResultScreen(
                elapsedTimeMillis = 74_000L,
                bestTimeMillis = 74_000L,
                onNext = {},
                onBackToPacks = {},
                levelRef = sampleLevel,
                boardState = sampleBoard(InstrumentationRegistry.getInstrumentation().targetContext),
            )
        }
        waitForText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.result_solved))
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
                                nextLevelRef = levelRef.copy(pack = pack),
                            )
                        },
                        continueLevelRef = levelRef,
                        isLoading = false,
                    ),
                    onContinue = {},
                    onSelectPack = {},
                    onOpenDaily = {},
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

    private fun contextString(resource: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)

    @Composable
    private fun CaptureFrame(title: Int, content: @Composable () -> Unit) {
        WitgridScreenFrame(topBar = { WitgridTopBar(contextString(title), onBack = {}) }) { content() }
    }

    @Composable
    private fun SampleGame() {
        GameScreen(GameUiState(gameId = "crowns", packs = Pack.entries.map {
            PackProgress(it, nextLevelRef = sampleLevel.copy(pack = it))
        }, continueLevelRef = sampleLevel, isLoading = false), {}, {}, {})
    }

    private fun sampleBoard(context: android.content.Context, solved: Boolean = true): CrownsState {
        val bank = AssetBankSource(context.assets)
        val ref = CrownsGameModule.levelRefs(Pack.EASY, bank).first { !it.isIntro }
        val puzzle = CrownsGameModule.loadLevel(ref, bank).puzzle
        return CrownsState(puzzle, List(puzzle.size * puzzle.size) { index ->
            if (solved && index % puzzle.size == puzzle.solutionColumns[index / puzzle.size] ||
                !solved && index == 0) Mark.FILLED else Mark.EMPTY
        })
    }

    private fun captureScreenshot(name: String, context: android.content.Context) {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MILLIS)
        composeRule.waitForIdle()
        awaitCommittedFrame()
        SystemClock.sleep(100L)
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

    private fun awaitCommittedFrame() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val committed = CountDownLatch(1)
            instrumentation.runOnMainSync {
                val view = composeRule.activity.window.decorView
                check(view.isHardwareAccelerated) { "Capture requires a hardware-rendered window" }
                // Semantics can be current while the previous screen is still in the display buffer.
                view.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
                view.invalidate()
            }
            check(committed.await(10, TimeUnit.SECONDS)) { "Timed out waiting for the capture frame" }
        } else {
            instrumentation.waitForIdleSync()
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

    private fun setScreenContent(content: @Composable () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            composeRule.activity.enableEdgeToEdge()
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
        val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit, (@Composable () -> Unit)?) -> Unit =
            { board, controls, modeTools ->
                PlayScreenLayout(
                    levelRef = levelRef,
                    elapsedTimeMillis = 0L,
                    canUndo = false,
                    canRedo = false,
                    saveFailed = false,
                    introCoachLine = null,
                    statusMessage = playPuzzleStatus(state),
                    hintOperationPending = false,
                    board = board,
                    gameControls = controls,
                    modeTools = modeTools,
                    onUndo = {},
                    onRedo = {},
                    onRequestHint = {},
                    onCheck = {},
                    onBack = {},
                )
            }

        CompositionLocalProvider(LocalBoardViewportMode provides BoardViewportMode(embedded = true)) {
            when (module.id) {
                "minisudoku" -> MiniSudokuBoard(
                    state = state as MiniSudokuState,
                    onMoves = {},
                    onUndo = {},
                    playLayout = { board, actions ->
                        layout(board, { PlaySudokuDigits(state, actions) }, { PlaySudokuTools(actions) })
                    },
                )

                "nonogram" -> NonogramBoard(
                    state = state as NonogramState,
                    onMoves = {},
                    onUndo = {},
                    layout = { board, controls -> layout(board, controls, null) },
                )

                else -> layout(
                    { module.Board(state, onMoves = {}, onUndo = {}) },
                    {},
                    null,
                )
            }
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
        const val ANIMATION_SETTLE_MILLIS = 1_500L
        val SCREENSHOT_NAMES = listOf(
            "home",
            "welcome",
            "game",
            "level_failed",
            "calendar",
            "statistics",
            "settings",
            "crowns_board",
            "binary_board",
            "nonogram_board",
            "mini_sudoku_board",
            "result",
            "offline_message",
        )
    }
}
