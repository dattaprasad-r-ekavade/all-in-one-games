package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact bank codec for Nonogram clues and row-major solution bits. */
object NonogramCodec {
    /**
     * Decodes a bank line. Payload format is `row clues/column clues`; lines use semicolons and
     * runs use commas, with `-` for a line containing no filled cells. Solution bits are `0`/`1`.
     */
    fun decode(line: String): NonogramPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size in NonogramPuzzle.supportedSizes) { "Unsupported Nonogram board size" }
        require(NonogramPackBands.sizeForPack(bankLine.pack) == bankLine.size) {
            "Nonogram pack must match its board size"
        }
        val clues = decodePayload(bankLine.payload, bankLine.size)
        require(bankLine.solution.length == bankLine.size * bankLine.size) {
            "Nonogram solution needs one bit per cell"
        }
        val solution = bankLine.solution.map { value ->
            require(value == '0' || value == '1') { "Nonogram solution must use zero or one" }
            value - '0'
        }
        val puzzle = NonogramPuzzle(
            id = bankLine.id,
            size = bankLine.size,
            rowClues = clues.first,
            columnClues = clues.second,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )
        require(NonogramRules.isValidSolution(puzzle)) {
            "Nonogram stored solution does not match its clues"
        }
        return puzzle
    }

    /** Encodes a validated Nonogram puzzle as one canonical shared bank line. */
    fun encode(puzzle: NonogramPuzzle): String {
        require(NonogramPackBands.sizeForPack(puzzle.pack) == puzzle.size) {
            "Nonogram pack must match its board size"
        }
        require(NonogramRules.isValidSolution(puzzle)) { "Nonogram stored solution does not match its clues" }
        val payload = encodePayload(puzzle.rowClues, puzzle.columnClues)
        val solution = puzzle.solution.joinToString(separator = "") { it.digitToChar().toString() }
        return BankLine(
            puzzle.id,
            puzzle.size,
            payload,
            solution,
            puzzle.pack,
            puzzle.storedGrade,
        ).encode()
    }

    private fun encodePayload(rows: List<List<Int>>, columns: List<List<Int>>): String =
        "${rows.joinToString(separator = ";", transform = ::encodeLine)}/" +
            columns.joinToString(separator = ";", transform = ::encodeLine)

    private fun decodePayload(payload: String, size: Int): Pair<List<List<Int>>, List<List<Int>>> {
        val sides = payload.split('/')
        require(sides.size == 2) { "Nonogram payload must separate row and column clues" }
        val rows = sides[0].split(';').map(::decodeLine)
        val columns = sides[1].split(';').map(::decodeLine)
        require(rows.size == size) { "Nonogram payload needs one clue line per row" }
        require(columns.size == size) { "Nonogram payload needs one clue line per column" }
        return rows to columns
    }

    private fun encodeLine(clues: List<Int>): String =
        if (clues.isEmpty()) "-" else clues.joinToString(separator = ",")

    private fun decodeLine(value: String): List<Int> {
        if (value == "-") return emptyList()
        require(value.isNotEmpty()) { "Nonogram clue line cannot be blank" }
        return value.split(',').map { token ->
            val clue = requireNotNull(token.toIntOrNull()) { "Invalid Nonogram clue length" }
            require(clue > 0 && clue.toString() == token) {
                "Nonogram clue lengths must be positive canonical integers"
            }
            clue
        }
    }

    private fun Int.digitToChar(): Char {
        require(this in 0..1) { "Nonogram solution bits must be zero or one" }
        return '0' + this
    }
}
