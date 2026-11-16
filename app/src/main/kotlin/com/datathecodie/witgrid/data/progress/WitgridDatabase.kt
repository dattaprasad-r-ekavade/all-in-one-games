package com.datathecodie.witgrid.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LevelProgressEntity::class, DailyPuzzleCompletionEntity::class],
    version = 2,
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
        ).addMigrations(MIGRATION_1_2).build()
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
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
