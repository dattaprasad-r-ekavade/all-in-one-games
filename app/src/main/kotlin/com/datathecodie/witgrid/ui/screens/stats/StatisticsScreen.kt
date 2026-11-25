package com.datathecodie.witgrid.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.domain.stats.GameStatistics
import com.datathecodie.witgrid.domain.stats.StatisticsViewModel
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.StatTile
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun StatisticsScreenRoute(
    gameIds: List<String>,
    levelRefsByGame: Map<String, List<LevelRef>>,
    progressRepository: GameProgressSummaryRepository,
    dailyPuzzleCompletionRepository: DailyPuzzleCompletionRepository,
) {
    val regularLevelIdsByGame = remember(levelRefsByGame) {
        levelRefsByGame.mapValues { (_, refs) ->
            refs.asSequence()
                .filterNot { it.isIntro || it.daily }
                .map(LevelRef::levelId)
                .toSet()
        }
    }
    val viewModel = remember(
        gameIds,
        regularLevelIdsByGame,
        progressRepository,
        dailyPuzzleCompletionRepository,
    ) {
        StatisticsViewModel(
            gameIds = gameIds,
            regularLevelIdsByGame = regularLevelIdsByGame,
            progressRepository = progressRepository,
            dailyPuzzleCompletionRepository = dailyPuzzleCompletionRepository,
        )
    }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    StatisticsScreen(
        isLoading = state.isLoading,
        games = state.games,
    )
}

@Composable
fun StatisticsScreen(
    isLoading: Boolean,
    games: List<GameStatistics>,
) {
    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                games.forEach { game ->
                    GameStatisticsCard(game)
                }
            }
        }
    }
}

@Composable
private fun GameStatisticsCard(game: GameStatistics) {
    WitgridCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(gameNameResource(game.gameId)), style = MaterialTheme.typography.titleLarge)
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth.value / fontScale >= 220) 2 else 1
            val statistics = listOf(
                game.solvedRegularLevels.toString() to stringResource(R.string.stats_solved_label),
                game.dailyCompletions.toString() to stringResource(R.string.stats_daily_label),
                formatDuration(game.bestTimeMillis) to stringResource(R.string.result_best_label),
                formatDuration(game.averageBestTimeMillis) to stringResource(R.string.stats_average_label),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                statistics.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (value, label) -> StatTile(value, label, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatDuration(milliseconds: Long?): String {
    if (milliseconds == null) return stringResource(R.string.stats_time_unavailable)

    val totalSeconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toInt()
    val totalMinutes = totalSeconds / SECONDS_PER_MINUTE
    return if (totalMinutes >= MINUTES_PER_HOUR) {
        stringResource(
            R.string.stats_duration_hours_minutes_seconds,
            totalMinutes / MINUTES_PER_HOUR,
            totalMinutes % MINUTES_PER_HOUR,
            seconds,
        )
    } else {
        stringResource(R.string.stats_duration_minutes_seconds, totalMinutes, seconds)
    }
}

private fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    "sudoku" -> R.string.game_sudoku
    "skyscrapers" -> R.string.game_skyscrapers
    else -> R.string.game_unknown
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
