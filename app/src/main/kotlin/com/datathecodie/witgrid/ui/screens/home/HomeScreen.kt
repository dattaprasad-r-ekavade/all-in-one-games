package com.datathecodie.witgrid.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.Chip
import com.datathecodie.witgrid.ui.components.ListRow
import com.datathecodie.witgrid.ui.components.PrimaryButton
import com.datathecodie.witgrid.ui.components.SectionHeader
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreenRoute(
    gameIds: List<String>,
    levelRefsByGame: Map<String, List<LevelRef>>,
    progressRepository: GameProgressSummaryRepository,
    streak: StreakData,
    completedDailyGameIds: Set<String>,
    onSelectGame: (String) -> Unit,
    onPlayDaily: (String) -> Unit,
    premiumEnabled: Boolean = false,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    val viewModel = remember(gameIds, levelRefsByGame, progressRepository) {
        HomeViewModel(gameIds, levelRefsByGame, progressRepository)
    }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    HomeScreen(state, streak, completedDailyGameIds, onSelectGame, onPlayDaily,
        premiumEnabled, billingState, onPurchasePremium)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    streak: StreakData = StreakData(),
    completedDailyGameIds: Set<String> = emptySet(),
    onSelectGame: (String) -> Unit,
    onPlayDaily: (String) -> Unit,
    premiumEnabled: Boolean = false,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
    todayLabel: String = DateFormat.getDateInstance(DateFormat.FULL).format(Date()),
) {
    var showPremiumOffer by rememberSaveable { mutableStateOf(false) }
    if (showPremiumOffer) {
        PremiumOfferDialog(premiumEnabled, billingState, onPurchasePremium, { showPremiumOffer = false })
    }
    val colors = WitgridTheme.colors
    AdaptiveScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                WitgridCard(Modifier.fillMaxWidth()) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(todayLabel, color = colors.mutedInk,
                                style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.home_today), style = MaterialTheme.typography.titleLarge)
                        }
                        Chip(stringResource(R.string.home_streak_days, streak.currentDays), highlighted = true)
                    }
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.games.forEach { game ->
                            val completed = game.gameId in completedDailyGameIds
                            Chip(stringResource(if (completed) R.string.home_daily_done else R.string.home_daily_ready,
                                stringResource(gameNameResource(game.gameId))), highlighted = completed,
                                onClick = { onPlayDaily(game.gameId) })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    val firstUnfinished = state.games.firstOrNull { it.gameId !in completedDailyGameIds }
                    if (firstUnfinished != null) {
                        PrimaryButton(stringResource(R.string.home_play_daily, stringResource(gameNameResource(firstUnfinished.gameId))),
                            { onPlayDaily(firstUnfinished.gameId) }, Modifier.fillMaxWidth())
                    } else if (state.games.isNotEmpty()) {
                        Text(stringResource(R.string.home_daily_all_done), color = colors.accent)
                    }
                }
                SectionHeader(stringResource(R.string.home_games))
                state.games.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { game ->
                            WitgridCard(Modifier.weight(1f), onClick = { onSelectGame(game.gameId) }) {
                                GameBoardPreview(game.gameId)
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(gameNameResource(game.gameId)), style = MaterialTheme.typography.titleMedium)
                                game.nextLevelRef?.let { level ->
                                    Text(if (level.isIntro) stringResource(R.string.home_guided_intro, stringResource(packNameResource(level.pack)))
                                        else stringResource(R.string.home_next_level, requireNotNull(level.levelNumber), stringResource(packNameResource(level.pack))),
                                        style = MaterialTheme.typography.bodyMedium, color = colors.mutedInk)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                ListRow(stringResource(R.string.home_premium_row), subtitle = stringResource(
                    if (premiumEnabled) R.string.premium_owned else R.string.home_premium_subtitle),
                    onClick = { showPremiumOffer = true })
            }
        }
    }
}

internal fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    else -> R.string.game_unknown
}

internal fun packNameResource(pack: Pack): Int = when (pack) {
    Pack.EASY -> R.string.pack_easy
    Pack.MEDIUM -> R.string.pack_medium
    Pack.HARD -> R.string.pack_hard
    Pack.EXPERT -> R.string.pack_expert
}
