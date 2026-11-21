package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FutoshikiTechniqueSolverTest {
    @Test
    fun solvesUniqueFixturesUsingEachPublishedTechniqueAndGradesTheirPack() {
        val fixtures = listOf(
            TechniqueFixture(Pack.EASY, FutoshikiTechniques.SIGN_BOUNDS, easyPuzzle()),
            TechniqueFixture(Pack.MEDIUM, FutoshikiTechniques.SIGN_CHAINS, mediumPuzzle()),
            TechniqueFixture(Pack.HARD, FutoshikiTechniques.SIGN_CHAINS, hardPuzzle()),
            TechniqueFixture(Pack.EXPERT, FutoshikiTechniques.NAKED_PAIR, expertPuzzle()),
        )

        fixtures.forEach { fixture ->
            val puzzle = fixture.puzzle
            assertEquals(1, FutoshikiRules.countSolutions(puzzle), "${fixture.pack} fixture must be unique")

            val (solved, steps) = solveWithHints(puzzle)
            assertTrue(FutoshikiRules.isSolved(puzzle, solved), "${fixture.pack} fixture must solve")
            assertTrue(steps.any { it.technique == fixture.technique }) {
                "${fixture.pack} fixture must use ${fixture.technique.displayName}"
            }
            steps.forEach(::assertOneSentence)
            val techniqueStep = steps.first { it.technique == fixture.technique }
            val placement = techniqueStep.moves.single() as Move.SetDigit
            assertEquals(puzzle.solution[placement.cell.index], placement.digit)
            assertTrue(techniqueStep.cells.contains(placement.cell))
            if (fixture.technique == FutoshikiTechniques.SIGN_BOUNDS ||
                fixture.technique == FutoshikiTechniques.SIGN_CHAINS ||
                fixture.technique == FutoshikiTechniques.NAKED_PAIR
            ) {
                assertTrue(techniqueStep.cells.size >= 2, "Advanced hints must highlight their source cells")
            }

            val expected = Grade(fixture.technique, steps.size, fixture.pack)
            val grade = FutoshikiTechniqueSolver.grade(puzzle)
            assertEquals(expected, grade)
            assertTrue(FutoshikiPackBands.isGradeInPackBand(fixture.pack, grade))
            val storedGrade = FutoshikiGradeCodec.encode(grade)
            FutoshikiGradeCodec.validate(storedGrade)
        }
    }

    @Test
    fun rejectsAUniquePuzzleThatIsStuckWithTheSupportedTechniques() {
        val puzzle = stuckPuzzle()

        assertEquals(1, FutoshikiRules.countSolutions(puzzle))
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiTechniqueSolver.grade(puzzle)
        }
    }

    @Test
    fun gradeRequiresExactlyOneSolutionAndAMatchingTechniqueBand() {
        val multiple = FutoshikiPuzzle(
            id = "FU-MULTIPLE",
            size = 5,
            givens = List(25) { null },
            horizontalSigns = List(20) { FutoshikiSign.NONE },
            verticalSigns = List(20) { FutoshikiSign.NONE },
            solution = easySolution,
            pack = Pack.EASY,
            storedGrade = "none:0",
        )
        val easyInWrongBand = easyPuzzle(pack = Pack.MEDIUM)
        val hardInWrongBand = hardPuzzle(pack = Pack.EXPERT)
        val expertInWrongBand = expertPuzzle(pack = Pack.HARD)

        val mismatchedGivens = easyPuzzle().let { source ->
            val givens = source.givens.toMutableList()
            givens[2] = if (source.solution[2] == 5) 1 else source.solution[2] + 1
            FutoshikiPuzzle(
                source.id,
                source.size,
                givens,
                source.horizontalSigns,
                source.verticalSigns,
                source.solution,
                source.pack,
                source.storedGrade,
            )
        }
        val invalidSolution = easyPuzzle().let { source ->
            val solutionCopy = source.solution.toMutableList()
            solutionCopy[0] = solutionCopy[1]
            FutoshikiPuzzle(
                source.id,
                source.size,
                source.givens,
                source.horizontalSigns,
                source.verticalSigns,
                solutionCopy,
                source.pack,
                source.storedGrade,
            )
        }

        assertEquals(2, FutoshikiRules.countSolutions(multiple, limit = 2))
        assertThrows(IllegalArgumentException::class.java) { FutoshikiTechniqueSolver.grade(multiple) }
        assertThrows(IllegalArgumentException::class.java) { FutoshikiTechniqueSolver.grade(easyInWrongBand) }
        assertThrows(IllegalArgumentException::class.java) { FutoshikiTechniqueSolver.grade(hardInWrongBand) }
        assertThrows(IllegalArgumentException::class.java) { FutoshikiTechniqueSolver.grade(expertInWrongBand) }
        assertThrows(IllegalArgumentException::class.java) { FutoshikiTechniqueSolver.grade(mismatchedGivens) }
        assertThrows(IllegalArgumentException::class.java) { FutoshikiTechniqueSolver.grade(invalidSolution) }

        assertFalse(
            FutoshikiPackBands.isGradeInPackBand(
                Pack.EXPERT,
                Grade(FutoshikiTechniques.SIGN_CHAINS, 10, Pack.EXPERT),
            ),
        )
        assertFalse(
            FutoshikiPackBands.isGradeInPackBand(
                Pack.HARD,
                Grade(FutoshikiTechniques.NAKED_PAIR, 10, Pack.HARD),
            ),
        )
        assertFalse(
            FutoshikiPackBands.isGradeInPackBand(
                Pack.EASY,
                Grade(FutoshikiTechniques.SIGN_CHAINS, 10, Pack.EASY),
            ),
        )
        assertFalse(
            FutoshikiPackBands.isGradeInPackBand(
                Pack.EASY,
                Grade(FutoshikiTechniques.SIGN_BOUNDS, 0, Pack.EASY),
            ),
        )
    }

    @Test
    fun engineDelegatesHintsAndGradeToTechniqueSolver() {
        val puzzle = easyPuzzle()
        val state = FutoshikiEngine.initialState(puzzle)

        assertEquals(FutoshikiTechniqueSolver.nextStep(puzzle, state), FutoshikiEngine.nextStep(puzzle, state))
        assertEquals(FutoshikiTechniqueSolver.grade(puzzle), FutoshikiEngine.grade(puzzle))
    }

    @Test
    fun gradeCodecRejectsUnknownTechniquesAndMalformedCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiGradeCodec.encode(Grade(Technique("outside", "Outside", 1), 1, Pack.MEDIUM))
        }
        listOf("outside:1", "none:1", "naked-single:0", "naked-single:-1", "naked-single:many")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { FutoshikiGradeCodec.validate(value) }
            }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiGradeCodec.validate("naked-single:1:extra")
        }
    }

    @Test
    fun nextStepSolvesNakedSingleAndHiddenSingleDirectly() {
        // Naked single: single missing cell in row
        val nakedGivens = easySolution.toMutableList<Int?>().also { it[0] = null }
        val nakedPuzzle = FutoshikiPuzzle(
            id = "FU-NAKED-TEST",
            size = 5,
            givens = nakedGivens,
            horizontalSigns = List(20) { FutoshikiSign.NONE },
            verticalSigns = List(20) { FutoshikiSign.NONE },
            solution = easySolution,
            pack = Pack.EASY,
            storedGrade = "naked-single:1",
        )
        val nakedState = FutoshikiState.initial(nakedPuzzle)
        val nakedStep = FutoshikiTechniqueSolver.nextStep(nakedPuzzle, nakedState)
        assertTrue(nakedStep != null)
        assertEquals(FutoshikiTechniques.NAKED_SINGLE, nakedStep!!.technique)
        assertOneSentence(nakedStep)

        // Hidden single: digit only fits in one cell in a row
        // In easySolution: row 0 is [1, 5, 2, 3, 4], row 1 is [5, 1, 3, 4, 2].
        // Empty row 0 except 1, empty (1, 1). Digit 5 in row 0 can only go in (0, 1).
        val hiddenGivens = easySolution.toMutableList<Int?>().also {
            it[1] = null // (0, 1) sol 5
            it[2] = null // (0, 2) sol 2
            it[3] = null // (0, 3) sol 3
            it[4] = null // (0, 4) sol 4
        }
        val hiddenPuzzle = FutoshikiPuzzle(
            id = "FU-HIDDEN-TEST",
            size = 5,
            givens = hiddenGivens,
            horizontalSigns = List(20) { FutoshikiSign.NONE },
            verticalSigns = List(20) { FutoshikiSign.NONE },
            solution = easySolution,
            pack = Pack.EASY,
            storedGrade = "hidden-single:1",
        )
        val hiddenState = FutoshikiState.initial(hiddenPuzzle)
        val hiddenStep = FutoshikiTechniqueSolver.nextStep(hiddenPuzzle, hiddenState)
        assertTrue(hiddenStep != null)
        assertOneSentence(hiddenStep!!)
    }

    private fun solveWithHints(puzzle: FutoshikiPuzzle): Pair<FutoshikiState, List<HintStep>> {
        var state = FutoshikiState.initial(puzzle)
        val steps = mutableListOf<HintStep>()
        while (!FutoshikiRules.isSolved(puzzle, state)) {
            val step = FutoshikiTechniqueSolver.nextStep(puzzle, state)
                ?: throw AssertionError("${puzzle.pack} fixture is stuck")
            assertOneSentence(step)
            steps += step
            state = step.moves.fold(state, FutoshikiState::withMove)
            assertTrue(steps.size <= puzzle.grid.cellCount)
        }
        return state to steps
    }

    private fun assertOneSentence(step: HintStep) {
        assertTrue(step.explanation.isNotBlank())
        assertTrue(step.explanation.endsWith('.'))
        assertEquals(1, step.explanation.count { it == '.' })
    }

    private data class TechniqueFixture(
        val pack: Pack,
        val technique: Technique,
        val puzzle: FutoshikiPuzzle,
    )

    private companion object {
        val easySolution = listOf(
            1, 5, 2, 3, 4,
            5, 1, 3, 4, 2,
            2, 3, 4, 1, 5,
            3, 4, 5, 2, 1,
            4, 2, 1, 5, 3,
        )

        fun easyPuzzle(pack: Pack = Pack.EASY): FutoshikiPuzzle {
            val givens = easySolution.toMutableList<Int?>().also {
                it[0] = null
                it[1] = null
                it[5] = null
                it[6] = null
            }
            val hSigns = MutableList(20) { FutoshikiSign.NONE }.also {
                it[0] = FutoshikiSign.LESS // r1c1 < r1c2
            }
            val vSigns = List(20) { FutoshikiSign.NONE }
            return FutoshikiPuzzle(
                id = "FU-EASY-FIXTURE",
                size = 5,
                givens = givens,
                horizontalSigns = hSigns,
                verticalSigns = vSigns,
                solution = easySolution,
                pack = pack,
                storedGrade = "sign-bounds:4",
            )
        }

        val mediumSolution = (0 until 36).map { (it / 6 + it % 6) % 6 + 1 }

        fun mediumPuzzle(pack: Pack = Pack.MEDIUM): FutoshikiPuzzle {
            val givens = mediumSolution.toMutableList<Int?>().also {
                listOf(0, 1, 2, 3, 18, 21).forEach { idx -> it[idx] = null }
            }
            val hSigns = MutableList(30) { FutoshikiSign.NONE }.also {
                it[0] = FutoshikiSign.LESS // r1c1 < r1c2
                it[1] = FutoshikiSign.LESS // r1c2 < r1c3
            }
            val vSigns = List(30) { FutoshikiSign.NONE }
            return FutoshikiPuzzle(
                id = "FU-MEDIUM-FIXTURE",
                size = 6,
                givens = givens,
                horizontalSigns = hSigns,
                verticalSigns = vSigns,
                solution = mediumSolution,
                pack = pack,
                storedGrade = "sign-chains:6",
            )
        }

        val hardSolution = (0 until 49).map { (it / 7 + it % 7) % 7 + 1 }

        fun hardPuzzle(pack: Pack = Pack.HARD): FutoshikiPuzzle {
            val givens = listOf<Int?>(
                null, null, 3, null, null, null, 7,
                null, 3, null, null, null, null, 1,
                null, null, 5, null, 7, 1, null,
                null, 5, null, 7, 1, 2, null,
                null, null, 7, 1, 2, null, null,
                6, null, 1, null, null, null, null,
                null, null, 2, null, null, null, null,
            )
            val hSigns = (0 until 42).map {
                val r = it / 6
                val c = it % 6
                if (hardSolution[r * 7 + c] < hardSolution[r * 7 + c + 1]) FutoshikiSign.LESS else FutoshikiSign.GREATER
            }
            val vSigns = (0 until 42).map {
                val r = it / 7
                val c = it % 7
                if (hardSolution[r * 7 + c] < hardSolution[(r + 1) * 7 + c]) FutoshikiSign.LESS else FutoshikiSign.GREATER
            }
            return FutoshikiPuzzle(
                id = "FU-HARD-FIXTURE",
                size = 7,
                givens = givens,
                horizontalSigns = hSigns,
                verticalSigns = vSigns,
                solution = hardSolution,
                pack = pack,
                storedGrade = "sign-chains:32",
            )
        }

        val expertSolution = listOf(
            1, 2, 3, 4, 5, 6, 7,
            2, 1, 4, 3, 7, 5, 6,
            3, 4, 1, 2, 6, 7, 5,
            4, 5, 6, 7, 1, 2, 3,
            5, 3, 7, 6, 2, 1, 4,
            6, 7, 2, 5, 3, 4, 1,
            7, 6, 5, 1, 4, 3, 2,
        )

        fun expertPuzzle(pack: Pack = Pack.EXPERT): FutoshikiPuzzle {
            val givens = listOf<Int?>(
                null, null, null, null, 5, null, null,
                null, null, null, null, 5, null, null,
                null, 1, 2, 6, null, null, 4,
                null, null, null, null, 2, null, null,
                null, 7, 6, null, null, null, null,
                null, null, null, null, null, 1, 7,
                null, 5, null, null, null, null, null,
            )
            val hSigns = "-<--<-><----<---->-------<------------>---".map {
                FutoshikiSign.fromHorizontalChar(it)
            }
            val vSigns = "--^-------v-^v^-----v---------------v--^--".map {
                FutoshikiSign.fromVerticalChar(it)
            }
            return FutoshikiPuzzle(
                id = "FU-EXPERT-FIXTURE",
                size = 7,
                givens = givens,
                horizontalSigns = hSigns,
                verticalSigns = vSigns,
                solution = expertSolution,
                pack = pack,
                storedGrade = "naked-pair:37",
            )
        }

        val stuckSolution = listOf(
            1, 2, 3, 4, 5,
            2, 3, 4, 5, 1,
            3, 4, 5, 1, 2,
            4, 5, 1, 2, 3,
            5, 1, 2, 3, 4,
        )

        fun stuckPuzzle(): FutoshikiPuzzle {
            val givens = listOf<Int?>(
                null, null, 3, null, null,
                null, null, 4, 5, null,
                null, 4, null, 1, 2,
                4, null, 1, null, null,
                null, null, null, null, 4,
            )
            val hSigns = "--------<----------<".map { FutoshikiSign.fromHorizontalChar(it) }
            val vSigns = "-^---^--------------".map { FutoshikiSign.fromVerticalChar(it) }
            return FutoshikiPuzzle(
                id = "FU-STUCK-FIXTURE",
                size = 5,
                givens = givens,
                horizontalSigns = hSigns,
                verticalSigns = vSigns,
                solution = stuckSolution,
                pack = Pack.EASY,
                storedGrade = "none:0",
            )
        }
    }
}
