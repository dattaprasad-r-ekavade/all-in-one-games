package com.datathecodie.witgrid.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LevelProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WitgridDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao

    companion object {
        const val DATABASE_NAME = "witgrid.db"

        fun create(context: Context): WitgridDatabase = Room.databaseBuilder(
            context.applicationContext,
            WitgridDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }
}
