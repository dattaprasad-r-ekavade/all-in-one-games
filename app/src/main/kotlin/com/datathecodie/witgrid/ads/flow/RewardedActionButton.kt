package com.datathecodie.witgrid.ads.flow

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.datathecodie.witgrid.billing.BillingNotice
import com.datathecodie.witgrid.billing.BillingUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RewardedActionButton(
    @StringRes titleResource: Int,
    @StringRes successResource: Int,
    onReward: suspend () -> AdShowResult,
    onOpenPremiumOffer: () -> Unit,
    alwaysShowPremiumOffer: Boolean = false,
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
        if (showPremiumOffer || alwaysShowPremiumOffer) {
            TextButton(onClick = onOpenPremiumOffer) {
                Text(stringResource(R.string.ads_view_premium_offer))
            }
        }
    }
}

@Composable
fun PremiumOfferDialog(
    premiumEnabled: Boolean,
    billingState: BillingUiState,
    onPurchasePremium: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_offer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.premium_offer_body))
                if (premiumEnabled) {
                    Text(stringResource(R.string.premium_owned))
                } else {
                    billingState.productPrice?.let { price ->
                        Text(stringResource(R.string.premium_price, price))
                    }
                    BillingNoticeMessage(billingState.notice)
                }
                if (billingState.isLoading && billingState.productPrice == null) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        },
        confirmButton = {
            if (premiumEnabled) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_close)) }
            } else {
                Button(
                    onClick = onPurchasePremium,
                    enabled = billingState.productPrice != null && !billingState.isProcessing && !billingState.isLoading,
                ) {
                    if (billingState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.premium_purchase))
                    }
                }
            }
        },
        dismissButton = {
            if (!premiumEnabled) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_close)) }
            }
        },
    )
}

@Composable
fun PremiumPurchaseControls(
    premiumEnabled: Boolean,
    billingState: BillingUiState,
    onPurchasePremium: () -> Unit,
    onRestorePurchases: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.premium_offer_body), style = MaterialTheme.typography.bodyMedium)
        if (premiumEnabled) {
            Text(stringResource(R.string.premium_owned))
        } else {
            billingState.productPrice?.let { price ->
                Text(stringResource(R.string.premium_price, price))
            }
            BillingNoticeMessage(billingState.notice)
            Button(
                onClick = onPurchasePremium,
                enabled = billingState.productPrice != null && !billingState.isLoading && !billingState.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (billingState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.premium_purchase))
                }
            }
        }
        OutlinedButton(
            onClick = onRestorePurchases,
            enabled = !billingState.isLoading && !billingState.isProcessing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_restore_purchases))
        }
        if (billingState.isLoading && billingState.productPrice == null && !premiumEnabled) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun BillingNoticeMessage(notice: BillingNotice) {
    val resource = when (notice) {
        BillingNotice.NONE -> null
        BillingNotice.STORE_UNAVAILABLE -> R.string.premium_store_unavailable
        BillingNotice.PRODUCT_UNAVAILABLE -> R.string.premium_product_unavailable
        BillingNotice.PURCHASE_PENDING -> R.string.premium_purchase_pending
        BillingNotice.PURCHASE_CANCELLED -> R.string.premium_purchase_cancelled
        BillingNotice.PURCHASE_COMPLETE -> R.string.premium_purchase_complete
        BillingNotice.PURCHASE_FAILED -> R.string.premium_purchase_failed
        BillingNotice.RESTORE_COMPLETE -> R.string.premium_restore_complete
        BillingNotice.RESTORE_EMPTY -> R.string.premium_restore_empty
        BillingNotice.RESTORE_FAILED -> R.string.premium_restore_failed
    }
    resource?.let { Text(stringResource(it), modifier = Modifier.padding(top = 2.dp)) }
}
