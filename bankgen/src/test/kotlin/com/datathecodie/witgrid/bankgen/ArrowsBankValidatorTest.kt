package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.arrows.ArrowsCodec
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsGenerator
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.PlayEngine
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class ArrowsBankValidatorTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun registeredBankGamesIncludesArrowsInProduction() {
        val games = registeredBankGames()
        assertTrue(games.any { it.id == "arrows" })

        val validator = defaultFamilyBValidators.single { it.id == "arrows" }
        for (pack in Pack.entries) {
            val puzzle = ArrowsEngine.generate(SplitMix64(100L + pack.ordinal), pack)
            val line = ArrowsEngine.encodePuzzle(puzzle)
            val entry = PlayEngineBankGame(validator).validateLine(line, pack)
            assertEquals(puzzle.bankId, entry.id)
            assertEquals(pack, entry.pack)
            assertEquals(ArrowsGenerator.configFor(pack).difficulty, entry.grade)
        }
    }

    @Test
    fun validatorRejectsDeadlockedNonCanonicalWrongPackAndWrongDifficultyLines() {
        val validLine = registeredBankGames().single { it.id == "arrows" }.generateLine(42L, Pack.EASY)
        val validParts = PlayBankLine.decode(validLine)
        val nonCanonicalPayload = validParts.payload
            .replace('^', 'U')
            .replace('v', 'D')
            .replace('<', 'L')
            .replace('>', 'R')
        val nonCanonicalLine = validParts.copy(payload = nonCanonicalPayload).encode()
        val wrongDifficultyLine = validParts.copy(difficulty = "d2").encode()
        val deadlockedLine = PlayBankLine(
            id = "AR-EASY-deadlock",
            size = 5,
            payload = "^.....>.<................",
            pack = Pack.EASY,
            difficulty = "d1",
        ).encode()

        val cases = listOf(
            "arrows-easy.txt.gz" to deadlockedLine,
            "arrows-easy.txt.gz" to nonCanonicalLine,
            "arrows-easy.txt.gz" to wrongDifficultyLine,
            "arrows-medium.txt.gz" to validLine,
        )

        cases.forEachIndexed { index, (fileName, line) ->
            val directory = temporaryDirectory.resolve("bad-$index")
            Files.createDirectories(directory)
            writeGzip(directory.resolve(fileName), listOf(line))

            val failure = assertThrows(BankValidationException::class.java) {
                BankgenService(registeredBankGames(), directory).validateAll()
            }
            assertTrue(failure.message.orEmpty().contains(fileName))
        }
    }

    @Test
    fun validatorAcceptsMultipleClearingOrdersAndSmallEasyIntroPuzzle() {
        val bankDirectory = temporaryDirectory.resolve("arrows-intro-and-pack")
        Files.createDirectories(bankDirectory)

        val multiOrderPayload = "..^..^....<...>.....v...."
        val multiOrderPuzzle = ArrowsEngine.decodePuzzle(
            PlayBankLine("AR-INTRO-01", 5, multiOrderPayload, Pack.EASY, "d1").encode(),
        )
        assertFalse(ArrowsEngine.isLost(multiOrderPuzzle, ArrowsEngine.initialState(multiOrderPuzzle)))
        assertEquals(25, ArrowsCodec.encodeLayout(multiOrderPuzzle.initialArrows, 5).length)

        Files.writeString(
            bankDirectory.resolve("arrows-intro.txt"),
            "${ArrowsEngine.encodePuzzle(multiOrderPuzzle)}\n",
            Charsets.UTF_8,
        )

        val service = BankgenService(registeredBankGames(), bankDirectory)
        val generatedPath = service.generate("arrows", Pack.EASY, 5, 777L)
        assertEquals(5, readGzip(generatedPath).size)

        val report = service.validateAll()
        assertEquals(6, report.puzzleCount)
    }

    @Test
    fun commandLineGeneratesAndValidatesArrowsPackAndDaily() {
        val bankDirectory = temporaryDirectory.resolve("cli-arrows")
        val service = BankgenService(registeredBankGames(), bankDirectory)
        val output = StringBuilder()
        val errors = StringBuilder()
        val cli = BankgenCommandLine(service, output, errors)

        assertEquals(
            0,
            cli.run(arrayOf("generate", "--game", "arrows", "--pack", "hard", "--count", "4", "--seed", "91")),
        )
        assertEquals(
            0,
            cli.run(arrayOf("generate", "--game", "arrows", "--daily", "--seed", "92")),
        )
        assertEquals(0, cli.run(arrayOf("validate", "--all")))
        assertTrue(output.toString().contains("Validated 369 puzzles in 2 files"))
    }

    @Test
    fun playEngineValidatorRejectsInitialStatesThatAreAlreadyWonOrLost() {
        val lostValidator = PlayEngineValidator(
            engine = FixedOutcomePlayEngine(startWon = false, startLost = true),
            expectedDifficulty = { "d1" },
            canWin = { true },
        )
        val wonValidator = PlayEngineValidator(
            engine = FixedOutcomePlayEngine(startWon = true, startLost = false),
            expectedDifficulty = { "d1" },
            canWin = { true },
        )

        assertFalse(lostValidator.isWinnable("puzzle"))
        assertFalse(wonValidator.isWinnable("puzzle"))
    }

    private class FixedOutcomePlayEngine(
        private val startWon: Boolean,
        private val startLost: Boolean,
    ) : PlayEngine<String, String, String> {
        override val id: String = "fixed-outcome"

        override fun decodePuzzle(line: String): String = line

        override fun encodePuzzle(puzzle: String): String = puzzle

        override fun initialState(puzzle: String): String = puzzle

        override fun applyAction(state: String, action: String): String = state

        override fun isWon(puzzle: String, state: String): Boolean = startWon

        override fun isLost(puzzle: String, state: String): Boolean = startLost

        override fun hintAction(puzzle: String, state: String): String? = null

        override fun generate(rng: SplitMix64, pack: Pack): String = "puzzle"

        override fun encodeState(state: String): ByteArray = byteArrayOf()

        override fun decodeState(bytes: ByteArray): String = "puzzle"

        override fun applyContinue(state: String): String = state
    }

    private fun readGzip(path: Path): List<String> =
        GZIPInputStream(Files.newInputStream(path)).bufferedReader(Charsets.UTF_8).use { it.readLines() }

    private fun writeGzip(path: Path, lines: List<String>) {
        GZIPOutputStream(Files.newOutputStream(path)).bufferedWriter(Charsets.UTF_8).use { writer ->
            lines.forEach { line ->
                writer.write(line)
                writer.write('\n'.code)
            }
        }
    }
}
