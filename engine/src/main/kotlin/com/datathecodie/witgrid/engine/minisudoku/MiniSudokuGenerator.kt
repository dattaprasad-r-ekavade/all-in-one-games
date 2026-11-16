package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic Mini Sudoku generator with uniqueness and technique-grade rejection. */
object MiniSudokuGenerator {
    private const val MAX_GENERATION_ATTEMPTS = 500

    /** Easy keeps 18 to 22 givens; removal stops at a random target in that range. */
    private const val EASY_GIVEN_MIN = 18
    private const val EASY_GIVEN_MAX = 22

    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): MiniSudokuPuzzle {
        val targetRank = targetRank(pack)
        repeat(MAX_GENERATION_ATTEMPTS) {
            val generatedGrid = randomValidGrid(rng)
            val solution = generatedGrid.solution
            val givens = solution.map { it as Int? }.toMutableList()
            val retainedSourceCells = randomizedCluePattern(generatedGrid, pack, rng)
            val easyGivenTarget = if (pack == Pack.EASY) {
                EASY_GIVEN_MIN + rng.nextInt(EASY_GIVEN_MAX - EASY_GIVEN_MIN + 1)
            } else {
                0
            }
            val removalOrder = generatedGrid.sourceCellAtOutput.indices.filter { outputIndex ->
                pack == Pack.EASY || generatedGrid.sourceCellAtOutput[outputIndex] !in retainedSourceCells
            }.toMutableList()
            shuffle(removalOrder, rng)

            var lastGrade: Grade? = null
            for (index in removalOrder) {
                if (pack == Pack.EASY && givens.count { it != null } <= easyGivenTarget) break
                val given = requireNotNull(givens[index])
                givens[index] = null
                val candidate = candidatePuzzle(givens, solution, pack)
                val grade = try {
                    MiniSudokuTechniqueSolver.gradeByTechniques(candidate)
                } catch (_: IllegalArgumentException) {
                    givens[index] = given
                    continue
                }
                if (grade.stepCount == 0 || (grade.hardestTechnique?.difficultyRank ?: -1) > targetRank) {
                    givens[index] = given
                    continue
                }
                lastGrade = grade
            }

            val grade = lastGrade ?: return@repeat
            if (pack == Pack.EASY && givens.count { it != null } > EASY_GIVEN_MAX) {
                return@repeat
            }
            if (!MiniSudokuPackBands.isGradeInPackBand(pack, grade)) {
                return@repeat
            }
            val puzzle = candidatePuzzle(
                givens = givens,
                solution = solution,
                pack = pack,
                id = "MS-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}",
                storedGrade = MiniSudokuGradeCodec.encode(grade),
            )
            check(MiniSudokuRules.countSolutions(puzzle, limit = 2) == 1) {
                "Technique-solvable Mini Sudoku puzzle must have exactly one solution"
            }
            return puzzle
        }
        throw IllegalStateException(
            "Could not generate a ${pack.name} Mini Sudoku puzzle in $MAX_GENERATION_ATTEMPTS attempts",
        )
    }

    private fun randomizedCluePattern(
        generatedGrid: RandomGrid,
        pack: Pack,
        rng: SplitMix64,
    ): Set<Int> {
        val retained = baseClues(pack).toMutableSet()
        var templateGrade = MiniSudokuTechniqueSolver.gradeByTechniques(
            candidatePuzzle(givensForSourceCells(generatedGrid, retained), generatedGrid.solution, pack),
        )
        val targetRank = targetRank(pack)

        repeat(24) {
            val present = retained.toList()
            val absent = (0 until MINI_SUDOKU_CELL_COUNT).filterNot { it in retained }
            if (present.isEmpty() || absent.isEmpty()) return@repeat
            val removed = present[rng.nextInt(present.size)]
            val added = absent[rng.nextInt(absent.size)]
            retained.remove(removed)
            retained.add(added)
            val grade = tryGradeForTemplate(generatedGrid, retained, pack)
            if (grade != null && grade.stepCount >= MiniSudokuPackBands.MINIMUM_HINT_STEP_COUNT &&
                grade.hardestTechnique?.difficultyRank == targetRank
            ) {
                templateGrade = grade
            } else {
                retained.remove(added)
                retained.add(removed)
            }
        }

        val optionalCells = (0 until MINI_SUDOKU_CELL_COUNT).filterNot { it in retained }.toMutableList()
        shuffle(optionalCells, rng)
        optionalCells.forEach { cell ->
            if (rng.nextInt(100) >= 30) return@forEach
            retained.add(cell)
            val grade = tryGradeForTemplate(generatedGrid, retained, pack)
            if (grade != null && grade.stepCount >= MiniSudokuPackBands.MINIMUM_HINT_STEP_COUNT &&
                grade.hardestTechnique?.difficultyRank == targetRank
            ) {
                templateGrade = grade
            } else {
                retained.remove(cell)
            }
        }

        check(MiniSudokuPackBands.isGradeInPackBand(pack, templateGrade)) {
            "Mini Sudoku clue pattern must match its target technique band"
        }
        return retained
    }

    private fun tryGradeForTemplate(
        generatedGrid: RandomGrid,
        retainedSourceCells: Set<Int>,
        pack: Pack,
    ): Grade? = try {
        MiniSudokuTechniqueSolver.gradeByTechniques(
            candidatePuzzle(
                givensForSourceCells(generatedGrid, retainedSourceCells),
                generatedGrid.solution,
                pack,
            ),
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun givensForSourceCells(generatedGrid: RandomGrid, retainedSourceCells: Set<Int>): List<Int?> =
        generatedGrid.sourceCellAtOutput.mapIndexed { outputIndex, sourceIndex ->
            if (sourceIndex in retainedSourceCells) generatedGrid.solution[outputIndex] else null
        }

    private fun candidatePuzzle(
        givens: List<Int?>,
        solution: List<Int>,
        pack: Pack,
        id: String = "MS-CANDIDATE-${pack.name}",
        storedGrade: String = "pending",
    ): MiniSudokuPuzzle = MiniSudokuPuzzle(
        id = id,
        givens = givens,
        solution = solution,
        pack = pack,
        storedGrade = storedGrade,
    )

    private fun randomValidGrid(rng: SplitMix64): RandomGrid {
        val rowBands = (0 until MINI_SUDOKU_SIZE / 2).toMutableList().also { shuffle(it, rng) }
        val rowOrder = rowBands.flatMap { band ->
            (0 until 2).map { rowOffset -> band * 2 + rowOffset }.toMutableList()
                .also { shuffle(it, rng) }
        }
        val columnStacks = (0 until MINI_SUDOKU_SIZE / 3).toMutableList().also { shuffle(it, rng) }
        val columnOrder = columnStacks.flatMap { stack ->
            (0 until 3).map { columnOffset -> stack * 3 + columnOffset }.toMutableList()
                .also { shuffle(it, rng) }
        }
        val digitMapping = (1..MINI_SUDOKU_SIZE).toMutableList().also { shuffle(it, rng) }
        val solution = ArrayList<Int>(MINI_SUDOKU_CELL_COUNT)
        val sourceCellAtOutput = ArrayList<Int>(MINI_SUDOKU_CELL_COUNT)
        for (row in rowOrder.indices) {
            for (column in columnOrder.indices) {
                val sourceRow = rowOrder[row]
                val sourceColumn = columnOrder[column]
                val baseDigit = (sourceRow * 3 + sourceRow / 2 + sourceColumn) % MINI_SUDOKU_SIZE + 1
                solution += digitMapping[baseDigit - 1]
                sourceCellAtOutput += sourceRow * MINI_SUDOKU_SIZE + sourceColumn
            }
        }
        return RandomGrid(solution, sourceCellAtOutput)
    }

    /** Base clue sets anchor each pack to a known technique grade after valid grid transformations. */
    private fun baseClues(pack: Pack): List<Int> = when (pack) {
        Pack.EASY -> listOf(0, 1, 2, 4, 9, 10, 11, 12, 13, 14, 15, 16, 18, 19, 20, 21, 22, 23, 24, 25, 27, 30, 31, 34, 35)
        Pack.MEDIUM -> listOf(0, 1, 11, 12, 16, 17, 21, 26, 30, 34)
        Pack.HARD -> listOf(3, 5, 10, 11, 14, 18, 19, 24, 26, 31, 33, 35)
        Pack.EXPERT -> listOf(1, 2, 6, 16, 17, 21, 24, 26, 32, 34)
    }

    private data class RandomGrid(val solution: List<Int>, val sourceCellAtOutput: List<Int>)

    private fun targetRank(pack: Pack): Int = when (pack) {
        Pack.EASY -> 0
        Pack.MEDIUM -> 1
        Pack.HARD -> 2
        Pack.EXPERT -> 3
    }

    private fun <T> shuffle(values: MutableList<T>, rng: SplitMix64) {
        for (index in values.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val value = values[index]
            values[index] = values[other]
            values[other] = value
        }
    }
}
