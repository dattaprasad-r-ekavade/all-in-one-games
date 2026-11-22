package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.BankLine
import java.util.ArrayList

/** Compact Path bank codec for endpoints, walls and row-major solution paths. */
object PathCodec {
    /**
     * Decodes a bank line.
     *
     * In the payload:
     * - '.' represents an empty non-wall cell
     * - '#' represents a wall cell
     * - '1'..'9', 'A'..'Z' represent endpoint pairs
     *
     * In the solution:
     * - '#' represents a wall cell
     * - '1'..'9', 'A'..'Z' represent path colors
     */
    fun decode(line: String): PathPuzzle {
        val bankLine = BankLine.decode(line)
        val size = bankLine.size
        require(size in PATH_SUPPORTED_SIZES) { "Unsupported Path board size: $size" }
        require(PathPackBands.packForSize(size) == bankLine.pack) {
            "Path pack must match its board size"
        }

        val cellCount = size * size
        require(bankLine.payload.length == cellCount) {
            "Path payload needs one character per cell, expected $cellCount got ${bankLine.payload.length}"
        }
        require(bankLine.solution.length == cellCount) {
            "Path solution needs one character per cell, expected $cellCount got ${bankLine.solution.length}"
        }

        val endpoints = ArrayList<Int?>(cellCount)
        val walls = ArrayList<Boolean>(cellCount)
        val solution = ArrayList<Int>(cellCount)

        for (i in 0 until cellCount) {
            val payloadChar = bankLine.payload[i]
            val solutionChar = bankLine.solution[i]

            when (payloadChar) {
                '#' -> {
                    walls.add(true)
                    endpoints.add(null)
                    require(solutionChar == '#') {
                        "Path solution must have '#' at wall cell $i, got '$solutionChar'"
                    }
                    solution.add(0)
                }
                '.' -> {
                    walls.add(false)
                    endpoints.add(null)
                    require(solutionChar != '#' && solutionChar != '.') {
                        "Path solution must have a path digit at non-wall cell $i, got '$solutionChar'"
                    }
                    solution.add(charToPair(solutionChar))
                }
                else -> {
                    val pair = charToPair(payloadChar)
                    walls.add(false)
                    endpoints.add(pair)
                    require(solutionChar != '#' && solutionChar != '.') {
                        "Path solution must have a path digit at endpoint cell $i, got '$solutionChar'"
                    }
                    val solPair = charToPair(solutionChar)
                    require(solPair == pair) {
                        "Path solution at endpoint cell $i must match endpoint pair $pair, got $solPair"
                    }
                    solution.add(solPair)
                }
            }
        }

        val puzzle = PathPuzzle(
            id = bankLine.id,
            size = size,
            endpoints = endpoints,
            walls = walls,
            solution = solution,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )

        require(PathRules.isValidSolution(puzzle)) {
            "Path stored solution violates path rules"
        }

        return puzzle
    }

    /** Encodes a validated puzzle as a canonical shared bank line. */
    fun encode(puzzle: PathPuzzle): String {
        require(PathRules.isValidSolution(puzzle)) {
            "Path stored solution violates path rules"
        }
        val payloadBuilder = StringBuilder(puzzle.grid.cellCount)
        val solutionBuilder = StringBuilder(puzzle.grid.cellCount)

        for (i in 0 until puzzle.grid.cellCount) {
            if (puzzle.walls[i]) {
                payloadBuilder.append('#')
                solutionBuilder.append('#')
            } else {
                val endpoint = puzzle.endpoints[i]
                if (endpoint != null) {
                    payloadBuilder.append(pairToChar(endpoint))
                } else {
                    payloadBuilder.append('.')
                }
                solutionBuilder.append(pairToChar(puzzle.solution[i]))
            }
        }

        return BankLine(
            id = puzzle.id,
            size = puzzle.size,
            payload = payloadBuilder.toString(),
            solution = solutionBuilder.toString(),
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }
}
