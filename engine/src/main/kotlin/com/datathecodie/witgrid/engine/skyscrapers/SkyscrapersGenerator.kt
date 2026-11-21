package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Deterministic Skyscrapers puzzle generator with uniqueness and technique-grade rejection. */
object SkyscrapersGenerator {
    private const val MAX_GENERATION_ATTEMPTS = 500

    /** Builds one unique, technique-solvable puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): SkyscrapersPuzzle {
        val size = SkyscrapersPackBands.sizeForPack(pack)
        repeat(MAX_GENERATION_ATTEMPTS) { attempt ->
            // Large unconstrained squares rarely give enough strong clues for a guess-free solve.
            // Seed ordered edge lines on some Expert attempts and randomize the remaining cells.
            val orderedLines = when {
                attempt == MAX_GENERATION_ATTEMPTS - 1 -> size
                pack == Pack.EXPERT -> attempt % 4
                else -> 0
            }
            val solution = randomLatinSquare(size, orderedLines, rng)
            val rows = solution.chunked(size)
            val columns = List(size) { column -> rows.map { it[column] } }
            val clues = buildList<Int?>(4 * size) {
                addAll(columns.map(SkyscrapersRules::countVisible))
                addAll(columns.map { SkyscrapersRules.countVisible(it.asReversed()) })
                addAll(rows.map(SkyscrapersRules::countVisible))
                addAll(rows.map { SkyscrapersRules.countVisible(it.asReversed()) })
            }.toMutableList()

            fun candidate(id: String = "SK-CANDIDATE", storedGrade: String = "pending") =
                SkyscrapersPuzzle(
                    id = id,
                    size = size,
                    topClues = clues.subList(0, size),
                    bottomClues = clues.subList(size, 2 * size),
                    leftClues = clues.subList(2 * size, 3 * size),
                    rightClues = clues.subList(3 * size, 4 * size),
                    solution = solution,
                    pack = pack,
                    storedGrade = storedGrade,
                )

            // Every candidate keeps the same valid solution because only clues are removed.
            var lastGrade = gradeIfUnique(candidate()) ?: return@repeat
            val removalOrder = clues.indices.toMutableList()
            shuffle(removalOrder, rng)

            var removedClue = false
            for (index in removalOrder) {
                val previousClue = clues[index]
                clues[index] = null
                val grade = gradeIfUnique(candidate())
                if (grade == null) {
                    clues[index] = previousClue
                } else {
                    removedClue = true
                    lastGrade = grade
                }
            }

            if (removedClue && SkyscrapersPackBands.isGradeInPackBand(pack, lastGrade)) {
                val suffix = rng.nextLong().toULong().toString(16).padStart(16, '0')
                return candidate("SK-${pack.name}-$suffix", SkyscrapersGradeCodec.encode(lastGrade))
            }
        }
        error("Could not generate a ${pack.name} Skyscrapers puzzle in $MAX_GENERATION_ATTEMPTS attempts")
    }

    private fun gradeIfUnique(puzzle: SkyscrapersPuzzle) =
        if (SkyscrapersRules.countSolutions(puzzle, limit = 2) == 1) {
            SkyscrapersTechniqueSolver.gradeUniquePuzzleOrNull(puzzle)
        } else {
            null
        }

    private fun randomLatinSquare(size: Int, orderedLines: Int, rng: SplitMix64): List<Int> {
        val grid = IntArray(size * size)
        val rowMasks = IntArray(size)
        val columnMasks = IntArray(size)
        val allHeights = (1 shl size) - 1
        var placed = 0
        for (cell in grid.indices) {
            val row = cell / size
            val column = cell % size
            if (row >= orderedLines && column >= orderedLines) continue
            val height = (row + column) % size + 1
            val bit = 1 shl (height - 1)
            grid[cell] = height
            rowMasks[row] = rowMasks[row] or bit
            columnMasks[column] = columnMasks[column] or bit
            placed++
        }

        fun fill(placed: Int): Boolean {
            if (placed == grid.size) return true

            var nextCell = -1
            var nextCandidates = 0
            var fewestCandidates = size + 1
            for (cell in grid.indices) {
                if (grid[cell] != 0) continue
                val used = rowMasks[cell / size] or columnMasks[cell % size]
                val candidates = allHeights and used.inv()
                val count = Integer.bitCount(candidates)
                if (count == 0) return false
                if (count < fewestCandidates) {
                    nextCell = cell
                    nextCandidates = candidates
                    fewestCandidates = count
                    if (count == 1) break
                }
            }

            val row = nextCell / size
            val column = nextCell % size
            val heights = (1..size).filter { nextCandidates and (1 shl (it - 1)) != 0 }.toMutableList()
            shuffle(heights, rng)

            for (height in heights) {
                val bit = 1 shl (height - 1)
                grid[nextCell] = height
                rowMasks[row] = rowMasks[row] or bit
                columnMasks[column] = columnMasks[column] or bit

                if (fill(placed + 1)) return true

                grid[nextCell] = 0
                rowMasks[row] = rowMasks[row] and bit.inv()
                columnMasks[column] = columnMasks[column] and bit.inv()
            }
            return false
        }

        check(fill(placed)) { "Could not create a Latin square of size $size" }
        if (orderedLines == 0) return grid.toList()

        val reverseRows = rng.nextInt(2) == 1
        val reverseColumns = rng.nextInt(2) == 1
        return List(grid.size) { cell ->
            val row = cell / size
            val column = cell % size
            val sourceRow = if (reverseRows) size - 1 - row else row
            val sourceColumn = if (reverseColumns) size - 1 - column else column
            grid[sourceRow * size + sourceColumn]
        }
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
