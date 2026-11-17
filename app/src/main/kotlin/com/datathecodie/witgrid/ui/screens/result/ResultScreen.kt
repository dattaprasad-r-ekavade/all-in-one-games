package com.datathecodie.witgrid.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.IconActionButton
import com.datathecodie.witgrid.ui.components.PrimaryButton
import com.datathecodie.witgrid.ui.components.SecondaryButton
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.screens.home.gameNameResource
import com.datathecodie.witgrid.ui.screens.home.packNameResource
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ResultScreenRoute(
    levelRef: LevelRef,
    progressRepository: ProgressRepository,
    onNext: () -> Unit,
    onBackToPacks: () -> Unit,
) {
    var progress by remember(levelRef) { mutableStateOf<LevelProgress?>(null) }
    var loaded by remember(levelRef) { mutableStateOf(false) }
    var boardState by remember(levelRef) { mutableStateOf<Any?>(null) }
    LaunchedEffect(levelRef, progressRepository) {
        withContext(Dispatchers.IO) {
            progress = progressRepository.getAttemptProgress(levelRef.gameId, levelRef.levelId, levelRef.levelNumber ?: 0)
            boardState = progress?.let { saved ->
                runCatching { GameRegistry.find(levelRef.gameId)?.decodeState(saved.stateBytes) }.getOrNull()
            }
        }
        loaded = true
    }
    ResultScreen(
        elapsedTimeMillis = if (loaded) progress?.elapsedTimeMillis ?: 0 else null,
        bestTimeMillis = progress?.bestTimeMillis,
        onNext = onNext,
        onBackToPacks = onBackToPacks,
        levelRef = levelRef,
        hintsUsed = progress?.hintsUsed ?: 0,
        boardState = boardState,
    )
}

@Composable
fun ResultScreen(
    elapsedTimeMillis: Long?,
    bestTimeMillis: Long?,
    onNext: () -> Unit,
    onBackToPacks: () -> Unit,
    levelRef: LevelRef? = null,
    hintsUsed: Int = 0,
    boardState: Any? = null,
) {
    var advancing by remember(levelRef) { mutableStateOf(false) }
    FinishScreenFrame(onClose = onBackToPacks, enabled = !advancing) {
        BoardThumbnail(boardState, Modifier.size(168.dp))
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.result_solved), style = MaterialTheme.typography.displaySmall)
        levelRef?.let { FinishLevelLabel(it) }
        Spacer(Modifier.height(12.dp))
        if (elapsedTimeMillis == null) CircularProgressIndicator() else {
            ResultStats(elapsedTimeMillis, bestTimeMillis, hintsUsed)
            if (bestTimeMillis != null && elapsedTimeMillis <= bestTimeMillis) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = WitgridTheme.colors.accent.copy(alpha = 0.12f),
                ) {
                    Text(stringResource(R.string.result_personal_best), Modifier.padding(16.dp),
                        color = WitgridTheme.colors.accent, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            stringResource(if (levelRef?.daily == true) R.string.result_daily_done else R.string.result_next_level),
            { if (!advancing) { advancing = true; onNext() } },
            Modifier.fillMaxWidth(), enabled = elapsedTimeMillis != null && !advancing,
        )
        SecondaryButton(stringResource(R.string.result_back_to_packs), onBackToPacks,
            Modifier.fillMaxWidth(), enabled = !advancing)
    }
}

@Composable
private fun ResultStats(elapsed: Long, best: Long?, hints: Int) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            fontScale <= 1.1f && maxWidth >= 330.dp -> 3
            maxWidth.value / fontScale >= 220 -> 2
            else -> 1
        }
        val stats = listOf(
            formatFinishDuration(elapsed) to stringResource(R.string.result_time_label),
            (best?.let(::formatFinishDuration) ?: stringResource(R.string.result_unavailable)) to stringResource(R.string.result_best_label),
            hints.toString() to pluralStringResource(R.plurals.result_hints_label, hints),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (value, label) ->
                        WitgridCard(Modifier.weight(1f)) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = WitgridTheme.colors.mutedInk,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FinishScreenFrame(
    onClose: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AdaptiveScreen {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentMinimumHeight = (maxHeight - 80.dp).coerceAtLeast(0.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth()) {
                    IconActionButton(painterResource(R.drawable.ic_close), stringResource(R.string.button_close),
                        onClose, enabled = enabled)
                }
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = contentMinimumHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
        }
    }
}

@Composable
internal fun FinishLevelLabel(level: LevelRef) {
    val game = stringResource(gameNameResource(level.gameId))
    val pack = stringResource(packNameResource(level.pack))
    val label = when {
        level.daily -> stringResource(R.string.result_daily_label, game, pack)
        level.isIntro -> stringResource(R.string.result_intro_label, game)
        else -> stringResource(R.string.result_level_label, game, pack, requireNotNull(level.levelNumber))
    }
    Text(label, style = MaterialTheme.typography.bodyLarge, color = WitgridTheme.colors.mutedInk,
        textAlign = TextAlign.Center)
}

internal fun formatFinishDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
