package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic Binary generator using randomized valid rows and solver-checked clue removal. */
object BinaryGenerator {
    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): BinaryPuzzle {
        val size = boardSize(pack)
        val solution = randomValidGrid(size, rng)
        val givens = solution.map { it as Int? }.toMutableList()
        val removalOrder = (0 until size * size).toMutableList()
        shuffle(removalOrder, rng)

        var acceptedRemovals = 0
        var lastGrade = gradePuzzle(size, givens, solution, pack)
        for (index in removalOrder) {
            val given = requireNotNull(givens[index])
            givens[index] = null
            val grade = try {
                gradePuzzle(size, givens, solution, pack)
            } catch (_: IllegalArgumentException) {
                givens[index] = given
                continue
            }
            lastGrade = grade
            acceptedRemovals++
        }
        check(acceptedRemovals > 0) { "Binary generator could not remove any givens" }

        val id = "BI-${pack.name}-${rng.nextLong().toString(16)}"
        val finalPuzzle = BinaryPuzzle(
            id = id,
            size = size,
            givens = givens,
            solution = solution,
            pack = pack,
            storedGrade = "pending",
        )
        check(BinaryRules.countSolutions(finalPuzzle, limit = 2) == 1) {
            "Technique-solvable Binary puzzle must have exactly one solution"
        }
        return BinaryPuzzle(
            id = finalPuzzle.id,
            size = finalPuzzle.size,
            givens = finalPuzzle.givens,
            solution = finalPuzzle.solution,
            pack = finalPuzzle.pack,
            storedGrade = BinaryGradeCodec.encode(lastGrade),
        )
    }

    private fun boardSize(pack: Pack): Int = when (pack) {
        Pack.EASY -> 6
        Pack.MEDIUM -> 8
        Pack.HARD -> 10
        Pack.EXPERT -> 12
    }

    private fun randomValidGrid(size: Int, rng: SplitMix64): List<Int> {
        val options = validRowMasks(size)
        val rows = IntArray(size)
        val usedRows = BooleanArray(1 shl size)
        val columnOneCounts = IntArray(size)
        val columnPatterns = IntArray(size)

        fun place(row: Int): Boolean {
            if (row == size) return columnPatterns.toSet().size == size
            val remainingRows = size - row - 1
            val shuffledOptions = options.toMutableList()
            shuffle(shuffledOptions, rng)
            for (mask in shuffledOptions) {
                if (usedRows[mask]) continue
                var valid = true
                for (column in 0 until size) {
                    val value = (mask ushr column) and 1
                    val onesAfter = columnOneCounts[column] + value
                    val zerosAfter = row + 1 - onesAfter
                    if (
                        onesAfter > size / 2 ||
                        zerosAfter > size / 2 ||
                        onesAfter + remainingRows < size / 2 ||
                        zerosAfter + remainingRows < size / 2 ||
                        (row >= 2 && bitAt(columnPatterns[column], row - 1) == value &&
                            bitAt(columnPatterns[column], row - 2) == value)
                    ) {
                        valid = false
                        break
                    }
                }
                if (!valid) continue

                rows[row] = mask
                usedRows[mask] = true
                for (column in 0 until size) {
                    val value = (mask ushr column) and 1
                    columnOneCounts[column] += value
                    columnPatterns[column] = columnPatterns[column] or (value shl row)
                }
                if (place(row + 1)) return true
                usedRows[mask] = false
                for (column in 0 until size) {
                    val value = (mask ushr column) and 1
                    columnOneCounts[column] -= value
                    columnPatterns[column] = columnPatterns[column] and (1 shl row).inv()
                }
            }
            return false
        }

        check(place(0)) { "Could not create a valid Binary layout for size $size" }
        return rows.flatMap { mask -> (0 until size).map { column -> bitAt(mask, column) } }
    }

    private fun validRowMasks(size: Int): List<Int> = (0 until (1 shl size)).filter { mask ->
        Integer.bitCount(mask) == size / 2 && (0 until size - 2).all { column ->
            val first = (mask ushr column) and 1
            val second = (mask ushr (column + 1)) and 1
            val third = (mask ushr (column + 2)) and 1
            first != second || second != third
        }
    }

    private fun bitAt(mask: Int, index: Int): Int = (mask ushr index) and 1

    private fun gradePuzzle(
        size: Int,
        givens: List<Int?>,
        solution: List<Int>,
        pack: Pack,
    ) = BinaryTechniqueSolver.gradeByTechniques(
        BinaryPuzzle(
            id = "BI-PENDING",
            size = size,
            givens = givens,
            solution = solution,
            pack = pack,
            storedGrade = "pending",
        ),
    )

    private fun <T> shuffle(values: MutableList<T>, rng: SplitMix64) {
        for (index in values.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val value = values[index]
            values[index] = values[other]
            values[other] = value
        }
    }
}
