package com.datathecodie.witgrid.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuPuzzle
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.ui.screens.CalendarScreen
import com.datathecodie.witgrid.ui.screens.settings.SettingsScreen
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.ReminderSettings
import com.datathecodie.witgrid.reminder.ReminderEnableResult
import com.datathecodie.witgrid.ui.screens.StatisticsScreen
import com.datathecodie.witgrid.ui.screens.failed.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.game.GameScreen
import com.datathecodie.witgrid.ui.screens.game.GameUiState
import com.datathecodie.witgrid.ui.screens.game.PackProgress
import com.datathecodie.witgrid.ui.screens.home.HomeGameProgress
import com.datathecodie.witgrid.ui.screens.home.HomeScreen
import com.datathecodie.witgrid.ui.screens.home.HomeUiState
import com.datathecodie.witgrid.ui.screens.play.PlayScreenLayout
import com.datathecodie.witgrid.ui.screens.result.ResultScreen
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Preview(name = "Home · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomePhonePreview() = PreviewFrame { SampleHomeScreen() }

@Preview(name = "Home · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun HomeTabletPreview() = PreviewFrame { SampleHomeScreen() }

@Composable
private fun SampleHomeScreen() {
    HomeScreen(
        state = HomeUiState(
            games = listOf("crowns", "binary", "nonogram", "minisudoku").map {
                HomeGameProgress(it, completedLevels = 12, totalLevels = 601)
            },
            isLoading = false,
        ),
        onSelectGame = {},
        onOpenCalendar = {},
        onOpenStatistics = {},
        onOpenSettings = {},
    )
}

@Preview(name = "Game · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun GamePhonePreview() = PreviewFrame { SampleGameScreen() }

@Preview(name = "Game · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun GameTabletPreview() = PreviewFrame { SampleGameScreen() }

@Preview(name = "Play · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PlayPhonePreview() = PreviewFrame { SampleMiniSudokuPlayScreen() }

@Preview(name = "Play · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun PlayTabletPreview() = PreviewFrame { SampleMiniSudokuPlayScreen() }

@Preview(name = "Play · 840 × 360 dp", group = "A13 landscape", widthDp = 840, heightDp = 360, showBackground = true)
@Composable
private fun PlayLandscapePreview() = PreviewFrame { SampleLargeNonogramPlayScreen() }

@Preview(name = "Result · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ResultPhonePreview() = PreviewFrame { ResultScreen(125_000L, 98_000L, {}, {}) }

@Preview(name = "Result · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun ResultTabletPreview() = PreviewFrame { ResultScreen(125_000L, 98_000L, {}, {}) }

@Preview(name = "Level failed · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun FailedPhonePreview() = PreviewFrame { LevelFailedScreen({}, {}, {}) }

@Preview(name = "Level failed · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun FailedTabletPreview() = PreviewFrame { LevelFailedScreen({}, {}, {}) }

@Preview(name = "Calendar · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CalendarPhonePreview() = PreviewFrame { CalendarScreen {} }

@Preview(name = "Calendar · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun CalendarTabletPreview() = PreviewFrame { CalendarScreen {} }

@Preview(name = "Statistics · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun StatisticsPhonePreview() = PreviewFrame { StatisticsScreen {} }

@Preview(name = "Statistics · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun StatisticsTabletPreview() = PreviewFrame { StatisticsScreen {} }

@Preview(name = "Settings · 360 dp", group = "A13 phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SettingsPhonePreview() = PreviewFrame {
    SettingsScreen(
        AppSettings(), ReminderSettings(), null, false, {}, { ReminderEnableResult.Enabled }, {}, { _, _ -> }, {}, {},
    )
}

@Preview(name = "Settings · 840 dp", group = "A13 tablet", widthDp = 840, heightDp = 1_000, showBackground = true)
@Composable
private fun SettingsTabletPreview() = PreviewFrame {
    SettingsScreen(
        AppSettings(), ReminderSettings(), null, false, {}, { ReminderEnableResult.Enabled }, {}, { _, _ -> }, {}, {},
    )
}

@Composable
private fun SampleGameScreen() {
    val easyLevel = sampleLevel()
    GameScreen(
        state = GameUiState(
            gameId = easyLevel.gameId,
            packs = Pack.entries.map { pack ->
                PackProgress(
                    pack,
                    completedLevels = 12,
                    totalLevels = if (pack == Pack.EASY) 151 else 150,
                    nextLevelRef = LevelRef(easyLevel.gameId, "CR-$pack-001", pack),
                )
            },
            continueLevelRef = easyLevel,
            isLoading = false,
        ),
        onContinue = {},
        onSelectPack = {},
        onOpenDaily = {},
        onHome = {},
    )
}

@Composable
private fun SampleMiniSudokuPlayScreen() {
    val puzzle = remember { sampleMiniSudokuPuzzle() }
    val state = remember(puzzle) { MiniSudokuState(puzzle, puzzle.givens, List(puzzle.grid.cellCount) { 0 }) }
    val levelRef = LevelRef(
        gameId = "minisudoku",
        levelId = puzzle.id,
        pack = Pack.EASY,
        introCoachLineResourceId = R.string.intro_coach_minisudoku,
    )
    MiniSudokuBoard(state, onMoves = {}, onUndo = {}) { board, controls ->
        PreviewPlayLayout(levelRef, board, controls)
    }
}

@Composable
private fun SampleLargeNonogramPlayScreen() {
    val puzzle = remember { sampleLargeNonogramPuzzle() }
    val state = remember(puzzle) { NonogramState.initial(puzzle) }
    val levelRef = LevelRef(gameId = "nonogram", levelId = puzzle.id, pack = Pack.EXPERT)
    NonogramBoard(state, onMoves = {}, onUndo = {}) { board, controls ->
        PreviewPlayLayout(levelRef, board, controls)
    }
}

@Composable
private fun PreviewPlayLayout(
    levelRef: LevelRef,
    board: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    val coachLine = levelRef.introCoachLineResourceId?.let { resourceId -> stringResource(resourceId) }
    PlayScreenLayout(
        levelRef = levelRef,
        elapsedTimeMillis = 125_000,
        canUndo = true,
        canRedo = false,
        saveFailed = false,
        introCoachLine = coachLine,
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

private fun sampleLevel() = LevelRef(
    gameId = "crowns",
    levelId = "CR-INTRO-01",
    pack = Pack.EASY,
    introCoachLineResourceId = R.string.intro_coach_crowns,
)

private fun sampleMiniSudokuPuzzle(): MiniSudokuPuzzle {
    val solution = listOf(
        1, 2, 3, 4, 5, 6,
        4, 5, 6, 1, 2, 3,
        2, 3, 4, 5, 6, 1,
        5, 6, 1, 2, 3, 4,
        3, 4, 5, 6, 1, 2,
        6, 1, 2, 3, 4, 5,
    )
    return MiniSudokuPuzzle(
        id = "MS-PREVIEW-01",
        givens = solution.mapIndexed { index, digit -> digit.takeIf { index % 3 == 0 } },
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "preview",
    )
}

private fun sampleLargeNonogramPuzzle(): NonogramPuzzle {
    val size = 15
    val solution = List(size * size) { index ->
        if ((index / size + index % size) % 2 == 0) 1 else 0
    }
    val rowClues = (0 until size).map { row ->
        clueRuns(solution.subList(row * size, (row + 1) * size))
    }
    val columnClues = (0 until size).map { column ->
        clueRuns((0 until size).map { row -> solution[row * size + column] })
    }
    return NonogramPuzzle(
        id = "NO-PREVIEW-15",
        size = size,
        rowClues = rowClues,
        columnClues = columnClues,
        solution = solution,
        pack = Pack.EXPERT,
        storedGrade = "preview",
    )
}

private fun clueRuns(line: List<Int>): List<Int> {
    val runs = mutableListOf<Int>()
    var currentRun = 0
    line.forEach { value ->
        if (value == 1) {
            currentRun += 1
        } else if (currentRun > 0) {
            runs += currentRun
            currentRun = 0
        }
    }
    if (currentRun > 0) runs += currentRun
    return runs
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    WitgridTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
