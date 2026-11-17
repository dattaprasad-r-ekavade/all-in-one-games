package com.datathecodie.witgrid.data.progress

import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    /** Observes the latest attempt for a bank puzzle, including its shared best time. */
    fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?>

    /** Returns the latest attempt; play restores through [getAttemptProgress] instead. */
    suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress?

    suspend fun getAttemptProgress(gameId: String, levelId: String, attemptNumber: Long): LevelProgress? =
        getLevelProgress(gameId, levelId)?.takeIf { it.attemptNumber == attemptNumber }

    suspend fun getBestTimeMillis(gameId: String, levelId: String): Long? =
        getLevelProgress(gameId, levelId)?.bestTimeMillis

    suspend fun saveLevelProgress(progress: LevelProgress)

    suspend fun deleteLevelProgress(gameId: String, levelId: String)
}

interface GameProgressSummaryRepository {
    suspend fun getProgressForGame(gameId: String): List<LevelProgress>
}
