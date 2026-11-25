package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Cell

/** Compact Killer Sudoku bank codec for givens, cages, and row-major solution digits. */
object KillerSudokuCodec {
    /**
     * Decodes a canonical bank line.
     *
     * Payload format is `<givens>/<cages>`:
     * - `<givens>` has 81 characters using `.` for empty cells and `'1'..'9'` for givens.
     * - `<cages>` is `;`-separated cages ordered strictly ascending by each cage's lowest cell index.
     * - Each cage is `<sum>:<cell0>,<cell1>,...` with canonical positive integer `<sum>` and
     *   canonical zero-based row-major cell indices in strictly ascending order.
     * - Solution contains 81 row-major digits `'1'..'9'`.
     */
    fun decode(line: String): KillerSudokuPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size == KILLER_SUDOKU_SIZE) { "Killer Sudoku board size must be 9" }
        require(bankLine.solution.length == KILLER_SUDOKU_CELL_COUNT) {
            "Killer Sudoku solution needs one digit per cell"
        }

        val parts = bankLine.payload.split('/')
        require(parts.size == 2) {
            "Killer Sudoku payload must have givens and cages separated by '/'"
        }
        val givensPart = parts[0]
        val cagesPart = parts[1]

        require(givensPart.length == KILLER_SUDOKU_CELL_COUNT) {
            "Killer Sudoku givens need one symbol per cell"
        }
        require(cagesPart.isNotEmpty()) { "Killer Sudoku cages payload must not be empty" }

        val givens = givensPart.map { symbol ->
            when (symbol) {
                '.' -> null
                in '1'..'9' -> symbol.digitToInt()
                else -> throw IllegalArgumentException(
                    "Killer Sudoku givens must use . or digits 1 through 9",
                )
            }
        }

        val cages = parseCages(cagesPart)
        val solution = bankLine.solution.map { symbol ->
            require(symbol in '1'..'9') { "Killer Sudoku solution must use digits 1 through 9" }
            symbol.digitToInt()
        }

        val puzzle = KillerSudokuPuzzle(
            id = bankLine.id,
            givens = givens,
            cages = cages,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )
        require(KillerSudokuRules.isValidSolution(puzzle)) {
            "Killer Sudoku stored solution violates the Sudoku, cage, or given constraints"
        }
        return puzzle
    }

    /** Encodes a validated puzzle as one canonical shared bank line. */
    fun encode(puzzle: KillerSudokuPuzzle): String {
        require(KillerSudokuRules.isValidSolution(puzzle)) {
            "Killer Sudoku stored solution violates the Sudoku, cage, or given constraints"
        }
        val givensPayload = puzzle.givens.joinToString(separator = "") { given ->
            given?.digitToChar()?.toString() ?: "."
        }
        val cagesPayload = puzzle.cages.joinToString(separator = ";") { cage ->
            val cellIndices = cage.cells.joinToString(separator = ",") { it.index.toString() }
            "${cage.sum}:$cellIndices"
        }
        val solution = puzzle.solution.joinToString(separator = "") { it.digitToChar().toString() }
        return BankLine(
            id = puzzle.id,
            size = KILLER_SUDOKU_SIZE,
            payload = "$givensPayload/$cagesPayload",
            solution = solution,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }

    private fun parseCages(cagesPart: String): List<KillerCage> {
        val cageTokens = cagesPart.split(';')
        val cages = ArrayList<KillerCage>(cageTokens.size)
        var previousFirstCell = -1
        for (cageToken in cageTokens) {
            val sumAndCells = cageToken.split(':')
            require(sumAndCells.size == 2) {
                "Each Killer Sudoku cage must use '<sum>:<cell0>,<cell1>,...'"
            }
            val sumToken = sumAndCells[0]
            val sum = requireNotNull(sumToken.toIntOrNull()) { "Invalid Killer Sudoku cage sum: $sumToken" }
            require(sum > 0 && sum.toString() == sumToken) {
                "Killer Sudoku cage sum must be a positive canonical integer: $sumToken"
            }
            val cellTokens = sumAndCells[1].split(',')
            require(cellTokens.isNotEmpty() && cellTokens.size <= KILLER_SUDOKU_SIZE) {
                "Killer Sudoku cage must list between 1 and 9 cells"
            }
            val cells = ArrayList<Cell>(cellTokens.size)
            var previousCell = -1
            for (cellToken in cellTokens) {
                val cellIndex = requireNotNull(cellToken.toIntOrNull()) {
                    "Invalid Killer Sudoku cage cell index: $cellToken"
                }
                require(cellIndex in 0 until KILLER_SUDOKU_CELL_COUNT && cellIndex.toString() == cellToken) {
                    "Killer Sudoku cage cell index must be canonical 0 through 80: $cellToken"
                }
                require(cellIndex > previousCell) {
                    "Killer Sudoku cage cells must be listed in strictly ascending order"
                }
                previousCell = cellIndex
                cells += Cell(cellIndex)
            }
            require(cells.first().index > previousFirstCell) {
                "Killer Sudoku cages must be ordered strictly by lowest cell index"
            }
            previousFirstCell = cells.first().index
            cages += KillerCage(cells, sum)
        }
        return cages
    }

    private fun Int.digitToChar(): Char {
        require(this in KILLER_SUDOKU_DIGITS) { "Killer Sudoku digits must be from 1 through 9" }
        return '0' + this
    }
}
