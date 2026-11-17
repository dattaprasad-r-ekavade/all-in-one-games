package com.datathecodie.witgrid.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for the first-launch crash: writing null optional values to a fresh store removed
 * absent keys inside an expression, which unboxed null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EmptyStoreWriteTest {
    @Test
    fun firstStreakWriteOnEmptyStoreSucceeds() = withFreshRepository { repository ->
        repository.updateStreak(StreakData())
        assertEquals(StreakData(), repository.playerData.first().streak)
    }

    @Test
    fun clearingMissedDayRemovesRepairFields() = withFreshRepository { repository ->
        val missed = StreakData(
            currentDays = 0,
            lastActivityEpochDay = 100,
            latestMissedEpochDay = 101,
            latestMissedPreviousStreakDays = 5,
            latestMissedPreviousActivityEpochDay = 99,
            latestMissedRepairDeadlineEpochMilli = 1_000L,
        )
        repository.updateStreak(missed)
        assertEquals(missed, repository.playerData.first().streak)

        val cleared = StreakData(currentDays = 6, lastActivityEpochDay = 102)
        repository.updateStreak(cleared)
        assertEquals(cleared, repository.playerData.first().streak)
    }

    @Test
    fun firstMonetisationWriteOnEmptyStoreSucceeds() = withFreshRepository { repository ->
        val before = repository.playerData.first()
        repository.updateMonetisationData { it }
        assertEquals(before, repository.playerData.first())
    }

    private fun withFreshRepository(block: suspend (DataStorePlayerDataRepository) -> Unit) = runBlocking {
        val directory = Files.createTempDirectory("witgrid-empty-store")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            block(DataStorePlayerDataRepository(dataStore))
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }
}
