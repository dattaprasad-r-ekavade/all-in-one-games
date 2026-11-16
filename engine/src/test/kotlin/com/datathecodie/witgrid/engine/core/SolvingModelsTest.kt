package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SolvingModelsTest {
    private val technique = Technique("single", "Single", 0)

    @Test
    fun packNamesAndOrderAreStableBankTokens() {
        assertEquals(listOf("EASY", "MEDIUM", "HARD", "EXPERT"), Pack.entries.map { it.name })
    }

    @Test
    fun techniqueUsesLaneLocalNonNegativeRankAndStableId() {
        assertEquals("single", technique.id)
        assertEquals("Single", technique.displayName)
        assertEquals(0, technique.difficultyRank)
        assertThrows(IllegalArgumentException::class.java) { technique.copy(id = " ") }
        assertThrows(IllegalArgumentException::class.java) { technique.copy(displayName = "") }
        assertThrows(IllegalArgumentException::class.java) { technique.copy(difficultyRank = -1) }
    }

    @Test
    fun hintSeparatesEvidenceFromOrderedEdits() {
        val moves = listOf(Move.SetNotes(Cell(1), 0), Move.SetDigit(Cell(1), 2))
        val hint = HintStep(technique, listOf(Cell(0)), "Only one digit fits here.", moves)
        assertEquals(technique, hint.technique)
        assertEquals(listOf(Cell(0)), hint.cells)
        assertEquals(moves, hint.moves)
        assertThrows(IllegalArgumentException::class.java) { hint.copy(cells = emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { hint.copy(explanation = " ") }
        assertThrows(IllegalArgumentException::class.java) { hint.copy(moves = emptyList()) }
    }

    @Test
    fun gradeAllowsAnAlreadyCompletePuzzleWithoutInventingATechnique() {
        val complete = Grade(null, 0, Pack.EASY)
        assertEquals(0, complete.stepCount)
        val grade = Grade(technique, 4, Pack.HARD)
        assertEquals(technique, grade.hardestTechnique)
        assertEquals(Pack.HARD, grade.pack)
        assertThrows(IllegalArgumentException::class.java) { grade.copy(stepCount = -1) }
        assertThrows(IllegalArgumentException::class.java) { grade.copy(stepCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { grade.copy(hardestTechnique = null) }
    }

    @Test
    fun movesRepresentClearsMarksDigitsAndNotesWithoutToggles() {
        val cell = Cell(0)
        assertEquals(listOf("EMPTY", "FILLED", "CROSSED"), Mark.entries.map { it.name })
        for (mark in Mark.entries) {
            assertEquals(cell, Move.SetMark(cell, mark).cell)
            assertEquals(mark, Move.SetMark(cell, mark).mark)
        }
        assertEquals(0, Move.SetDigit(cell, 0).digit)
        assertEquals(6, Move.SetDigit(cell, 6).digit)
        assertEquals(cell, Move.SetDigit(cell, 6).cell)
        assertEquals(0, Move.SetNotes(cell, 0).digitMask)
        assertEquals(33, Move.SetNotes(cell, (1 shl 0) or (1 shl 5)).digitMask)
        assertEquals(cell, Move.SetNotes(cell, 33).cell)
        assertThrows(IllegalArgumentException::class.java) { Move.SetDigit(cell, -1) }
        assertThrows(IllegalArgumentException::class.java) { Move.SetNotes(cell, -1) }
    }
}
