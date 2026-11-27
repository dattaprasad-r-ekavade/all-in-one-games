package com.datathecodie.witgrid.games.coloursort

/** Shared motion policy for Colour Sort board transitions. */
internal object ColourSortMotion {
    fun pourDurationMillis(reduceMotion: Boolean): Int =
        if (reduceMotion) 0 else POUR_DURATION_MILLIS

    fun completePopDurationMillis(reduceMotion: Boolean): Int =
        if (reduceMotion) 0 else COMPLETE_POP_DURATION_MILLIS

    private const val POUR_DURATION_MILLIS = 240
    private const val COMPLETE_POP_DURATION_MILLIS = 180
}
