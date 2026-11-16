package com.datathecodie.witgrid.data.progress

import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?>

    suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress?

    suspend fun saveLevelProgress(progress: LevelProgress)

    suspend fun deleteLevelProgress(gameId: String, levelId: String)
}

interface GameProgressSummaryRepository {
    suspend fun getProgressForGame(gameId: String): List<LevelProgress>
}
