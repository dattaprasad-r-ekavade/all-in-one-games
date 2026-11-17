package com.datathecodie.witgrid.ads.flow

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.AdShowResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RewardedActionButton(
    @StringRes titleResource: Int,
    @StringRes successResource: Int,
    onReward: suspend () -> AdShowResult,
    onOpenPremiumOffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pending by remember { mutableStateOf(false) }
    var messageResource by remember { mutableStateOf<Int?>(null) }
    var showPremiumOffer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
            onClick = {
                pending = true
                messageResource = null
                showPremiumOffer = false
                scope.launch {
                    try {
                        when (onReward()) {
                            AdShowResult.RewardGranted -> messageResource = successResource
                            AdShowResult.DismissedWithoutReward,
                            AdShowResult.Shown -> messageResource = R.string.ads_reward_not_completed
                            AdShowResult.Unavailable,
                            AdShowResult.Failed -> {
                                messageResource = R.string.ads_reward_unavailable
                                showPremiumOffer = true
                            }
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        messageResource = R.string.ads_reward_unavailable
                        showPremiumOffer = true
                    } finally {
                        pending = false
                    }
                }
            },
            enabled = !pending,
        ) {
            if (pending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(titleResource))
            }
        }
        messageResource?.let { resourceId ->
            Text(
                text = stringResource(resourceId),
                style = MaterialTheme.typography.bodySmall,
                color = if (showPremiumOffer) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showPremiumOffer) {
            TextButton(onClick = onOpenPremiumOffer) {
                Text(stringResource(R.string.ads_view_premium_offer))
            }
        }
    }
}

@Composable
fun PremiumOfferDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_offer_title)) },
        text = { Text(stringResource(R.string.premium_offer_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_close)) }
        },
    )
}
