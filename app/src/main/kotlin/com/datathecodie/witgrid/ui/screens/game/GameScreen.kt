package com.datathecodie.witgrid.ui.screens.game

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
import androidx.compose.material3.OutlinedButton
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
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.ads.flow.RewardedActionButton
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun GameScreenRoute(
    gameId: String,
    levelRefsByPack: Map<Pack, List<LevelRef>>,
    progressRepository: GameProgressSummaryRepository,
    onContinue: (LevelRef) -> Unit,
    onSelectPack: (LevelRef) -> Unit,
    onOpenDaily: () -> Unit,
    dailyCompletedToday: Boolean = false,
    premiumEnabled: Boolean = false,
    onEarnHintCredit: suspend () -> AdShowResult = { AdShowResult.Unavailable },
    onEarnCheckCredit: suspend () -> AdShowResult = { AdShowResult.Unavailable },
    onHome: () -> Unit,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    val viewModel = remember(gameId, levelRefsByPack, progressRepository) {
        GameViewModel(gameId, levelRefsByPack, progressRepository)
    }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    GameScreen(
        state = state,
        onContinue = onContinue,
        onSelectPack = onSelectPack,
        onOpenDaily = onOpenDaily,
        dailyCompletedToday = dailyCompletedToday,
        premiumEnabled = premiumEnabled,
        onEarnHintCredit = onEarnHintCredit,
        onEarnCheckCredit = onEarnCheckCredit,
        onHome = onHome,
        billingState = billingState,
        onPurchasePremium = onPurchasePremium,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onContinue: (LevelRef) -> Unit,
    onSelectPack: (LevelRef) -> Unit,
    onOpenDaily: () -> Unit,
    dailyCompletedToday: Boolean = false,
    premiumEnabled: Boolean = false,
    onEarnHintCredit: suspend () -> AdShowResult = { AdShowResult.Unavailable },
    onEarnCheckCredit: suspend () -> AdShowResult = { AdShowResult.Unavailable },
    onHome: () -> Unit,
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
            Text(
                stringResource(R.string.screen_game, stringResource(gameNameResource(state.gameId))),
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedButton(onClick = onOpenDaily, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (dailyCompletedToday) R.string.game_daily_entry_completed else R.string.game_daily_entry,
                    ),
                )
            }

            if (!premiumEnabled) {
                Text(stringResource(R.string.ads_earn_credits_header), style = MaterialTheme.typography.titleMedium)
                RewardedActionButton(
                    titleResource = R.string.ads_earn_hint_credit,
                    successResource = R.string.ads_hint_credit_added,
                    onReward = onEarnHintCredit,
                    onOpenPremiumOffer = { showPremiumOffer = true },
                )
                RewardedActionButton(
                    titleResource = R.string.ads_earn_check_credit,
                    successResource = R.string.ads_check_credit_added,
                    onReward = onEarnCheckCredit,
                    onOpenPremiumOffer = { showPremiumOffer = true },
                )
            }

            val continueLevelRef = state.continueLevelRef
            Button(
                onClick = { continueLevelRef?.let(onContinue) },
                enabled = !state.isLoading && continueLevelRef != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.button_continue))
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                state.packs.forEach { packProgress ->
                    val nextLevelRef = packProgress.nextLevelRef
                    Card(
                        onClick = { nextLevelRef?.let(onSelectPack) },
                        enabled = nextLevelRef != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(packNameResource(packProgress.pack)),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(
                                    R.string.game_pack_progress,
                                    packProgress.completedLevels,
                                    packProgress.totalLevels,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.button_home))
            }
        }
    }
}

private fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    else -> R.string.game_unknown
}

private fun packNameResource(pack: Pack): Int = when (pack) {
    Pack.EASY -> R.string.pack_easy
    Pack.MEDIUM -> R.string.pack_medium
    Pack.HARD -> R.string.pack_hard
    Pack.EXPERT -> R.string.pack_expert
}
