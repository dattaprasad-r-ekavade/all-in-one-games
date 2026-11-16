package com.datathecodie.witgrid.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress WHERE gameId = :gameId AND levelId = :levelId")
    fun observe(gameId: String, levelId: String): Flow<LevelProgressEntity?>

    @Query("SELECT * FROM level_progress WHERE gameId = :gameId AND levelId = :levelId")
    suspend fun get(gameId: String, levelId: String): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LevelProgressEntity)

    @Query("DELETE FROM level_progress WHERE gameId = :gameId AND levelId = :levelId")
    suspend fun delete(gameId: String, levelId: String)
}
