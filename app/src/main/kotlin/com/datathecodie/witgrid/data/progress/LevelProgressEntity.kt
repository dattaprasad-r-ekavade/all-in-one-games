package com.datathecodie.witgrid.data.progress

import androidx.room.Entity

@Entity(
    tableName = "level_progress",
    primaryKeys = ["gameId", "levelId"],
)
data class LevelProgressEntity(
    val gameId: String,
    val levelId: String,
    val stateBytes: ByteArray,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val completed: Boolean,
    val bestTimeMillis: Long?,
)

internal fun LevelProgressEntity.toModel(): LevelProgress = LevelProgress(
    gameId = gameId,
    levelId = levelId,
    stateBytes = stateBytes.copyOf(),
    elapsedTimeMillis = elapsedTimeMillis,
    hintsUsed = hintsUsed,
    completed = completed,
    bestTimeMillis = bestTimeMillis,
)

internal fun LevelProgress.toEntity(): LevelProgressEntity = LevelProgressEntity(
    gameId = gameId,
    levelId = levelId,
    stateBytes = stateBytes.copyOf(),
    elapsedTimeMillis = elapsedTimeMillis,
    hintsUsed = hintsUsed,
    completed = completed,
    bestTimeMillis = bestTimeMillis,
)
