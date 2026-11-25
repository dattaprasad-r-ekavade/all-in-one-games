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

    /**
     * Unique Easy board whose first hint is a cage-combination placement.
     *
     * Hand check:
     * - [solution] is a valid Sudoku (each row, column and box holds 1-9 once).
     * - Cell 0 is given 5, which matches [solution].
     * - Cage `{0,1}` uses digits 5 and 3, which are distinct and sum to 8.
     * - Every other cell `i` is a 1-cell cage of sum `solution[i]`.
     * - The cages partition 0..80: `{0,1}` plus singletons 2..80.
     * Cell 1 is not a Sudoku single: only one given exists, so row 1, column 2
     * and box 1 still have many empty cells. The cage leftover 8-5=3 forces it.
     */
    fun cageCombinationPuzzle(): KillerSudokuPuzzle = KillerSudokuPuzzle(
        id = "KS-SPEC-CAGE-COMBO",
        givens = List(81) { index -> if (index == 0) 5 else null },
        cages = groupsAndSingletons(listOf(listOf(0, 1))),
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "hand",
    )

    /**
     * Unique Medium board whose 45-rule innie in row 1 is r1c1 = 5.
     *
     * Hand check of the 45 step:
     * - Row 1 is empty. Two cages sit wholly in it: `{1,2,3,4}` = 3+4+6+7 = 20
     *   and `{5,6,7,8}` = 8+9+1+2 = 20. The leftover innie is cell 0.
     * - 45 - 20 - 20 = 5, and [solution][0] is 5.
     * - Cell 0 shares a cage with cell 9 (5+6=11), so the innie is not a 1-cell cage.
     * Rows 3 to 9 are given, so each column of the innie still has two empty
     * cells and is not a Sudoku single.
     *
     * Hand check of the empty-region cages (cells 0-17). Each group is a
     * horizontal piece of one row, so digits are distinct; sums are from [solution]:
     * `{1,2,3,4}`=20, `{5,6,7,8}`=20, `{0,9}`=11, `{10,11}`=9, `{12,13}`=10,
     * `{14,15}`=8, `{16,17}`=12. That is 18 cells, 0 through 17.
     * Cells 18-80 are given 1-cell cages. Together they partition 0..80.
     * Givens match [solution].
     */
    fun rule45Puzzle(): KillerSudokuPuzzle = KillerSudokuPuzzle(
        id = "KS-SPEC-RULE-45",
        givens = List(81) { index -> if (index >= 18) solution[index] else null },
        cages = groupsAndSingletons(RULE_45_EMPTY_GROUPS),
        solution = solution,
        pack = Pack.MEDIUM,
        storedGrade = "hand",
    )

    /**
     * Unique board that needs a guess: a published 17-clue Sudoku (Norvig)
     * with cages that add no extra constraint.
     *
     * Hand check of the solution grid:
     * - Each row, column and box of [STUCK_SOLUTION_TEXT] holds 1-9 once
     *   (verified by sorting each house).
     * - Each of the 17 givens equals the solution at that cell:
     *   0=4, 6=8, 8=5, 10=3, 21=7, 28=2, 34=6, 40=8, 42=4, 49=1,
     *   57=6, 59=3, 61=7, 63=5, 66=2, 72=1, 74=4.
     *
     * Hand check of the cages:
     * - Each given cell is a 1-cell cage of that given.
     * - Empty cells inside each 3x3 box form one cage. Digits in a box are
     *   distinct, and the cage sum is the sum of those solution digits.
     * - Empty counts per box are 7,8,7, 8,7,7, 6,6,8, so no empty 1-cell cage.
     * - Givens plus the nine box cages partition 0..80.
     * A box cage of the empty cells is the same constraint as the box rule
     * plus the givens, so uniqueness is the Sudoku uniqueness of this 17-clue.
     */
    fun uniqueStuckPuzzle(): KillerSudokuPuzzle {
        val values = stuckSolution
        val givens = STUCK_GIVENS_TEXT.map { symbol ->
            if (symbol == '.') null else symbol.digitToInt()
        }
        val groups = (0 until 9).map { box ->
            boxCells(box).filter { index -> givens[index] == null }
        }
        return KillerSudokuPuzzle(
            id = "KS-SPEC-STUCK",
            givens = givens,
            cages = groupsAndSingletons(groups, values),
            solution = values,
            pack = Pack.EXPERT,
            storedGrade = "hand",
        )
    }

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

    /** Norvig 17-clue given mask. Dots are empty. */
    const val STUCK_GIVENS_TEXT =
        "4.....8.5" +
            ".3......." +
            "...7....." +
            ".2.....6." +
            "....8.4.." +
            "....1...." +
            "...6.3.7." +
            "5..2....." +
            "1.4......"

    /** Published unique completion of [STUCK_GIVENS_TEXT]. */
    const val STUCK_SOLUTION_TEXT =
        "417369825" +
            "632158947" +
            "958724316" +
            "825437169" +
            "791586432" +
            "346912758" +
            "289643571" +
            "573291684" +
            "164875293"

    /** Row-major digits of [STUCK_SOLUTION_TEXT]. */
    val stuckSolution: List<Int> = STUCK_SOLUTION_TEXT.map { it.digitToInt() }

    private val RULE_45_EMPTY_GROUPS: List<List<Int>> = listOf(
        listOf(1, 2, 3, 4),
        listOf(5, 6, 7, 8),
        listOf(0, 9),
        listOf(10, 11),
        listOf(12, 13),
        listOf(14, 15),
        listOf(16, 17),
    )

    /**
     * Builds [groups] as cages, then a 1-cell cage for every uncovered cell.
     * Each cage sum is the sum of [values] in that cage.
     */
    private fun groupsAndSingletons(
        groups: List<List<Int>>,
        values: List<Int> = solution,
    ): List<KillerCage> {
        val covered = groups.flatten()
        require(covered.size == covered.toSet().size) { "Cage groups overlap" }
        val grouped = groups.map { indices ->
            KillerCage(indices.map(::Cell), indices.sumOf { values[it] })
        }
        val singletons = values.indices
            .filter { index -> index !in covered }
            .map { index -> KillerCage(listOf(Cell(index)), values[index]) }
        return grouped + singletons
    }

    private fun boxCells(box: Int): List<Int> {
        val boxRow = box / 3
        val boxColumn = box % 3
        return (0 until 9).map { offset ->
            (boxRow * 3 + offset / 3) * 9 + (boxColumn * 3 + offset % 3)
        }
    }

    private fun cage(sum: Int, cells: IntRange): KillerCage = KillerCage(cells.map(::Cell), sum)
}
