package com.datathecodie.witgrid.data.progress

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameProgressSummaryRepositoryTest {
    @Test
    fun returnsOnlyProgressForRequestedGame() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WitgridDatabase::class.java,
        ).build()

        try {
            val repository = RoomProgressRepository(database.levelProgressDao())
            repository.saveLevelProgress(progress("crowns", "easy-001", completed = true))
            repository.saveLevelProgress(progress("crowns", "easy-002", completed = false))
            repository.saveLevelProgress(progress("binary", "easy-001", completed = true))

            val result = repository.getProgressForGame("crowns")

            assertEquals(listOf("easy-001", "easy-002"), result.map { it.levelId }.sorted())
            assertEquals(listOf(true, false), result.sortedBy { it.levelId }.map { it.completed })
        } finally {
            database.close()
        }
    }

    private fun progress(gameId: String, levelId: String, completed: Boolean) = LevelProgress(
        gameId = gameId,
        levelId = levelId,
        stateBytes = byteArrayOf(),
        elapsedTimeMillis = 0,
        hintsUsed = 0,
        completed = completed,
        bestTimeMillis = null,
    )
}
