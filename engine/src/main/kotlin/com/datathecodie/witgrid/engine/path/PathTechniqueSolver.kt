package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import java.util.ArrayDeque
import java.util.ArrayList

/** Guess-free Path deductions, deterministic hint selection and grading. */
object PathTechniqueSolver {

    /** Returns the easiest next deduction, or null for a solved, mistaken or stuck board. */
    fun nextStep(puzzle: PathPuzzle, state: PathState): HintStep? {
        require(state.puzzle == puzzle) { "Path state belongs to a different puzzle" }
        if (PathRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (PathRules.isSolved(puzzle, state)) return null

        val graphs = buildPairGraphs(puzzle, state)

        endpointExtensionStep(puzzle, state, graphs)?.let { return it }
        forcedNeighbourStep(puzzle, state, graphs)?.let { return it }
        deadEndAvoidanceStep(puzzle, state, graphs)?.let { return it }
        return lastEmptyCellStep(puzzle, state, graphs)
    }

    /** Solves from the givens with the published techniques and returns the hardest-step grade. */
    fun grade(puzzle: PathPuzzle): Grade {
        val pack = validateGradeInput(puzzle)
        require(PathRules.countSolutions(puzzle, limit = 2) == 1) {
            "Path grading requires exactly one solution"
        }
        return gradeByTechniques(puzzle, pack)
    }

    /** Computes the technique grade without a solution-count search; callers validate uniqueness separately. */
    internal fun gradeByTechniques(puzzle: PathPuzzle): Grade =
        gradeByTechniques(puzzle, validateGradeInput(puzzle))

    private fun validateGradeInput(puzzle: PathPuzzle): Pack {
        require(PathRules.isValidSolution(puzzle)) {
            "Path grading requires a valid stored solution"
        }
        require(puzzle.endpoints.indices.all { index ->
            puzzle.endpoints[index] == null || puzzle.endpoints[index] == puzzle.solution[index]
        }) { "Path endpoints must agree with the stored solution" }
        val pack = PathPackBands.packForSize(puzzle.size)
        require(puzzle.pack == pack) { "Path pack must match its board size" }
        return pack
    }

    private fun gradeByTechniques(puzzle: PathPuzzle, pack: Pack): Grade {
        var state = PathState.initial(puzzle)
        val visited = hashSetOf(state)
        var stepCount = 0
        var hardestTechnique: Technique? = null

        while (!PathRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Path puzzle is stuck with the supported techniques"
            }
            val nextState = step.moves.fold(state, PathState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Path hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }

        return Grade(hardestTechnique, stepCount, pack)
    }

    // --- Technique 0: Endpoint Extension ---

    private fun endpointExtensionStep(
        puzzle: PathPuzzle,
        state: PathState,
        graphs: List<PairGraph>,
    ): HintStep? {
        val size = puzzle.size
        for (graph in graphs) {
            if (graph.isComplete) continue
            val pair = graph.pair
            val e1 = graph.startEndpoint
            val e2 = graph.targetEndpoint
            val comp1 = graph.componentOf(e1) ?: continue
            val comp2 = graph.componentOf(e2) ?: continue
            if (comp1 == comp2) continue

            val h1 = comp1.intersect(graph.openHeads.toSet()).firstOrNull() ?: continue
            val h2 = comp2.intersect(graph.openHeads.toSet()).firstOrNull() ?: continue

            // Case 1: h1 and h2 share an empty common neighbor c that connects them as their only route
            val h1Neighbors = orthogonalNeighbors(h1, size).filter { !puzzle.walls[it] && state.digits[it] == null }
            val h2Neighbors = orthogonalNeighbors(h2, size).filter { !puzzle.walls[it] && state.digits[it] == null }
            val common = h1Neighbors.intersect(h2Neighbors.toSet())

            for (c in common) {
                if (isLegalExtension(puzzle, state, h1, pair, c, graph) &&
                    isLegalExtension(puzzle, state, h2, pair, c, graph)
                ) {
                    val manhattan = manhattanDistance(h1, h2, size)
                    if (manhattan == 2 && common.size == 1 && !canReachTarget(puzzle, state, h1, h2, pair, avoidCell = c)) {
                        return HintStep(
                            technique = PathTechniques.ENDPOINT_EXTENSION,
                            cells = immutableList(listOf(Cell(c), Cell(h1), Cell(h2))),
                            explanation = "Endpoints for pair $pair connect through ${cellName(c, size)} as their only route.",
                            moves = immutableList(listOf(Move.SetDigit(Cell(c), pair))),
                        )
                    }
                }
            }

            // Case 2: Endpoint given (degree 0) with a single route to the other endpoint
            if (e1 == h1) {
                val legalFromE1 = h1Neighbors.filter { isLegalExtension(puzzle, state, e1, pair, it, graph) }
                val reachingRoutes = legalFromE1.filter { canReachTarget(puzzle, state, it, e2, pair) }
                if (reachingRoutes.size == 1) {
                    val c = reachingRoutes.single()
                    return HintStep(
                        technique = PathTechniques.ENDPOINT_EXTENSION,
                        cells = immutableList(listOf(Cell(c), Cell(e1))),
                        explanation = "The only route between the endpoints of pair $pair passes through ${cellName(c, size)}.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(c), pair))),
                    )
                }
            }

            if (e2 == h2) {
                val legalFromE2 = h2Neighbors.filter { isLegalExtension(puzzle, state, e2, pair, it, graph) }
                val reachingRoutes = legalFromE2.filter { canReachTarget(puzzle, state, it, e1, pair) }
                if (reachingRoutes.size == 1) {
                    val c = reachingRoutes.single()
                    return HintStep(
                        technique = PathTechniques.ENDPOINT_EXTENSION,
                        cells = immutableList(listOf(Cell(c), Cell(e2))),
                        explanation = "The only route between the endpoints of pair $pair passes through ${cellName(c, size)}.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(c), pair))),
                    )
                }
            }
        }
        return null
    }

    // --- Technique 1: Forced Neighbour ---

    private fun forcedNeighbourStep(
        puzzle: PathPuzzle,
        state: PathState,
        graphs: List<PairGraph>,
    ): HintStep? {
        val size = puzzle.size

        // Subcase 1A: A path head has only one legal continuation
        for (graph in graphs) {
            if (graph.isComplete) continue
            val pair = graph.pair
            for (head in graph.openHeads) {
                val legal = legalContinuations(puzzle, state, head, pair, graph)
                if (legal.size == 1) {
                    val c = legal.single()
                    return HintStep(
                        technique = PathTechniques.FORCED_NEIGHBOUR,
                        cells = immutableList(listOf(Cell(head), Cell(c))),
                        explanation = "The path for pair $pair at ${cellName(head, size)} can only continue to ${cellName(c, size)}.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(c), pair))),
                    )
                }
            }
        }

        // Subcase 1B: Empty cell with only two usable neighbors
        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.walls[index] || state.digits[index] != null) continue
            val usable = usableNeighbors(puzzle, state, index, graphs)
            if (usable.size == 2) {
                for (nb in usable) {
                    val digit = state.digits[nb] ?: continue
                    val graph = graphs[digit - 1]
                    if (nb in graph.openHeads && isLegalExtension(puzzle, state, nb, digit, index, graph)) {
                        return HintStep(
                            technique = PathTechniques.FORCED_NEIGHBOUR,
                            cells = immutableList(listOf(Cell(index), Cell(nb))),
                            explanation = "Empty cell ${cellName(index, size)} has only two usable neighbours and must connect to pair $digit at ${cellName(nb, size)}.",
                            moves = immutableList(listOf(Move.SetDigit(Cell(index), digit))),
                        )
                    }
                }
            }
        }

        // Subcase 1C: Empty cell that only one pair can enter
        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.walls[index] || state.digits[index] != null) continue
            val enteringPairs = ArrayList<Pair<Int, Int>>() // Pair<pair, head>
            for (graph in graphs) {
                if (graph.isComplete) continue
                val pair = graph.pair
                for (head in graph.openHeads) {
                    if (isLegalExtension(puzzle, state, head, pair, index, graph)) {
                        enteringPairs.add(pair to head)
                        break
                    }
                }
            }
            val distinctPairs = enteringPairs.map { it.first }.distinct()
            if (distinctPairs.size == 1) {
                val pair = distinctPairs.single()
                val head = enteringPairs.first { it.first == pair }.second
                return HintStep(
                    technique = PathTechniques.FORCED_NEIGHBOUR,
                    cells = immutableList(listOf(Cell(index), Cell(head))),
                    explanation = "Only pair $pair can enter empty cell ${cellName(index, size)}.",
                    moves = immutableList(listOf(Move.SetDigit(Cell(index), pair))),
                )
            }
        }

        return null
    }

    // --- Technique 2: Dead-End Avoidance ---

    private fun deadEndAvoidanceStep(
        puzzle: PathPuzzle,
        state: PathState,
        graphs: List<PairGraph>,
    ): HintStep? {
        val size = puzzle.size

        for (graph in graphs) {
            if (graph.isComplete) continue
            val pair = graph.pair
            for (head in graph.openHeads) {
                val legal = legalContinuations(puzzle, state, head, pair, graph)
                if (legal.size <= 1) continue

                val nonDeadEndMoves = ArrayList<Int>()
                var deadEndWitnessCell = -1

                for (cand in legal) {
                    val witness = findDeadEndCellIfMoved(puzzle, state, head, pair, cand, graphs)
                    if (witness == null) {
                        nonDeadEndMoves.add(cand)
                    } else if (deadEndWitnessCell == -1) {
                        deadEndWitnessCell = witness
                    }
                }

                if (nonDeadEndMoves.size == 1) {
                    val goodMove = nonDeadEndMoves.single()
                    val witnessName = if (deadEndWitnessCell != -1) cellName(deadEndWitnessCell, size) else "an empty cell"
                    return HintStep(
                        technique = PathTechniques.DEAD_END_AVOIDANCE,
                        cells = immutableList(listOf(Cell(head), Cell(goodMove))),
                        explanation = "Pair $pair at ${cellName(head, size)} must move to ${cellName(goodMove, size)} to avoid leaving $witnessName with fewer than two usable neighbours.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(goodMove), pair))),
                    )
                }
            }
        }

        return null
    }

    // --- Technique 3: Last Empty Cell in a Region ---

    private fun lastEmptyCellStep(
        puzzle: PathPuzzle,
        state: PathState,
        graphs: List<PairGraph>,
    ): HintStep? {
        val size = puzzle.size
        val cellCount = puzzle.grid.cellCount
        val visited = BooleanArray(cellCount)

        for (i in 0 until cellCount) {
            if (puzzle.walls[i] || state.digits[i] != null || visited[i]) continue

            // Find connected component of empty cells
            val region = ArrayList<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(i)
            visited[i] = true

            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                region.add(curr)
                for (nb in orthogonalNeighbors(curr, size)) {
                    if (!puzzle.walls[nb] && state.digits[nb] == null && !visited[nb]) {
                        visited[nb] = true
                        queue.add(nb)
                    }
                }
            }

            // Find which pairs can reach this region
            val reachingPairs = ArrayList<Pair<Int, Int>>() // pair to (head, entryCell)
            val regionSet = region.toSet()

            for (graph in graphs) {
                if (graph.isComplete) continue
                val pair = graph.pair
                for (head in graph.openHeads) {
                    for (nb in orthogonalNeighbors(head, size)) {
                        if (nb in regionSet && isLegalExtension(puzzle, state, head, pair, nb, graph)) {
                            reachingPairs.add(pair to head)
                            break
                        }
                    }
                }
            }

            val distinctPairs = reachingPairs.map { it.first }.distinct()
            if (distinctPairs.size == 1) {
                val pair = distinctPairs.single()
                val head = reachingPairs.first { it.first == pair }.second
                val entryCell = orthogonalNeighbors(head, size).firstOrNull { it in regionSet && isLegalExtension(puzzle, state, head, pair, it, graphForPair(graphs, pair)) }
                    ?: region.first()

                return HintStep(
                    technique = PathTechniques.LAST_EMPTY_CELL,
                    cells = immutableList((listOf(head) + region).distinct().map(::Cell)),
                    explanation = "The empty region containing ${cellName(entryCell, size)} can only be reached by pair $pair, so it must take that colour.",
                    moves = immutableList(listOf(Move.SetDigit(Cell(entryCell), pair))),
                )
            }
        }

        return null
    }

    // --- Graph and Connectivity Helpers ---

    private class PairGraph(
        val pair: Int,
        val startEndpoint: Int,
        val targetEndpoint: Int,
        val components: List<Set<Int>>,
        val openHeads: List<Int>,
        val isComplete: Boolean,
    ) {
        fun componentOf(cell: Int): Set<Int>? = components.firstOrNull { cell in it }
    }

    private fun graphForPair(graphs: List<PairGraph>, pair: Int): PairGraph = graphs[pair - 1]

    private fun buildPairGraphs(puzzle: PathPuzzle, state: PathState): List<PairGraph> {
        val size = puzzle.size
        val cellCount = puzzle.grid.cellCount
        val pairCount = puzzle.pairCount

        val startEndpoints = IntArray(pairCount + 1)
        val targetEndpoints = IntArray(pairCount + 1)

        for (pair in 1..pairCount) {
            var first = -1
            for (i in 0 until cellCount) {
                if (puzzle.endpoints[i] == pair) {
                    if (first == -1) {
                        first = i
                    } else {
                        startEndpoints[pair] = first
                        targetEndpoints[pair] = i
                        break
                    }
                }
            }
        }

        return (1..pairCount).map { pair ->
            val start = startEndpoints[pair]
            val target = targetEndpoints[pair]
            val cells = (0 until cellCount).filter { state.digits[it] == pair }

            val visited = BooleanArray(cellCount)
            val components = ArrayList<Set<Int>>()
            val openHeads = ArrayList<Int>()

            for (cell in cells) {
                val sameColorNbs = orthogonalNeighbors(cell, size).filter { state.digits[it] == pair }
                val degree = sameColorNbs.size
                val expectedDegree = if (cell == start || cell == target) 1 else 2
                if (degree < expectedDegree) {
                    openHeads.add(cell)
                }

                if (!visited[cell]) {
                    val comp = HashSet<Int>()
                    val queue = ArrayDeque<Int>()
                    queue.add(cell)
                    visited[cell] = true

                    while (queue.isNotEmpty()) {
                        val curr = queue.removeFirst()
                        comp.add(curr)
                        for (nb in orthogonalNeighbors(curr, size)) {
                            if (state.digits[nb] == pair && !visited[nb]) {
                                visited[nb] = true
                                queue.add(nb)
                            }
                        }
                    }
                    components.add(comp)
                }
            }

            val startComp = components.firstOrNull { start in it }
            val isComplete = startComp != null && target in startComp && openHeads.isEmpty()

            PairGraph(
                pair = pair,
                startEndpoint = start,
                targetEndpoint = target,
                components = components,
                openHeads = openHeads,
                isComplete = isComplete,
            )
        }
    }

    private fun legalContinuations(
        puzzle: PathPuzzle,
        state: PathState,
        head: Int,
        pair: Int,
        graph: PairGraph,
    ): List<Int> {
        val size = puzzle.size
        val neighbors = orthogonalNeighbors(head, size)
        return neighbors.filter { isLegalExtension(puzzle, state, head, pair, it, graph) }
    }

    private fun isLegalExtension(
        puzzle: PathPuzzle,
        state: PathState,
        head: Int,
        pair: Int,
        cand: Int,
        graph: PairGraph,
    ): Boolean {
        if (puzzle.walls[cand] || state.digits[cand] != null) return false
        val size = puzzle.size

        val pNeighbors = orthogonalNeighbors(cand, size).filter { state.digits[it] == pair }
        if (pNeighbors.size > 2) return false

        val headComp = graph.componentOf(head) ?: return false

        if (pNeighbors.size == 2) {
            val other = pNeighbors.first { it != head }
            if (other !in graph.openHeads) return false
            val otherComp = graph.componentOf(other) ?: return false

            if (otherComp == headComp) {
                // Cycle: connecting two cells of same component is illegal
                return false
            }
        } else {
            // pNeighbors.size == 1 (only head)
            // cand will have degree 1 in pair; it must have at least one usable neighbor to exit through
            val exitCandidateCount = orthogonalNeighbors(cand, size).count { exit ->
                exit != head && !puzzle.walls[exit] && (state.digits[exit] == null || (state.digits[exit] == pair && exit in graph.openHeads && graph.componentOf(exit) != headComp))
            }
            if (exitCandidateCount == 0) return false
        }

        // Reachability: can the endpoints still connect?
        val otherEndpoint = if (headComp.contains(graph.startEndpoint)) graph.targetEndpoint else graph.startEndpoint
        val otherComp = graph.componentOf(otherEndpoint)
        if (otherComp != null && otherComp == headComp) {
            // Both endpoints already in same component, shouldn't happen for incomplete pair
            return false
        }

        return canReachTarget(puzzle, state, cand, otherEndpoint, pair)
    }

    private fun canReachTarget(
        puzzle: PathPuzzle,
        state: PathState,
        fromCell: Int,
        targetCell: Int,
        pair: Int,
        avoidCell: Int = -1,
    ): Boolean {
        val size = puzzle.size
        val cellCount = puzzle.grid.cellCount
        val visited = BooleanArray(cellCount)
        val queue = ArrayDeque<Int>()

        queue.add(fromCell)
        visited[fromCell] = true

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if (curr == targetCell) return true

            for (nb in orthogonalNeighbors(curr, size)) {
                if (visited[nb] || puzzle.walls[nb] || nb == avoidCell) continue
                val digit = state.digits[nb]
                if (digit == null || digit == pair) {
                    visited[nb] = true
                    queue.add(nb)
                }
            }
        }

        return false
    }

    private fun usableNeighbors(
        puzzle: PathPuzzle,
        state: PathState,
        cell: Int,
        graphs: List<PairGraph>,
    ): List<Int> {
        val size = puzzle.size
        val neighbors = orthogonalNeighbors(cell, size)
        val result = ArrayList<Int>(4)

        for (nb in neighbors) {
            if (puzzle.walls[nb]) continue
            val digit = state.digits[nb]
            if (digit == null) {
                result.add(nb)
            } else {
                val graph = graphs[digit - 1]
                if (nb in graph.openHeads && isLegalExtension(puzzle, state, nb, digit, cell, graph)) {
                    result.add(nb)
                }
            }
        }

        return result
    }

    private fun findDeadEndCellIfMoved(
        puzzle: PathPuzzle,
        state: PathState,
        head: Int,
        pair: Int,
        cand: Int,
        graphs: List<PairGraph>,
    ): Int? {
        val size = puzzle.size
        val neighborsOfCand = orthogonalNeighbors(cand, size)

        for (nb in neighborsOfCand) {
            if (nb == head || puzzle.walls[nb] || state.digits[nb] != null) continue

            // Count usable neighbors of nb if cand were set to pair
            var usableCount = 0
            for (adj in orthogonalNeighbors(nb, size)) {
                if (puzzle.walls[adj]) continue
                if (adj == cand) {
                    // Can cand be used by nb? Only if cand still needs another neighbor after connecting to head
                    val pNeighborsOfCand = orthogonalNeighbors(cand, size).filter { it == head || state.digits[it] == pair }
                    if (pNeighborsOfCand.size < 2) {
                        usableCount++
                    }
                } else if (state.digits[adj] == null) {
                    usableCount++
                } else {
                    val digit = state.digits[adj]!!
                    val graph = graphs[digit - 1]
                    if (adj in graph.openHeads) {
                        usableCount++
                    }
                }
            }

            if (usableCount < 2) {
                return nb
            }
        }

        return null
    }

    private fun manhattanDistance(a: Int, b: Int, size: Int): Int {
        val ra = a / size
        val ca = a % size
        val rb = b / size
        val cb = b % size
        return Math.abs(ra - rb) + Math.abs(ca - cb)
    }

    private fun orthogonalNeighbors(index: Int, size: Int): IntArray {
        val row = index / size
        val col = index % size
        val list = ArrayList<Int>(4)
        if (row > 0) list.add((row - 1) * size + col)
        if (row < size - 1) list.add((row + 1) * size + col)
        if (col > 0) list.add(row * size + col - 1)
        if (col < size - 1) list.add(row * size + col + 1)
        return list.toIntArray()
    }

    private fun cellName(index: Int, size: Int): String =
        "r${index / size + 1}c${index % size + 1}"

    private fun <T> immutableList(values: List<T>): List<T> =
        java.util.Collections.unmodifiableList(ArrayList(values))
}
