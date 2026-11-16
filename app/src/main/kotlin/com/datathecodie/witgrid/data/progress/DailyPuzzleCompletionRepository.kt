package com.datathecodie.witgrid.data.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DailyPuzzleCompletionRepository {
    fun observeBetween(firstEpochDay: Long, lastEpochDay: Long): Flow<List<DailyPuzzleCompletion>>

    suspend fun recordCompletion(dateEpochDay: Long, gameId: String, levelId: String)
}

class RoomDailyPuzzleCompletionRepository(
    private val dailyPuzzleCompletionDao: DailyPuzzleCompletionDao,
) : DailyPuzzleCompletionRepository {
    override fun observeBetween(
        firstEpochDay: Long,
        lastEpochDay: Long,
    ): Flow<List<DailyPuzzleCompletion>> =
        dailyPuzzleCompletionDao.observeBetween(firstEpochDay, lastEpochDay).map { completions ->
            completions.map { it.toModel() }
        }

    override suspend fun recordCompletion(dateEpochDay: Long, gameId: String, levelId: String) {
        require(gameId.isNotBlank()) { "Game ID must not be blank" }
        require(levelId.isNotBlank()) { "Level ID must not be blank" }
        dailyPuzzleCompletionDao.recordCompletion(
            DailyPuzzleCompletionEntity(dateEpochDay, gameId, levelId),
        )
    }
}
