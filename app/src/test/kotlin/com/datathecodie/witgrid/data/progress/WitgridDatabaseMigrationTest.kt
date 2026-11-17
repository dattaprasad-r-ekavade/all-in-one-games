package com.datathecodie.witgrid.data.progress

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
class WitgridDatabaseMigrationTest {
    @Test
    fun versionTwoProgressMigratesWithMistakeCountersAtZero() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "witgrid-migration-test.db"
        context.deleteDatabase(databaseName)
        val oldDatabase = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        oldDatabase.use { database ->
            database.execSQL(
                """
                CREATE TABLE level_progress (
                    gameId TEXT NOT NULL,
                    levelId TEXT NOT NULL,
                    stateBytes BLOB NOT NULL,
                    elapsedTimeMillis INTEGER NOT NULL,
                    hintsUsed INTEGER NOT NULL,
                    completed INTEGER NOT NULL,
                    bestTimeMillis INTEGER,
                    PRIMARY KEY(gameId, levelId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE daily_puzzle_completions (
                    dateEpochDay INTEGER NOT NULL,
                    gameId TEXT NOT NULL,
                    levelId TEXT NOT NULL,
                    PRIMARY KEY(dateEpochDay, gameId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO level_progress
                    (gameId, levelId, stateBytes, elapsedTimeMillis, hintsUsed, completed, bestTimeMillis)
                VALUES ('crowns', 'easy-001', X'010203', 4500, 2, 0, NULL)
                """.trimIndent(),
            )
            database.version = 2
        }

        val database = Room.databaseBuilder(context, WitgridDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val restored = requireNotNull(
                database.levelProgressDao().get(gameId = "crowns", levelId = "easy-001"),
            )
            assertArrayEquals(byteArrayOf(1, 2, 3), restored.stateBytes)
            assertEquals(4_500, restored.elapsedTimeMillis)
            assertEquals(2, restored.hintsUsed)
            assertEquals(0, restored.mistakesMade)
            assertEquals(0, restored.extraMistakesGranted)
            assertEquals(null, restored.pendingContinueTransactionId)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
