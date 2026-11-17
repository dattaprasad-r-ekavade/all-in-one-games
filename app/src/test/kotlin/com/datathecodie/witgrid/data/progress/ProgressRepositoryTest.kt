package com.datathecodie.witgrid.data.progress

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressRepositoryTest {
    @Test
    fun savedProgressSurvivesRepositoryRecreation() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WitgridDatabase::class.java,
        ).build()

        try {
            val saved = LevelProgress(
                gameId = "binary",
                levelId = "easy-001",
                stateBytes = byteArrayOf(1, 3, 5, 8),
                elapsedTimeMillis = 42_000,
                hintsUsed = 2,
                completed = true,
                bestTimeMillis = 39_500,
                mistakesMade = 3,
                extraMistakesGranted = 2,
                pendingContinueTransactionId = "continue-1",
            )
            RoomProgressRepository(database.levelProgressDao()).saveLevelProgress(saved)

            val restored = RoomProgressRepository(database.levelProgressDao())
                .getLevelProgress(gameId = saved.gameId, levelId = saved.levelId)

            requireNotNull(restored)
            assertArrayEquals(saved.stateBytes, restored.stateBytes)
            assertEquals(saved.gameId, restored.gameId)
            assertEquals(saved.levelId, restored.levelId)
            assertEquals(saved.elapsedTimeMillis, restored.elapsedTimeMillis)
            assertEquals(saved.hintsUsed, restored.hintsUsed)
            assertEquals(saved.completed, restored.completed)
            assertEquals(saved.bestTimeMillis, restored.bestTimeMillis)
            assertEquals(saved.mistakesMade, restored.mistakesMade)
            assertEquals(saved.extraMistakesGranted, restored.extraMistakesGranted)
            assertEquals(saved.pendingContinueTransactionId, restored.pendingContinueTransactionId)
        } finally {
            database.close()
        }
    }
}
