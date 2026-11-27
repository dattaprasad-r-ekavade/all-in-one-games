package com.datathecodie.witgrid.domain.monetisation

import com.datathecodie.witgrid.data.settings.AllowanceUsage
import com.datathecodie.witgrid.data.settings.MonetisationData
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.domain.config.Config
import com.datathecodie.witgrid.domain.config.ConfigKeys
import com.datathecodie.witgrid.domain.daily.RepairableStreakMiss
import com.datathecodie.witgrid.play.Allowance
import com.datathecodie.witgrid.play.HintAllowanceDecision

class AllowanceLedger(
    private val playerDataRepository: PlayerDataRepository,
    private val config: Config,
    private val localEpochDay: () -> Long,
    private val isOnline: () -> Boolean,
    private val freeChecksInEasyPack: Boolean,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : Allowance {
    override suspend fun allowHint(hintKey: String): HintAllowanceDecision {
        require(hintKey.isNotBlank()) { "Hint key must not be blank" }
        val todayEpochDay = localEpochDay()
        val online = isOnline()
        var decision: HintAllowanceDecision = HintAllowanceDecision.Denied
        playerDataRepository.updateMonetisationData { current ->
            val usage = current.allowanceUsage.forDate(todayEpochDay)
            when {
                hintKey in usage.chargedHintKeys -> {
                    decision = HintAllowanceDecision.AlreadyCharged
                    current.copy(allowanceUsage = usage)
                }

                current.premiumEnabled -> {
                    decision = HintAllowanceDecision.Allowed
                    current.copy(allowanceUsage = usage.withChargedHint(hintKey))
                }

                usage.hintsUsedToday < config.int(ConfigKeys.FREE_HINTS_PER_DAY).coerceAtLeast(0) -> {
                    decision = HintAllowanceDecision.Allowed
                    current.copy(
                        allowanceUsage = usage.copy(
                            hintsUsedToday = usage.hintsUsedToday + 1,
                            chargedHintKeys = usage.chargedHintKeys + hintKey,
                        ),
                    )
                }

                !online && usage.offlineHintsUsedToday <
                    config.int(ConfigKeys.OFFLINE_FREE_HINTS_PER_DAY).coerceAtLeast(0) -> {
                    decision = HintAllowanceDecision.Allowed
                    current.copy(
                        allowanceUsage = usage.copy(
                            offlineHintsUsedToday = usage.offlineHintsUsedToday + 1,
                            chargedHintKeys = usage.chargedHintKeys + hintKey,
                        ),
                    )
                }

                (current.rewardBalances[RewardType.HINT] ?: 0) > 0 -> {
                    decision = HintAllowanceDecision.Allowed
                    current.copy(
                        rewardBalances = current.rewardBalances +
                            (RewardType.HINT to current.rewardBalances.getValue(RewardType.HINT).dec()),
                        allowanceUsage = usage.withChargedHint(hintKey),
                    )
                }

                else -> current.copy(allowanceUsage = usage)
            }
        }
        return decision
    }

    override suspend fun allowCheck(): Boolean = updateWithReward(RewardType.CHECK) { current ->
        current.premiumEnabled || freeChecksInEasyPack
    }

    suspend fun allowContinue(): Boolean = updateWithReward(RewardType.EXTRA_LIFE) { current ->
        current.premiumEnabled
    }

    /** Consumes at most one Continue credit for [transactionId], safely across retries. */
    suspend fun allowContinue(
        transactionId: String,
        rewardType: RewardType = RewardType.EXTRA_LIFE,
    ): Boolean {
        require(transactionId.isNotBlank()) { "Continue transaction ID must not be blank" }
        require(rewardType == RewardType.EXTRA_LIFE || rewardType == RewardType.EXTRA_TUBE) {
            "Continue must consume EXTRA_LIFE or EXTRA_TUBE"
        }
        var allowed = false
        playerDataRepository.updateMonetisationData { current ->
            val processedIds = current.processedContinueTransactionIds
            if (transactionId in processedIds) {
                allowed = true
                current
            } else {
                val balance = current.rewardBalances[rewardType] ?: 0
                allowed = current.premiumEnabled || balance > 0
                if (!allowed) {
                    current
                } else {
                    current.copy(
                        rewardBalances = if (current.premiumEnabled) {
                            current.rewardBalances
                        } else {
                            current.rewardBalances + (rewardType to balance - 1)
                        },
                        processedContinueTransactionIds = processedIds + transactionId,
                    )
                }
            }
        }
        return allowed
    }

    /**
     * [repairableMiss] must come from a freshly reconciled daily-progress snapshot. The stored
     * miss and deadline are checked again in the allowance transaction before a reward is spent.
     */
    suspend fun allowStreakRepair(repairableMiss: RepairableStreakMiss?): Boolean {
        if (repairableMiss == null) return false
        var allowed = false
        playerDataRepository.updateMonetisationData { current ->
            val stillEligible = current.latestMissedEpochDay == repairableMiss.dateEpochDay &&
                current.latestMissedRepairDeadlineEpochMillis == repairableMiss.deadlineEpochMillis &&
                nowEpochMillis() < repairableMiss.deadlineEpochMillis
            val balance = current.rewardBalances[RewardType.STREAK_REPAIR] ?: 0
            allowed = stillEligible && (current.premiumEnabled || balance > 0)
            if (allowed && !current.premiumEnabled) {
                current.copy(
                    rewardBalances = current.rewardBalances + (RewardType.STREAK_REPAIR to balance - 1),
                )
            } else {
                current
            }
        }
        return allowed
    }

    suspend fun grantReward(grantId: String, type: RewardType, amount: Int = 1): Int {
        require(grantId.isNotBlank()) { "Reward grant ID must not be blank" }
        require(amount > 0) { "Reward amount must be positive" }
        var balance = 0
        playerDataRepository.updateMonetisationData { current ->
            balance = current.rewardBalances[type] ?: 0
            if (grantId in current.processedRewardGrantIds) {
                current
            } else {
                balance = Math.addExact(balance, amount)
                current.copy(
                    rewardBalances = current.rewardBalances + (type to balance),
                    processedRewardGrantIds = current.processedRewardGrantIds + grantId,
                )
            }
        }
        return balance
    }

    private suspend fun updateWithReward(
        type: RewardType,
        freeAllowance: (MonetisationData) -> Boolean,
    ): Boolean {
        var allowed = false
        playerDataRepository.updateMonetisationData { current ->
            val balance = current.rewardBalances[type] ?: 0
            allowed = when {
                freeAllowance(current) -> true
                balance > 0 -> true
                else -> false
            }
            if (allowed && !freeAllowance(current)) {
                current.copy(rewardBalances = current.rewardBalances + (type to balance - 1))
            } else {
                current
            }
        }
        return allowed
    }

    private fun AllowanceUsage.forDate(epochDay: Long): AllowanceUsage =
        if (localDateEpochDay == epochDay) {
            this
        } else {
            copy(
                localDateEpochDay = epochDay,
                hintsUsedToday = 0,
                offlineHintsUsedToday = 0,
                chargedHintKeys = emptySet(),
            )
        }

    private fun AllowanceUsage.withChargedHint(hintKey: String): AllowanceUsage =
        copy(chargedHintKeys = chargedHintKeys + hintKey)
}
