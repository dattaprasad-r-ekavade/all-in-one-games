package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact Mini Sudoku bank codec for givens and row-major solution digits. */
object MiniSudokuCodec {
    /** Decodes a bank line whose payload uses `.` for an empty cell and digits 1 through 6. */
    fun decode(line: String): MiniSudokuPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size == MINI_SUDOKU_SIZE) { "Mini Sudoku board size must be 6" }
        require(bankLine.payload.length == MINI_SUDOKU_CELL_COUNT) {
            "Mini Sudoku givens need one symbol per cell"
        }
        require(bankLine.solution.length == MINI_SUDOKU_CELL_COUNT) {
            "Mini Sudoku solution needs one digit per cell"
        }
        val givens = bankLine.payload.map { value ->
            when (value) {
                '.' -> null
                in '1'..'6' -> value.digitToInt()
                else -> throw IllegalArgumentException("Mini Sudoku givens must use . or digits 1 through 6")
            }
        }
        val solution = bankLine.solution.map { value ->
            require(value in '1'..'6') { "Mini Sudoku solution must use digits 1 through 6" }
            value.digitToInt()
        }
        val puzzle = MiniSudokuPuzzle(
            id = bankLine.id,
            givens = givens,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )
        require(MiniSudokuRules.isValidSolution(puzzle)) {
            "Mini Sudoku stored solution violates the row, column or box rule"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Mini Sudoku givens must match the stored solution" }
        return puzzle
    }

    /** Encodes a validated puzzle as a canonical shared bank line. */
    fun encode(puzzle: MiniSudokuPuzzle): String {
        require(MiniSudokuRules.isValidSolution(puzzle)) {
            "Mini Sudoku stored solution violates the row, column or box rule"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Mini Sudoku givens must match the stored solution" }
        val payload = puzzle.givens.joinToString(separator = "") { given ->
            if (given == null) "." else given.digitToChar().toString()
        }
        val solution = puzzle.solution.joinToString(separator = "") { it.digitToChar().toString() }
        return BankLine(
            id = puzzle.id,
            size = MINI_SUDOKU_SIZE,
            payload = payload,
            solution = solution,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }

    private fun Int.digitToChar(): Char {
        require(this in MINI_SUDOKU_DIGITS) { "Mini Sudoku digits must be from 1 through 6" }
        return '0' + this
    }
}
