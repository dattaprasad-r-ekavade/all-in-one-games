package com.datathecodie.witgrid.engine.coloursort

import java.util.PriorityQueue

/**
 * A* search solver for Colour Sort with an admissible heuristic.
 *
 * Explores legal forward pours using an admissible heuristic:
 *   h = max(boundaries, spread)
 * where boundaries is the number of colour changes across all tubes,
 * and spread is the sum of (tubes containing colour - 1) across all colours.
 *
 * Canonical state keys are used for the closed set and distance tracking,
 * pruning tube permutations and redundant moves into empty tubes.
 *
 * Returns a shortest action sequence to reach a won state, or null if unsolvable.
 */
object ColourSortSolver {
    const val MAX_EXPLORED_STATES = 150_000

    var maxExploredNodesSeen: Int = 0
        internal set

    var lastExploredNodes: Int = 0
        internal set

    /** Solves [tubes], returning the shortest action sequence to win, or null if unsolvable. */
    fun solve(tubes: List<List<Int>>): List<ColourSortAction>? {
        if (ColourSortRules.isWon(tubes)) return emptyList()
        if (ColourSortRules.legalPours(tubes).isEmpty()) return null

        val startKey = canonicalKey(tubes)
        val startH = heuristic(tubes)

        val openSet = PriorityQueue<AStarNode>()
        val gScore = HashMap<CanonicalStateKey, Int>()

        var insertionCounter = 0L
        val startNode = AStarNode(
            tubes = tubes,
            key = startKey,
            parent = null,
            action = null,
            g = 0,
            h = startH,
            insertionOrder = insertionCounter++,
        )

        openSet.add(startNode)
        gScore[startKey] = 0

        var explored = 0
        while (openSet.isNotEmpty() && explored < MAX_EXPLORED_STATES) {
            val current = openSet.poll() ?: break

            // Skip if we already reached this canonical state with a strictly smaller g
            val recordedG = gScore[current.key]
            if (recordedG != null && current.g > recordedG) {
                continue
            }

            explored++

            if (ColourSortRules.isWon(current.tubes)) {
                lastExploredNodes = explored
                if (explored > maxExploredNodesSeen) {
                    maxExploredNodesSeen = explored
                }
                return reconstructPath(current)
            }

            val moves = legalMoves(current.tubes, current.action)
            val nextG = current.g + 1

            for (action in moves) {
                val nextTubes = ColourSortRules.applyPour(current.tubes, action.from, action.to)
                val nextKey = canonicalKey(nextTubes)

                val prevG = gScore[nextKey]
                if (prevG != null && nextG >= prevG) {
                    continue
                }

                gScore[nextKey] = nextG
                val nextH = heuristic(nextTubes)

                val nextNode = AStarNode(
                    tubes = nextTubes,
                    key = nextKey,
                    parent = current,
                    action = action,
                    g = nextG,
                    h = nextH,
                    insertionOrder = insertionCounter++,
                )
                openSet.add(nextNode)
            }
        }

        lastExploredNodes = explored
        if (explored > maxExploredNodesSeen) {
            maxExploredNodesSeen = explored
        }
        return null
    }

    /** Returns true if [tubes] can be solved from its current position. */
    fun isWinnable(tubes: List<List<Int>>): Boolean = solve(tubes) != null

    private fun reconstructPath(goalNode: AStarNode): List<ColourSortAction> {
        val path = ArrayList<ColourSortAction>(goalNode.g)
        var curr: AStarNode? = goalNode
        while (curr?.parent != null) {
            path.add(curr.action!!)
            curr = curr.parent
        }
        path.reverse()
        return path
    }

    internal fun heuristic(tubes: List<List<Int>>): Int {
        var boundaries = 0
        var maxColour = 0
        for (tube in tubes) {
            for (colour in tube) {
                if (colour > maxColour) maxColour = colour
            }
        }
        val colourTubeCounts = IntArray(maxColour + 1)

        for (tube in tubes) {
            if (tube.isEmpty()) continue
            for (i in 0 until tube.size - 1) {
                if (tube[i] != tube[i + 1]) {
                    boundaries++
                }
            }
            for (i in tube.indices) {
                val colour = tube[i]
                var seenEarlierInTube = false
                for (j in 0 until i) {
                    if (tube[j] == colour) {
                        seenEarlierInTube = true
                        break
                    }
                }
                if (!seenEarlierInTube) {
                    colourTubeCounts[colour]++
                }
            }
        }

        var spread = 0
        for (c in 1..maxColour) {
            if (colourTubeCounts[c] > 1) {
                spread += (colourTubeCounts[c] - 1)
            }
        }

        return maxOf(boundaries, spread)
    }

    internal fun legalMoves(
        tubes: List<List<Int>>,
        lastAction: ColourSortAction? = null,
    ): List<ColourSortAction> {
        val actions = ArrayList<ColourSortAction>()
        val firstEmptyTube = tubes.indexOfFirst { it.isEmpty() }

        for (from in tubes.indices) {
            val source = tubes[from]
            if (source.isEmpty()) continue

            val sourceIsPure = source.all { it == source[0] }
            // Never disturb a fully sorted tube
            if (source.size == ColourSortRules.TUBE_CAPACITY && sourceIsPure) continue

            val sourceTop = source.last()

            for (to in tubes.indices) {
                if (from == to) continue
                // Don't immediately undo the previous pour
                if (lastAction != null && from == lastAction.to && to == lastAction.from) continue

                val dest = tubes[to]
                if (dest.size == ColourSortRules.TUBE_CAPACITY) continue

                if (dest.isEmpty()) {
                    // Only pour into the first empty tube
                    if (to != firstEmptyTube) continue
                    // Never pour a pure tube into an empty tube
                    if (sourceIsPure) continue
                } else {
                    if (dest.last() != sourceTop) continue
                }

                actions.add(ColourSortAction(from, to))
            }
        }
        return actions
    }

    private class AStarNode(
        val tubes: List<List<Int>>,
        val key: CanonicalStateKey,
        val parent: AStarNode?,
        val action: ColourSortAction?,
        val g: Int,
        val h: Int,
        val insertionOrder: Long,
    ) : Comparable<AStarNode> {
        val f: Int = g + h

        override fun compareTo(other: AStarNode): Int {
            val fComp = this.f.compareTo(other.f)
            if (fComp != 0) return fComp
            // Tie-breaker 1: smaller h (closer to goal, i.e. larger g)
            val hComp = this.h.compareTo(other.h)
            if (hComp != 0) return hComp
            // Tie-breaker 2: insertion order
            return this.insertionOrder.compareTo(other.insertionOrder)
        }
    }

    internal data class CanonicalStateKey(val hi: Long, val mid: Long, val lo: Long)

    internal fun canonicalKey(tubes: List<List<Int>>): CanonicalStateKey {
        val codes = IntArray(tubes.size) { encodeTube(tubes[it]) }
        codes.sort()
        var hi = 0L
        var mid = 0L
        var lo = 0L
        for (i in 0 until minOf(4, codes.size)) {
            hi = (hi shl 16) or (codes[i].toLong() and 0xFFFFL)
        }
        for (i in 4 until minOf(8, codes.size)) {
            mid = (mid shl 16) or (codes[i].toLong() and 0xFFFFL)
        }
        for (i in 8 until minOf(12, codes.size)) {
            lo = (lo shl 16) or (codes[i].toLong() and 0xFFFFL)
        }
        return CanonicalStateKey(hi, mid, lo)
    }

    private fun encodeTube(tube: List<Int>): Int {
        var code = 0
        for (i in 0 until ColourSortRules.TUBE_CAPACITY) {
            val colour = if (i < tube.size) tube[i] else 0
            code = (code shl 4) or colour
        }
        return code
    }
}
