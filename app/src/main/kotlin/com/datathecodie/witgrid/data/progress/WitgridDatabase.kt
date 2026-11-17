package com.datathecodie.witgrid.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LevelProgressEntity::class, DailyPuzzleCompletionEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class WitgridDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao

    abstract fun dailyPuzzleCompletionDao(): DailyPuzzleCompletionDao

    companion object {
        const val DATABASE_NAME = "witgrid.db"

        fun create(context: Context): WitgridDatabase = Room.databaseBuilder(
            context.applicationContext,
            WitgridDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_puzzle_completions (
                dateEpochDay INTEGER NOT NULL,
                gameId TEXT NOT NULL,
                levelId TEXT NOT NULL,
                PRIMARY KEY(dateEpochDay, gameId)
            )
            """.trimIndent(),
        )
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE level_progress ADD COLUMN mistakesMade INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE level_progress ADD COLUMN extraMistakesGranted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE level_progress ADD COLUMN pendingContinueTransactionId TEXT DEFAULT NULL")
    }
}

// Bank order lives in the catalog, so legacy attempt zero is numbered when read with that catalog.
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE level_progress_numbered (
                gameId TEXT NOT NULL,
                levelId TEXT NOT NULL,
                stateBytes BLOB NOT NULL,
                elapsedTimeMillis INTEGER NOT NULL,
                hintsUsed INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                bestTimeMillis INTEGER,
                mistakesMade INTEGER NOT NULL DEFAULT 0,
                extraMistakesGranted INTEGER NOT NULL DEFAULT 0,
                pendingContinueTransactionId TEXT DEFAULT NULL,
                attemptNumber INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(gameId, levelId, attemptNumber)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO level_progress_numbered
            SELECT gameId, levelId, stateBytes, elapsedTimeMillis, hintsUsed, completed,
                bestTimeMillis, mistakesMade, extraMistakesGranted, pendingContinueTransactionId, 0
            FROM level_progress
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE level_progress")
        db.execSQL("ALTER TABLE level_progress_numbered RENAME TO level_progress")
    }
}
