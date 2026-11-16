package com.datathecodie.witgrid.ui.screens.play

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class PlayPersistenceScopeTest {
    @Test
    fun closingWaitsForAcceptedEditsAndFinalTimerSaveBeforeClosingDatabase() = runBlocking {
        val persistence = PlayPersistenceScope(Dispatchers.Unconfined)
        val releaseEdit = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        persistence.registerController { events += "pause and save" }
        persistence.scope.launch {
            releaseEdit.await()
            events += "edit saved"
        }

        persistence.close {
            events += "database closed"
            closed.complete(Unit)
        }

        assertFalse(closed.isCompleted)
        releaseEdit.complete(Unit)
        closed.await()
        assertEquals(listOf("edit saved", "pause and save", "database closed"), events)
    }
}
