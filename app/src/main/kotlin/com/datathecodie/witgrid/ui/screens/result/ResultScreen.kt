package com.datathecodie.witgrid.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun ResultScreenRoute(
    levelRef: LevelRef,
    progressRepository: ProgressRepository,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    var elapsedTimeMillis by remember(levelRef) { mutableStateOf<Long?>(null) }
    var bestTimeMillis by remember(levelRef) { mutableStateOf<Long?>(null) }

    LaunchedEffect(levelRef) {
        val progress = progressRepository.getLevelProgress(levelRef.gameId, levelRef.levelId)
        elapsedTimeMillis = progress?.elapsedTimeMillis ?: 0L
        bestTimeMillis = progress?.bestTimeMillis
    }

    ResultScreen(
        elapsedTimeMillis = elapsedTimeMillis,
        bestTimeMillis = bestTimeMillis,
        onNext = onNext,
        onHome = onHome,
    )
}

@Composable
fun ResultScreen(
    elapsedTimeMillis: Long?,
    bestTimeMillis: Long?,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    var advancing by remember { mutableStateOf(false) }

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.screen_result), style = MaterialTheme.typography.headlineMedium)
            if (elapsedTimeMillis == null) {
                CircularProgressIndicator()
            } else {
                Text(
                    stringResource(R.string.result_time, formatDuration(elapsedTimeMillis)),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    bestTimeMillis?.let { stringResource(R.string.result_best_time, formatDuration(it)) }
                        ?: stringResource(R.string.result_no_best_time),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Button(
                onClick = {
                    if (!advancing) {
                        advancing = true
                        onNext()
                    }
                },
                enabled = elapsedTimeMillis != null && !advancing,
            ) {
                if (advancing) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.button_next))
                }
            }
            OutlinedButton(onClick = onHome, enabled = !advancing) {
                Text(stringResource(R.string.button_home))
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
