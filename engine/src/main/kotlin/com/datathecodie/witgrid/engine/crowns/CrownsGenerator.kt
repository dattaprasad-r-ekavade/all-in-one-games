package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic random flood-fill generator for unique, technique-solvable Crowns puzzles. */
object CrownsGenerator {
    private const val MAX_PUZZLE_ATTEMPTS = 20_000
    private const val MIN_REGION_CELLS = 3
    private const val MAX_REFINE_MOVES = 3_000
    private const val SOLUTION_COUNT_CAP = 8
    private const val PLACEHOLDER_ID = "CR-SEARCH"

    /** Builds one puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): CrownsPuzzle {
        val size = boardSize(rng, pack)
        repeat(MAX_PUZZLE_ATTEMPTS) {
            val solution = randomCrownLayout(size, rng) ?: return@repeat
            val initialRegions = randomRegions(size, solution, rng) ?: return@repeat
            val regions = refineToUnique(size, solution, initialRegions, pack, rng) ?: return@repeat
            val id = "CR-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}"
            val grade = CrownsTechniqueSolver.grade(puzzleFor(id, size, regions, solution, pack, "pending"))
            return puzzleFor(id, size, regions, solution, pack, CrownsGradeCodec.encode(grade))
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
        val regions = IntArray(cellCount) { -1 }
        val counts = IntArray(size)
        for (region in 0 until size) {
            val seed = region * size + solution[region]
            regions[seed] = region
            counts[region] = 1
        }

        var assignedCount = size
        while (assignedCount < cellCount) {
            val frontiers = (0 until size).map { frontierCells(size, it, regions) }
            // A region below the minimum with no free neighbour is boxed in, so this layout is rejected.
            if ((0 until size).any { counts[it] < MIN_REGION_CELLS && frontiers[it].isEmpty() }) return null
            val growable = (0 until size).filter { frontiers[it].isNotEmpty() }
            if (growable.isEmpty()) return null
            // Small regions grow first, so they reach the minimum before neighbours box them in.
            val smallRegions = growable.filter { counts[it] < MIN_REGION_CELLS }
            val pool = smallRegions.ifEmpty { growable }
            val region = pool[rng.nextInt(pool.size)]
            val frontier = frontiers[region]
            regions[frontier[rng.nextInt(frontier.size)]] = region
            counts[region]++
            assignedCount++
        }

        return regions.toList()
    }

    /**
     * Moves border cells between neighbouring regions until the puzzle has one solution
     * that the technique solver can reach. Crown cells stay in their regions. A move that
     * keeps every region connected and at least [MIN_REGION_CELLS] large is kept when it
     * does not add solutions, so the walk stays among unique layouts once it finds one.
     */
    private fun refineToUnique(
        size: Int,
        solution: List<Int>,
        initialRegions: List<Int>,
        pack: Pack,
        rng: SplitMix64,
    ): List<Int>? {
        val crownCells = solution.indices.map { row -> row * size + solution[row] }.toSet()
        var regions = initialRegions.toIntArray()
        var solutions = countSolutions(size, regions, solution, pack)
        repeat(MAX_REFINE_MOVES) {
            if (solutions == 1 && solvesByTechnique(size, regions, solution, pack)) return regions.toList()
            val cell = rng.nextInt(regions.size)
            if (cell in crownCells) return@repeat
            val neighbourRegions = orthogonalNeighbours(cell, size).map { regions[it] }.filter { it != regions[cell] }
            if (neighbourRegions.isEmpty()) return@repeat
            val moved = regions.copyOf()
            moved[cell] = neighbourRegions[rng.nextInt(neighbourRegions.size)]
            if (!allRegionsValid(size, moved)) return@repeat
            val movedSolutions = countSolutions(size, moved, solution, pack)
            if (movedSolutions <= solutions) {
                regions = moved
                solutions = movedSolutions
            }
        }
        return null
    }

    private fun puzzleFor(
        id: String,
        size: Int,
        regions: List<Int>,
        solution: List<Int>,
        pack: Pack,
        storedGrade: String,
    ): CrownsPuzzle = CrownsPuzzle(
        id = id,
        size = size,
        regions = regions,
        solutionColumns = solution,
        pack = pack,
        storedGrade = storedGrade,
    )

    private fun solvesByTechnique(size: Int, regions: IntArray, solution: List<Int>, pack: Pack): Boolean {
        val puzzle = puzzleFor(PLACEHOLDER_ID, size, regions.toList(), solution, pack, "pending")
        return try {
            CrownsTechniqueSolver.grade(puzzle)
            true
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun countSolutions(size: Int, regions: IntArray, solution: List<Int>, pack: Pack): Int =
        CrownsRules.countSolutions(
            puzzleFor(PLACEHOLDER_ID, size, regions.toList(), solution, pack, "pending"),
            limit = SOLUTION_COUNT_CAP,
        )

    private fun allRegionsValid(size: Int, regions: IntArray): Boolean {
        for (region in 0 until size) {
            val cells = regions.indices.filter { regions[it] == region }
            if (cells.size < MIN_REGION_CELLS) return false
            val reached = mutableSetOf(cells.first())
            val queue = ArrayDeque(listOf(cells.first()))
            while (queue.isNotEmpty()) {
                val cell = queue.removeFirst()
                orthogonalNeighbours(cell, size)
                    .filter { regions[it] == region && reached.add(it) }
                    .forEach(queue::addLast)
            }
            if (reached.size != cells.size) return false
        }
        return true
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
