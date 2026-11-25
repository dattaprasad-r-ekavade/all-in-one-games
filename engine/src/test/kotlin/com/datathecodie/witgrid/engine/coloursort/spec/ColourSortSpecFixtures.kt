package com.datathecodie.witgrid.engine.coloursort.spec

import com.datathecodie.witgrid.engine.coloursort.ColourSortPuzzle
import com.datathecodie.witgrid.engine.core.Pack

/**
 * Hand-made Colour Sort boards. Each tube is bottom-first; capacity is 4 (CS1 lead note).
 * Expected outcomes sit next to the stacks so a reader can re-check them without the engine.
 */
internal object ColourSortSpecFixtures {

    /** Plan 12.2 / CS1: every tube holds at most four units. */
    const val CAPACITY = 4

    /**
     * Easy, 4 tubes, two empty. Colour 1 and colour 2, four units each.
     * Top of tube 0 is a run of two 2s; pouring 0→2 onto empty moves that whole run
     * and leaves [1,1] | [1,1,2,2] | [2,2] | [].
     */
    val twoEmptyDeal: List<List<Int>> = listOf(
        listOf(1, 1, 2, 2),
        listOf(1, 1, 2, 2),
        emptyList(),
        emptyList(),
    )

    /** After pouring the top run of tube 0 onto empty tube 2. Not won: tube 1 still mixed. */
    val twoEmptyAfterFirstPour: List<List<Int>> = listOf(
        listOf(1, 1),
        listOf(1, 1, 2, 2),
        listOf(2, 2),
        emptyList(),
    )

    /**
     * Easy, 4 tubes, one empty. Tube 0 is mixed (1s under a 2), so the deal is not won.
     * Pour 0→2: dest top matches colour 2, dest has space 1, source top run length 1 →
     * [1,1] | [1,1] | [2,2,2,2] | []. Every tube empty or one colour: won.
     */
    val onePourFromWin: List<List<Int>> = listOf(
        listOf(1, 1, 2),
        listOf(1, 1),
        listOf(2, 2, 2),
        emptyList(),
    )

    val wonSingleColourOrEmpty: List<List<Int>> = listOf(
        listOf(1, 1, 1, 1),
        listOf(2, 2, 2, 2),
        emptyList(),
        emptyList(),
    )

    /**
     * Won even though colour 2 does not fill the tube. Plan 12.2: empty or a single colour,
     * not "must be full".
     */
    val wonPartialSingleColour: List<List<Int>> = listOf(
        listOf(1, 1, 1, 1),
        listOf(2, 2),
        emptyList(),
        emptyList(),
    )

    /**
     * Easy, 4 tubes, one empty. Tube 0 top run is three 1s; dest 2 has matching top and
     * space 2, so only two of those 1s fit.
     * Pour 0→2 → [2, 1] | [2, 2] | [2, 1, 1, 1] | [].
     */
    val partialTopRunFits: List<List<Int>> = listOf(
        listOf(2, 1, 1, 1),
        listOf(2, 2),
        listOf(2, 1),
        emptyList(),
    )

    val afterPartialTopRunFits: List<List<Int>> = listOf(
        listOf(2, 1),
        listOf(2, 2),
        listOf(2, 1, 1, 1),
        emptyList(),
    )

    /**
     * Easy, 4 tubes, one empty. Dest 0 already has a 1 and space 2; source 1 top run is
     * two 1s, so the whole run fits.
     * Pour 1→0 → [1,1,1,1] | [2,2] | [2,2] | []. Won.
     */
    val wholeTopRunOntoMatch: List<List<Int>> = listOf(
        listOf(1, 1),
        listOf(2, 2, 1, 1),
        listOf(2, 2),
        emptyList(),
    )

    /**
     * Predecessor of a deadlock. One empty tube, so a pour is legal and [isLost] is false
     * on the deal (PlayEngine / CS1). Colour counts: four 1s, four 2s, four 3s.
     *
     * Pour 2→3 (top run of one 1 onto empty) yields [deadlockAfterPour]: every tube
     * non-empty, tops 3 / 3 / 2 / 1, the two 3s sit on full tubes, so no legal pour remains
     * and the board is not won.
     */
    val deadlockPredecessor: List<List<Int>> = listOf(
        listOf(1, 1, 2, 3),
        listOf(2, 2, 1, 3),
        listOf(3, 3, 2, 1),
        emptyList(),
    )

    val deadlockAfterPour: List<List<Int>> = listOf(
        listOf(1, 1, 2, 3),
        listOf(2, 2, 1, 3),
        listOf(3, 3, 2),
        listOf(1),
    )

    /** Dest full of colour 1; a matching 1 cannot pour because there is no space. */
    val fullMatchingDest: List<List<Int>> = listOf(
        listOf(1, 1, 1, 1),
        listOf(1),
        listOf(2, 2, 2),
        emptyList(),
    )

    fun easyPuzzle(bankId: String, tubes: List<List<Int>>): ColourSortPuzzle {
        require(tubes.size == 4) { "Easy pack is 4 tubes (plan 12.2)" }
        return ColourSortPuzzle(
            bankId = bankId,
            pack = Pack.EASY,
            difficulty = "easy",
            initialTubes = tubes.map { it.toList() },
        )
    }

    fun isWon(tubes: List<List<Int>>): Boolean =
        tubes.all { tube -> tube.isEmpty() || tube.all { unit -> unit == tube.first() } }

    fun colourRuns(tubes: List<List<Int>>): Int =
        tubes.sumOf { tube ->
            if (tube.isEmpty()) {
                0
            } else {
                1 + tube.zipWithNext().count { (lower, upper) -> lower != upper }
            }
        }

    fun topRunLength(tube: List<Int>): Int {
        if (tube.isEmpty()) return 0
        val colour = tube.last()
        var length = 0
        for (index in tube.lastIndex downTo 0) {
            if (tube[index] != colour) break
            length++
        }
        return length
    }

    /**
     * Plan 12.2: legal when the source top colour matches the dest top colour, or the dest
     * is empty, and the dest has space. Source must be non-empty; from and to distinct.
     */
    fun isLegalPour(tubes: List<List<Int>>, from: Int, to: Int): Boolean {
        if (from == to) return false
        if (from !in tubes.indices || to !in tubes.indices) return false
        val source = tubes[from]
        val dest = tubes[to]
        if (source.isEmpty()) return false
        if (dest.size >= CAPACITY) return false
        val destTop = dest.lastOrNull()
        return destTop == null || destTop == source.last()
    }

    fun hasLegalPour(tubes: List<List<Int>>): Boolean {
        for (from in tubes.indices) {
            for (to in tubes.indices) {
                if (isLegalPour(tubes, from, to)) return true
            }
        }
        return false
    }

    /** Moves the whole top run, or only as many units as dest space allows. */
    fun pouredTubes(tubes: List<List<Int>>, from: Int, to: Int): List<List<Int>> {
        require(isLegalPour(tubes, from, to))
        val source = tubes[from].toMutableList()
        val dest = tubes[to].toMutableList()
        val colour = source.last()
        val moving = minOf(topRunLength(source), CAPACITY - dest.size)
        repeat(moving) {
            source.removeAt(source.lastIndex)
            dest.add(colour)
        }
        return tubes.mapIndexed { index, tube ->
            when (index) {
                from -> source.toList()
                to -> dest.toList()
                else -> tube
            }
        }
    }
}
