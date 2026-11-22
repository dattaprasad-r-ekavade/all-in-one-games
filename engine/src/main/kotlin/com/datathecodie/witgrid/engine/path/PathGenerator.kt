package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import java.util.ArrayList

/**
 * Deterministic Path generator that builds boards of non-self-touching paths covering every non-wall cell.
 *
 * Randomness is consumed strictly through [SplitMix64].
 * Puzzles are accepted only if they have exactly one solution and are solvable by the technique solver.
 */
object PathGenerator {
    private const val MAX_ATTEMPTS = 5_000

    /**
     * Builds one unique, technique-solvable [PathPuzzle] in [pack], consuming randomness only from [rng].
     */
    fun generate(rng: SplitMix64, pack: Pack): PathPuzzle {
        val size = PathPackBands.sizeForPack(pack)
        val cellCount = size * size

        repeat(MAX_ATTEMPTS) {
            val candidate = tryGenerateCandidate(size, pack, rng) ?: return@repeat

            // Fast uniqueness check
            if (PathRules.countSolutions(candidate, limit = 2) != 1) {
                return@repeat
            }

            // Technique solver check and grading (candidate is already verified unique above)
            val grade = try {
                PathTechniqueSolver.gradeByTechniques(candidate)
            } catch (_: Exception) {
                return@repeat
            }

            if (grade.pack != pack) {
                return@repeat
            }

            val id = "PA-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}"
            val encodedGrade = PathGradeCodec.encode(grade)

            return PathPuzzle(
                id = id,
                size = size,
                endpoints = candidate.endpoints,
                walls = candidate.walls,
                solution = candidate.solution,
                pack = pack,
                storedGrade = encodedGrade,
            )
        }

        throw IllegalStateException("Failed to generate a valid $pack Path puzzle after $MAX_ATTEMPTS attempts")
    }

    internal fun tryGenerateCandidate(size: Int, pack: Pack, rng: SplitMix64): PathPuzzle? {
        val cellCount = size * size
        val gridColors = IntArray(cellCount)
        val walls = BooleanArray(cellCount)

        // Precompute orthogonal neighbors
        val neighbors = Array(cellCount) { index ->
            val r = index / size
            val c = index % size
            val list = ArrayList<Int>(4)
            if (r > 0) list.add((r - 1) * size + c)
            if (r < size - 1) list.add((r + 1) * size + c)
            if (c > 0) list.add(r * size + c - 1)
            if (c < size - 1) list.add(r * size + c + 1)
            list.toIntArray()
        }

        // Optionally place initial sparse walls (0 to a few walls)
        val initialWallBudget = when (pack) {
            Pack.EASY -> rng.nextInt(3) // 0..2
            Pack.MEDIUM -> rng.nextInt(4) // 0..3
            Pack.HARD -> 1 + rng.nextInt(4) // 1..4
            Pack.EXPERT -> 2 + rng.nextInt(5) // 2..6
        }

        var placedInitialWalls = 0
        var wallAttempts = 0
        while (placedInitialWalls < initialWallBudget && wallAttempts < 50) {
            wallAttempts++
            val cell = rng.nextInt(cellCount)
            val r = cell / size
            val c = cell % size
            // Avoid corners and immediate edges for initial walls to keep borders playable
            if (r in 1 until size - 1 && c in 1 until size - 1 && !walls[cell]) {
                walls[cell] = true
                placedInitialWalls++
            }
        }

        val minPathLength = 3
        val maxPathLength = when (pack) {
            Pack.EASY -> 6
            Pack.MEDIUM -> 8
            Pack.HARD -> 10
            Pack.EXPERT -> 12
        }

        var currentPair = 1
        val pathEndpoints = ArrayList<Pair<Int, Int>>() // start to end for each pair

        fun countUnassignedNeighbors(cell: Int): Int {
            var count = 0
            for (nb in neighbors[cell]) {
                if (!walls[nb] && gridColors[nb] == 0) count++
            }
            return count
        }

        var remainingUnassigned = (0 until cellCount).count { !walls[it] && gridColors[it] == 0 }

        while (remainingUnassigned > 0) {
            if (currentPair > MAX_PAIR_COUNT) return null

            // Pick unassigned cell with lowest number of unassigned neighbors (Warnsdorff-like)
            var bestStart = -1
            var minDegree = 100
            for (i in 0 until cellCount) {
                if (!walls[i] && gridColors[i] == 0) {
                    val deg = countUnassignedNeighbors(i)
                    if (deg < minDegree) {
                        minDegree = deg
                        bestStart = i
                    }
                }
            }

            if (bestStart == -1) break

            val pathCells = ArrayList<Int>()
            pathCells.add(bestStart)
            gridColors[bestStart] = currentPair
            remainingUnassigned--

            val targetLen = minPathLength + rng.nextInt(maxPathLength - minPathLength + 1)
            var head = bestStart

            while (pathCells.size < targetLen) {
                val candidateNeighbors = ArrayList<Int>()
                for (nb in neighbors[head]) {
                    if (walls[nb] || gridColors[nb] != 0) continue
                    // Check no self-touch: nb must not touch any cell of currentPair except head
                    var touchesSelf = false
                    for (adj in neighbors[nb]) {
                        if (adj != head && gridColors[adj] == currentPair) {
                            touchesSelf = true
                            break
                        }
                    }
                    if (!touchesSelf) {
                        candidateNeighbors.add(nb)
                    }
                }

                if (candidateNeighbors.isEmpty()) break

                // Pick next neighbor: prefer one with fewer unassigned neighbors to avoid leaving isolated cells
                candidateNeighbors.sortBy { countUnassignedNeighbors(it) }
                val next = if (candidateNeighbors.size > 1 && rng.nextInt(3) == 0) {
                    candidateNeighbors[1]
                } else {
                    candidateNeighbors[0]
                }

                pathCells.add(next)
                gridColors[next] = currentPair
                remainingUnassigned--
                head = next
            }

            if (pathCells.size < 2) {
                // Single cell cannot form a path; convert it to a wall
                val singleCell = pathCells[0]
                walls[singleCell] = true
                gridColors[singleCell] = 0
                // do not increment currentPair
            } else {
                pathEndpoints.add(pathCells.first() to pathCells.last())
                currentPair++
            }
        }

        val totalPairs = currentPair - 1
        if (totalPairs < 2 || totalPairs > MAX_PAIR_COUNT) return null

        val wallCount = walls.count { it }
        val maxAllowedWalls = cellCount / 5 // at most 20% walls
        if (wallCount > maxAllowedWalls) return null

        val endpointList = arrayOfNulls<Int>(cellCount)
        for (p in 1..totalPairs) {
            val (start, end) = pathEndpoints[p - 1]
            endpointList[start] = p
            endpointList[end] = p
        }

        val candidate = PathPuzzle(
            id = "PA-CANDIDATE",
            size = size,
            endpoints = endpointList.toList(),
            walls = walls.toList(),
            solution = gridColors.toList(),
            pack = pack,
            storedGrade = "pending",
        )

        if (!PathRules.isValidSolution(candidate)) {
            return null
        }

        return candidate
    }
}
