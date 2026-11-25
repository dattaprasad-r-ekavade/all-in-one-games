package com.datathecodie.witgrid.engine.skyscrapers.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersCodec
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersGenerator
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersGradeCodec
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersStateCodec
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersTechniques
import com.datathecodie.witgrid.engine.skyscrapers.spec.EdgeClues
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.fullClues4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.puzzle
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.solution4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.topTwoRowGivens4
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Independent Skyscrapers codec cases: bank-line and saved-state round trips. */
class SkyscrapersCodecSpecTest {

    @Test
    fun bankLineWithoutGivensRoundTrips() {
        val puzzle = puzzle(
            size = 4,
            solution = solution4,
            clues = fullClues4,
            id = "SK-EASY-0000000000000001",
        )

        assertRoundTrip(puzzle)
    }

    @Test
    fun bankLineWithGivensRoundTrips() {
        val puzzle = puzzle(
            size = 4,
            solution = solution4,
            clues = fullClues4,
            givens = topTwoRowGivens4,
            id = "SK-EASY-0000000000000002",
        )

        assertRoundTrip(puzzle)
    }

    @Test
    fun bankLineWithSparseCluesRoundTrips() {
        val clues = EdgeClues.empty(4).copy(top = listOf(4, null, 2, null), right = listOf(null, 2, null, 4))
        val puzzle = puzzle(size = 4, solution = solution4, clues = clues, id = "SK-EASY-0000000000000003")

        assertRoundTrip(puzzle)
    }

    @Test
    fun generatedBankLinesRoundTripForEveryPack() {
        for ((index, pack) in Pack.values().withIndex()) {
            val generated = SkyscrapersGenerator.generate(SplitMix64(100L + index), pack)

            assertRoundTrip(generated)
        }
    }

    @Test
    fun savedStateRoundTripsDigitsAndPencilNotes() {
        val puzzle = puzzle(4, solution4, fullClues4, topTwoRowGivens4)
        val state = SkyscrapersState.initial(puzzle)
            .withMove(Move.SetDigit(Cell(12), 4))
            .withMove(Move.SetNotes(Cell(13), 1 shl 2))

        val restored = SkyscrapersStateCodec.decode(SkyscrapersStateCodec.encode(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun gradeCodecWritesTheTechniqueAndStepCount() {
        val grade = Grade(SkyscrapersTechniques.HEIGHT_BOUNDS, 3, Pack.EASY)

        assertEquals("height-bounds:3", SkyscrapersGradeCodec.encode(grade))
        assertEquals("none:0", SkyscrapersGradeCodec.encode(Grade(null, 0, Pack.EASY)))
    }

    /** Encodes [puzzle], decodes it, and checks both the puzzle and its second encoding. */
    private fun assertRoundTrip(puzzle: SkyscrapersPuzzle) {
        val line = SkyscrapersCodec.encode(puzzle)
        val decoded = SkyscrapersCodec.decode(line)

        assertEquals(puzzle, decoded)
        assertEquals(line, SkyscrapersCodec.encode(decoded))
    }
}
