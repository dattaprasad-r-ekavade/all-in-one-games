package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine

object ColourSortCodec {
    fun expectedTubeCount(pack: Pack): Int = when (pack) {
        Pack.EASY -> 4
        Pack.MEDIUM -> 6
        Pack.HARD -> 8
        Pack.EXPERT -> 10
    }

    fun decodePayload(payload: String, expectedTubes: Int): List<List<Int>> {
        val segments = payload.split('/')
        require(segments.size == expectedTubes) {
            "Payload must have $expectedTubes tubes separated by '/'"
        }
        return segments.map { segment -> decodeTube(segment) }
    }

    fun encodePayload(tubes: List<List<Int>>): String =
        tubes.joinToString(separator = "/") { encodeTube(it) }

    private fun decodeTube(segment: String): List<Int> {
        require(segment.length == ColourSortRules.TUBE_CAPACITY) {
            "Each tube must encode exactly ${ColourSortRules.TUBE_CAPACITY} slots"
        }
        val stack = ArrayList<Int>(ColourSortRules.TUBE_CAPACITY)
        for (ch in segment) {
            when (ch) {
                '.' -> break
                in '1'..'9' -> stack += ch.code - '0'.code
                else -> throw IllegalArgumentException("Invalid tube character: $ch")
            }
        }
        var seenEmpty = false
        for (ch in segment) {
            when (ch) {
                '.' -> seenEmpty = true
                in '1'..'9' -> require(!seenEmpty) { "Colour below an empty slot in a tube" }
                else -> throw IllegalArgumentException("Invalid tube character: $ch")
            }
        }
        require(stack.size <= ColourSortRules.TUBE_CAPACITY) { "Tube exceeds capacity" }
        return stack.toList()
    }

    private fun encodeTube(tube: List<Int>): String {
        require(tube.size <= ColourSortRules.TUBE_CAPACITY) { "Tube exceeds capacity" }
        val chars = CharArray(ColourSortRules.TUBE_CAPACITY) { '.' }
        tube.forEachIndexed { index, colour ->
            require(colour in 1..9) { "Colour must be 1..9" }
            chars[index] = ('0'.code + colour).toChar()
        }
        return String(chars)
    }

    fun decodeLine(line: String): ColourSortPuzzle {
        val bank = PlayBankLine.decode(line)
        val tubes = decodePayload(bank.payload, bank.size)
        require(bank.size == expectedTubeCount(bank.pack)) {
            "Pack ${bank.pack} requires ${expectedTubeCount(bank.pack)} tubes, not ${bank.size}"
        }
        return ColourSortPuzzle(bank.id, bank.pack, bank.difficulty, tubes)
    }

    fun encodePuzzle(puzzle: ColourSortPuzzle): String {
        require(puzzle.initialTubes.size == expectedTubeCount(puzzle.pack)) {
            "Pack ${puzzle.pack} requires ${expectedTubeCount(puzzle.pack)} tubes"
        }
        return PlayBankLine(
            id = puzzle.bankId,
            size = puzzle.initialTubes.size,
            payload = encodePayload(puzzle.initialTubes),
            pack = puzzle.pack,
            difficulty = puzzle.difficulty,
        ).encode()
    }
}