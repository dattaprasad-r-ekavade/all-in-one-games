package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic Sudoku generator with uniqueness and technique-grade rejection. */
object SudokuGenerator {
    private const val MAX_GENERATION_ATTEMPTS = 500
    private const val MUTATION_SWAP_ATTEMPTS = 8
    private const val OPTIONAL_CLUE_ATTEMPTS = 8
    private const val OPTIONAL_CLUE_PROBABILITY_PERCENT = 35

    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): SudokuPuzzle {
        repeat(MAX_GENERATION_ATTEMPTS) {
            val generatedGrid = randomValidGrid(rng)
            val (retainedSourceCells, grade) = randomizedCluePattern(generatedGrid, pack, rng)
            val givens = givensForSourceCells(generatedGrid, retainedSourceCells)
            val puzzle = candidatePuzzle(
                givens = givens,
                solution = generatedGrid.solution,
                pack = pack,
                id = "SU-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}",
                storedGrade = SudokuGradeCodec.encode(grade),
            )
            check(SudokuRules.countSolutions(puzzle, limit = 2) == 1) {
                "Technique-solvable Sudoku puzzle must have exactly one solution"
            }
            return puzzle
        }
        throw IllegalStateException(
            "Could not generate a ${pack.name} Sudoku puzzle in $MAX_GENERATION_ATTEMPTS attempts",
        )
    }

    private fun randomizedCluePattern(
        generatedGrid: RandomGrid,
        pack: Pack,
        rng: SplitMix64,
    ): Pair<Set<Int>, Grade> {
        val base = baseClues(pack, rng)
        val retained = base.toMutableSet()
        var templateGrade = requireNotNull(tryGradeForTemplate(generatedGrid, retained, pack)) {
            "Base clue pattern must be valid for ${pack.name}"
        }

        repeat(MUTATION_SWAP_ATTEMPTS) {
            val present = retained.toList()
            val absent = (0 until SUDOKU_CELL_COUNT).filterNot { it in retained }
            if (present.isEmpty() || absent.isEmpty()) return@repeat
            val removed = present[rng.nextInt(present.size)]
            val added = absent[rng.nextInt(absent.size)]
            retained.remove(removed)
            retained.add(added)
            val grade = tryGradeForTemplate(generatedGrid, retained, pack)
            if (grade != null && SudokuPackBands.isGradeInPackBand(pack, grade)) {
                templateGrade = grade
            } else {
                retained.remove(added)
                retained.add(removed)
            }
        }

        val optionalCells = (0 until SUDOKU_CELL_COUNT).filterNot { it in retained }.toMutableList()
        shuffle(optionalCells, rng)
        optionalCells.take(OPTIONAL_CLUE_ATTEMPTS).forEach { cell ->
            if (rng.nextInt(100) >= OPTIONAL_CLUE_PROBABILITY_PERCENT) return@forEach
            retained.add(cell)
            val grade = tryGradeForTemplate(generatedGrid, retained, pack)
            if (grade != null && SudokuPackBands.isGradeInPackBand(pack, grade)) {
                templateGrade = grade
            } else {
                retained.remove(cell)
            }
        }

        check(SudokuPackBands.isGradeInPackBand(pack, templateGrade)) {
            "Sudoku clue pattern must match its target technique band"
        }
        return retained to templateGrade
    }

    private fun tryGradeForTemplate(
        generatedGrid: RandomGrid,
        retainedSourceCells: Set<Int>,
        pack: Pack,
    ): Grade? = try {
        SudokuTechniqueSolver.gradeByTechniques(
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
        id: String = "SU-CANDIDATE-${pack.name}",
        storedGrade: String = "pending",
    ): SudokuPuzzle = SudokuPuzzle(
        id = id,
        givens = givens,
        solution = solution,
        pack = pack,
        storedGrade = storedGrade,
    )

    private fun randomValidGrid(rng: SplitMix64): RandomGrid {
        val rowBands = (0 until 3).toMutableList().also { shuffle(it, rng) }
        val rowOrder = rowBands.flatMap { band ->
            (0 until 3).map { rowOffset -> band * 3 + rowOffset }.toMutableList()
                .also { shuffle(it, rng) }
        }
        val columnStacks = (0 until 3).toMutableList().also { shuffle(it, rng) }
        val columnOrder = columnStacks.flatMap { stack ->
            (0 until 3).map { columnOffset -> stack * 3 + columnOffset }.toMutableList()
                .also { shuffle(it, rng) }
        }
        val digitMapping = (1..SUDOKU_SIZE).toMutableList().also { shuffle(it, rng) }
        val transpose = rng.nextInt(2) == 1
        val solution = ArrayList<Int>(SUDOKU_CELL_COUNT)
        val sourceCellAtOutput = ArrayList<Int>(SUDOKU_CELL_COUNT)
        for (rowCoordinate in 0 until SUDOKU_SIZE) {
            for (columnCoordinate in 0 until SUDOKU_SIZE) {
                val row = if (transpose) columnCoordinate else rowCoordinate
                val column = if (transpose) rowCoordinate else columnCoordinate
                val sourceRow = rowOrder[row]
                val sourceColumn = columnOrder[column]
                val sourceCell = sourceRow * SUDOKU_SIZE + sourceColumn
                val baseDigit = BASE_SOLUTION[sourceCell]
                solution += digitMapping[baseDigit - 1]
                sourceCellAtOutput += sourceCell
            }
        }
        return RandomGrid(solution, sourceCellAtOutput)
    }

    private fun baseClues(pack: Pack, rng: SplitMix64): List<Int> = when (pack) {
        Pack.EASY -> nakedSingleClues
        Pack.MEDIUM -> hiddenSingleClues
        Pack.HARD -> if (rng.nextInt(2) == 0) lockedCandidatesClues else nakedPairClues
        Pack.EXPERT -> when (rng.nextInt(3)) {
            0 -> hiddenPairClues
            1 -> nakedTripleClues
            else -> xWingClues
        }
    }

    private data class RandomGrid(val solution: List<Int>, val sourceCellAtOutput: List<Int>)

    private fun <T> shuffle(values: MutableList<T>, rng: SplitMix64) {
        for (index in values.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val value = values[index]
            values[index] = values[other]
            values[other] = value
        }
    }

    private val BASE_SOLUTION: List<Int> = listOf(
        8, 1, 2, 7, 5, 3, 6, 4, 9,
        9, 4, 3, 6, 8, 2, 1, 7, 5,
        6, 7, 5, 4, 9, 1, 2, 8, 3,
        1, 5, 4, 2, 3, 7, 8, 9, 6,
        3, 6, 9, 8, 4, 5, 7, 2, 1,
        2, 8, 7, 1, 6, 9, 5, 3, 4,
        5, 2, 1, 9, 7, 4, 3, 6, 8,
        4, 3, 8, 5, 2, 6, 9, 1, 7,
        7, 9, 6, 3, 1, 8, 4, 5, 2,
    )

    private val nakedSingleClues: List<Int> = listOf(
        0, 2, 11, 12, 15, 18, 19, 21, 22, 24, 28, 30, 32, 38, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78, 80,
    )
    private val hiddenSingleClues: List<Int> = listOf(
        0, 11, 12, 18, 19, 21, 22, 24, 28, 30, 32, 38, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78, 80,
    )
    private val lockedCandidatesClues: List<Int> = listOf(
        0, 11, 12, 18, 19, 21, 22, 24, 28, 32, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78,
    )
    private val nakedPairClues: List<Int> = listOf(
        0, 11, 12, 15, 19, 22, 24, 28, 29, 31, 32, 40, 41, 42, 48, 52, 56, 60, 61, 62, 65, 66, 70, 71, 73, 78,
    )
    private val hiddenPairClues: List<Int> = listOf(
        0, 8, 11, 12, 19, 22, 24, 28, 32, 40, 41, 42, 47, 48, 52, 56, 61, 62, 63, 65, 66, 68, 70, 73, 78,
    )
    private val nakedTripleClues: List<Int> = listOf(
        0, 11, 12, 19, 22, 24, 28, 32, 34, 37, 40, 41, 42, 46, 48, 52, 56, 61, 62, 64, 65, 66, 70, 72, 73, 78,
    )
    private val xWingClues: List<Int> = listOf(
        0, 1, 11, 12, 13, 19, 22, 24, 28, 32, 40, 41, 42, 44, 48, 52, 56, 61, 62, 64, 65, 66, 70, 73, 78,
    )
}
