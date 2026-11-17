package com.datathecodie.witgrid.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.ads.flow.RewardedActionButton
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.ListRow
import com.datathecodie.witgrid.ui.components.PrimaryButton
import com.datathecodie.witgrid.ui.components.SectionHeader
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.screens.home.GameBoardPreview
import com.datathecodie.witgrid.ui.screens.home.gameNameResource
import com.datathecodie.witgrid.ui.screens.home.packNameResource
import com.datathecodie.witgrid.ui.screens.play.playPuzzleStatus
import com.datathecodie.witgrid.ui.theme.WitgridTheme

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
    hintCredits: Int = 0,
    checkCredits: Int = 0,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    val assets = LocalContext.current.assets
    val viewModel = remember(gameId, levelRefsByPack, progressRepository, assets) {
        GameViewModel(gameId, levelRefsByPack, progressRepository, AssetBankSource(assets))
    }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    GameScreen(state, onContinue, onSelectPack, onOpenDaily, dailyCompletedToday, premiumEnabled,
        onEarnHintCredit, onEarnCheckCredit, hintCredits, checkCredits, billingState, onPurchasePremium)
}

@OptIn(ExperimentalMaterial3Api::class)
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
    hintCredits: Int = 0,
    checkCredits: Int = 0,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    var showPremiumOffer by rememberSaveable { mutableStateOf(false) }
    var showRewards by rememberSaveable { mutableStateOf(false) }
    val colors = WitgridTheme.colors
    if (showPremiumOffer) {
        PremiumOfferDialog(premiumEnabled, billingState, onPurchasePremium, { showPremiumOffer = false })
    }
    if (showRewards) {
        ModalBottomSheet(onDismissRequest = { showRewards = false }, containerColor = colors.card) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.ads_earn_credits_header), style = MaterialTheme.typography.titleLarge)
                RewardedActionButton(R.string.ads_earn_hint_credit, R.string.ads_hint_credit_added,
                    onEarnHintCredit, { showRewards = false; showPremiumOffer = true })
                RewardedActionButton(R.string.ads_earn_check_credit, R.string.ads_check_credit_added,
                    onEarnCheckCredit, { showRewards = false; showPremiumOffer = true })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    AdaptiveScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                state.continueLevelRef?.let { level ->
                    WitgridCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GameBoardPreview(state.gameId, Modifier.size(84.dp), state.continueState)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(if (state.continueProgress != null) R.string.game_in_progress else R.string.game_ready),
                                    color = colors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                                Text(if (level.isIntro) stringResource(R.string.home_guided_intro, stringResource(packNameResource(level.pack)))
                                    else stringResource(R.string.home_next_level, requireNotNull(level.levelNumber), stringResource(packNameResource(level.pack))),
                                    style = MaterialTheme.typography.titleLarge)
                                state.continueProgress?.let { saved ->
                                    val status = state.continueState?.let { playPuzzleStatus(it) }
                                    val time = stringResource(R.string.game_elapsed, saved.elapsedTimeMillis / 60_000,
                                        saved.elapsedTimeMillis / 1_000 % 60)
                                    Text(if (status != null) stringResource(R.string.game_continue_status, status, time) else time,
                                        color = colors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(stringResource(R.string.button_continue), { onContinue(level) }, Modifier.fillMaxWidth())
                    }
                }
                WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    ListRow(stringResource(R.string.game_daily_name, stringResource(gameNameResource(state.gameId))),
                        subtitle = stringResource(if (dailyCompletedToday) R.string.game_daily_solved else R.string.game_daily_entry),
                        trailingValue = if (dailyCompletedToday) stringResource(R.string.game_done_tick) else null,
                        onClick = onOpenDaily, showChevron = !dailyCompletedToday)
                }
                SectionHeader(stringResource(R.string.game_packs))
                state.packs.forEach { pack ->
                    val next = pack.nextLevelRef
                    WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                        ListRow(stringResource(packNameResource(pack.pack)),
                            subtitle = next?.let { if (it.isIntro) stringResource(R.string.game_guided_intro)
                                else stringResource(R.string.game_next_level, requireNotNull(it.levelNumber)) },
                            trailingValue = pack.boardSizes?.let { sizes ->
                                if (sizes.first == sizes.last) stringResource(R.string.game_board_size, sizes.first)
                                else stringResource(R.string.game_board_size_range, sizes.first, sizes.last)
                            }, onClick = { next?.let(onSelectPack) }, enabled = next != null)
                    }
                }
            }
            if (!premiumEnabled) {
                ListRow(stringResource(R.string.game_saved_credits, hintCredits, checkCredits),
                    onClick = { showRewards = true })
            }
        }
    }
}
