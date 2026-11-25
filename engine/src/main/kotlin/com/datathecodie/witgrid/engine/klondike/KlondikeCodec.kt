package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.PlayBankLine

object KlondikeCodec {
    const val DECK_SIZE = 52

    fun decodeLine(line: String): KlondikePuzzle {
        val bank = PlayBankLine.decode(line)
        require(bank.size == DECK_SIZE) {
            "Expected Klondike size $DECK_SIZE, got ${bank.size}"
        }
        val seed = bank.payload.removePrefix("seed=").toLongOrNull()
            ?: throw IllegalArgumentException("Invalid deal seed in bank line: ${bank.payload}")
        return KlondikePuzzle(
            bankId = bank.id,
            pack = bank.pack,
            difficulty = bank.difficulty,
            seed = seed,
            size = bank.size,
        )
    }

    fun encodePuzzle(puzzle: KlondikePuzzle): String =
        PlayBankLine(
            id = puzzle.bankId,
            size = puzzle.size,
            payload = puzzle.seed.toString(),
            pack = puzzle.pack,
            difficulty = puzzle.difficulty,
        ).encode()
}
