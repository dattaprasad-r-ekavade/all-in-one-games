package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiniSudokuGeneratorTest {
    @Test
    fun seedOneCreatesTenUniqueSolvedAndCorrectlyGradedPuzzlesPerPackDeterministically() {
        val firstRun = generateTenPerPack()
        val secondRun = generateTenPerPack()

        Pack.values().forEach { pack ->
            val firstPuzzles = requireNotNull(firstRun[pack])
            assertEquals(firstPuzzles, secondRun[pack], "$pack generation must be deterministic")
            assertEquals(10, firstPuzzles.size)
            assertEquals(
                firstPuzzles.size,
                firstPuzzles.map { it.givens }.toSet().size,
                "$pack generation should vary clue layouts",
            )
            assertTrue(
                firstPuzzles.map { puzzle -> puzzle.givens.map { it != null } }.toSet().size > 1,
                "$pack generation should vary which cells are clues",
            )
            assertTrue(
                firstPuzzles.map { puzzle -> puzzle.givens.count { it != null } }.toSet().size > 1,
                "$pack generation should vary clue counts",
            )
            firstPuzzles.forEach { puzzle ->
                assertEquals(pack, puzzle.pack)
                assertTrue(MiniSudokuRules.isValidSolution(puzzle))
                assertEquals(1, MiniSudokuRules.countSolutions(puzzle, limit = 2))

                val grade = MiniSudokuTechniqueSolver.grade(puzzle)
                assertTrue(MiniSudokuPackBands.isGradeInPackBand(pack, grade))
                assertEquals(MiniSudokuGradeCodec.encode(grade), puzzle.storedGrade)
                assertTrue(solveWithHints(puzzle))
            }
        }
    }

    @Test
    fun easyPuzzlesHaveEighteenToTwentyTwoGivensAndNakedSinglesOnly() {
        val puzzles = generateTenPerPack().getValue(Pack.EASY)

        puzzles.forEach { puzzle ->
            val givenCount = puzzle.givens.count { it != null }
            assertTrue(givenCount in 18..22, "Easy puzzle has $givenCount givens")
            assertEquals(1, MiniSudokuRules.countSolutions(puzzle, limit = 2))
            val grade = MiniSudokuTechniqueSolver.grade(puzzle)
            assertEquals(MiniSudokuTechniques.NAKED_SINGLE, grade.hardestTechnique)
            assertTrue(solveWithHints(puzzle))
        }
    }

    @Test
    fun generatedIdsAreUnsignedSixteenDigitHex() {
        val idPattern = Regex("^MS-[A-Z]+-[0-9a-f]{16}$")

        generateTenPerPack().values.flatten().forEach { puzzle ->
            assertTrue(idPattern.matches(puzzle.id), "ID ${puzzle.id} does not match $idPattern")
        }
    }

    @Test
    fun engineDelegatesGenerationUsingTheSuppliedRandomStream() {
        val enginePuzzle = MiniSudokuEngine.generate(SplitMix64(54), Pack.HARD)
        val directPuzzle = MiniSudokuGenerator.generate(SplitMix64(54), Pack.HARD)

        assertEquals(directPuzzle, enginePuzzle)
    }

    private fun generateTenPerPack(): Map<Pack, List<MiniSudokuPuzzle>> =
        Pack.values().associateWith { pack ->
            val rng = SplitMix64(1)
            List(10) { MiniSudokuGenerator.generate(rng, pack) }
        }

    private fun solveWithHints(puzzle: MiniSudokuPuzzle): Boolean {
        var state = MiniSudokuState.initial(puzzle)
        var stepCount = 0
        while (!MiniSudokuRules.isSolved(puzzle, state)) {
            val step = MiniSudokuEngine.nextStep(puzzle, state) ?: return false
            state = step.moves.fold(state, MiniSudokuState::withMove)
            stepCount++
            if (stepCount > MINI_SUDOKU_CELL_COUNT) return false
        }
        return true
    }
}
