package com.datathecodie.witgrid.engine.path.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.path.PathRules
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.engine.path.PathTechniqueSolver
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.corridorPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.deadEndPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.rowsPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.solvedDigits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Path solver cases: guess-free hints, one-sentence explanations, and grading. */
class PathTechniqueSolverSpecTest {

    private val publishedTechniqueIds = setOf(
        "endpoint-extension",
        "forced-neighbour",
        "dead-end-avoidance",
        "last-empty-cell",
    )

    @Test
    fun everyHintUsesPublishedTechniqueAndOneSentenceExplanation() {
        val step = requireNotNull(PathTechniqueSolver.nextStep(corridorPuzzle, PathState.initial(corridorPuzzle)))

        assertTrue(step.technique.id in publishedTechniqueIds, "technique ${step.technique.id}")
        assertTrue(step.explanation.isNotBlank())
        assertTrue(step.explanation.endsWith("."), "explanation ends with a full stop")
        assertEquals(1, step.explanation.count { it == '.' }, "one sentence: ${step.explanation}")
        assertTrue(step.moves.isNotEmpty())
        assertTrue(step.moves.all { it is Move.SetDigit && it.digit == 1 })
    }

    @Test
    fun repeatedHintsSolveCorridorWithoutGuessing() {
        var state = PathState.initial(corridorPuzzle)
        var steps = 0
        while (!PathRules.isSolved(corridorPuzzle, state) && steps < 50) {
            val step = PathTechniqueSolver.nextStep(corridorPuzzle, state)
            assertNotNull(step, "solver stuck after $steps steps")
            state = step!!.moves.fold(state, PathState::withMove)
            assertTrue(PathRules.mistakes(corridorPuzzle, state).isEmpty(), "hint made a mistake")
            steps++
        }
        assertTrue(PathRules.isSolved(corridorPuzzle, state))
    }

    @Test
    fun solvedBoardHasNoHint() {
        val solved = PathState(corridorPuzzle, solvedDigits(corridorPuzzle))
        assertNull(PathTechniqueSolver.nextStep(corridorPuzzle, solved))
    }

    @Test
    fun mistakenBoardHasNoHint() {
        val mistaken = PathState.initial(rowsPuzzle).withMove(Move.SetDigit(Cell(5), 3))
        assertNull(PathTechniqueSolver.nextStep(rowsPuzzle, mistaken))
    }

    @Test
    fun corridorGradesAsEasyWithAtLeastOneStep() {
        val grade = PathTechniqueSolver.grade(corridorPuzzle)
        assertEquals(Pack.EASY, grade.pack)
        assertTrue(grade.stepCount >= 1)
        assertNotNull(grade.hardestTechnique)
        assertTrue(grade.hardestTechnique!!.id in publishedTechniqueIds)
    }

    @Test
    fun gradingRejectsBoardWithoutSolution() {
        assertThrows(IllegalArgumentException::class.java) {
            PathTechniqueSolver.grade(deadEndPuzzle)
        }
    }
}
