package com.datathecodie.witgrid.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPuzzleCompletionDao {
    @Query(
        """
        SELECT * FROM daily_puzzle_completions
        WHERE dateEpochDay BETWEEN :firstEpochDay AND :lastEpochDay
        ORDER BY dateEpochDay, gameId
        """,
    )
    fun observeBetween(firstEpochDay: Long, lastEpochDay: Long): Flow<List<DailyPuzzleCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordCompletion(completion: DailyPuzzleCompletionEntity)
}
