package com.datathecodie.witgrid.data.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProgressRepository(
    private val levelProgressDao: LevelProgressDao,
) : ProgressRepository, GameProgressSummaryRepository {
    override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> =
        levelProgressDao.observe(gameId, levelId).map { it?.toModel() }

    override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? =
        levelProgressDao.get(gameId, levelId)?.toModel()

    override suspend fun getProgressForGame(gameId: String): List<LevelProgress> =
        levelProgressDao.getForGame(gameId).map { it.toModel() }

    override suspend fun saveLevelProgress(progress: LevelProgress) {
        levelProgressDao.upsert(progress.toEntity())
    }

    override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
        levelProgressDao.delete(gameId, levelId)
    }
}
