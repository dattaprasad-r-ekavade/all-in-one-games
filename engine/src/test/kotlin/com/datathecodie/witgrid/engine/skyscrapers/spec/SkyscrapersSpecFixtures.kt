package com.datathecodie.witgrid.engine.skyscrapers.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle

/** Visibility clues on the four edges of a square board, in plan order. */
internal data class EdgeClues(
    val top: List<Int?>,
    val bottom: List<Int?>,
    val left: List<Int?>,
    val right: List<Int?>,
) {
    companion object {
        /** Returns clues that are all empty for a board of [size]. */
        fun empty(size: Int): EdgeClues = SkyscrapersSpecFixtures.let { EdgeClues(it.nulls(size), it.nulls(size), it.nulls(size), it.nulls(size)) }
    }
}

/** Hand-made Skyscrapers fixtures. Each clue list was counted by hand from the visibility rule. */
internal object SkyscrapersSpecFixtures {

    /** 4x4 Latin square in row-major order. Rows: 1234, 2143, 3412, 4321. */
    val solution4: List<Int> = listOf(
        1, 2, 3, 4,
        2, 1, 4, 3,
        3, 4, 1, 2,
        4, 3, 2, 1,
    )

    /** Every clue for [solution4]. Only the answer 3412 in row 2 fits all sixteen clues. */
    val fullClues4 = EdgeClues(
        top = listOf(4, 2, 2, 1),
        bottom = listOf(1, 2, 2, 4),
        left = listOf(4, 2, 2, 1),
        right = listOf(1, 2, 2, 4),
    )

    /** Givens for rows 1 and 2 of [solution4]. Rows 3 and 4 stay empty. */
    val topTwoRowGivens4: List<Int?> = List(16) { index -> if (index < 8) solution4[index] else null }

    /** Returns [count] empty entries. */
    fun nulls(count: Int): List<Int?> = List(count) { null }

    /** Returns the pack that the plan assigns to a board size. */
    fun packFor(size: Int): Pack = when (size) {
        4 -> Pack.EASY
        5 -> Pack.MEDIUM
        6 -> Pack.HARD
        7 -> Pack.EXPERT
        else -> throw IllegalArgumentException("No plan pack for size $size")
    }

    /** Returns a valid Latin square of [size] with digit `(row + column) mod size + 1`. */
    fun cyclicSquare(size: Int): List<Int> = List(size * size) { index ->
        (index / size + index % size) % size + 1
    }

    /** Returns the number of buildings seen from the first end of [line], counting each taller building once. */
    fun visibleFrom(line: List<Int>): Int {
        var tallest = 0
        var seen = 0
        for (height in line) {
            if (height > tallest) {
                tallest = height
                seen++
            }
        }
        return seen
    }

    /** Returns the full set of edge clues that [solution] (row-major, [size] wide) implies. */
    fun fullClues(size: Int, solution: List<Int>): EdgeClues {
        val rows = solution.chunked(size)
        val columns = List(size) { column -> rows.map { it[column] } }
        return EdgeClues(
            top = columns.map(::visibleFrom),
            bottom = columns.map { visibleFrom(it.asReversed()) },
            left = rows.map(::visibleFrom),
            right = rows.map { visibleFrom(it.asReversed()) },
        )
    }

    /** Builds a puzzle for [size] with the plan pack for that size. */
    fun puzzle(
        size: Int,
        solution: List<Int>,
        clues: EdgeClues = EdgeClues.empty(size),
        givens: List<Int?> = nulls(size * size),
        id: String = "SK-SPEC-${size}x$size",
    ): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = id,
        size = size,
        topClues = clues.top,
        bottomClues = clues.bottom,
        leftClues = clues.left,
        rightClues = clues.right,
        solution = solution,
        pack = packFor(size),
        storedGrade = "none:0",
        givens = givens,
    )
}
