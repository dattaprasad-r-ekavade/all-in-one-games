package com.datathecodie.witgrid.engine.coloursort

object ColourSortRules {
    const val TUBE_CAPACITY = 4

    fun topColour(tube: List<Int>): Int? = tube.lastOrNull()

    fun topRunLength(tube: List<Int>): Int {
        if (tube.isEmpty()) return 0
        val top = tube.last()
        var run = 0
        for (index in tube.indices.reversed()) {
            if (tube[index] != top) break
            run++
        }
        return run
    }

    fun freeSpace(tube: List<Int>): Int = TUBE_CAPACITY - tube.size

    fun isPourLegal(tubes: List<List<Int>>, from: Int, to: Int): Boolean {
        if (from !in tubes.indices || to !in tubes.indices) return false
        if (from == to) return false
        val source = tubes[from]
        val dest = tubes[to]
        if (source.isEmpty()) return false
        val space = freeSpace(dest)
        if (space == 0) return false
        val sourceTop = topColour(source) ?: return false
        val destTop = topColour(dest)
        if (destTop != null && destTop != sourceTop) return false
        return true
    }

    fun pourUnits(tubes: List<List<Int>>, from: Int, to: Int): Int {
        val space = freeSpace(tubes[to])
        val run = topRunLength(tubes[from])
        return minOf(run, space)
    }

    fun applyPour(tubes: List<List<Int>>, from: Int, to: Int): List<List<Int>> {
        require(isPourLegal(tubes, from, to)) { "Illegal pour from $from to $to" }
        val units = pourUnits(tubes, from, to)
        require(units > 0) { "Nothing to pour from $from to $to" }
        val sourceTop = topColour(tubes[from])!!
        val next = tubes.map { it.toMutableList() }
        repeat(units) {
            next[from].removeAt(next[from].lastIndex)
            next[to].add(sourceTop)
        }
        return next.map { it.toList() }
    }

    fun legalPours(tubes: List<List<Int>>): List<ColourSortAction> {
        val actions = ArrayList<ColourSortAction>()
        for (from in tubes.indices) {
            for (to in tubes.indices) {
                if (isPourLegal(tubes, from, to)) {
                    actions += ColourSortAction(from, to)
                }
            }
        }
        return actions
    }

    fun isTubeSorted(tube: List<Int>): Boolean {
        if (tube.isEmpty()) return true
        if (tube.size > TUBE_CAPACITY) return false
        val colour = tube.first()
        return tube.all { it == colour }
    }

    fun isWon(tubes: List<List<Int>>): Boolean = tubes.all { isTubeSorted(it) }

    fun isStuckAfterPlay(tubes: List<List<Int>>, pourCount: Int): Boolean =
        pourCount > 0 && !isWon(tubes) && legalPours(tubes).isEmpty()
}