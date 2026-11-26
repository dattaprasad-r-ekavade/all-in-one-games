package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.ColourSortGenerator
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class ColourSortBankValidatorTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun registeredBankGamesIncludesColourSortInProduction() {
        val games = registeredBankGames()
        assertTrue(games.any { it.id == "coloursort" })

        val validator = defaultFamilyBValidators.single { it.id == "coloursort" }
        for (pack in Pack.entries) {
            val puzzle = ColourSortEngine.generate(SplitMix64(200L + pack.ordinal), pack)
            val line = ColourSortEngine.encodePuzzle(puzzle)
            val entry = PlayEngineBankGame(validator).validateLine(line, pack)
            assertEquals(puzzle.bankId, entry.id)
            assertEquals(pack, entry.pack)
            assertEquals(ColourSortGenerator.configFor(pack).difficulty, entry.grade)
        }
    }

    @Test
    fun validatorRejectsAlreadyWonNonCanonicalWrongPackAndWrongDifficultyLines() {
        val validLine = registeredBankGames().single { it.id == "coloursort" }.generateLine(42L, Pack.EASY)
        val validParts = PlayBankLine.decode(validLine)
        val nonCanonicalLine = validParts.copy(payload = validParts.payload.replace('.', 'x')).encode()
        val wrongDifficultyLine = validParts.copy(difficulty = "d2").encode()
        val alreadyWonLine = PlayBankLine(
            id = "CS-EASY-won",
            size = 4,
            payload = "1111/2222/3333/....",
            pack = Pack.EASY,
            difficulty = "d1",
        ).encode()

        val cases = listOf(
            "coloursort-easy.txt.gz" to alreadyWonLine,
            "coloursort-easy.txt.gz" to nonCanonicalLine,
            "coloursort-easy.txt.gz" to wrongDifficultyLine,
            "coloursort-medium.txt.gz" to validLine,
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
    fun commandLineGeneratesAndValidatesASmallColourSortPack() {
        val bankDirectory = temporaryDirectory.resolve("cli-coloursort")
        val service = BankgenService(registeredBankGames(), bankDirectory)
        val output = StringBuilder()
        val errors = StringBuilder()
        val cli = BankgenCommandLine(service, output, errors)

        assertEquals(
            0,
            cli.run(arrayOf("generate", "--game", "coloursort", "--pack", "easy", "--count", "3", "--seed", "71")),
        )
        assertEquals(0, cli.run(arrayOf("validate", "--all")))
        assertTrue(output.toString().contains("Validated 3 puzzles in 1 files"))
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
