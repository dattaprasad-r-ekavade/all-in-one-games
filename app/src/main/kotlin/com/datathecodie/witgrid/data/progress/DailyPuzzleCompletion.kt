package com.datathecodie.witgrid.data.progress

import androidx.room.Entity

data class DailyPuzzleCompletion(
    val dateEpochDay: Long,
    val gameId: String,
    val levelId: String,
)

@Entity(
    tableName = "daily_puzzle_completions",
    primaryKeys = ["dateEpochDay", "gameId"],
)
data class DailyPuzzleCompletionEntity(
    val dateEpochDay: Long,
    val gameId: String,
    val levelId: String,
)

internal fun DailyPuzzleCompletionEntity.toModel(): DailyPuzzleCompletion = DailyPuzzleCompletion(
    dateEpochDay = dateEpochDay,
    gameId = gameId,
    levelId = levelId,
)

internal fun DailyPuzzleCompletion.toEntity(): DailyPuzzleCompletionEntity = DailyPuzzleCompletionEntity(
    dateEpochDay = dateEpochDay,
    gameId = gameId,
    levelId = levelId,
)
