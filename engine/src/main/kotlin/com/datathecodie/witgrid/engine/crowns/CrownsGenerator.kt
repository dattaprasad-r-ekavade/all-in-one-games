package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic random flood-fill generator for unique, technique-solvable Crowns puzzles. */
object CrownsGenerator {
    private const val MAX_PUZZLE_ATTEMPTS = 20_000

    /** Builds one puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): CrownsPuzzle {
        val size = boardSize(rng, pack)
        repeat(MAX_PUZZLE_ATTEMPTS) {
            val solution = randomCrownLayout(size, rng) ?: return@repeat
            val regions = randomRegions(size, solution, rng) ?: return@repeat
            val candidate = CrownsPuzzle(
                id = "CR-${pack.name}-${rng.nextLong().toString(16)}",
                size = size,
                regions = regions,
                solutionColumns = solution,
                pack = pack,
                storedGrade = "pending",
            )
            val grade = try {
                CrownsTechniqueSolver.grade(candidate)
            } catch (_: IllegalArgumentException) {
                return@repeat
            }
            return CrownsPuzzle(
                id = candidate.id,
                size = candidate.size,
                regions = candidate.regions,
                solutionColumns = candidate.solutionColumns,
                pack = candidate.pack,
                storedGrade = CrownsGradeCodec.encode(grade),
            )
        }
        throw IllegalStateException("Could not generate a technique-solvable Crowns puzzle after $MAX_PUZZLE_ATTEMPTS attempts")
    }

    private fun boardSize(rng: SplitMix64, pack: Pack): Int = when (pack) {
        Pack.EASY -> 6
        Pack.MEDIUM -> 7
        Pack.HARD -> 8
        Pack.EXPERT -> 9 + rng.nextInt(2)
    }

    private fun randomCrownLayout(size: Int, rng: SplitMix64): List<Int>? {
        val columnsByRow = IntArray(size)
        val usedColumns = BooleanArray(size)

        fun place(row: Int): Boolean {
            if (row == size) return true
            val options = (0 until size).filter { column ->
                !usedColumns[column] &&
                    (row == 0 || kotlin.math.abs(column - columnsByRow[row - 1]) > 1)
            }.toMutableList()
            shuffle(options, rng)
            for (column in options) {
                columnsByRow[row] = column
                usedColumns[column] = true
                if (place(row + 1)) return true
                usedColumns[column] = false
            }
            return false
        }

        return if (place(0)) columnsByRow.toList() else null
    }

    private fun randomRegions(size: Int, solution: List<Int>, rng: SplitMix64): List<Int>? {
        val cellCount = size * size
        val capacities = regionCapacities(size, rng)
        val regions = IntArray(cellCount) { -1 }
        val counts = IntArray(size)
        for (region in 0 until size) {
            val seed = region * size + solution[region]
            regions[seed] = region
            counts[region] = 1
        }

        var assignedCount = size
        while (assignedCount < cellCount) {
            val frontiers = arrayOfNulls<List<Int>>(size)
            val growableRegions = ArrayList<Int>()
            for (region in 0 until size) {
                if (counts[region] >= capacities[region]) continue
                val frontier = frontierCells(size, region, regions)
                if (frontier.isEmpty()) return null
                frontiers[region] = frontier
                growableRegions += region
            }
            if (growableRegions.isEmpty()) return null

            val fewestOptions = growableRegions.minOf { requireNotNull(frontiers[it]).size }
            val urgentRegions = growableRegions.filter { requireNotNull(frontiers[it]).size == fewestOptions }
            val region = urgentRegions[rng.nextInt(urgentRegions.size)]
            val frontier = requireNotNull(frontiers[region])
            val cell = frontier[rng.nextInt(frontier.size)]
            regions[cell] = region
            counts[region]++
            assignedCount++
        }

        return regions.toList()
    }

    private fun regionCapacities(size: Int, rng: SplitMix64): IntArray {
        val capacities = IntArray(size)
        val regionIds = (0 until size).toMutableList()
        shuffle(regionIds, rng)
        // Small connected regions create opening line deductions for the no-givens solver.
        val singletonCount = maxOf(2, size / 3)
        val pairCount = maxOf(2, size / 3)
        regionIds.take(singletonCount).forEach { capacities[it] = 1 }
        regionIds.drop(singletonCount).take(pairCount).forEach { capacities[it] = 2 }
        val remainingRegions = regionIds.drop(singletonCount + pairCount).toMutableList()
        val remainingCells = size * size - singletonCount - pairCount * 2
        val baseCapacity = remainingCells / remainingRegions.size
        val extraCells = remainingCells % remainingRegions.size
        remainingRegions.forEach { capacities[it] = baseCapacity }
        shuffle(remainingRegions, rng)
        for (index in 0 until extraCells) capacities[remainingRegions[index]]++
        return capacities
    }

    private fun frontierCells(size: Int, region: Int, regions: IntArray): List<Int> =
        (0 until regions.size).filter { cell ->
            regions[cell] == -1 && orthogonalNeighbours(cell, size).any { regions[it] == region }
        }

    private fun orthogonalNeighbours(cell: Int, size: Int): List<Int> {
        val row = cell / size
        val column = cell % size
        val neighbours = ArrayList<Int>(4)
        if (row > 0) neighbours += cell - size
        if (column > 0) neighbours += cell - 1
        if (column + 1 < size) neighbours += cell + 1
        if (row + 1 < size) neighbours += cell + size
        return neighbours
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
