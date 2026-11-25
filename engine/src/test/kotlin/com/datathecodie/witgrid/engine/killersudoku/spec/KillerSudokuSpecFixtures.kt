package com.datathecodie.witgrid.engine.killersudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.killersudoku.KillerCage
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle

/**
 * Hand-made Killer Sudoku fixtures for the plan 12.7 spec tests.
 *
 * The solution is a standard valid Sudoku grid. Each cage is one horizontal piece of a row,
 * so its digits never repeat. Every cage sum was added by hand from [SOLUTION_TEXT].
 */
object KillerSudokuSpecFixtures {
    const val SOLUTION_TEXT =
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

    /** Bank cage payload for [baseCages], listed by lowest cell index. */
    const val CAGE_TEXT =
        "8:0,1;17:2,3,4;20:5,6,7,8;" +
            "6:9;10:10,11,12;29:13,14,15,16,17;" +
            "18:18,19,20;7:21,22;20:23,24,25,26;" +
            "13:27,28;23:29,30,31,32;9:33,34,35;" +
            "4:36;16:37,38,39;25:40,41,42,43,44;" +
            "11:45,46,47;11:48,49;23:50,51,52,53;" +
            "15:54,55;9:56,57,58;21:59,60,61,62;" +
            "17:63,64,65;5:66,67;23:68,69,70,71;" +
            "3:72;11:73,74,75;31:76,77,78,79,80"

    /** Row-major digits of the stored solution. */
    val solution: List<Int> = SOLUTION_TEXT.map { it.digitToInt() }

    /** Twenty-seven horizontal cages that partition the board. */
    val baseCages: List<KillerCage> = listOf(
        cage(8, 0..1), cage(17, 2..4), cage(20, 5..8),
        cage(6, 9..9), cage(10, 10..12), cage(29, 13..17),
        cage(18, 18..20), cage(7, 21..22), cage(20, 23..26),
        cage(13, 27..28), cage(23, 29..32), cage(9, 33..35),
        cage(4, 36..36), cage(16, 37..39), cage(25, 40..44),
        cage(11, 45..47), cage(11, 48..49), cage(23, 50..53),
        cage(15, 54..55), cage(9, 56..58), cage(21, 59..62),
        cage(17, 63..65), cage(5, 66..67), cage(23, 68..71),
        cage(3, 72..72), cage(11, 73..75), cage(31, 76..80),
    )

    /** Nine row cages of sum 45. Any Sudoku grid satisfies them, so many solutions exist. */
    val rowCages: List<KillerCage> = (0 until 9).map { row ->
        cage(45, row * 9..row * 9 + 8)
    }

    /**
     * [baseCages] with cells 0 and 28 in one cage. Both hold digit 5, in different rows,
     * columns and boxes. The cage sum 10 is correct, so only the distinct-digit rule fails.
     */
    val repeatedDigitCages: List<KillerCage> = baseCages.toMutableList().apply {
        this[0] = cage(3, 1..1)
        this[9] = cage(8, 27..27)
        add(KillerCage(listOf(Cell(0), Cell(28)), sum = 10))
    }

    val emptyGivens: List<Int?> = List(81) { null }

    /** Builds a fixture puzzle with no givens and [baseCages] unless told otherwise. */
    fun puzzle(
        givens: List<Int?> = emptyGivens,
        cages: List<KillerCage> = baseCages,
    ): KillerSudokuPuzzle = KillerSudokuPuzzle(
        id = "KS-SPEC-9",
        givens = givens,
        cages = cages,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "hand",
    )

    /** Returns whether each row, column and 3x3 box holds digits 1 through 9 once. */
    fun isSudokuGrid(values: List<Int>): Boolean {
        val digits = (1..9).toList()
        val rows = (0 until 9).map { row -> (0 until 9).map { column -> values[row * 9 + column] } }
        val columns = (0 until 9).map { column -> (0 until 9).map { row -> values[row * 9 + column] } }
        val boxes = (0 until 9).map { box ->
            (0 until 9).map { offset ->
                val row = (box / 3) * 3 + offset / 3
                val column = (box % 3) * 3 + offset % 3
                values[row * 9 + column]
            }
        }
        return (rows + columns + boxes).all { house -> house.sorted() == digits }
    }

    private fun cage(sum: Int, cells: IntRange): KillerCage = KillerCage(cells.map(::Cell), sum)
}
