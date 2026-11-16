package com.datathecodie.witgrid.engine.testkit

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.core.Technique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EngineTestKitTest {
    @Test
    fun parsesAsciiGridFromIndentedRawString() {
        val fixture = AsciiGridFixture.parse(
            """
                .#..
                ..#.
                ##..
            """,
        )

        assertEquals(3, fixture.height)
        assertEquals(4, fixture.width)
        assertEquals('#', fixture.cellAt(2, 0))
    }

    @Test
    fun rejectsRaggedAsciiGrid() {
        assertThrows(IllegalArgumentException::class.java) {
            AsciiGridFixture.parse(
                """
                    .#.
                    ..
                """,
            )
        }
    }

    @Test
    fun uniqueAssertionAcceptsOneSolutionFakeEngine() {
        assertUnique(FakeEngine, puzzle = 4)
    }

    @Test
    fun techniqueSolveAssertionAppliesFakeHint() {
        assertTechniqueSolves(FakeEngine, puzzle = 4)
    }

    @Test
    fun deterministicAssertionResetsFakeEngineSeed() {
        assertDeterministic(FakeEngine, seed = 17, pack = Pack.MEDIUM)
    }

    @Test
    fun solveWithHintsReturnsSolvedFakeState() {
        assertEquals(4, solveWithHints(FakeEngine, puzzle = 4))
    }

    private object FakeEngine : GameEngine<Int, Int> {
        override val id: String = "testkit-fake"

        override fun decodePuzzle(line: String): Int = line.toInt()

        override fun encodePuzzle(puzzle: Int): String = puzzle.toString()

        override fun initialState(puzzle: Int): Int = 0

        override fun applyMove(state: Int, move: Move): Int = when (move) {
            is Move.SetDigit -> move.digit
            is Move.SetMark, is Move.SetNotes -> error("Fake engine only supports digit edits")
        }

        override fun isSolved(puzzle: Int, state: Int): Boolean = state == puzzle

        override fun mistakes(puzzle: Int, state: Int): List<Cell> = emptyList()

        override fun countSolutions(puzzle: Int, limit: Int): Int {
            require(limit > 0)
            return 1
        }

        override fun nextStep(puzzle: Int, state: Int): HintStep? = if (state == puzzle) {
            null
        } else {
            HintStep(
                technique = Technique("single", "Single step", 0),
                cells = listOf(Cell(0)),
                explanation = "The target digit completes the fixture.",
                moves = listOf(Move.SetDigit(Cell(0), puzzle)),
            )
        }

        override fun grade(puzzle: Int): Grade = Grade(
            hardestTechnique = Technique("single", "Single step", 0),
            stepCount = 1,
            pack = Pack.EASY,
        )

        override fun generate(rng: SplitMix64, pack: Pack): Int = rng.nextInt(9) + 1

        override fun encodeState(state: Int): ByteArray = byteArrayOf(state.toByte())

        override fun decodeState(bytes: ByteArray): Int {
            require(bytes.size == 1)
            return bytes.single().toInt()
        }
    }
}
