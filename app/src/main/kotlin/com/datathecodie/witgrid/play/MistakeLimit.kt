package com.datathecodie.witgrid.play

internal object MistakeLimit {
    const val INITIAL_MISTAKES = 3

    fun mistakesAllowed(extraMistakesGranted: Int): Int =
        INITIAL_MISTAKES + extraMistakesGranted.coerceAtLeast(0)

    fun hasFailed(enabled: Boolean, mistakesMade: Int, extraMistakesGranted: Int): Boolean =
        enabled && mistakesMade >= mistakesAllowed(extraMistakesGranted)
}
