package com.datathecodie.witgrid.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun HomeScreenRoute(
    gameIds: List<String>,
    levelRefsByGame: Map<String, List<LevelRef>>,
    progressRepository: GameProgressSummaryRepository,
    streak: StreakData,
    completedDailyGamesToday: Int,
    onSelectGame: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    premiumEnabled: Boolean = false,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    val viewModel = remember(gameIds, levelRefsByGame, progressRepository) {
        HomeViewModel(gameIds, levelRefsByGame, progressRepository)
    }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    HomeScreen(
        state = state,
        streak = streak,
        completedDailyGamesToday = completedDailyGamesToday,
        onSelectGame = onSelectGame,
        onOpenCalendar = onOpenCalendar,
        onOpenStatistics = onOpenStatistics,
        onOpenSettings = onOpenSettings,
        premiumEnabled = premiumEnabled,
        billingState = billingState,
        onPurchasePremium = onPurchasePremium,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    streak: StreakData = StreakData(),
    completedDailyGamesToday: Int = 0,
    onSelectGame: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    premiumEnabled: Boolean = false,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    var showPremiumOffer by rememberSaveable { mutableStateOf(false) }
    if (showPremiumOffer) {
        PremiumOfferDialog(
            premiumEnabled = premiumEnabled,
            billingState = billingState,
            onPurchasePremium = onPurchasePremium,
            onDismiss = { showPremiumOffer = false },
        )
    }
    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.screen_home), style = MaterialTheme.typography.headlineMedium)

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                state.games.forEach { game ->
                    Card(
                        onClick = { onSelectGame(game.gameId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(gameNameResource(game.gameId)),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_progress,
                                    game.completedLevels,
                                    game.totalLevels,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            Card(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.screen_daily_puzzle), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.home_daily_status, completedDailyGamesToday.coerceIn(0, DAILY_GAME_COUNT)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.home_streak_status,
                            streak.currentDays,
                            streak.freezesAvailable,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Card(onClick = { showPremiumOffer = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_premium_row), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (premiumEnabled) {
                            stringResource(R.string.premium_owned)
                        } else {
                            billingState.productPrice?.let { price ->
                                stringResource(R.string.premium_home_offer, price)
                            } ?: stringResource(R.string.premium_product_unavailable)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            TextButtonRow(
                label = stringResource(R.string.screen_calendar),
                onClick = onOpenCalendar,
            )
            TextButtonRow(
                label = stringResource(R.string.screen_statistics),
                onClick = onOpenStatistics,
            )
            TextButtonRow(
                label = stringResource(R.string.screen_settings),
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun TextButtonRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label)
    }
}

private fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    else -> R.string.game_unknown
}

private const val DAILY_GAME_COUNT = 4
