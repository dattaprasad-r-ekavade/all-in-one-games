package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact lane codec for Crowns rows in the shared puzzle bank. */
object CrownsCodec {
    /**
     * Decodes a complete bank line. The payload has one decimal region digit per row-major cell;
     * the solution has one decimal column digit per row.
     */
    fun decode(line: String): CrownsPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size in 6..10) { "Crowns bank size must be between 6 and 10" }
        require(bankLine.pack == CrownsPackBands.packForSize(bankLine.size)) {
            "Crowns pack must match its board size"
        }
        require(bankLine.payload.length == bankLine.size * bankLine.size) {
            "Crowns region payload must contain one digit per cell"
        }
        require(bankLine.solution.length == bankLine.size) {
            "Crowns solution must contain one digit per row"
        }

        val regions = bankLine.payload.map { decodeDigit(it, bankLine.size, "region") }
        val solutionColumns = bankLine.solution.map { decodeDigit(it, bankLine.size, "solution") }
        val puzzle = CrownsPuzzle(
            id = bankLine.id,
            size = bankLine.size,
            regions = regions,
            solutionColumns = solutionColumns,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )
        require(CrownsRules.isValidSolution(puzzle)) { "Crowns stored solution violates the game rules" }
        return puzzle
    }

    /** Encodes a validated Crowns puzzle as one canonical shared bank line. */
    fun encode(puzzle: CrownsPuzzle): String {
        require(puzzle.size in 6..10) { "Crowns bank size must be between 6 and 10" }
        require(puzzle.pack == CrownsPackBands.packForSize(puzzle.size)) {
            "Crowns pack must match its board size"
        }
        require(CrownsRules.isValidSolution(puzzle)) { "Crowns stored solution violates the game rules" }
        val payload = puzzle.regions.joinToString(separator = "") { it.digitToChar().toString() }
        val solution = puzzle.solutionColumns.joinToString(separator = "") { it.digitToChar().toString() }
        return BankLine(puzzle.id, puzzle.size, payload, solution, puzzle.pack, puzzle.storedGrade).encode()
    }

    private fun decodeDigit(value: Char, size: Int, field: String): Int {
        require(value in '0'..'9') { "Crowns $field data must use decimal digits" }
        val digit = value - '0'
        require(digit < size) { "Crowns $field digit outside the puzzle" }
        return digit
    }

    private fun Int.digitToChar(): Char {
        require(this in 0..9) { "Crowns bank digits must be between zero and nine" }
        return '0' + this
    }
}
