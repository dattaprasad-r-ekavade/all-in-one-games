package com.datathecodie.witgrid.data.progress

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyPuzzleCompletionRepositoryTest {
    @Test
    fun completionHistorySeparatesLocalDatesAndGames() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WitgridDatabase::class.java,
        ).build()

        try {
            val repository = RoomDailyPuzzleCompletionRepository(database.dailyPuzzleCompletionDao())
            repository.recordCompletion(dateEpochDay = 10, gameId = "crowns", levelId = "daily-011")
            repository.recordCompletion(dateEpochDay = 375, gameId = "crowns", levelId = "daily-011")
            repository.recordCompletion(dateEpochDay = 10, gameId = "binary", levelId = "daily-021")

            val completions = repository.observeBetween(10, 375).first()

            assertEquals(3, completions.size)
            assertEquals(
                listOf("10:binary:daily-021", "10:crowns:daily-011", "375:crowns:daily-011"),
                completions.map { "${it.dateEpochDay}:${it.gameId}:${it.levelId}" },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun repeatedCompletionForOneGameAndDateReplacesItsRecord() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WitgridDatabase::class.java,
        ).build()

        try {
            val repository = RoomDailyPuzzleCompletionRepository(database.dailyPuzzleCompletionDao())
            repository.recordCompletion(dateEpochDay = 10, gameId = "crowns", levelId = "daily-011")
            repository.recordCompletion(dateEpochDay = 10, gameId = "crowns", levelId = "daily-011-retry")

            val completions = repository.observeBetween(10, 10).first()

            assertEquals(1, completions.size)
            assertEquals("daily-011-retry", completions.single().levelId)
        } finally {
            database.close()
        }
    }
}
