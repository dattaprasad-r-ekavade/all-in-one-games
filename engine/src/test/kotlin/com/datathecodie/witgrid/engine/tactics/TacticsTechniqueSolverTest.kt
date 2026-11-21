package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsTechniqueSolverTest {

    @Test
    fun solvesEasyDirectMateWithHints() {
        val puzzle = easyDirectMateFixture()
        val solvedState = solveWithHints(TacticsEngine, puzzle)
        assertTrue(TacticsEngine.isSolved(puzzle, solvedState))

        val grade = TacticsEngine.grade(puzzle)
        assertEquals(Pack.EASY, grade.pack)
        assertEquals(1, grade.stepCount)
        assertEquals(TacticsTechniques.CHECKMATE, grade.hardestTechnique)
    }

    @Test
    fun solvesEasySmotheredMateWithHints() {
        val puzzle = easySmotheredMateFixture()
        val solvedState = solveWithHints(TacticsEngine, puzzle)
        assertTrue(TacticsEngine.isSolved(puzzle, solvedState))

        val grade = TacticsEngine.grade(puzzle)
        assertEquals(Pack.EASY, grade.pack)
        assertEquals(1, grade.stepCount)
        assertEquals(TacticsTechniques.SMOTHERED_MATE, grade.hardestTechnique)
    }

    @Test
    fun solvesEasyBackRankMateWithHints() {
        val puzzle = easyBackRankMateFixture()
        val solvedState = solveWithHints(TacticsEngine, puzzle)
        assertTrue(TacticsEngine.isSolved(puzzle, solvedState))

        val grade = TacticsEngine.grade(puzzle)
        assertEquals(Pack.EASY, grade.pack)
        assertEquals(1, grade.stepCount)
        assertEquals(TacticsTechniques.BACK_RANK_MATE, grade.hardestTechnique)
    }

    @Test
    fun namesArabianAndAnastasiaMates() {
        val examples = listOf(
            Triple("7k/R7/5N2/8/8/8/8/K7", "a7h7", TacticsTechniques.ARABIAN_MATE),
            Triple("8/4N1pk/8/8/8/8/5K2/R7", "a1h1", TacticsTechniques.ANASTASIA_MATE),
        )
        for ((fen, uci, technique) in examples) {
            val puzzle = TacticsPuzzle(
                technique.id, TacticsBoard.fromFen("$fen w - -"), PieceColor.WHITE, 1,
                listOf(ChessMove.fromUci(uci)), Pack.EASY, "${technique.id}:1",
            )
            val step = requireNotNull(TacticsEngine.nextStep(puzzle, TacticsEngine.initialState(puzzle)))
            assertEquals(technique, step.technique)
            assertTrue(step.explanation.contains(technique.displayName))
            assertTechniqueSolves(TacticsEngine, puzzle)
        }
    }

    @Test
    fun solvesMediumMateInTwoWithHints() {
        val puzzle = mediumMateInTwoFixture()
        assertTechniqueSolves(TacticsEngine, puzzle)

        val grade = TacticsEngine.grade(puzzle)
        assertEquals(Pack.MEDIUM, grade.pack)
        assertEquals(2, grade.stepCount)
        assertEquals(TacticsTechniques.BACK_RANK_MATE, grade.hardestTechnique)
    }

    @Test
    fun solvesHardMateInTwoWithHints() {
        val puzzle = hardMateInTwoFixture()
        assertTechniqueSolves(TacticsEngine, puzzle)

        val grade = TacticsEngine.grade(puzzle)
        assertEquals(Pack.HARD, grade.pack)
        assertEquals(2, grade.stepCount)
    }

    @Test
    fun solvesExpertMateInThreeWithHints() {
        val puzzle = expertMateInThreeFixture()
        assertTechniqueSolves(TacticsEngine, puzzle)

        val grade = TacticsEngine.grade(puzzle)
        assertEquals(Pack.EXPERT, grade.pack)
        assertEquals(3, grade.stepCount)
    }

    @Test
    fun nextStepDerivesMoveDirectlyFromPosition() {
        val start = mediumMateInTwoFixture().initialBoard
        val puzzle = TacticsPuzzle(
            "wrong-stored-line", start, PieceColor.WHITE, 2,
            listOf("g5h4", "f8e8", "a3f8").map(ChessMove.Companion::fromUci),
            Pack.MEDIUM, "checkmate:2",
        )
        val hint = requireNotNull(TacticsEngine.nextStep(puzzle, TacticsEngine.initialState(puzzle)))
        assertEquals(listOf(squareToCell("a3"), squareToCell("f8")), hint.cells)
        assertTrue(hint.explanation.contains("g8 to f8"))
        assertTrue(hint.explanation.contains("back-rank mate on d8"))
    }

    @Test
    fun nextStepHandlesPendingFromStateTransitions() {
        val puzzle = mediumMateInTwoFixture()
        val state = TacticsEngine.initialState(puzzle)
        val from = squareToCell("a3")
        val to = squareToCell("f8")
        val other = squareToCell("g5")

        fun hintCells(state: TacticsState) =
            requireNotNull(TacticsEngine.nextStep(puzzle, state)).moves.map { it.cell }

        assertEquals(listOf(from, to), hintCells(state))
        assertEquals(listOf(to), hintCells(state.withMove(Move.SetMark(from, Mark.FILLED))))
        assertEquals(listOf(other, from, to), hintCells(state.withMove(Move.SetMark(other, Mark.FILLED))))
        assertNull(TacticsEngine.nextStep(puzzle, solveWithHints(TacticsEngine, puzzle)))
        val mistake = state.withMove(Move.SetMark(other, Mark.FILLED))
            .withMove(Move.SetMark(squareToCell("h4"), Mark.FILLED))
        assertNull(TacticsEngine.nextStep(puzzle, mistake))
    }

    @Test
    fun hintExplanationsAreNonEmptySingleSentences() {
        val fixtures = listOf(
            easyDirectMateFixture(),
            easySmotheredMateFixture(),
            easyBackRankMateFixture(),
            mediumMateInTwoFixture(),
            hardMateInTwoFixture(),
            expertMateInThreeFixture(),
        )

        for (puzzle in fixtures) {
            var state = TacticsEngine.initialState(puzzle)
            while (!TacticsEngine.isSolved(puzzle, state)) {
                val step = TacticsEngine.nextStep(puzzle, state)
                assertNotNull(step, "Step should not be null for solvable puzzle ${puzzle.id}")
                val explanation = step!!.explanation
                assertTrue(explanation.isNotBlank(), "Explanation must not be blank")
                assertTrue(explanation.endsWith("."), "Explanation must end with a period: $explanation")
                assertEquals(1, explanation.count { it == '.' }, "Explanation must be one sentence")
                assertFalse(explanation.contains("\n") || explanation.contains("\r"), "Explanation must be single-line")
                assertFalse(explanation.contains("forces mate in"), "Explanation must provide tactical reasons, not generic fallback")

                val nextState = step.moves.fold(state) { acc, m -> acc.withMove(m) }
                state = nextState
            }
        }
    }

    @Test
    fun gradeRejectsInvalidOrStuckPuzzles() {
        // Double solution rejected
        val doubleSolutionPuzzle = TacticsRulesTest.mate1DoublePuzzle()
        assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(doubleSolutionPuzzle)
        }

        // Zero solution rejected
        val zeroMatePuzzle = TacticsRulesTest.zeroMatePuzzle()
        assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(zeroMatePuzzle)
        }

        // Blocker 3: Declared horizon disagrees with solution line length
        assertThrows(IllegalArgumentException::class.java) {
            TacticsPuzzle(
                id = "declared-horizon-mismatch",
                initialBoard = easyBackRankMateFixture().initialBoard,
                sideToMove = PieceColor.WHITE,
                mateIn = 3,
                solutionLine = listOf(ChessMove(squareToCell("a1"), squareToCell("a8"))),
                pack = Pack.EXPERT,
                storedGrade = "back-rank-mate:1",
            )
        }

        // Pack mismatch: Easy puzzle labeled MEDIUM
        val mismatchedPack = TacticsPuzzle(
            id = "mismatched-pack",
            initialBoard = easyDirectMateFixture().initialBoard,
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = easyDirectMateFixture().solutionLine,
            pack = Pack.MEDIUM,
            storedGrade = "checkmate:1",
        )
        val packError = assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(mismatchedPack)
        }
        assertTrue(packError.message!!.contains("does not match expected pack"))

        // Blocker 4: Valid unique mate-in-2 puzzle whose winning first move is a quiet move (Kf6)
        // rather than a check, capture, or recognized mating pattern -> must be rejected as stuck
        val quietMoveStuckPuzzle = TacticsPuzzle(
            id = "quiet-stuck-puzzle",
            initialBoard = TacticsBoard.fromFen("7k/8/p5Q1/4K3/8/8/8/8 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("e5"), squareToCell("f6")),
                ChessMove(squareToCell("a6"), squareToCell("a5")),
                ChessMove(squareToCell("g6"), squareToCell("g7")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "checkmate:2",
        )
        // The exact mate is unique, but there is no supported technique for the quiet king move.
        assertEquals(1, TacticsRules.countSolutions(quietMoveStuckPuzzle))
        assertTrue(TacticsRules.isValidSolution(quietMoveStuckPuzzle))
        assertNull(TacticsTechniqueSolver.nextStep(quietMoveStuckPuzzle, TacticsState.initial(quietMoveStuckPuzzle)))

        val stuckError = assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(quietMoveStuckPuzzle)
        }
        assertTrue(stuckError.message!!.contains("stuck with the supported techniques"))
    }

    @Test
    fun gradeCodecRoundTripsAndValidates() {
        val grade = Grade(TacticsTechniques.BACK_RANK_MATE, 2, Pack.MEDIUM)
        val encoded = TacticsGradeCodec.encode(grade)
        assertEquals("back-rank-mate:2", encoded)

        TacticsGradeCodec.validate(encoded)
        TacticsGradeCodec.validate("none:0")
        TacticsGradeCodec.validate("smothered-mate:1")

        assertThrows(IllegalArgumentException::class.java) {
            TacticsGradeCodec.validate("unknown-technique:1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsGradeCodec.validate("back-rank-mate:-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsGradeCodec.validate("none:1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsGradeCodec.validate("invalid-format")
        }
    }

    companion object {
        fun easyDirectMateFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "easy-direct-mate",
            initialBoard = TacticsBoard.fromFen("7k/3Q4/5K2/8/8/8/8/8 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("d7"), squareToCell("g7"))),
            pack = Pack.EASY,
            storedGrade = "checkmate:1",
        )

        fun easySmotheredMateFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "easy-smothered-mate",
            initialBoard = TacticsBoard.fromFen("6rk/6pp/8/4N3/2Q5/8/8/6K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("e5"), squareToCell("f7"))),
            pack = Pack.EASY,
            storedGrade = "smothered-mate:1",
        )

        fun easyBackRankMateFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "easy-back-rank-mate",
            initialBoard = TacticsBoard.fromFen("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("a1"), squareToCell("a8"))),
            pack = Pack.EASY,
            storedGrade = "back-rank-mate:1",
        )

        fun mediumMateInTwoFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "medium-mate-in-two",
            initialBoard = TacticsBoard.fromFen("5rk1/p4ppp/8/6B1/8/Q7/8/3R2K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("a3"), squareToCell("f8")),
                ChessMove(squareToCell("g8"), squareToCell("f8")),
                ChessMove(squareToCell("d1"), squareToCell("d8")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "back-rank-mate:2",
        )

        fun hardMateInTwoFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "hard-mate-in-two",
            initialBoard = TacticsBoard.fromFen("5r1k/6pp/3N4/3Q1b2/8/1B6/PP5R/K7 w - -"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf("d5g8", "f8g8", "d6f7").map(ChessMove.Companion::fromUci),
            pack = Pack.HARD,
            storedGrade = "smothered-mate:2",
        )

        fun expertMateInThreeFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "expert-mate-in-three",
            initialBoard = TacticsBoard.fromFen("5rk1/5p1p/5Bp1/8/8/6K1/8/R6R w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 3,
            solutionLine = listOf(
                ChessMove(squareToCell("h1"), squareToCell("h7")),
                ChessMove(squareToCell("g8"), squareToCell("h7")),
                ChessMove(squareToCell("a1"), squareToCell("h1")),
                ChessMove(squareToCell("h7"), squareToCell("g8")),
                ChessMove(squareToCell("h1"), squareToCell("h8")),
            ),
            pack = Pack.EXPERT,
            storedGrade = "checkmate:3",
        )
    }
}
