package com.datathecodie.witgrid.ui.screens.play

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Keeps accepted edits alive when the player leaves the screen. */
internal class PlayPersistenceScope(context: CoroutineContext) {
    private val operationsJob = SupervisorJob()
    val scope = CoroutineScope(context + operationsJob)
    private val cleanupScope = CoroutineScope(context)
    private var pauseActiveController: suspend () -> Unit = {}

    fun registerController(pause: suspend () -> Unit) {
        pauseActiveController = pause
    }

    fun close(afterSaves: () -> Unit) {
        operationsJob.complete()
        cleanupScope.launch {
            try {
                operationsJob.join()
                pauseActiveController()
            } finally {
                afterSaves()
            }
        }
    }
}
