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
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderPlayerDataRepositoryTest {
    @Test
    fun reminderDefaultsOffAndSelectedTimePersists() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-reminder-data")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )

        try {
            val repository = DataStorePlayerDataRepository(dataStore)
            assertFalse(repository.playerData.first().reminder.enabled)

            val expected = ReminderSettings(enabled = true, hourOfDay = 22, minute = 35)
            repository.updateReminderSettings(expected)

            assertEquals(expected, repository.playerData.first().reminder)
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }
}
