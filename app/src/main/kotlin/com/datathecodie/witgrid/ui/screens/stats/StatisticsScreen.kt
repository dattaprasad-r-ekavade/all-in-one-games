package com.datathecodie.witgrid.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun StatisticsScreenRoute(
    gameIds: List<String>,
    levelRefsByGame: Map<String, List<LevelRef>>,
    progressRepository: GameProgressSummaryRepository,
    dailyPuzzleCompletionRepository: DailyPuzzleCompletionRepository,
    onHome: () -> Unit,
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
        onHome = onHome,
    )
}

@Composable
private fun StatisticsScreen(
    isLoading: Boolean,
    games: List<GameStatistics>,
    onHome: () -> Unit,
) {
    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(gameNameResource(game.gameId)),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.stats_solved_levels, game.solvedRegularLevels))
            Text(stringResource(R.string.stats_daily_completions, game.dailyCompletions))
            Text(stringResource(R.string.stats_best_time, formatDuration(game.bestTimeMillis)))
            Text(stringResource(R.string.stats_average_best_time, formatDuration(game.averageBestTimeMillis)))
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
    else -> R.string.game_unknown
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
