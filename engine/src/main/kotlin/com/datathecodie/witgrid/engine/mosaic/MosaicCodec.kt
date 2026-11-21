package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact bank codec for Mosaic clues and row-major solution bits. */
object MosaicCodec {
    /**
     * Decodes a bank line.
     * The payload has length size * size: each character is '.' for no clue or '0'..'9' for a clue.
     * The solution has length size * size: each character is '0' or '1'.
     */
    fun decode(line: String): MosaicPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size in MosaicPackBands.launchSizes) { "Mosaic bank size must be 8, 10, 12 or 15" }
        require(bankLine.pack == MosaicPackBands.packForSize(bankLine.size)) {
            "Mosaic pack must match its board size"
        }
        val cellCount = bankLine.size * bankLine.size
        require(bankLine.payload.length == cellCount) {
            "Mosaic clues payload must have exactly one character per cell"
        }
        require(bankLine.solution.length == cellCount) {
            "Mosaic solution must have exactly one bit per cell"
        }

        val clues = bankLine.payload.map { char ->
            when (char) {
                '.' -> null
                in '0'..'9' -> char.digitToInt()
                else -> throw IllegalArgumentException("Mosaic clues must use '.' or digits 0 through 9")
            }
        }
        val solution = bankLine.solution.map { char ->
            require(char == '0' || char == '1') { "Mosaic solution must use '0' or '1'" }
            char - '0'
        }

        val puzzle = MosaicPuzzle(
            id = bankLine.id,
            size = bankLine.size,
            clues = clues,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )
        require(MosaicRules.isValidSolution(puzzle)) {
            "Mosaic stored solution does not match its clues"
        }
        return puzzle
    }

    /** Encodes a validated Mosaic puzzle as one canonical shared bank line. */
    fun encode(puzzle: MosaicPuzzle): String {
        require(puzzle.size in MosaicPackBands.launchSizes) { "Mosaic bank size must be 8, 10, 12 or 15" }
        require(puzzle.pack == MosaicPackBands.packForSize(puzzle.size)) {
            "Mosaic pack must match its board size"
        }
        require(MosaicRules.isValidSolution(puzzle)) { "Mosaic stored solution does not match its clues" }
        val payload = puzzle.clues.joinToString(separator = "") { it?.toString() ?: "." }
        val solution = puzzle.solution.joinToString(separator = "") { it.toString() }
        return BankLine(
            puzzle.id,
            puzzle.size,
            payload,
            solution,
            puzzle.pack,
            puzzle.storedGrade,
        ).encode()
    }
}
