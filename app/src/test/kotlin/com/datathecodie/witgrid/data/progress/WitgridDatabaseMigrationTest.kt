package com.datathecodie.witgrid.data.progress

import android.content.Context
import androidx.room.Room
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
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
    fun versionTwoProgressMigratesWithMistakeCountersAtZero() = checkMigration(2)

    @Test
    fun versionThreeProgressKeepsAttemptStateBestTimeAndDailyHistory() = checkMigration(3)

    private fun checkMigration(oldVersion: Int) = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "witgrid-migration-$oldVersion-test.db"
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
                VALUES ('crowns', 'easy-001', X'010203', 4500, 2, 0, 3900)
                """.trimIndent(),
            )
            database.execSQL("INSERT INTO daily_puzzle_completions VALUES (100, 'crowns', 'daily-001')")
            if (oldVersion == 3) {
                database.execSQL("ALTER TABLE level_progress ADD COLUMN mistakesMade INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE level_progress ADD COLUMN extraMistakesGranted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE level_progress ADD COLUMN pendingContinueTransactionId TEXT DEFAULT NULL")
                database.execSQL(
                    "UPDATE level_progress SET mistakesMade = 3, extraMistakesGranted = 1, " +
                        "pendingContinueTransactionId = 'pending-continue'",
                )
            }
            database.version = oldVersion
        }

        val database = Room.databaseBuilder(context, WitgridDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            val restored = requireNotNull(
                database.levelProgressDao().get(gameId = "crowns", levelId = "easy-001"),
            )
            assertArrayEquals(byteArrayOf(1, 2, 3), restored.stateBytes)
            assertEquals(4_500, restored.elapsedTimeMillis)
            assertEquals(2, restored.hintsUsed)
            assertEquals(if (oldVersion == 3) 3 else 0, restored.mistakesMade)
            assertEquals(if (oldVersion == 3) 1 else 0, restored.extraMistakesGranted)
            assertEquals(if (oldVersion == 3) "pending-continue" else null, restored.pendingContinueTransactionId)
            assertEquals(0L, restored.attemptNumber)
            assertEquals(3_900L, restored.bestTimeMillis)
            val repository = RoomProgressRepository(database.levelProgressDao())
            val refs = listOf("easy-000", "easy-001", "easy-002").map { LevelRef("crowns", it, Pack.EASY) }
            assertEquals(2L, nextPackLevel(refs, repository.getProgressForGame("crowns"))?.levelNumber)
            repository.saveLevelProgress(restored.toModel().copy(attemptNumber = 5, stateBytes = byteArrayOf(4)))
            assertArrayEquals(
                byteArrayOf(1, 2, 3),
                requireNotNull(repository.getAttemptProgress("crowns", "easy-001", 0)).stateBytes,
            )
            assertArrayEquals(
                byteArrayOf(4),
                requireNotNull(repository.getAttemptProgress("crowns", "easy-001", 5)).stateBytes,
            )
            database.openHelper.readableDatabase.query("SELECT * FROM daily_puzzle_completions").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("daily-001", cursor.getString(cursor.getColumnIndexOrThrow("levelId")))
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
