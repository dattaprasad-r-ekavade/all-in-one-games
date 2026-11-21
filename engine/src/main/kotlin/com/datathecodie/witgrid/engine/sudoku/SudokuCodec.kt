package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact Sudoku bank codec for givens and row-major solution digits. */
object SudokuCodec {
    /** Decodes a bank line whose payload uses `.` for an empty cell and digits 1 through 9. */
    fun decode(line: String): SudokuPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size == SUDOKU_SIZE) { "Sudoku board size must be 9" }
        require(bankLine.payload.length == SUDOKU_CELL_COUNT) {
            "Sudoku givens need one symbol per cell"
        }
        require(bankLine.solution.length == SUDOKU_CELL_COUNT) {
            "Sudoku solution needs one digit per cell"
        }
        val givens = bankLine.payload.map { value ->
            when (value) {
                '.' -> null
                in '1'..'9' -> value.digitToInt()
                else -> throw IllegalArgumentException("Sudoku givens must use . or digits 1 through 9")
            }
        }
        val solution = bankLine.solution.map { value ->
            require(value in '1'..'9') { "Sudoku solution must use digits 1 through 9" }
            value.digitToInt()
        }
        val puzzle = SudokuPuzzle(
            id = bankLine.id,
            givens = givens,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )
        require(SudokuRules.isValidSolution(puzzle)) {
            "Sudoku stored solution violates the row, column or box rule"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Sudoku givens must match the stored solution" }
        return puzzle
    }

    /** Encodes a validated puzzle as a canonical shared bank line. */
    fun encode(puzzle: SudokuPuzzle): String {
        require(SudokuRules.isValidSolution(puzzle)) {
            "Sudoku stored solution violates the row, column or box rule"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Sudoku givens must match the stored solution" }
        val payload = puzzle.givens.joinToString(separator = "") { given ->
            if (given == null) "." else given.digitToChar().toString()
        }
        val solution = puzzle.solution.joinToString(separator = "") { it.digitToChar().toString() }
        return BankLine(
            id = puzzle.id,
            size = SUDOKU_SIZE,
            payload = payload,
            solution = solution,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }

    private fun Int.digitToChar(): Char {
        require(this in SUDOKU_DIGITS) { "Sudoku digits must be from 1 through 9" }
        return '0' + this
    }
}