package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.BankLine

/** Compact Futoshiki bank codec for givens, signs and row-major solution digits. */
object FutoshikiCodec {
    /**
     * Decodes a bank line.
     * Payload format is `<givens>/<hSigns>/<vSigns>`.
     * Solution contains row-major digits 1 to size.
     */
    fun decode(line: String): FutoshikiPuzzle {
        val bankLine = BankLine.decode(line)
        FutoshikiGradeCodec.validate(bankLine.grade)
        val size = bankLine.size
        require(size in FutoshikiPackBands.supportedSizes) { "Unsupported Futoshiki board size: $size" }
        require(FutoshikiPackBands.sizeForPack(bankLine.pack) == size) {
            "Futoshiki pack must match its board size"
        }

        val parts = bankLine.payload.split('/')
        require(parts.size == 3) {
            "Futoshiki payload must have givens, horizontal signs, and vertical signs"
        }
        val givensPart = parts[0]
        val hSignsPart = parts[1]
        val vSignsPart = parts[2]

        val cellCount = size * size
        val hSignCount = size * (size - 1)
        val vSignCount = (size - 1) * size

        require(givensPart.length == cellCount) {
            "Futoshiki givens need one symbol per cell"
        }
        require(hSignsPart.length == hSignCount) {
            "Futoshiki horizontal signs need one symbol per edge"
        }
        require(vSignsPart.length == vSignCount) {
            "Futoshiki vertical signs need one symbol per edge"
        }
        require(bankLine.solution.length == cellCount) {
            "Futoshiki solution needs one digit per cell"
        }

        val maxDigitChar = '0' + size
        val givens = givensPart.map { ch ->
            when (ch) {
                '.' -> null
                in '1'..maxDigitChar -> ch.digitToInt()
                else -> throw IllegalArgumentException("Futoshiki givens must use . or digits 1 through $size")
            }
        }
        val hSigns = hSignsPart.map { FutoshikiSign.fromHorizontalChar(it) }
        val vSigns = vSignsPart.map { FutoshikiSign.fromVerticalChar(it) }
        val solution = bankLine.solution.map { ch ->
            require(ch in '1'..maxDigitChar) { "Futoshiki solution must use digits 1 through $size" }
            ch.digitToInt()
        }

        val puzzle = FutoshikiPuzzle(
            id = bankLine.id,
            size = size,
            givens = givens,
            horizontalSigns = hSigns,
            verticalSigns = vSigns,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )

        require(FutoshikiRules.isValidSolution(puzzle)) {
            "Futoshiki stored solution violates the Latin square or inequality rules"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Futoshiki givens must match the stored solution" }

        return puzzle
    }

    /** Encodes a validated puzzle as a canonical shared bank line. */
    fun encode(puzzle: FutoshikiPuzzle): String {
        require(puzzle.size in FutoshikiPackBands.supportedSizes) {
            "Unsupported Futoshiki board size: ${puzzle.size}"
        }
        require(FutoshikiPackBands.sizeForPack(puzzle.pack) == puzzle.size) {
            "Futoshiki pack must match its board size"
        }
        FutoshikiGradeCodec.validate(puzzle.storedGrade)
        require(FutoshikiRules.isValidSolution(puzzle)) {
            "Futoshiki stored solution violates the Latin square or inequality rules"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Futoshiki givens must match the stored solution" }

        val givensString = puzzle.givens.joinToString(separator = "") { given ->
            given?.toString() ?: "."
        }
        val hSignsString = puzzle.horizontalSigns.joinToString(separator = "") { sign ->
            sign.horizontalSymbol.toString()
        }
        val vSignsString = puzzle.verticalSigns.joinToString(separator = "") { sign ->
            sign.verticalSymbol.toString()
        }
        val payload = "$givensString/$hSignsString/$vSignsString"
        val solutionString = puzzle.solution.joinToString(separator = "") { it.toString() }

        return BankLine(
            id = puzzle.id,
            size = puzzle.size,
            payload = payload,
            solution = solutionString,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }
}
