package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryTest {
    @Test
    fun emptyHistoryHasSafeBoundaries() {
        val history = History("initial")
        assertEquals("initial", history.present)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertSame(history, history.undo())
        assertSame(history, history.redo())
    }

    @Test
    fun undoesAndRedoesMultipleSnapshotsWithoutMutatingEarlierHistory() {
        val initial = History("initial")
        val first = initial.record("first")
        val second = first.record("second")
        val third = second.record("third")
        val undoOnce = third.undo()
        assertEquals("second", undoOnce.present)
        assertTrue(undoOnce.canUndo)
        assertTrue(undoOnce.canRedo)
        val undoAll = undoOnce.undo().undo()
        assertEquals("initial", undoAll.present)
        assertFalse(undoAll.canUndo)
        assertEquals("first", undoAll.redo().present)
        assertEquals("second", undoAll.redo().redo().present)
        val redoAll = undoAll.redo().redo().redo()
        assertEquals("third", redoAll.present)
        assertFalse(redoAll.canRedo)
        assertEquals("initial", initial.present)
        assertEquals("first", first.present)
        assertFalse(first.canRedo)
        assertEquals("third", third.present)
    }

    @Test
    fun newEditDropsRedoButEqualStatePreservesIt() {
        val undone = History(0).record(1).record(2).undo()
        assertSame(undone, undone.record(1))
        assertEquals(2, undone.record(1).redo().present)
        val branched = undone.record(3)
        assertFalse(branched.canRedo)
        assertEquals(1, branched.undo().present)
        assertEquals(3, branched.undo().redo().present)
        assertEquals(2, undone.redo().present)
    }

    @Test
    fun oneSnapshotRestoresAnEntireHintIncludingNotes() {
        val before = listOf(Move.SetNotes(Cell(0), 3), Move.SetDigit(Cell(1), 0))
        val after = listOf(Move.SetNotes(Cell(0), 0), Move.SetDigit(Cell(1), 2))
        val history = History(before).record(after)
        assertEquals(before, history.undo().present)
        assertEquals(after, history.undo().redo().present)
    }
}
