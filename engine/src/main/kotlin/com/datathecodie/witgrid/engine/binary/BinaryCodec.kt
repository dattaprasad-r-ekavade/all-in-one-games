package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact bank codec for Binary puzzles. */
object BinaryCodec {
    /**
     * Decodes a complete bank line. The payload is row-major givens (`0`, `1` or `.`), and the
     * solution field is one row-major `0` or `1` symbol per cell.
     */
    fun decode(line: String): BinaryPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size in BinaryPuzzle.supportedSizes) { "Unsupported Binary board size" }
        require(bankLine.pack == BinaryPackBands.packForSize(bankLine.size)) {
            "Binary pack must match its board size"
        }
        val cellCount = bankLine.size * bankLine.size
        require(bankLine.payload.length == cellCount) { "Binary givens must contain one symbol per cell" }
        require(bankLine.solution.length == cellCount) { "Binary solution must contain one symbol per cell" }

        val givens = bankLine.payload.map { value ->
            when (value) {
                '.' -> null
                '0' -> 0
                '1' -> 1
                else -> throw IllegalArgumentException("Binary givens must use 0, 1 or .")
            }
        }
        val solution = bankLine.solution.map { value ->
            require(value == '0' || value == '1') { "Binary solution must use zero or one" }
            value - '0'
        }
        return BinaryPuzzle(
            id = bankLine.id,
            size = bankLine.size,
            givens = givens,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        ).also(::requireBankPuzzle)
    }

    /** Encodes a puzzle only when its stored solution is valid and every given matches it. */
    fun encode(puzzle: BinaryPuzzle): String {
        requireBankPuzzle(puzzle)
        val payload = puzzle.givens.joinToString(separator = "") { it?.digitToChar()?.toString() ?: "." }
        val solution = puzzle.solution.joinToString(separator = "") { it.digitToChar().toString() }
        return BankLine(puzzle.id, puzzle.size, payload, solution, puzzle.pack, puzzle.storedGrade).encode()
    }

    private fun requireBankPuzzle(puzzle: BinaryPuzzle) {
        require(puzzle.pack == BinaryPackBands.packForSize(puzzle.size)) {
            "Binary pack must match its board size"
        }
        require(BinaryRules.isValidSolution(puzzle)) { "Binary stored solution violates the game rules" }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Binary givens must agree with the stored solution" }
    }

    private fun Int.digitToChar(): Char {
        require(this in 0..1) { "Binary symbols must be zero or one" }
        return '0' + this
    }
}
