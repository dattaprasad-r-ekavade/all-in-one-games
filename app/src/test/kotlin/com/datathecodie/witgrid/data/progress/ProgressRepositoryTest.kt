package com.datathecodie.witgrid.data.progress

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressRepositoryTest {
    @Test
    fun attemptsKeepSeparateBoardsAndBestTimeBelongsToBankPuzzle() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WitgridDatabase::class.java,
        ).build()
        try {
            val repository = RoomProgressRepository(database.levelProgressDao())
            val first = LevelProgress("binary", "bank-first", byteArrayOf(1), 300, 2, true, 300, attemptNumber = 1)
            repository.saveLevelProgress(first)
            assertNull(repository.getAttemptProgress("binary", "bank-first", 4))
            assertEquals(300L, repository.getBestTimeMillis("binary", "bank-first"))
            repository.saveLevelProgress(first.copy(
                stateBytes = byteArrayOf(0), elapsedTimeMillis = 0, hintsUsed = 0, completed = false, attemptNumber = 4,
            ))
            val recreated = RoomProgressRepository(database.levelProgressDao())
            assertArrayEquals(byteArrayOf(1), requireNotNull(recreated.getAttemptProgress("binary", "bank-first", 1)).stateBytes)
            assertArrayEquals(byteArrayOf(0), requireNotNull(recreated.getAttemptProgress("binary", "bank-first", 4)).stateBytes)
            recreated.saveLevelProgress(first.copy(attemptNumber = 7, bestTimeMillis = 200))
            assertEquals(200L, recreated.getBestTimeMillis("binary", "bank-first"))
            recreated.saveLevelProgress(first.copy(bestTimeMillis = 150))
            assertEquals(150L, recreated.getLevelProgress("binary", "bank-first")?.bestTimeMillis)
            recreated.saveLevelProgress(first.copy(bestTimeMillis = 500))
            assertEquals(150L, recreated.getBestTimeMillis("binary", "bank-first"))
            assertNull(recreated.getBestTimeMillis("binary", "another-bank-puzzle"))
            recreated.deleteLevelProgress("binary", "bank-first")
            assertNull(recreated.getAttemptProgress("binary", "bank-first", 1))
            assertNull(recreated.getBestTimeMillis("binary", "bank-first"))
        } finally {
            database.close()
        }
    }

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
