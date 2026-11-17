package com.datathecodie.witgrid.ui.screens.failed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.ads.flow.RewardedActionButton
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun LevelFailedScreen(
    onContinue: suspend () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    premiumEnabled: Boolean = false,
    continueCreditAvailable: Boolean = false,
    onRewardedContinue: suspend () -> AdShowResult = { AdShowResult.Unavailable },
) {
    var showPremiumOffer by rememberSaveable { mutableStateOf(false) }
    if (showPremiumOffer) PremiumOfferDialog(onDismiss = { showPremiumOffer = false })

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.screen_level_failed), style = MaterialTheme.typography.headlineMedium)
            RewardedActionButton(
                titleResource = when {
                    premiumEnabled -> R.string.failed_continue_premium
                    continueCreditAvailable -> R.string.failed_continue_credit
                    else -> R.string.ads_continue_rewarded
                },
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
            )
            OutlinedButton(onClick = onRestart) {
                Text(stringResource(R.string.button_restart))
            }
            OutlinedButton(onClick = onHome) {
                Text(stringResource(R.string.button_home))
            }
        }
    }
}
