package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikeGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class KlondikeBankValidatorTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun registeredBankGamesIncludesKlondikeInProduction() {
        val games = registeredBankGames()
        assertTrue(games.any { it.id == "klondike" })

        val validator = defaultFamilyBValidators.single { it.id == "klondike" }
        val puzzle = KlondikeEngine.generate(SplitMix64(200L), Pack.EASY)
        val line = KlondikeEngine.encodePuzzle(puzzle)
        val entry = PlayEngineBankGame(validator).validateLine(line, Pack.EASY)
        assertEquals(puzzle.bankId, entry.id)
        assertEquals(Pack.EASY, entry.pack)
        assertEquals(KlondikeGenerator.configFor(Pack.EASY).difficulty, entry.grade)
    }

    @Test
    fun validatorRejectsNonCanonicalWrongPackAndWrongDifficultyLines() {
        val validLine = registeredBankGames().single { it.id == "klondike" }.generateLine(42L, Pack.EASY)
        val validParts = PlayBankLine.decode(validLine)
        val nonCanonicalLine = validParts.copy(payload = "seed=${validParts.payload}").encode()
        val wrongDifficultyLine = validParts.copy(difficulty = "d2").encode()
        val invalidPayloadLine = validParts.copy(payload = "not-a-seed").encode()

        val cases = listOf(
            "klondike-easy.txt.gz" to invalidPayloadLine,
            "klondike-easy.txt.gz" to nonCanonicalLine,
            "klondike-easy.txt.gz" to wrongDifficultyLine,
            "klondike-medium.txt.gz" to validLine,
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
    fun commandLineGeneratesAndValidatesASmallKlondikePack() {
        val bankDirectory = temporaryDirectory.resolve("cli-klondike")
        val service = BankgenService(registeredBankGames(), bankDirectory)
        val output = StringBuilder()
        val errors = StringBuilder()
        val cli = BankgenCommandLine(service, output, errors)

        assertEquals(
            0,
            cli.run(arrayOf("generate", "--game", "klondike", "--pack", "easy", "--count", "2", "--seed", "71")),
        )
        assertEquals(0, cli.run(arrayOf("validate", "--all")))
        assertTrue(output.toString().contains("Validated 2 puzzles in 1 files"))
    }

    private fun writeGzip(path: Path, lines: List<String>) {
        GZIPOutputStream(Files.newOutputStream(path)).bufferedWriter(Charsets.UTF_8).use { writer ->
            lines.forEach { line ->
                writer.write(line)
                writer.write('\n'.code)
            }
        }
    }
}
