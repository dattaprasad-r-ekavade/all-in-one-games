package com.datathecodie.witgrid.ui.screens.game

// R1 will replace this UTC entry mapping with the shared daily clock and time-zone policy.
internal fun dailyPuzzleIndex(nowMillis: Long): Int = Math.floorMod(
    Math.floorDiv(nowMillis, MILLIS_PER_DAY) - DAILY_EPOCH_DAY,
    DAILY_LEVEL_COUNT,
).toInt()

private const val DAILY_EPOCH_DAY = 20_454L // 2026-01-01 UTC.
private const val MILLIS_PER_DAY = 86_400_000L
private const val DAILY_LEVEL_COUNT = 365L
