package com.datathecodie.witgrid.data.progress

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "level_progress",
    primaryKeys = ["gameId", "levelId", "attemptNumber"],
)
data class LevelProgressEntity(
    val gameId: String,
    val levelId: String,
    val stateBytes: ByteArray,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val completed: Boolean,
    val bestTimeMillis: Long?,
    @ColumnInfo(defaultValue = "0")
    val mistakesMade: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val extraMistakesGranted: Int = 0,
    @ColumnInfo(defaultValue = "NULL")
    val pendingContinueTransactionId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val attemptNumber: Long = 0,
)

internal fun LevelProgressEntity.toModel(): LevelProgress = LevelProgress(
    gameId = gameId,
    levelId = levelId,
    stateBytes = stateBytes.copyOf(),
    elapsedTimeMillis = elapsedTimeMillis,
    hintsUsed = hintsUsed,
    completed = completed,
    bestTimeMillis = bestTimeMillis,
    mistakesMade = mistakesMade,
    extraMistakesGranted = extraMistakesGranted,
    pendingContinueTransactionId = pendingContinueTransactionId,
    attemptNumber = attemptNumber,
)

internal fun LevelProgress.toEntity(): LevelProgressEntity = LevelProgressEntity(
    gameId = gameId,
    levelId = levelId,
    stateBytes = stateBytes.copyOf(),
    elapsedTimeMillis = elapsedTimeMillis,
    hintsUsed = hintsUsed,
    completed = completed,
    bestTimeMillis = bestTimeMillis,
    mistakesMade = mistakesMade,
    extraMistakesGranted = extraMistakesGranted,
    pendingContinueTransactionId = pendingContinueTransactionId,
    attemptNumber = attemptNumber,
)
