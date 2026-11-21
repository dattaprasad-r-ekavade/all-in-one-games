package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
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
    fun solvesEasyMateInOneHandFixtures() {
        val directMate = easyDirectMateFixture()
        assertUnique(TacticsEngine, directMate)
        assertTechniqueSolves(TacticsEngine, directMate)

        val smotheredMate = easySmotheredMateFixture()
        assertUnique(TacticsEngine, smotheredMate)
        assertTechniqueSolves(TacticsEngine, smotheredMate)

        val backRankMate = easyBackRankMateFixture()
        assertUnique(TacticsEngine, backRankMate)
        assertTechniqueSolves(TacticsEngine, backRankMate)
    }

    @Test
    fun solvesMediumMateInTwoHandFixture() {
        val puzzle = mediumMateInTwoFixture()
        assertUnique(TacticsEngine, puzzle)
        assertTechniqueSolves(TacticsEngine, puzzle)
    }

    @Test
    fun solvesHardMateInTwoHandFixtureWithMorePieces() {
        val puzzle = hardMateInTwoFixture()
        assertTrue(puzzle.initialBoard.pieces.count { it != null } > TacticsPackBands.MAX_MEDIUM_PIECES)
        assertUnique(TacticsEngine, puzzle)
        assertTechniqueSolves(TacticsEngine, puzzle)
    }

    @Test
    fun solvesExpertMateInThreeHandFixture() {
        val puzzle = expertMateInThreeFixture()
        assertEquals(3, puzzle.mateIn)
        assertUnique(TacticsEngine, puzzle)
        assertTechniqueSolves(TacticsEngine, puzzle)
    }

    @Test
    fun everyHintStepHasNonEmptyOneSentenceExplanationNamingIdea() {
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
                val step = requireNotNull(TacticsEngine.nextStep(puzzle, state))
                val explanation = step.explanation

                assertTrue(explanation.isNotBlank(), "Explanation must not be blank")
                assertTrue(explanation.endsWith("."), "Explanation must end with a period: $explanation")
                assertEquals(1, explanation.count { it == '.' }, "Explanation must be exactly one sentence: $explanation")
                assertFalse(explanation.contains("\n") || explanation.contains("\r"), "Explanation must be single-line")

                val matchesKnownIdea = explanation.contains("check", ignoreCase = true) ||
                    explanation.contains("capture", ignoreCase = true) ||
                    explanation.contains("back-rank mate", ignoreCase = true) ||
                    explanation.contains("smothered mate", ignoreCase = true) ||
                    explanation.contains("forces mate in", ignoreCase = true)

                assertTrue(matchesKnownIdea, "Explanation must name the idea: $explanation")
                state = step.moves.fold(state, TacticsEngine::applyMove)
            }
        }
    }

    @Test
    fun nextStepHandlesPendingFromStateTransitions() {
        val puzzle = mediumMateInTwoFixture()
        val s0 = TacticsEngine.initialState(puzzle)

        // Clean initial state produces both from and to picks
        val hint0 = requireNotNull(TacticsEngine.nextStep(puzzle, s0))
        assertEquals(2, hint0.moves.size)
        assertEquals(squareToCell("d3"), hint0.moves[0].cell)
        assertEquals(squareToCell("d8"), hint0.moves[1].cell)

        // When correct from-square is already selected, hint produces only the destination pick
        val s1 = TacticsEngine.applyMove(s0, Move.SetMark(squareToCell("d3"), Mark.FILLED))
        val hint1 = requireNotNull(TacticsEngine.nextStep(puzzle, s1))
        assertEquals(1, hint1.moves.size)
        assertEquals(squareToCell("d8"), hint1.moves[0].cell)

        // When wrong friendly from-square is selected, hint deselects and plays correct move
        val sWrong = TacticsEngine.applyMove(s0, Move.SetMark(squareToCell("c5"), Mark.FILLED))
        val hintWrong = requireNotNull(TacticsEngine.nextStep(puzzle, sWrong))
        assertEquals(3, hintWrong.moves.size)
        assertEquals(squareToCell("c5"), hintWrong.moves[0].cell)
        assertEquals(squareToCell("d3"), hintWrong.moves[1].cell)
        assertEquals(squareToCell("d8"), hintWrong.moves[2].cell)

        // Hint returns null for solved or mistaken board
        val solvedState = solveWithHints(TacticsEngine, puzzle)
        assertNull(TacticsEngine.nextStep(puzzle, solvedState))

        val mistakeState = s0
            .withMove(Move.SetMark(squareToCell("d3"), Mark.FILLED))
            .withMove(Move.SetMark(squareToCell("d7"), Mark.FILLED))
        assertNull(TacticsEngine.nextStep(puzzle, mistakeState))
    }

    @Test
    fun gradeAssignsCorrectPackBandAndTechniques() {
        val easy = easyDirectMateFixture()
        val easyGrade = TacticsEngine.grade(easy)
        assertEquals(Pack.EASY, easyGrade.pack)
        assertEquals(1, easyGrade.stepCount)
        assertEquals(TacticsTechniques.CHECKMATE, easyGrade.hardestTechnique)

        val smothered = easySmotheredMateFixture()
        val smotheredGrade = TacticsEngine.grade(smothered)
        assertEquals(Pack.EASY, smotheredGrade.pack)
        assertEquals(1, smotheredGrade.stepCount)
        assertEquals(TacticsTechniques.SMOTHERED_MATE, smotheredGrade.hardestTechnique)

        val medium = mediumMateInTwoFixture()
        val mediumGrade = TacticsEngine.grade(medium)
        assertEquals(Pack.MEDIUM, mediumGrade.pack)
        assertEquals(2, mediumGrade.stepCount)
        assertEquals(TacticsTechniques.BACK_RANK_MATE, mediumGrade.hardestTechnique)

        val hard = hardMateInTwoFixture()
        val hardGrade = TacticsEngine.grade(hard)
        assertEquals(Pack.HARD, hardGrade.pack)
        assertEquals(2, hardGrade.stepCount)
        assertEquals(TacticsTechniques.BACK_RANK_MATE, hardGrade.hardestTechnique)

        val expert = expertMateInThreeFixture()
        val expertGrade = TacticsEngine.grade(expert)
        assertEquals(Pack.EXPERT, expertGrade.pack)
        assertEquals(3, expertGrade.stepCount)
    }

    @Test
    fun gradeRejectsPuzzlesFailingUniquenessPackOrSolvability() {
        val doubleSolutionPuzzle = TacticsRulesTest.mate1DoublePuzzle()
        val doubleError = assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(doubleSolutionPuzzle)
        }
        assertTrue(doubleError.message!!.contains("unique") || doubleError.message!!.contains("exactly one"))

        val zeroMatePuzzle = TacticsRulesTest.zeroMatePuzzle()
        assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(zeroMatePuzzle)
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

        // Stuck puzzle: stored solution does not force checkmate
        val stuckPuzzle = TacticsPuzzle(
            id = "stuck-puzzle",
            initialBoard = TacticsBoard.fromFen("7k/3Q4/5K2/8/8/8/8/8 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("d7"), squareToCell("d8"))),
            pack = Pack.EASY,
            storedGrade = "none:1",
        )
        assertThrows(IllegalArgumentException::class.java) {
            TacticsEngine.grade(stuckPuzzle)
        }
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
            initialBoard = TacticsBoard.fromFen("3r2k1/4bppp/8/2B5/8/3Q4/8/3R2K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("d3"), squareToCell("d8")),
                ChessMove(squareToCell("e7"), squareToCell("d8")),
                ChessMove(squareToCell("d1"), squareToCell("d8")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "back-rank-mate:2",
        )

        fun hardMateInTwoFixture(): TacticsPuzzle = TacticsPuzzle(
            id = "hard-mate-in-two",
            initialBoard = TacticsBoard.fromFen("3r2k1/pp2bppp/8/2B5/8/3Q4/PP6/3R2K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("d3"), squareToCell("d8")),
                ChessMove(squareToCell("e7"), squareToCell("d8")),
                ChessMove(squareToCell("d1"), squareToCell("d8")),
            ),
            pack = Pack.HARD,
            storedGrade = "back-rank-mate:2",
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
            storedGrade = "forces-mate:3",
        )
    }
}
