package com.datathecodie.witgrid.ui.screens.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
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
import com.datathecodie.witgrid.ui.components.SectionHeader
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.screens.home.GameBoardPreview
import com.datathecodie.witgrid.ui.screens.home.gameNameResource
import com.datathecodie.witgrid.ui.screens.home.packNameResource
import com.datathecodie.witgrid.ui.screens.play.playPuzzleStatus
import com.datathecodie.witgrid.ui.screens.play.playRulesResource
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
    val context = LocalContext.current
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
                        ContinueButton { onContinue(level) }
                    }
                }
                WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(role = Role.Button, onClick = onOpenDaily)
                            .heightIn(min = 88.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = colors.accent,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(R.string.game_daily_name, stringResource(gameNameResource(state.gameId))),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(if (dailyCompletedToday) R.string.game_daily_solved else R.string.game_daily_entry),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedInk,
                            )
                        }
                        if (dailyCompletedToday) {
                            DailyCompletedIcon()
                        } else {
                            RowChevron()
                        }
                    }
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
                val hintCount = context.resources.getQuantityString(
                    R.plurals.game_hint_credit_count,
                    hintCredits,
                    hintCredits,
                )
                SavedCreditsRow(
                    title = hintCount,
                    onClick = { showRewards = true },
                )
            }
        }
    }
}

@Composable
private fun ContinueButton(onClick: () -> Unit) {
    val colors = WitgridTheme.colors
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(16.dp)) {
                val play = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.1f)
                    lineTo(size.width * 0.88f, size.height * 0.5f)
                    lineTo(size.width * 0.2f, size.height * 0.9f)
                    close()
                }
                drawPath(play, colors.onAccent)
            }
            Text(stringResource(R.string.button_continue))
        }
    }
}

@Composable
private fun DailyCompletedIcon() {
    val colors = WitgridTheme.colors
    Canvas(Modifier.size(40.dp)) {
        drawCircle(colors.accent)
        val check = Path().apply {
            moveTo(size.width * 0.27f, size.height * 0.52f)
            lineTo(size.width * 0.43f, size.height * 0.68f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
        }
        drawPath(
            path = check,
            color = colors.onAccent,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun RowChevron() {
    val colors = WitgridTheme.colors
    Icon(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = null,
        modifier = Modifier.size(20.dp).rotate(
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) 180f else 0f,
        ),
        tint = colors.mutedInk,
    )
}

@Composable
private fun SavedCreditsRow(title: String, onClick: () -> Unit) {
    val colors = WitgridTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HintCreditsIcon()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.game_get_more), style = MaterialTheme.typography.bodyMedium, color = colors.mutedInk)
        }
        RowChevron()
    }
}

@Composable
private fun HintCreditsIcon() {
    val color = WitgridTheme.colors.mutedInk
    Canvas(Modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()
        val center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.39f)
        drawCircle(color, radius = size.minDimension * 0.25f, center = center, style = Stroke(stroke))
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.61f),
            androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.7f), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.61f),
            androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.7f), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.73f),
            androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.73f), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.83f),
            androidx.compose.ui.geometry.Offset(size.width * 0.56f, size.height * 0.83f), stroke)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRulesSheet(gameId: String, onDismiss: () -> Unit) {
    val colors = WitgridTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(
                stringResource(R.string.play_rules_title, stringResource(gameNameResource(gameId))),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(playRulesResource(gameId)),
                Modifier.padding(top = 16.dp, bottom = 32.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
