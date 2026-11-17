package com.datathecodie.witgrid.domain.monetisation.spec

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.datathecodie.witgrid.data.settings.DataStorePlayerDataRepository
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.domain.config.DefaultConfig
import com.datathecodie.witgrid.domain.monetisation.AllowanceLedger
import com.datathecodie.witgrid.play.HintAllowanceDecision
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RewardGrantPersistenceSpecTest {
    @Test
    fun grantedRewardAndItsIdSurviveRepositoryAndStoreRecreation() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-reward-grant")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val initialJob = SupervisorJob()
        val initialScope = CoroutineScope(initialJob + Dispatchers.IO)
        var reopenedJob: Job? = null

        try {
            val initialRepository = repository(initialScope, file)
            val initialLedger = ledger(initialRepository)
            assertEquals(HintAllowanceDecision.Allowed, initialLedger.allowHint("free-hint"))
            assertEquals(1, initialLedger.grantReward("sdk-reward-17", RewardType.HINT))

            initialJob.cancelAndJoin()
            val activeReopenedJob = SupervisorJob()
            reopenedJob = activeReopenedJob
            val reopenedScope = CoroutineScope(activeReopenedJob + Dispatchers.IO)

            val reopenedRepository = repository(reopenedScope, file)
            val reopenedLedger = ledger(reopenedRepository)
            assertEquals(1, reopenedRepository.playerData.first().rewardBalances[RewardType.HINT])
            assertEquals(HintAllowanceDecision.Allowed, reopenedLedger.allowHint("rewarded-hint"))
            assertEquals(0, reopenedLedger.grantReward("sdk-reward-17", RewardType.HINT))
            assertEquals(HintAllowanceDecision.Denied, reopenedLedger.allowHint("duplicate-reward-check"))
        } finally {
            initialJob.cancelAndJoin()
            reopenedJob?.cancelAndJoin()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }

    private fun repository(
        scope: CoroutineScope,
        file: java.io.File,
    ): PlayerDataRepository {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return DataStorePlayerDataRepository(dataStore)
    }

    private fun ledger(repository: PlayerDataRepository) = AllowanceLedger(
        playerDataRepository = repository,
        config = DefaultConfig,
        localEpochDay = { 20_000 },
        isOnline = { true },
        freeChecksInEasyPack = false,
        nowEpochMillis = { 100 },
    )
}
