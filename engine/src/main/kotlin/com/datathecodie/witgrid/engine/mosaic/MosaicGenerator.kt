package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Deterministic Mosaic generator using random fill and solver-checked clue removal.
 */
object MosaicGenerator {
    private const val MAX_ATTEMPTS = 500

    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): MosaicPuzzle {
        val size = MosaicPackBands.sizeForPack(pack)
        val grid = Grid(size)
        val densityPercent = densityTargetPercent(pack)

        repeat(MAX_ATTEMPTS) {
            val solution = List(grid.cellCount) {
                if (rng.nextInt(100) < densityPercent) 1 else 0
            }

            val clues = (0 until grid.cellCount).map {
                MosaicRules.clueAt(grid, solution, Cell(it)) as Int?
            }.toMutableList()

            // Verify full-clue grid is solvable
            val initialPuzzle = candidatePuzzle(
                size = size,
                clues = clues,
                solution = solution,
                pack = pack,
            )

            var lastGrade = try {
                MosaicTechniqueSolver.gradeByTechniques(initialPuzzle)
            } catch (_: IllegalArgumentException) {
                return@repeat
            }

            // Precompute area cells for each cell index
            val areaByCell = Array(grid.cellCount) { MosaicRules.areaCells(grid, Cell(it)) }
            val clueCoverage = IntArray(grid.cellCount)
            for (index in 0 until grid.cellCount) {
                for (cell in areaByCell[index]) {
                    clueCoverage[cell.index]++
                }
            }

            // Remove clues in a seeded random order while technique solver still finishes
            val removalOrder = (0 until grid.cellCount).toMutableList()
            shuffle(removalOrder, rng)

            for (index in removalOrder) {
                val clue = clues[index] ?: continue
                val area = areaByCell[index]

                // A cell with zero clues covering it can never be solved
                var canRemove = true
                for (cell in area) {
                    if (clueCoverage[cell.index] <= 1) {
                        canRemove = false
                        break
                    }
                }
                if (!canRemove) continue

                clues[index] = null
                val candidate = candidatePuzzle(
                    size = size,
                    clues = clues,
                    solution = solution,
                    pack = pack,
                )
                val grade = try {
                    MosaicTechniqueSolver.gradeByTechniques(candidate)
                } catch (_: IllegalArgumentException) {
                    clues[index] = clue
                    continue
                }
                for (cell in area) {
                    clueCoverage[cell.index]--
                }
                lastGrade = grade
            }

            val id = "MO-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}"
            val finalPuzzle = MosaicPuzzle(
                id = id,
                size = size,
                clues = clues,
                solution = solution,
                pack = pack,
                storedGrade = MosaicGradeCodec.encode(lastGrade),
            )

            check(MosaicRules.countSolutions(finalPuzzle, limit = 2) == 1) {
                "Technique-solvable Mosaic puzzle must have exactly one solution"
            }
            check(MosaicPackBands.isGradeInPackBand(pack, lastGrade)) {
                "Mosaic grade must fit the $pack pack band"
            }

            return finalPuzzle
        }

        throw IllegalStateException(
            "Could not generate a technique-solvable Mosaic puzzle for $pack in $MAX_ATTEMPTS attempts",
        )
    }

    private fun densityTargetPercent(pack: Pack): Int = when (pack) {
        Pack.EASY -> 40
        Pack.MEDIUM -> 40
        Pack.HARD -> 45
        Pack.EXPERT -> 45
    }

    private fun candidatePuzzle(
        size: Int,
        clues: List<Int?>,
        solution: List<Int>,
        pack: Pack,
    ): MosaicPuzzle = MosaicPuzzle(
        id = "MO-CANDIDATE-${pack.name}",
        size = size,
        clues = clues,
        solution = solution,
        pack = pack,
        storedGrade = "pending",
    )

    private fun <T> shuffle(values: MutableList<T>, rng: SplitMix64) {
        for (index in values.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val temp = values[index]
            values[index] = values[other]
            values[other] = temp
        }
    }
}
