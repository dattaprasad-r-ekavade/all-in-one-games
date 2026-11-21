package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.Registry
import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Type-safe bank operations for one registered game. */
internal interface BankGame {
    val id: String

    fun generateLine(seed: Long, pack: Pack): String

    fun validateLine(line: String, expectedPack: Pack? = null): BankEntry
}

internal data class BankEntry(
    val id: String,
    val pack: Pack,
    val grade: String,
    val fingerprint: String,
)

/** Captures the puzzle and state types from one heterogeneous engine registry entry. */
internal class EngineBankGame<P : Any, S : Any>(
    private val engine: GameEngine<P, S>,
    private val gradeEncoder: (Grade) -> String = BankGrade::encode,
) : BankGame {
    override val id: String = engine.id

    override fun generateLine(seed: Long, pack: Pack): String {
        val puzzle = engine.generate(SplitMix64(seed), pack)
        return engine.encodePuzzle(puzzle)
    }

    override fun validateLine(line: String, expectedPack: Pack?): BankEntry {
        val puzzle = engine.decodePuzzle(line)
        val canonicalLine = engine.encodePuzzle(puzzle)
        require(canonicalLine == line) { "Puzzle line is not in canonical bank format" }
        val envelope = BankLine.decode(canonicalLine)
        require(expectedPack == null || envelope.pack == expectedPack) {
            "Puzzle pack ${envelope.pack} does not match file pack $expectedPack"
        }
        require(engine.countSolutions(puzzle, limit = 2) == 1) {
            "Puzzle ${envelope.id} must have exactly one solution"
        }
        val grade = engine.grade(puzzle)
        require(grade.pack == envelope.pack) { "Puzzle ${envelope.id} grade uses a different pack" }
        require(envelope.grade == gradeEncoder(grade)) {
            "Puzzle ${envelope.id} stored grade does not match its technique grade"
        }
        return BankEntry(
            id = envelope.id,
            pack = envelope.pack,
            grade = envelope.grade,
            fingerprint = "${envelope.size}|${envelope.payload}|${envelope.solution}|${envelope.pack.name}",
        )
    }
}

internal fun registeredBankGames(
    extraFamilyA: List<GameEngine<*, *>> = emptyList(),
): List<BankGame> {
    val familyA = (Registry.engines + extraFamilyA).map { bindEngine(it) }
    val familyB = discoverFamilyBBankGames()
    return familyA + familyB
}

private fun bindEngine(engine: GameEngine<*, *>): BankGame = bindCapturedEngine(engine)

private fun <P : Any, S : Any> bindCapturedEngine(engine: GameEngine<P, S>): BankGame =
    EngineBankGame(engine, ::encodeFamilyAGrade)

private fun encodeFamilyAGrade(grade: Grade): String = BankGrade.encode(grade)
