package com.datathecodie.witgrid.engine.sudoku.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle

/** Hand-made Sudoku 9x9 board, copied from a published classic puzzle with its known solution. */
object SudokuSpecFixtures {
    const val CELL_COUNT = 81
    const val GIVEN_EMPTY_CELL_COUNT = 51
    const val FIXTURE_ID = "SU-SPEC-FIXTURE"
    const val FIXTURE_GRADE = "naked-single:51"

    private val givenRows = listOf(
        "53..7....",
        "6..195...",
        ".98....6.",
        "8...6...3",
        "4..8.3..1",
        "7...2...6",
        ".6....28.",
        "...419..5",
        "....8..79",
    )

    private val solutionRows = listOf(
        "534678912",
        "672195348",
        "198342567",
        "859761423",
        "426853791",
        "713924856",
        "961537284",
        "287419635",
        "345286179",
    )

    /** Row-major stored solution digits. */
    val solution: List<Int> = solutionRows.flatMap { row -> row.map(Char::digitToInt) }

    /** Row-major givens, with null for each empty cell. */
    val givens: List<Int?> = givenRows.flatMap { row ->
        row.map { symbol -> if (symbol == '.') null else symbol.digitToInt() }
    }

    /** Row-major given symbols, with `.` for each empty cell, as stored in a bank line. */
    val payload: String = givenRows.joinToString(separator = "")

    /** Row-major stored solution symbols, as stored in a bank line. */
    val solutionText: String = solutionRows.joinToString(separator = "")

    /** Builds the fixture puzzle; tests override only the parts they need. */
    fun puzzle(
        cellGivens: List<Int?> = givens,
        pack: Pack = Pack.EASY,
        id: String = FIXTURE_ID,
    ): SudokuPuzzle = SudokuPuzzle(
        id = id,
        givens = cellGivens,
        solution = solution,
        pack = pack,
        storedGrade = FIXTURE_GRADE,
    )

    /** Builds the canonical bank line for the fixture puzzle with an optional altered payload or solution. */
    fun bankLine(
        payloadText: String = payload,
        solutionDigits: String = solutionText,
    ): String = "$FIXTURE_ID|9|$payloadText|$solutionDigits|EASY|$FIXTURE_GRADE"
}
