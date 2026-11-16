package com.datathecodie.witgrid.data.progress

data class LevelProgress(
    val gameId: String,
    val levelId: String,
    val stateBytes: ByteArray,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val completed: Boolean,
    val bestTimeMillis: Long?,
)
