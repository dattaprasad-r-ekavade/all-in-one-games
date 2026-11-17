package com.datathecodie.witgrid.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress WHERE gameId = :gameId AND levelId = :levelId ORDER BY attemptNumber DESC LIMIT 1")
    fun observe(gameId: String, levelId: String): Flow<LevelProgressEntity?>

    @Query("SELECT * FROM level_progress WHERE gameId = :gameId AND levelId = :levelId ORDER BY attemptNumber DESC LIMIT 1")
    suspend fun get(gameId: String, levelId: String): LevelProgressEntity?

    @Query("SELECT * FROM level_progress WHERE gameId = :gameId AND levelId = :levelId AND attemptNumber = :attemptNumber")
    suspend fun getAttempt(gameId: String, levelId: String, attemptNumber: Long): LevelProgressEntity?

    @Query("SELECT MIN(bestTimeMillis) FROM level_progress WHERE gameId = :gameId AND levelId = :levelId")
    suspend fun getBestTime(gameId: String, levelId: String): Long?

    @Query("SELECT * FROM level_progress WHERE gameId = :gameId ORDER BY attemptNumber DESC")
    suspend fun getForGame(gameId: String): List<LevelProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LevelProgressEntity)

    @Query("UPDATE level_progress SET bestTimeMillis = :bestTimeMillis WHERE gameId = :gameId AND levelId = :levelId")
    suspend fun updateBankBestTime(gameId: String, levelId: String, bestTimeMillis: Long)

    @Transaction
    suspend fun save(progress: LevelProgressEntity) {
        val bestTimeMillis = listOfNotNull(progress.bestTimeMillis, getBestTime(progress.gameId, progress.levelId))
            .minOrNull()
        upsert(progress.copy(bestTimeMillis = bestTimeMillis))
        if (bestTimeMillis != null) {
            updateBankBestTime(progress.gameId, progress.levelId, bestTimeMillis)
        }
    }

    @Query("DELETE FROM level_progress WHERE gameId = :gameId AND levelId = :levelId")
    suspend fun delete(gameId: String, levelId: String)
}
