package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.core.Technique
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class FamilyEngineBankTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private lateinit var savedFamilyBRegistry: () -> List<PlayBankValidator<*>>

    @BeforeEach
    fun saveFamilyBRegistry() {
        savedFamilyBRegistry = familyBRegistry
    }

    @AfterEach
    fun resetFamilyBRegistry() {
        familyBRegistry = savedFamilyBRegistry
    }

    @Test
    fun fifthFamilyAEngineBindsWithoutHardcodedGameSwitch() {
        val engine = FifthFamilyAEngine()
        val game = registeredBankGames(extraFamilyA = listOf(engine))
            .single { it.id == "fifth-grid" }

        val line = game.generateLine(77, Pack.MEDIUM)
        val entry = game.validateLine(line, Pack.MEDIUM)

        assertEquals("FIFTH-MEDIUM-4d", entry.id)
        assertEquals("extra-rule:3", entry.grade)
    }

    @Test
    fun familyBEngineWithManySolutionsStillPassesValidate() {
        val validator = FakePlayValidator(solutionPaths = 2, winnable = true)
        familyBRegistry = { listOf(validator) }
        val bankDirectory = temporaryDirectory.resolve("play-multi")
        Files.createDirectories(bankDirectory)
        val line = PlayEngineBankGame(validator).generateLine(5, Pack.EASY)
        writeGzip(bankDirectory.resolve("playfake-easy.txt.gz"), listOf(line))

        val report = BankgenService(registeredBankGames(), bankDirectory).validateAll()

        assertEquals(1, report.puzzleCount)
        assertEquals(2, validator.solutionPaths)
    }

    @Test
    fun familyBEngineRejectsUnwinnableLayout() {
        val validator = FakePlayValidator(solutionPaths = 1, winnable = false)
        val bankDirectory = temporaryDirectory.resolve("play-broken")
        Files.createDirectories(bankDirectory)
        val line = PlayEngineBankGame(validator).generateLine(3, Pack.HARD)
        writeGzip(bankDirectory.resolve("playfake-hard.txt.gz"), listOf(line))

        val failure = assertThrows(BankValidationException::class.java) {
            BankgenService(listOf(PlayEngineBankGame(validator)), bankDirectory).validateAll()
        }

        assertTrue(failure.message.orEmpty().contains("not winnable"))
    }

    private fun writeGzip(path: Path, lines: List<String>) {
        GZIPOutputStream(Files.newOutputStream(path)).bufferedWriter(Charsets.UTF_8).use { writer ->
            lines.forEach { line ->
                writer.write(line)
                writer.write('\n'.code)
            }
        }
    }

    private data class FifthPuzzle(
        val id: String,
        val pack: Pack,
        val payload: String,
        val solution: String,
        val storedGrade: String,
    )

    private data class FifthState(val puzzle: FifthPuzzle)

    private class FifthFamilyAEngine : GameEngine<FifthPuzzle, FifthState> {
        override val id: String = "fifth-grid"

        override fun decodePuzzle(line: String): FifthPuzzle {
            val bankLine = BankLine.decode(line)
            return FifthPuzzle(
                bankLine.id,
                bankLine.pack,
                bankLine.payload,
                bankLine.solution,
                bankLine.grade,
            )
        }

        override fun encodePuzzle(puzzle: FifthPuzzle): String = BankLine(
            puzzle.id,
            size = 4,
            payload = puzzle.payload,
            solution = puzzle.solution,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()

        override fun initialState(puzzle: FifthPuzzle): FifthState = FifthState(puzzle)

        override fun applyMove(state: FifthState, move: Move): FifthState = state

        override fun isSolved(puzzle: FifthPuzzle, state: FifthState): Boolean = true

        override fun mistakes(puzzle: FifthPuzzle, state: FifthState): List<Cell> = emptyList()

        override fun countSolutions(puzzle: FifthPuzzle, limit: Int): Int = 1

        override fun nextStep(puzzle: FifthPuzzle, state: FifthState): HintStep? = null

        override fun grade(puzzle: FifthPuzzle): Grade =
            Grade(FIFTH_TECHNIQUE, 3, puzzle.pack)

        override fun generate(rng: SplitMix64, pack: Pack): FifthPuzzle =
            FifthPuzzle(
                id = "FIFTH-${pack.name}-4d",
                pack = pack,
                payload = "layout-${rng.nextLong().toString(16)}",
                solution = "solve-${rng.nextLong().toString(16)}",
                storedGrade = "extra-rule:3",
            )

        override fun encodeState(state: FifthState): ByteArray = byteArrayOf()

        override fun decodeState(bytes: ByteArray): FifthState =
            FifthState(FifthPuzzle("FIFTH-RESTORED", Pack.EASY, "p", "s", "extra-rule:3"))

        private companion object {
            val FIFTH_TECHNIQUE = Technique("extra-rule", "Extra rule", 1)
        }
    }

    private data class FakePlayPuzzle(
        val id: String,
        val pack: Pack,
        val layout: String,
        val difficulty: String,
    )

    private class FakePlayValidator(
        val solutionPaths: Int,
        private val winnable: Boolean,
    ) : PlayBankValidator<FakePlayPuzzle> {
        override val id: String = "playfake"

        override fun decodePuzzle(line: String): FakePlayPuzzle {
            val bankLine = PlayBankLine.decode(line)
            return FakePlayPuzzle(bankLine.id, bankLine.pack, bankLine.payload, bankLine.difficulty)
        }

        override fun encodePuzzle(puzzle: FakePlayPuzzle): String = PlayBankLine(
            puzzle.id,
            size = 6,
            payload = puzzle.layout,
            pack = puzzle.pack,
            difficulty = puzzle.difficulty,
        ).encode()

        override fun generate(rng: SplitMix64, pack: Pack): FakePlayPuzzle =
            FakePlayPuzzle(
                id = "PLAY-${pack.name}-${rng.nextLong().toString(16)}",
                pack = pack,
                layout = if (winnable) "good-layout" else "broken-layout",
                difficulty = "tier-${pack.ordinal + 1}",
            )

        override fun isWinnable(puzzle: FakePlayPuzzle): Boolean =
            puzzle.layout != "broken-layout"

        override fun difficulty(puzzle: FakePlayPuzzle): String = puzzle.difficulty
    }
}