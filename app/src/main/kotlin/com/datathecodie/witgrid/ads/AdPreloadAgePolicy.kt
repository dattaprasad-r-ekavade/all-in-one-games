package com.datathecodie.witgrid.ads

class AdPreloadAgePolicy(
    private val maximumAgeMillis: Long = DEFAULT_MAXIMUM_AGE_MILLIS,
) {
    private var startedAtEpochMillis: Long? = null

    init {
        require(maximumAgeMillis > 0) { "Maximum preload age must be positive" }
    }

    fun shouldDrop(nowEpochMillis: Long): Boolean {
        val startedAt = startedAtEpochMillis ?: return false
        return nowEpochMillis < startedAt || nowEpochMillis - startedAt >= maximumAgeMillis
    }

    fun markStarted(nowEpochMillis: Long) {
        startedAtEpochMillis = nowEpochMillis
    }

    fun clear() {
        startedAtEpochMillis = null
    }

    fun hasStarted(): Boolean = startedAtEpochMillis != null

    companion object {
        const val DEFAULT_MAXIMUM_AGE_MILLIS = 60 * 60 * 1_000L
    }
}
