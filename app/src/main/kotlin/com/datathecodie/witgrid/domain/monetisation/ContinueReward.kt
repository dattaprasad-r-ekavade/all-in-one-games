package com.datathecodie.witgrid.domain.monetisation

import com.datathecodie.witgrid.data.settings.RewardType

/** Continue credit consumed after a failed level; Colour Sort uses an extra empty tube. */
object ContinueReward {
    fun typeFor(gameId: String): RewardType =
        if (gameId == "coloursort") RewardType.EXTRA_TUBE else RewardType.EXTRA_LIFE
}
