package com.datathecodie.witgrid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun HomeScreen(
    gameIds: List<String>,
    onSelectGame: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenColumn {
        Text(stringResource(R.string.screen_home), style = MaterialTheme.typography.headlineMedium)
        gameIds.forEach { gameId ->
            val title = stringResource(gameNameResource(gameId))
            Button(onClick = { onSelectGame(gameId) }) { Text(title) }
        }
        Button(onClick = onOpenCalendar) { Text(stringResource(R.string.screen_calendar)) }
        Button(onClick = onOpenStatistics) { Text(stringResource(R.string.screen_statistics)) }
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.screen_settings)) }
    }
}

@Composable
fun GameScreen(gameId: String, onStart: () -> Unit, onHome: () -> Unit) {
    ScreenColumn {
        Text(
            stringResource(R.string.screen_game, stringResource(gameNameResource(gameId))),
            style = MaterialTheme.typography.headlineMedium,
        )
        Button(onClick = onStart) { Text(stringResource(R.string.button_start)) }
        Button(onClick = onHome) { Text(stringResource(R.string.button_home)) }
    }
}

@Composable
fun PlayScreen(
    levelRef: LevelRef,
    onComplete: () -> Unit,
    onFail: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenColumn {
        Text(stringResource(R.string.screen_play), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.screen_game, stringResource(gameNameResource(levelRef.gameId))))
        Button(onClick = onComplete) { Text(stringResource(R.string.button_result)) }
        Button(onClick = onFail) { Text(stringResource(R.string.button_level_failed)) }
        Button(onClick = onBack) { Text(stringResource(R.string.button_back)) }
    }
}

@Composable
fun ResultScreen(onNext: () -> Unit, onHome: () -> Unit) {
    ScreenColumn {
        Text(stringResource(R.string.screen_result), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onNext) { Text(stringResource(R.string.button_next)) }
        Button(onClick = onHome) { Text(stringResource(R.string.button_home)) }
    }
}

@Composable
fun LevelFailedScreen(onContinue: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    ScreenColumn {
        Text(stringResource(R.string.screen_level_failed), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onContinue) { Text(stringResource(R.string.button_continue)) }
        Button(onClick = onRestart) { Text(stringResource(R.string.button_restart)) }
        Button(onClick = onHome) { Text(stringResource(R.string.button_home)) }
    }
}

@Composable
fun CalendarScreen(onHome: () -> Unit) = SimpleStubScreen(R.string.screen_calendar, onHome)

@Composable
fun StatisticsScreen(onHome: () -> Unit) = SimpleStubScreen(R.string.screen_statistics, onHome)

@Composable
private fun SimpleStubScreen(titleResource: Int, onHome: () -> Unit) {
    ScreenColumn {
        Text(stringResource(titleResource), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onHome) { Text(stringResource(R.string.button_home)) }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
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
