package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.SplitMix64

internal class PlayEngineBankGame<P : Any>(
    private val validator: PlayBankValidator<P>,
) : BankGame {
    override val id: String = validator.id

    override fun generateLine(seed: Long, pack: Pack): String {
        val puzzle = validator.generate(SplitMix64(seed), pack)
        return validator.encodePuzzle(puzzle)
    }

    override fun validateLine(line: String, expectedPack: Pack?): BankEntry {
        val puzzle = validator.decodePuzzle(line)
        val canonicalLine = validator.encodePuzzle(puzzle)
        require(canonicalLine == line) { "Puzzle line is not in canonical bank format" }
        val envelope = PlayBankLine.decode(canonicalLine)
        require(expectedPack == null || envelope.pack == expectedPack) {
            "Puzzle pack ${envelope.pack} does not match file pack $expectedPack"
        }
        require(validator.isWinnable(puzzle)) {
            "Puzzle ${envelope.id} is not winnable"
        }
        require(envelope.difficulty == validator.difficulty(puzzle)) {
            "Puzzle ${envelope.id} stored difficulty does not match its layout"
        }
        return BankEntry(
            id = envelope.id,
            pack = envelope.pack,
            grade = envelope.difficulty,
            fingerprint = "${envelope.size}|${envelope.payload}|${envelope.pack.name}",
        )
    }
}