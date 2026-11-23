package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic Sudoku generator with uniqueness and technique-grade rejection. */
object SudokuGenerator {
    private const val MAX_GENERATION_ATTEMPTS = 500

    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): SudokuPuzzle {
        val targetRank = targetRank(pack)
        repeat(MAX_GENERATION_ATTEMPTS) {
            val solution = randomValidGrid(rng)
            val givens = solution.map { it as Int? }.toMutableList()
            val removalOrder = (0 until SUDOKU_CELL_COUNT).toMutableList()
            shuffle(removalOrder, rng)

            var lastGrade: Grade? = null
            for (index in removalOrder) {
                val given = requireNotNull(givens[index])
                givens[index] = null
                val candidate = candidatePuzzle(givens, solution, pack)
                if (SudokuRules.countSolutions(candidate, limit = 2) != 1) {
                    givens[index] = given
                    continue
                }
                val grade = try {
                    SudokuTechniqueSolver.gradeByTechniques(
                        candidate,
                        maxRank = targetRank,
                        checkUnique = false,
                    )
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
            if (!SudokuPackBands.isGradeInPackBand(pack, grade)) {
                return@repeat
            }

            val puzzle = candidatePuzzle(
                givens = givens,
                solution = solution,
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

    private fun targetRank(pack: Pack): Int = when (pack) {
        Pack.EASY -> SudokuTechniques.NAKED_SINGLE.difficultyRank
        Pack.MEDIUM -> SudokuTechniques.HIDDEN_SINGLE.difficultyRank
        Pack.HARD -> SudokuTechniques.NAKED_PAIR.difficultyRank
        Pack.EXPERT -> SudokuTechniques.X_WING.difficultyRank
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

    private fun randomValidGrid(rng: SplitMix64): List<Int> {
        val grid = IntArray(SUDOKU_CELL_COUNT)
        val rowUsed = IntArray(SUDOKU_SIZE)
        val columnUsed = IntArray(SUDOKU_SIZE)
        val boxUsed = IntArray(SUDOKU_SIZE)

        fun solve(): Boolean {
            var bestCell = -1
            var bestMask = 0
            var minCandidates = SUDOKU_SIZE + 1

            for (cell in 0 until SUDOKU_CELL_COUNT) {
                if (grid[cell] != 0) continue
                val row = cell / SUDOKU_SIZE
                val column = cell % SUDOKU_SIZE
                val box = (row / 3) * 3 + (column / 3)
                val used = rowUsed[row] or columnUsed[column] or boxUsed[box]
                val mask = SUDOKU_NOTE_MASK and used.inv()
                val count = Integer.bitCount(mask)
                if (count == 0) return false
                if (count < minCandidates) {
                    minCandidates = count
                    bestCell = cell
                    bestMask = mask
                    if (count == 1) break
                }
            }

            if (bestCell == -1) return true

            val row = bestCell / SUDOKU_SIZE
            val column = bestCell % SUDOKU_SIZE
            val box = (row / 3) * 3 + (column / 3)

            val digits = ArrayList<Int>(minCandidates)
            var m = bestMask
            while (m != 0) {
                val bit = Integer.lowestOneBit(m)
                digits.add(Integer.numberOfTrailingZeros(bit) + 1)
                m = m and bit.inv()
            }
            shuffle(digits, rng)

            for (d in digits) {
                val bit = 1 shl (d - 1)
                grid[bestCell] = d
                rowUsed[row] = rowUsed[row] or bit
                columnUsed[column] = columnUsed[column] or bit
                boxUsed[box] = boxUsed[box] or bit

                if (solve()) return true

                grid[bestCell] = 0
                rowUsed[row] = rowUsed[row] and bit.inv()
                columnUsed[column] = columnUsed[column] and bit.inv()
                boxUsed[box] = boxUsed[box] and bit.inv()
            }
            return false
        }

        check(solve()) { "Failed to generate random Sudoku solution" }

        val rowBands = (0 until 3).toMutableList().also { shuffle(it, rng) }
        val rowOrder = rowBands.flatMap { band ->
            (0 until 3).map { rowOffset -> band * 3 + rowOffset }.toMutableList().also { shuffle(it, rng) }
        }
        val columnStacks = (0 until 3).toMutableList().also { shuffle(it, rng) }
        val columnOrder = columnStacks.flatMap { stack ->
            (0 until 3).map { colOffset -> stack * 3 + colOffset }.toMutableList().also { shuffle(it, rng) }
        }
        val digitMapping = (1..SUDOKU_SIZE).toMutableList().also { shuffle(it, rng) }
        val transpose = rng.nextInt(2) == 1

        val finalSolution = ArrayList<Int>(SUDOKU_CELL_COUNT)
        for (r in 0 until SUDOKU_SIZE) {
            for (c in 0 until SUDOKU_SIZE) {
                val rowCoord = if (transpose) c else r
                val colCoord = if (transpose) r else c
                val mappedRow = rowOrder[rowCoord]
                val mappedCol = columnOrder[colCoord]
                val digit = grid[mappedRow * SUDOKU_SIZE + mappedCol]
                finalSolution += digitMapping[digit - 1]
            }
        }
        return finalSolution
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
