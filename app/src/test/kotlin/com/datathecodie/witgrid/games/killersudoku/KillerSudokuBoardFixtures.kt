package com.datathecodie.witgrid.games.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.killersudoku.KillerCage
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState

/** Hand grids for board tests. Cages stay inside one row unless a test builds its own. */
internal object KillerSudokuBoardFixtures {
    val solution: List<Int> = listOf(
        "534678912",
        "672195348",
        "198342567",
        "859761423",
        "426853791",
        "713924856",
        "961537284",
        "287419635",
        "345286179",
    ).flatMap { row -> row.map(Char::digitToInt) }

    val handGivens: List<Int?> = listOf(
        "53..7....",
        "6..195...",
        ".98....6.",
        "8...6...3",
        "4..8.3..1",
        "7...2...6",
        ".6....28.",
        "...419..5",
        "....8..79",
    ).flatMap { row -> row.map { if (it == '.') null else it.digitToInt() } }

    fun puzzle(
        id: String,
        givens: List<Int?>,
        cages: List<KillerCage> = rowCages(),
        storedGrade: String = "fixture",
    ): KillerSudokuPuzzle = KillerSudokuPuzzle(
        id = id,
        givens = givens,
        cages = cages,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = storedGrade,
    )

    fun state(givens: List<Int?>, id: String = "KS-TEST-1"): KillerSudokuState =
        KillerSudokuState.initial(puzzle(id, givens))

    /** Each row is split into cages of 2, 2, 2 and 3. A sudoku row has distinct digits. */
    fun rowCages(): List<KillerCage> {
        val cages = ArrayList<KillerCage>(BOARD_SIZE * ROW_CAGE_SIZES.size)
        for (row in 0 until BOARD_SIZE) {
            var column = 0
            for (cageSize in ROW_CAGE_SIZES) {
                val cells = ArrayList<Cell>(cageSize)
                var sum = 0
                repeat(cageSize) { offset ->
                    val index = row * BOARD_SIZE + column + offset
                    cells += Cell(index)
                    sum += solution[index]
                }
                cages += KillerCage(cells, sum)
                column += cageSize
            }
        }
        return cages
    }

    /** One cage joins [first] and [second]. Every other cell is its own cage. */
    fun cagesLinking(first: Int, second: Int): List<KillerCage> {
        val cages = ArrayList<KillerCage>(solution.size - 1)
        cages += KillerCage(
            cells = listOf(Cell(first), Cell(second)),
            sum = solution[first] + solution[second],
        )
        for (index in solution.indices) {
            if (index != first && index != second) {
                cages += KillerCage(listOf(Cell(index)), solution[index])
            }
        }
        return cages
    }

    private val ROW_CAGE_SIZES = intArrayOf(2, 2, 2, 3)
    private const val BOARD_SIZE = 9
}
