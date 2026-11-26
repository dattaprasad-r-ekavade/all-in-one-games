package com.datathecodie.witgrid.ui.screens.failed

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.SecondaryButton
import com.datathecodie.witgrid.ui.screens.result.BoardThumbnail
import com.datathecodie.witgrid.ui.screens.result.FinishLevelLabel
import com.datathecodie.witgrid.ui.screens.result.FinishScreenFrame

@Composable
fun LevelFailedScreen(
    onContinue: suspend () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
    levelRef: LevelRef? = null,
    boardState: Any? = null,
    premiumEnabled: Boolean = false,
    continueCreditAvailable: Boolean = false,
    onRewardedContinue: suspend () -> AdShowResult = { AdShowResult.Unavailable },
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

    FinishScreenFrame(onClose) {
        BoardThumbnail(boardState, Modifier.size(168.dp))
        Text(stringResource(R.string.screen_level_failed), style = MaterialTheme.typography.headlineLarge)
        levelRef?.let { FinishLevelLabel(it) }
        Text(stringResource(R.string.failed_explanation), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        RewardedActionButton(
            titleResource = continueTitleResource(
                gameId = levelRef?.gameId,
                premiumEnabled = premiumEnabled,
                continueCreditAvailable = continueCreditAvailable,
            ),
            successResource = R.string.ads_continue_granted,
            onReward = {
                if (premiumEnabled || continueCreditAvailable) {
                    onContinue()
                    AdShowResult.RewardGranted
                } else {
                    val result = onRewardedContinue()
                    if (result == AdShowResult.RewardGranted) onContinue()
                    result
                }
            },
            onOpenPremiumOffer = { showPremiumOffer = true },
            alwaysShowPremiumOffer = !premiumEnabled,
            primaryStyle = true,
        )
        SecondaryButton(stringResource(R.string.button_restart), onRestart, Modifier.fillMaxWidth())
    }
}

private fun continueTitleResource(
    gameId: String?,
    premiumEnabled: Boolean,
    continueCreditAvailable: Boolean,
): Int {
    val extraTube = gameId == "coloursort"
    return when {
        premiumEnabled && extraTube -> R.string.failed_continue_premium_tube
        premiumEnabled -> R.string.failed_continue_premium
        continueCreditAvailable && extraTube -> R.string.failed_continue_credit_tube
        continueCreditAvailable -> R.string.failed_continue_credit
        extraTube -> R.string.ads_continue_rewarded_tube
        else -> R.string.ads_continue_rewarded
    }
}
