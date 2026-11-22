package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.BankLine
import java.util.Collections

/** Compact bank codec for Skyscrapers puzzles. */
object SkyscrapersCodec {

    /**
     * Decodes a bank line. Payload format is `top/bottom/left/right` with an optional
     * fifth segment `/givens`. Empty clues and empty givens use `.` (or `0`).
     */
    fun decode(line: String): SkyscrapersPuzzle {
        val bankLine = BankLine.decode(line)
        val size = bankLine.size
        require(size in SKYSCRAPERS_SUPPORTED_SIZES) { "Unsupported Skyscrapers board size: $size" }
        require(bankLine.pack == SkyscrapersPackBands.packForSize(size)) {
            "Skyscrapers pack must match its board size"
        }

        val segments = bankLine.payload.split('/')
        require(segments.size in 4..5) {
            "Skyscrapers payload must have 4 clue sides with optional givens: ${bankLine.payload}"
        }

        val topClues = parseClueLine(segments[0], size, "top")
        val bottomClues = parseClueLine(segments[1], size, "bottom")
        val leftClues = parseClueLine(segments[2], size, "left")
        val rightClues = parseClueLine(segments[3], size, "right")

        val givens = if (segments.size == 5) {
            parseGivens(segments[4], size)
        } else {
            Collections.nCopies(size * size, null)
        }

        val cellCount = size * size
        require(bankLine.solution.length == cellCount) {
            "Skyscrapers solution needs one digit per cell"
        }
        val solution = bankLine.solution.map { char ->
            require(char in '1'..'0' + size) {
                "Skyscrapers solution digits must be 1 through $size"
            }
            char.digitToInt()
        }

        val puzzle = SkyscrapersPuzzle(
            id = bankLine.id,
            size = size,
            topClues = topClues,
            bottomClues = bottomClues,
            leftClues = leftClues,
            rightClues = rightClues,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
            givens = givens,
        )

        require(SkyscrapersRules.isValidSolution(puzzle)) {
            "Skyscrapers stored solution violates the Latin square or clue constraints"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Skyscrapers givens must agree with the stored solution" }

        return puzzle
    }

    /** Encodes a validated Skyscrapers puzzle as a canonical bank line. */
    fun encode(puzzle: SkyscrapersPuzzle): String {
        require(SkyscrapersRules.isValidSolution(puzzle)) {
            "Skyscrapers stored solution violates the Latin square or clue constraints"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Skyscrapers givens must agree with the stored solution" }

        val topStr = puzzle.topClues.joinToString(separator = "") { it?.toString() ?: "." }
        val botStr = puzzle.bottomClues.joinToString(separator = "") { it?.toString() ?: "." }
        val leftStr = puzzle.leftClues.joinToString(separator = "") { it?.toString() ?: "." }
        val rightStr = puzzle.rightClues.joinToString(separator = "") { it?.toString() ?: "." }
        val cluePayload = "$topStr/$botStr/$leftStr/$rightStr"

        val hasGivens = puzzle.givens.any { it != null }
        val payload = if (hasGivens) {
            val givensStr = puzzle.givens.joinToString(separator = "") { it?.toString() ?: "." }
            "$cluePayload/$givensStr"
        } else {
            cluePayload
        }

        val solutionStr = puzzle.solution.joinToString(separator = "") { it.toString() }

        return BankLine(
            id = puzzle.id,
            size = puzzle.size,
            payload = payload,
            solution = solutionStr,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }

    private fun parseClueLine(segment: String, size: Int, edge: String): List<Int?> {
        require(segment.length == size) {
            "Skyscrapers $edge clues must have length $size"
        }
        return segment.map { char ->
            when (char) {
                '.', '0' -> null
                in '1'..'0' + size -> char.digitToInt()
                else -> throw IllegalArgumentException("Invalid $edge clue symbol: $char")
            }
        }
    }

    private fun parseGivens(segment: String, size: Int): List<Int?> {
        val cellCount = size * size
        require(segment.length == cellCount) {
            "Skyscrapers givens must have length $cellCount"
        }
        return segment.map { char ->
            when (char) {
                '.' -> null
                in '1'..'0' + size -> char.digitToInt()
                else -> throw IllegalArgumentException("Invalid givens symbol: $char")
            }
        }
    }
}
