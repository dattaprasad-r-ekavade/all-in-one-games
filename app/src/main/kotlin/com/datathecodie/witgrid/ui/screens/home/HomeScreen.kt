package com.datathecodie.witgrid.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.Chip
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
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
            WitgridCard(
                Modifier.fillMaxWidth(),
                onClick = { showPremiumOffer = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumCrownIcon()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.home_premium_row), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(if (premiumEnabled) R.string.premium_owned else R.string.home_premium_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WitgridTheme.colors.mutedInk,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).rotate(
                            if (LocalLayoutDirection.current == LayoutDirection.Ltr) 180f else 0f,
                        ),
                        tint = WitgridTheme.colors.mutedInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumCrownIcon() {
    val color = WitgridTheme.colors.ink
    Canvas(Modifier.size(24.dp)) {
        val crown = Path().apply {
            moveTo(size.width * 0.13f, size.height * 0.34f)
            lineTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.43f, size.height * 0.28f)
            lineTo(size.width * 0.52f, size.height * 0.52f)
            lineTo(size.width * 0.76f, size.height * 0.30f)
            lineTo(size.width * 0.88f, size.height * 0.76f)
            lineTo(size.width * 0.14f, size.height * 0.76f)
            close()
        }
        drawPath(crown, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.88f),
            androidx.compose.ui.geometry.Offset(size.width * 0.87f, size.height * 0.88f),
            2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

internal fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    "sudoku" -> R.string.game_sudoku
    "skyscrapers" -> R.string.game_skyscrapers
    "futoshiki" -> R.string.game_futoshiki
    "path" -> R.string.game_path
    "killersudoku" -> R.string.game_killersudoku
    "arrows" -> R.string.game_arrows
    "coloursort" -> R.string.game_coloursort
    "mosaic" -> R.string.game_mosaic
    "klondike" -> R.string.game_klondike
    "tactics" -> R.string.game_tactics
    else -> R.string.game_unknown
}

internal fun packNameResource(pack: Pack): Int = when (pack) {
    Pack.EASY -> R.string.pack_easy
    Pack.MEDIUM -> R.string.pack_medium
    Pack.HARD -> R.string.pack_hard
    Pack.EXPERT -> R.string.pack_expert
}
