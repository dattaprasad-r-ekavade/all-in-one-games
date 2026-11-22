package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import java.util.ArrayList

/**
 * Path rule checks, mistake detection and capped solution counting.
 *
 * Fundamental rule: A path connects each pair of equal numbers. Paths do not branch, cross,
 * or touch themselves (no path cell is orthogonally adjacent to a cell of the same colour other
 * than its previous and next cell on the path; this also bans 2x2 blocks of one colour).
 * Every non-wall cell must be filled. One route equals one colouring, so solution counting
 * counts colourings.
 */
object PathRules {

    /**
     * Returns whether [gridColors] is a rule-valid solution for [puzzle].
     *
     * A valid solution satisfies:
     * 1. Every non-wall cell has a pair color in 1..[PathPuzzle.pairCount].
     * 2. Every wall cell has color 0.
     * 3. For each pair 1..[PathPuzzle.pairCount], the cells of that color form a single
     *    unbranched, non-crossing simple path connecting the pair's two given endpoints.
     *    Every internal path cell has degree 2 in the induced subgraph; each endpoint has degree 1.
     *    All cells of that color are connected to the path (no disconnected cycles or components).
     * 4. A path must never touch itself: no path cell is orthogonally adjacent to a cell of the
     *    same colour other than its previous and next cell on the path (this also bans 2x2 blocks
     *    of one colour). One route equals one colouring.
     */
    fun isValidSolution(puzzle: PathPuzzle, gridColors: List<Int> = puzzle.solution): Boolean {
        if (gridColors.size != puzzle.grid.cellCount) return false
        val size = puzzle.size

        // Check walls and non-wall values
        for (index in 0 until puzzle.grid.cellCount) {
            val color = gridColors[index]
            if (puzzle.walls[index]) {
                if (color != 0) return false
            } else {
                if (color !in 1..puzzle.pairCount) return false
                val endpoint = puzzle.endpoints[index]
                if (endpoint != null && endpoint != color) return false
            }
        }

        // Check for 2x2 blocks of one non-wall color
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val color = gridColors[r * size + c]
                if (color != 0 &&
                    color == gridColors[r * size + c + 1] &&
                    color == gridColors[(r + 1) * size + c] &&
                    color == gridColors[(r + 1) * size + c + 1]
                ) {
                    return false
                }
            }
        }

        // For each pair, verify it forms a single simple path between its two endpoints
        for (pair in 1..puzzle.pairCount) {
            val pairEndpoints = ArrayList<Int>(2)
            var cellCountInPair = 0
            for (index in 0 until puzzle.grid.cellCount) {
                if (gridColors[index] == pair) {
                    cellCountInPair++
                    if (puzzle.endpoints[index] == pair) {
                        pairEndpoints.add(index)
                    }
                }
            }
            if (pairEndpoints.size != 2) return false
            val start = pairEndpoints[0]
            val end = pairEndpoints[1]

            // Degree check in the induced subgraph of this pair.
            // Enforces that endpoints have degree 1 and internal cells have degree 2.
            // Together with connectivity, this guarantees a path never touches itself:
            // no path cell is orthogonally adjacent to a cell of the same color other than
            // its predecessor and successor on the path.
            for (index in 0 until puzzle.grid.cellCount) {
                if (gridColors[index] != pair) continue
                var neighborCount = 0
                val row = index / size
                val col = index % size
                if (row > 0 && gridColors[(row - 1) * size + col] == pair) neighborCount++
                if (row < size - 1 && gridColors[(row + 1) * size + col] == pair) neighborCount++
                if (col > 0 && gridColors[row * size + col - 1] == pair) neighborCount++
                if (col < size - 1 && gridColors[row * size + col + 1] == pair) neighborCount++

                val expectedDegree = if (index == start || index == end) 1 else 2
                if (neighborCount != expectedDegree) return false
            }

            // Connectivity check: walk from start to end
            var visitedCount = 1
            var prev = -1
            var curr = start
            while (curr != end) {
                val row = curr / size
                val col = curr % size
                var next = -1
                if (row > 0) {
                    val up = (row - 1) * size + col
                    if (up != prev && gridColors[up] == pair) next = up
                }
                if (next == -1 && row < size - 1) {
                    val down = (row + 1) * size + col
                    if (down != prev && gridColors[down] == pair) next = down
                }
                if (next == -1 && col > 0) {
                    val left = row * size + col - 1
                    if (left != prev && gridColors[left] == pair) next = left
                }
                if (next == -1 && col < size - 1) {
                    val right = row * size + col + 1
                    if (right != prev && gridColors[right] == pair) next = right
                }

                if (next == -1) return false
                prev = curr
                curr = next
                visitedCount++
                if (visitedCount > cellCountInPair) return false
            }
            if (visitedCount != cellCountInPair) return false
        }

        return true
    }

    /**
     * Returns whether the player's board is completely solved.
     * All non-wall cells must match the stored solution and all walls must remain unedited.
     */
    fun isSolved(puzzle: PathPuzzle, state: PathState): Boolean {
        require(state.puzzle == puzzle) { "Path state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.walls[index]) {
                if (state.digits[index] != null) return false
            } else {
                if (state.digits[index] != puzzle.solution[index]) return false
            }
        }
        return true
    }

    /**
     * Returns incorrect committed player entries in row-major order.
     *
     * A player-entered color that differs from the stored solution is a mistake.
     * Empty non-wall cells, given endpoints, and walls are never mistakes.
     */
    fun mistakes(puzzle: PathPuzzle, state: PathState): List<Cell> {
        require(state.puzzle == puzzle) { "Path state belongs to a different puzzle" }
        val result = ArrayList<Cell>()
        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.walls[index] || puzzle.endpoints[index] != null) continue
            val playerDigit = state.digits[index]
            if (playerDigit != null && playerDigit != puzzle.solution[index]) {
                result.add(Cell(index))
            }
        }
        return result
    }

    /**
     * Counts rule-valid solutions up to [limit] using path-extension backtracking
     * with no-self-touch pruning, dead-cell pruning and stranded-endpoint pruning.
     */
    fun countSolutions(puzzle: PathPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val size = puzzle.size
        val cellCount = puzzle.grid.cellCount
        val pairCount = puzzle.pairCount

        val startEndpoint = IntArray(pairCount + 1)
        val targetEndpoint = IntArray(pairCount + 1)
        for (pair in 1..pairCount) {
            var foundFirst = false
            for (index in 0 until cellCount) {
                if (puzzle.endpoints[index] == pair) {
                    if (!foundFirst) {
                        startEndpoint[pair] = index
                        foundFirst = true
                    } else {
                        targetEndpoint[pair] = index
                        break
                    }
                }
            }
        }

        val head = IntArray(pairCount + 1) { startEndpoint[it] }
        val isCompleted = BooleanArray(pairCount + 1)
        var uncompletedCount = pairCount

        val headPair = IntArray(cellCount)
        val targetPair = IntArray(cellCount)
        val cellColor = IntArray(cellCount)
        for (pair in 1..pairCount) {
            val s = startEndpoint[pair]
            val t = targetEndpoint[pair]
            headPair[s] = pair
            targetPair[t] = pair
            cellColor[s] = pair
        }

        val isUnassigned = BooleanArray(cellCount) { !puzzle.walls[it] && puzzle.endpoints[it] == null }
        var unassignedCount = isUnassigned.count { it }

        // Precompute orthogonal neighbors for each cell
        val neighbors = Array(cellCount) { index ->
            val row = index / size
            val col = index % size
            val list = ArrayList<Int>(4)
            if (row > 0) list.add((row - 1) * size + col)
            if (row < size - 1) list.add((row + 1) * size + col)
            if (col > 0) list.add(row * size + col - 1)
            if (col < size - 1) list.add(row * size + col + 1)
            list.toIntArray()
        }

        var solutionCount = 0

        fun isDeadCell(cell: Int): Boolean {
            var unassignedNeighbors = 0
            var hasAdjacentHeadOrTarget = false
            val nbs = neighbors[cell]
            for (nb in nbs) {
                if (isUnassigned[nb]) {
                    unassignedNeighbors++
                } else if (headPair[nb] != 0 || targetPair[nb] != 0) {
                    hasAdjacentHeadOrTarget = true
                }
            }
            if (unassignedNeighbors == 0) {
                // Alive if ANY pair has both its head and its target adjacent
                for (nb1 in nbs) {
                    val hp = headPair[nb1]
                    if (hp != 0) {
                        for (nb2 in nbs) {
                            if (targetPair[nb2] == hp) {
                                return false
                            }
                        }
                    }
                }
                return true
            }
            if (unassignedNeighbors == 1) {
                // Alive if any head or target is adjacent
                return !hasAdjacentHeadOrTarget
            }
            return false
        }

        val visited = BooleanArray(cellCount)
        val queue = IntArray(cellCount)

        fun checkReachabilityAndIslands(): Boolean {
            for (pair in 1..pairCount) {
                if (isCompleted[pair]) continue
                val tgt = targetEndpoint[pair]
                val hd = head[pair]
                var canReach = false
                for (nb in neighbors[tgt]) {
                    if (nb == hd || isUnassigned[nb]) {
                        canReach = true
                        break
                    }
                }
                if (!canReach) return false
            }

            if (unassignedCount == 0) return true

            visited.fill(false)

            for (i in 0 until cellCount) {
                if (!isUnassigned[i] || visited[i]) continue

                var qHead = 0
                var qTail = 0
                queue[qTail++] = i
                visited[i] = true

                var lastTouchedEndpoint = -1
                var touchedMultipleEndpoints = false

                while (qHead < qTail) {
                    val curr = queue[qHead++]
                    for (nb in neighbors[curr]) {
                        if (isUnassigned[nb]) {
                            if (!visited[nb]) {
                                visited[nb] = true
                                queue[qTail++] = nb
                            }
                        } else {
                            val hp = headPair[nb]
                            val tp = targetPair[nb]
                            if (hp != 0 || tp != 0) {
                                if (lastTouchedEndpoint == -1) {
                                    lastTouchedEndpoint = nb
                                } else if (lastTouchedEndpoint != nb) {
                                    touchedMultipleEndpoints = true
                                }
                            }
                        }
                    }
                }

                if (!touchedMultipleEndpoints) return false
            }

            return true
        }

        fun search() {
            if (solutionCount >= limit) return

            if (uncompletedCount == 0) {
                if (unassignedCount == 0) {
                    solutionCount++
                }
                return
            }

            var bestPair = -1
            var minMoveCount = 5
            var bestTargetMove = false

            for (pair in 1..pairCount) {
                if (isCompleted[pair]) continue
                val hd = head[pair]
                val tgt = targetEndpoint[pair]
                var hasTarget = false
                var uCount = 0

                for (nb in neighbors[hd]) {
                    if (nb == tgt) {
                        hasTarget = true
                    } else if (isUnassigned[nb]) {
                        // Enforce no-self-touch pruning: nb must not touch any other cell of pair
                        var selfTouch = false
                        for (adj in neighbors[nb]) {
                            if (adj != hd && cellColor[adj] == pair) {
                                selfTouch = true
                                break
                            }
                        }
                        if (!selfTouch) {
                            uCount++
                        }
                    }
                }

                val totalMoves = (if (hasTarget) 1 else 0) + uCount
                if (totalMoves == 0) return

                if (totalMoves < minMoveCount) {
                    minMoveCount = totalMoves
                    bestPair = pair
                    bestTargetMove = hasTarget
                    if (totalMoves == 1) break
                }
            }

            if (bestPair == -1) return

            val pair = bestPair
            val hd = head[pair]
            val tgt = targetEndpoint[pair]

            if (bestTargetMove) {
                // Connecting to target endpoint: target must not touch any other cell of pair
                var targetTouchesOther = false
                for (adj in neighbors[tgt]) {
                    if (adj != hd && cellColor[adj] == pair) {
                        targetTouchesOther = true
                        break
                    }
                }
                if (targetTouchesOther) return

                isCompleted[pair] = true
                uncompletedCount--
                headPair[hd] = 0
                targetPair[tgt] = 0
                cellColor[tgt] = pair

                var valid = true
                for (nb in neighbors[tgt]) {
                    if (isUnassigned[nb] && isDeadCell(nb)) {
                        valid = false
                        break
                    }
                }

                if (valid && (uncompletedCount == 0 || checkReachabilityAndIslands())) {
                    search()
                }

                cellColor[tgt] = 0
                targetPair[tgt] = pair
                headPair[hd] = pair
                uncompletedCount++
                isCompleted[pair] = false

                // If target move was available, no unassigned neighbor move is legal
                return
            }

            for (nb in neighbors[hd]) {
                if (!isUnassigned[nb]) continue

                // Enforce no-self-touch pruning: nb must not touch any other cell of pair
                var selfTouch = false
                for (adj in neighbors[nb]) {
                    if (adj != hd && cellColor[adj] == pair) {
                        selfTouch = true
                        break
                    }
                }
                if (selfTouch) continue

                isUnassigned[nb] = false
                unassignedCount--
                headPair[hd] = 0
                headPair[nb] = pair
                head[pair] = nb
                cellColor[nb] = pair

                var valid = true
                for (adj in neighbors[nb]) {
                    if (isUnassigned[adj] && isDeadCell(adj)) {
                        valid = false
                        break
                    }
                }

                if (valid && checkReachabilityAndIslands()) {
                    search()
                }

                cellColor[nb] = 0
                head[pair] = hd
                headPair[nb] = 0
                headPair[hd] = pair
                unassignedCount++
                isUnassigned[nb] = true

                if (solutionCount >= limit) return
            }
        }

        for (i in 0 until cellCount) {
            if (isUnassigned[i] && isDeadCell(i)) return 0
        }
        if (!checkReachabilityAndIslands()) return 0

        search()
        return solutionCount
    }
}
