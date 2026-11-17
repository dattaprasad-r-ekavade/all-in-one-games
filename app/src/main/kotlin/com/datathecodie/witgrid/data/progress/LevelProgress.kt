package com.datathecodie.witgrid.data.progress

data class LevelProgress(
    val gameId: String,
    val levelId: String,
    val stateBytes: ByteArray,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val completed: Boolean,
    val bestTimeMillis: Long?,
    val mistakesMade: Int = 0,
    val extraMistakesGranted: Int = 0,
    val pendingContinueTransactionId: String? = null,
)
