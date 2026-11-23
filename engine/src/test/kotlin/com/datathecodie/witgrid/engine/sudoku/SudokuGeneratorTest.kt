package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SudokuGeneratorTest {
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
                assertTrue(SudokuRules.isValidSolution(puzzle))
                assertEquals(1, SudokuRules.countSolutions(puzzle, limit = 2))

                val grade = SudokuTechniqueSolver.grade(puzzle)
                assertTrue(SudokuPackBands.isGradeInPackBand(pack, grade))
                assertEquals(SudokuGradeCodec.encode(grade), puzzle.storedGrade)

                val (solvedState, steps) = collectHintSolve(puzzle)
                assertTrue(SudokuRules.isSolved(puzzle, solvedState))
                val hardestStep = steps.maxBy { it.technique.difficultyRank }
                assertEquals(
                    hardestStep.technique,
                    grade.hardestTechnique,
                    "Stored grade technique must match the hardest step used in solve",
                )
                assertTechniqueInPackBand(pack, grade.hardestTechnique)
            }
        }
    }

    @Test
    fun generatedIdsAreUnsignedSixteenDigitHex() {
        val idPattern = Regex("^SU-[A-Z]+-[0-9a-f]{16}$")

        generateTenPerPack().values.flatten().forEach { puzzle ->
            assertTrue(idPattern.matches(puzzle.id), "ID ${puzzle.id} does not match $idPattern")
        }
    }

    @Test
    fun engineDelegatesGenerationUsingTheSuppliedRandomStream() {
        val enginePuzzle = SudokuEngine.generate(SplitMix64(54), Pack.HARD)
        val directPuzzle = SudokuGenerator.generate(SplitMix64(54), Pack.HARD)

        assertEquals(directPuzzle, enginePuzzle)
    }

    @Test
    fun generatesOneHundredFiftyDistinctPuzzlesPerPackFromOneSeed() {
        Pack.values().forEach { pack ->
            val rng = SplitMix64(42)
            val startTime = System.currentTimeMillis()
            val puzzles = List(150) { SudokuGenerator.generate(rng, pack) }
            val durationMs = System.currentTimeMillis() - startTime
            println("Generation timing: $pack 150 puzzles generated in ${durationMs}ms (average ${durationMs / 150}ms/puzzle)")
            assertEquals(150, puzzles.size)
            assertEquals(150, puzzles.map { it.givens }.toSet().size, "$pack clue layouts must all be distinct")
            assertEquals(150, puzzles.map { it.id }.toSet().size, "$pack puzzle IDs must all be distinct")
            puzzles.forEach { puzzle ->
                assertEquals(pack, puzzle.pack)
                assertTrue(SudokuRules.isValidSolution(puzzle))
                assertEquals(1, SudokuRules.countSolutions(puzzle, limit = 2))
                val grade = SudokuTechniqueSolver.grade(puzzle)
                assertTrue(SudokuPackBands.isGradeInPackBand(pack, grade))
                assertEquals(SudokuGradeCodec.encode(grade), puzzle.storedGrade)
            }
        }
    }

    @Test
    fun easyPuzzlesHaveAtLeastOneHundredDistinctCluePositionPatternsAcrossOneHundredFiftyLevels() {
        val rng = SplitMix64(101)
        val puzzles = List(150) { SudokuGenerator.generate(rng, Pack.EASY) }
        val distinctPositionPatterns = puzzles.map { puzzle ->
            puzzle.givens.indices.filter { puzzle.givens[it] != null }.toSet()
        }.toSet().size
        assertTrue(
            distinctPositionPatterns >= 100,
            "150 Easy puzzles must have at least 100 distinct clue position patterns, got $distinctPositionPatterns",
        )
    }

    private fun generateTenPerPack(): Map<Pack, List<SudokuPuzzle>> =
        Pack.values().associateWith { pack ->
            val rng = SplitMix64(1)
            List(10) { SudokuGenerator.generate(rng, pack) }
        }

    private fun collectHintSolve(puzzle: SudokuPuzzle): Pair<SudokuState, List<HintStep>> {
        var state = SudokuState.initial(puzzle)
        val steps = mutableListOf<HintStep>()
        while (!SudokuRules.isSolved(puzzle, state)) {
            val step = SudokuTechniqueSolver.nextStep(puzzle, state)
                ?: throw AssertionError("${puzzle.pack} puzzle is stuck")
            steps += step
            state = step.moves.fold(state, SudokuState::withMove)
            assertTrue(steps.size <= SUDOKU_CELL_COUNT)
        }
        return state to steps
    }

    private fun assertTechniqueInPackBand(pack: Pack, technique: com.datathecodie.witgrid.engine.core.Technique?) {
        when (pack) {
            Pack.EASY -> assertEquals(SudokuTechniques.NAKED_SINGLE, technique)
            Pack.MEDIUM -> assertEquals(SudokuTechniques.HIDDEN_SINGLE, technique)
            Pack.HARD -> assertTrue(
                technique == SudokuTechniques.LOCKED_CANDIDATES || technique == SudokuTechniques.NAKED_PAIR,
                "Expected Hard technique, got ${technique?.displayName}",
            )
            Pack.EXPERT -> assertTrue(
                technique == SudokuTechniques.HIDDEN_PAIR ||
                    technique == SudokuTechniques.NAKED_TRIPLE ||
                    technique == SudokuTechniques.X_WING,
                "Expected Expert technique, got ${technique?.displayName}",
            )
        }
    }
}
