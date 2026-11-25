package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.PlayBankLine

object ArrowsCodec {
    fun decodeLayout(layout: String, size: Int): List<ArrowsDirection?> {
        val cellCount = size * size
        require(layout.length == cellCount) {
            "Layout length must be $cellCount for a ${size}x${size} board, got ${layout.length}"
        }
        return layout.map { char -> ArrowsDirection.fromCharOrNull(char) }
    }

    fun encodeLayout(arrows: List<ArrowsDirection?>, size: Int): String {
        val cellCount = size * size
        require(arrows.size == cellCount) {
            "Arrows count must be $cellCount for a ${size}x${size} board, got ${arrows.size}"
        }
        return arrows.joinToString(separator = "") { it?.symbol?.toString() ?: "." }
    }

    fun decodeLine(line: String): ArrowsPuzzle {
        val bank = PlayBankLine.decode(line)
        require(bank.size in ArrowsPuzzle.SUPPORTED_SIZES) {
            "Unsupported Arrows board size: ${bank.size}"
        }
        require(bank.size == ArrowsPuzzle.expectedGridSize(bank.pack)) {
            "Pack ${bank.pack} requires size ${ArrowsPuzzle.expectedGridSize(bank.pack)}, not ${bank.size}"
        }
        val arrows = decodeLayout(bank.payload, bank.size)
        return ArrowsPuzzle(bank.id, bank.size, bank.pack, bank.difficulty, arrows)
    }

    fun encodePuzzle(puzzle: ArrowsPuzzle): String =
        PlayBankLine(
            id = puzzle.bankId,
            size = puzzle.size,
            payload = encodeLayout(puzzle.initialArrows, puzzle.size),
            pack = puzzle.pack,
            difficulty = puzzle.difficulty,
        ).encode()
}
