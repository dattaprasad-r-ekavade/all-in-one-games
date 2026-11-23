package com.datathecodie.witgrid.engine.coloursort

/**
 * Breadth-first search solver for Colour Sort.
 *
 * Explores legal forward pours, pruning tube permutations and redundant moves into
 * empty tubes. Returns the shortest sequence of [ColourSortAction]s to reach a won state,
 * or null if no winning path exists.
 */
object ColourSortSolver {
    private const val MAX_EXPLORED_STATES = 100_000

    /** Solves [tubes], returning the shortest action sequence to win, or null if unsolvable. */
    fun solve(tubes: List<List<Int>>): List<ColourSortAction>? {
        if (ColourSortRules.isWon(tubes)) return emptyList()
        if (ColourSortRules.legalPours(tubes).isEmpty()) return null

        val queue = ArrayDeque<SearchNode>()
        val visited = HashSet<CanonicalStateKey>()

        val startKey = canonicalKey(tubes)
        visited.add(startKey)
        queue.add(SearchNode(tubes, null, null))

        var explored = 0
        while (queue.isNotEmpty() && explored < MAX_EXPLORED_STATES) {
            explored++
            val current = queue.removeFirst()
            val currentTubes = current.tubes

            for (from in currentTubes.indices) {
                val source = currentTubes[from]
                if (source.isEmpty()) continue

                val sourceIsPure = source.all { it == source[0] }
                val sourceTop = source.last()

                var seenEmptyDestination = false

                for (to in currentTubes.indices) {
                    if (from == to) continue
                    val destination = currentTubes[to]
                    if (destination.size == ColourSortRules.TUBE_CAPACITY) continue

                    if (destination.isEmpty()) {
                        if (seenEmptyDestination) continue
                        seenEmptyDestination = true
                        if (sourceIsPure) continue
                    } else {
                        if (destination.last() != sourceTop) continue
                    }

                    val nextTubes = ColourSortRules.applyPour(currentTubes, from, to)
                    val action = ColourSortAction(from, to)

                    if (ColourSortRules.isWon(nextTubes)) {
                        return reconstructPath(current, action)
                    }

                    val key = canonicalKey(nextTubes)
                    if (visited.add(key)) {
                        queue.add(SearchNode(nextTubes, current, action))
                    }
                }
            }
        }

        return null
    }

    /** Returns true if [tubes] can be solved from its current position. */
    fun isWinnable(tubes: List<List<Int>>): Boolean = solve(tubes) != null

    private fun reconstructPath(node: SearchNode, lastAction: ColourSortAction): List<ColourSortAction> {
        val path = ArrayList<ColourSortAction>()
        path.add(lastAction)
        var current: SearchNode? = node
        while (current?.action != null) {
            path.add(current.action)
            current = current.parent
        }
        path.reverse()
        return path
    }

    private class SearchNode(
        val tubes: List<List<Int>>,
        val parent: SearchNode?,
        val action: ColourSortAction?,
    )

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
