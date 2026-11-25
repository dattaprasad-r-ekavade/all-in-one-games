package com.datathecodie.witgrid.engine.futoshiki.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiPuzzle
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign

/** Small hand-made Latin squares and edge signs for independent plan checks. */
internal object FutoshikiSpecFixtures {
    fun cyclicSquare(size: Int): List<Int> = List(size * size) { index ->
        (index / size + index % size) % size + 1
    }

    fun packFor(size: Int): Pack = when (size) {
        5 -> Pack.EASY
        6 -> Pack.MEDIUM
        7 -> Pack.HARD
        else -> throw IllegalArgumentException("Unsupported Futoshiki size: $size")
    }

    fun puzzle(
        size: Int,
        solution: List<Int> = cyclicSquare(size),
        givens: List<Int?> = List(size * size) { null },
        horizontal: List<FutoshikiSign> = List(size * (size - 1)) { FutoshikiSign.NONE },
        vertical: List<FutoshikiSign> = List((size - 1) * size) { FutoshikiSign.NONE },
    ): FutoshikiPuzzle = FutoshikiPuzzle(
        id = "FU-SPEC-${size}x$size",
        size = size,
        givens = givens,
        horizontalSigns = horizontal,
        verticalSigns = vertical,
        solution = solution,
        pack = packFor(size),
        storedGrade = "naked-single:1",
    )
}
