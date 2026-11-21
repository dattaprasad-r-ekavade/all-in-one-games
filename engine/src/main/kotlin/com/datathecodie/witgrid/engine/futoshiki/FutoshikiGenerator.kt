package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic Futoshiki generator with Latin square creation and solver-checked sign/given removal. */
object FutoshikiGenerator {
    private const val MAX_GENERATION_ATTEMPTS = 200

    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): FutoshikiPuzzle {
        val size = FutoshikiPackBands.sizeForPack(pack)
        val targetRank = targetRank(pack)

        repeat(MAX_GENERATION_ATTEMPTS) {
            val solution = randomLatinSquare(size, rng)
            val (horizontalSigns, verticalSigns) = allSigns(size, solution)
            val givens = solution.map { it as Int? }.toMutableList()
            val mutableHorizontalSigns = horizontalSigns.toMutableList()
            val mutableVerticalSigns = verticalSigns.toMutableList()

            var lastGrade: Grade? = null
            // Two passes allow later removals to unblock items that were required in earlier steps
            repeat(2) {
                val items = mutableListOf<RemovableItem>()
                for (index in givens.indices) {
                    if (givens[index] != null) items.add(RemovableItem.Given(index))
                }
                for (index in mutableHorizontalSigns.indices) {
                    if (mutableHorizontalSigns[index] != FutoshikiSign.NONE) {
                        items.add(RemovableItem.HorizontalSign(index))
                    }
                }
                for (index in mutableVerticalSigns.indices) {
                    if (mutableVerticalSigns[index] != FutoshikiSign.NONE) {
                        items.add(RemovableItem.VerticalSign(index))
                    }
                }
                shuffle(items, rng)

                for (item in items) {
                    when (item) {
                        is RemovableItem.Given -> {
                            val previous = givens[item.index]
                            givens[item.index] = null
                            val grade = tryGrade(
                                size = size,
                                givens = givens,
                                horizontalSigns = mutableHorizontalSigns,
                                verticalSigns = mutableVerticalSigns,
                                solution = solution,
                                pack = pack,
                            )
                            if (grade == null || (grade.hardestTechnique?.difficultyRank ?: -1) > targetRank) {
                                givens[item.index] = previous
                            } else {
                                lastGrade = grade
                            }
                        }
                        is RemovableItem.HorizontalSign -> {
                            val previous = mutableHorizontalSigns[item.index]
                            mutableHorizontalSigns[item.index] = FutoshikiSign.NONE
                            val grade = tryGrade(
                                size = size,
                                givens = givens,
                                horizontalSigns = mutableHorizontalSigns,
                                verticalSigns = mutableVerticalSigns,
                                solution = solution,
                                pack = pack,
                            )
                            if (grade == null || (grade.hardestTechnique?.difficultyRank ?: -1) > targetRank) {
                                mutableHorizontalSigns[item.index] = previous
                            } else {
                                lastGrade = grade
                            }
                        }
                        is RemovableItem.VerticalSign -> {
                            val previous = mutableVerticalSigns[item.index]
                            mutableVerticalSigns[item.index] = FutoshikiSign.NONE
                            val grade = tryGrade(
                                size = size,
                                givens = givens,
                                horizontalSigns = mutableHorizontalSigns,
                                verticalSigns = mutableVerticalSigns,
                                solution = solution,
                                pack = pack,
                            )
                            if (grade == null || (grade.hardestTechnique?.difficultyRank ?: -1) > targetRank) {
                                mutableVerticalSigns[item.index] = previous
                            } else {
                                lastGrade = grade
                            }
                        }
                    }
                }
            }

            val grade = lastGrade ?: return@repeat
            if (!FutoshikiPackBands.isGradeInPackBand(pack, grade)) {
                return@repeat
            }

            val id = "FU-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}"
            val puzzle = candidatePuzzle(
                size = size,
                givens = givens,
                horizontalSigns = mutableHorizontalSigns,
                verticalSigns = mutableVerticalSigns,
                solution = solution,
                pack = pack,
                id = id,
                storedGrade = FutoshikiGradeCodec.encode(grade),
            )
            check(FutoshikiRules.countSolutions(puzzle, limit = 2) == 1) {
                "Futoshiki puzzle must have exactly one solution"
            }
            return puzzle
        }

        throw IllegalStateException(
            "Could not generate a ${pack.name} Futoshiki puzzle in $MAX_GENERATION_ATTEMPTS attempts",
        )
    }

    private fun targetRank(pack: Pack): Int = when (pack) {
        Pack.EASY -> FutoshikiTechniques.SIGN_BOUNDS.difficultyRank
        Pack.MEDIUM -> FutoshikiTechniques.SIGN_CHAINS.difficultyRank
        Pack.HARD -> FutoshikiTechniques.SIGN_CHAINS.difficultyRank
        Pack.EXPERT -> FutoshikiTechniques.NAKED_PAIR.difficultyRank
    }

    private fun randomLatinSquare(size: Int, rng: SplitMix64): List<Int> {
        val rowOrder = (0 until size).toMutableList().also { shuffle(it, rng) }
        val columnOrder = (0 until size).toMutableList().also { shuffle(it, rng) }
        val digitMap = (1..size).toMutableList().also { shuffle(it, rng) }
        val transpose = rng.nextInt(2) == 1

        val solution = IntArray(size * size)
        for (row in 0 until size) {
            for (column in 0 until size) {
                val sourceRow = rowOrder[row]
                val sourceColumn = columnOrder[column]
                val digit = digitMap[(sourceRow + sourceColumn) % size]
                if (transpose) {
                    solution[column * size + row] = digit
                } else {
                    solution[row * size + column] = digit
                }
            }
        }
        return solution.toList()
    }

    private fun allSigns(
        size: Int,
        solution: List<Int>,
    ): Pair<List<FutoshikiSign>, List<FutoshikiSign>> {
        val horizontalSigns = MutableList(size * (size - 1)) { FutoshikiSign.NONE }
        for (row in 0 until size) {
            for (column in 0 until size - 1) {
                val left = solution[row * size + column]
                val right = solution[row * size + column + 1]
                horizontalSigns[row * (size - 1) + column] =
                    if (left < right) FutoshikiSign.LESS else FutoshikiSign.GREATER
            }
        }
        val verticalSigns = MutableList((size - 1) * size) { FutoshikiSign.NONE }
        for (row in 0 until size - 1) {
            for (column in 0 until size) {
                val top = solution[row * size + column]
                val bottom = solution[(row + 1) * size + column]
                verticalSigns[row * size + column] =
                    if (top < bottom) FutoshikiSign.LESS else FutoshikiSign.GREATER
            }
        }
        return horizontalSigns to verticalSigns
    }

    private fun tryGrade(
        size: Int,
        givens: List<Int?>,
        horizontalSigns: List<FutoshikiSign>,
        verticalSigns: List<FutoshikiSign>,
        solution: List<Int>,
        pack: Pack,
    ): Grade? = try {
        FutoshikiTechniqueSolver.gradeByTechniques(
            candidatePuzzle(
                size = size,
                givens = givens,
                horizontalSigns = horizontalSigns,
                verticalSigns = verticalSigns,
                solution = solution,
                pack = pack,
            ),
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun candidatePuzzle(
        size: Int,
        givens: List<Int?>,
        horizontalSigns: List<FutoshikiSign>,
        verticalSigns: List<FutoshikiSign>,
        solution: List<Int>,
        pack: Pack,
        id: String = "FU-CANDIDATE-${pack.name}",
        storedGrade: String = "pending",
    ): FutoshikiPuzzle = FutoshikiPuzzle(
        id = id,
        size = size,
        givens = givens,
        horizontalSigns = horizontalSigns,
        verticalSigns = verticalSigns,
        solution = solution,
        pack = pack,
        storedGrade = storedGrade,
    )

    private fun <T> shuffle(values: MutableList<T>, rng: SplitMix64) {
        for (index in values.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val value = values[index]
            values[index] = values[other]
            values[other] = value
        }
    }

    private sealed class RemovableItem {
        data class Given(val index: Int) : RemovableItem()
        data class HorizontalSign(val index: Int) : RemovableItem()
        data class VerticalSign(val index: Int) : RemovableItem()
    }
}
