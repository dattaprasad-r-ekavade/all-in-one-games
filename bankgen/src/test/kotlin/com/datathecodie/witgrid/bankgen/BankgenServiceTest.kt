package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.core.Technique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class BankgenServiceTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun generationUsesWorkerThreadsAndKeepsPuzzleLinesStableForASeed() {
        val firstDirectory = temporaryDirectory.resolve("first")
        val secondDirectory = temporaryDirectory.resolve("second")
        val progress = java.util.concurrent.atomic.AtomicInteger()
        val parallelEngine = FakeEngine(CountDownLatch(3))
        val firstService = service(firstDirectory, workerCount = 3, engine = parallelEngine)
        val firstPath = firstService.generate("fake", Pack.EASY, 12, 991) { _, _ ->
            progress.incrementAndGet()
        }
        val secondPath = service(secondDirectory, workerCount = 1).generate("fake", Pack.EASY, 12, 991)
        val firstLines = readGzip(firstPath)
        val secondLines = readGzip(secondPath)

        assertEquals(firstLines, secondLines)
        assertTrue(parallelEngine.generationThreads.size >= 3)
        assertTrue(Files.readAllBytes(firstPath).contentEquals(Files.readAllBytes(secondPath)))
        assertEquals(12, firstLines.size)
        assertEquals(12, progress.get())
        assertEquals(12, firstLines.map { BankLine.decode(it).id }.toSet().size)
        assertEquals(12, service(firstDirectory).validateAll().puzzleCount)

        val stats = StringBuilder()
        service(firstDirectory).printStats(stats)
        assertTrue(stats.contains("fake-step:1: 12"))
        assertTrue(stats.contains("Total: 12 puzzles in 1 files"))
    }

    @Test
    fun dailyGenerationWrites365PuzzlesAcrossMixedPacks() {
        val path = service(temporaryDirectory).generateDaily("fake", seed = 31)
        val lines = readGzip(path)
        val packCounts = lines.map { BankLine.decode(it).pack }.groupingBy { it }.eachCount()

        assertEquals("fake-daily.txt.gz", path.fileName.toString())
        assertEquals(365, lines.size)
        assertEquals(92, packCounts[Pack.EASY])
        assertEquals(91, packCounts[Pack.MEDIUM])
        assertEquals(91, packCounts[Pack.HARD])
        assertEquals(91, packCounts[Pack.EXPERT])
        assertEquals(365, service(temporaryDirectory).validateAll().puzzleCount)
    }

    @Test
    fun validationRejectsDailyFilesThatOmitAPack() {
        val bankDirectory = temporaryDirectory.resolve("single-pack-daily")
        Files.createDirectories(bankDirectory)
        val engine = FakeEngine()
        val lines = (0 until BankgenService.DAILY_PUZZLE_COUNT).map { index ->
            engine.encodePuzzle(engine.generate(SplitMix64(index.toLong()), Pack.EASY))
        }
        writeGzip(bankDirectory.resolve("fake-daily.txt.gz"), lines)

        val failure = org.junit.jupiter.api.Assertions.assertThrows(BankValidationException::class.java) {
            service(bankDirectory).validateAll()
        }

        assertTrue(failure.message.orEmpty().contains("must include every pack"))
    }

    @Test
    fun validateAllFailsWithFileAndLineForACorruptedPuzzle() {
        val bankDirectory = temporaryDirectory.resolve("bank")
        Files.createDirectories(bankDirectory)
        val validLine = fakeGame().generateLine(8, Pack.MEDIUM)
        writeGzip(bankDirectory.resolve("fake-medium.txt.gz"), listOf(validLine, "broken"))
        val output = StringBuilder()
        val errors = StringBuilder()

        val exitCode = BankgenCommandLine(service(bankDirectory), output, errors)
            .run(arrayOf("validate", "--all"))

        assertEquals(1, exitCode)
        assertTrue(errors.contains("fake-medium.txt.gz line 2"))
        assertFalse(output.contains("Validated"))
    }

    @Test
    fun validationRejectsDuplicatePuzzleContentWithDifferentIds() {
        val bankDirectory = temporaryDirectory.resolve("duplicates")
        Files.createDirectories(bankDirectory)
        val engine = FakeEngine()
        val first = engine.encodePuzzle(FakePuzzle("FAKE-A", Pack.EASY, "same", "solution", "fake-step:1"))
        val second = engine.encodePuzzle(FakePuzzle("FAKE-B", Pack.EASY, "same", "solution", "fake-step:1"))
        writeGzip(bankDirectory.resolve("fake-easy.txt.gz"), listOf(first, second))

        val failure = org.junit.jupiter.api.Assertions.assertThrows(BankValidationException::class.java) {
            service(bankDirectory).validateAll()
        }

        assertTrue(failure.message.orEmpty().contains("Duplicate fake puzzle"))
    }

    @Test
    fun validationRejectsAmbiguousPuzzlesWrongGradesAndWrongFilePacks() {
        val bankDirectory = temporaryDirectory.resolve("invalid-records")
        Files.createDirectories(bankDirectory)
        val engine = FakeEngine()
        val cases = listOf(
            "fake-easy.txt.gz" to engine.encodePuzzle(
                FakePuzzle("FAKE-AMBIGUOUS", Pack.EASY, "ambiguous", "solution", "fake-step:1"),
            ),
            "fake-hard.txt.gz" to engine.encodePuzzle(
                FakePuzzle("FAKE-WRONG-PACK", Pack.MEDIUM, "payload", "solution", "fake-step:1"),
            ),
            "fake-medium.txt.gz" to engine.encodePuzzle(
                FakePuzzle("FAKE-WRONG-GRADE", Pack.MEDIUM, "payload", "solution", "wrong-grade"),
            ),
        )

        cases.forEach { (fileName, line) ->
            writeGzip(bankDirectory.resolve(fileName), listOf(line))
        }

        cases.forEach { (fileName, _) ->
            val failure = org.junit.jupiter.api.Assertions.assertThrows(BankValidationException::class.java) {
                service(bankDirectory).validateAll()
            }
            assertTrue(failure.message.orEmpty().contains(fileName))
            Files.delete(bankDirectory.resolve(fileName))
        }
    }

    @Test
    fun validationReportsTruncatedGzipFileName() {
        val bankDirectory = temporaryDirectory.resolve("truncated")
        Files.createDirectories(bankDirectory)
        val path = bankDirectory.resolve("fake-easy.txt.gz")
        val completeGzip = ByteArrayOutputStream().use { buffer ->
            GZIPOutputStream(buffer).use { gzip -> gzip.write("valid-line".toByteArray()) }
            buffer.toByteArray()
        }
        Files.write(path, completeGzip.copyOf(completeGzip.size - 4))

        val failure = org.junit.jupiter.api.Assertions.assertThrows(BankValidationException::class.java) {
            service(bankDirectory).validateAll()
        }

        assertTrue(failure.message.orEmpty().contains("fake-easy.txt.gz"))
    }

    @Test
    fun commandLineGeneratesPackFileAndRejectsBadArguments() {
        val output = StringBuilder()
        val errors = StringBuilder()
        val cli = BankgenCommandLine(service(temporaryDirectory), output, errors)
        val exitCode = cli.run(
            arrayOf(
                "generate",
                "--game", "fake",
                "--pack", "hard",
                "--count", "3",
                "--seed", "5",
            ),
        )

        assertEquals(0, exitCode)
        assertTrue(output.contains("fake-hard.txt.gz"))
        assertEquals(3, readGzip(temporaryDirectory.resolve("fake-hard.txt.gz")).size)
        assertEquals(1, cli.run(arrayOf("generate", "--game", "missing", "--pack", "easy", "--count", "1", "--seed", "1")))
    }

    @Test
    fun defaultBankDirectoryResolvesFromTheGradleModuleWorkingDirectory() {
        val currentDirectory = Path.of("").toAbsolutePath().normalize()

        assertEquals(defaultBankDirectory(), defaultBankDirectory(currentDirectory.resolve("bankgen")))
    }

    private fun service(
        directory: Path,
        workerCount: Int = 3,
        engine: FakeEngine = FakeEngine(),
    ) = BankgenService(
        games = listOf(fakeGame(engine)),
        bankDirectory = directory,
        workerCount = workerCount,
    )

    private fun fakeGame(engine: FakeEngine = FakeEngine()): BankGame = EngineBankGame(engine) { grade ->
        "${grade.hardestTechnique?.id ?: "none"}:${grade.stepCount}"
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

    private data class FakePuzzle(
        val id: String,
        val pack: Pack,
        val payload: String,
        val solution: String,
        val storedGrade: String,
    )

    private data class FakeState(val puzzle: FakePuzzle)

    private class FakeEngine(private val generationBarrier: CountDownLatch? = null) : GameEngine<FakePuzzle, FakeState> {
        val generationThreads = ConcurrentHashMap.newKeySet<String>()

        override val id: String = "fake"

        override fun decodePuzzle(line: String): FakePuzzle {
            val bankLine = BankLine.decode(line)
            return FakePuzzle(
                bankLine.id,
                bankLine.pack,
                bankLine.payload,
                bankLine.solution,
                bankLine.grade,
            )
        }

        override fun encodePuzzle(puzzle: FakePuzzle): String = BankLine(
            puzzle.id,
            size = 1,
            payload = puzzle.payload,
            solution = puzzle.solution,
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()

        override fun initialState(puzzle: FakePuzzle): FakeState = FakeState(puzzle)

        override fun applyMove(state: FakeState, move: Move): FakeState = state

        override fun isSolved(puzzle: FakePuzzle, state: FakeState): Boolean = true

        override fun mistakes(puzzle: FakePuzzle, state: FakeState): List<Cell> = emptyList()

        override fun countSolutions(puzzle: FakePuzzle, limit: Int): Int =
            if (puzzle.payload == "ambiguous") minOf(2, limit) else 1

        override fun nextStep(puzzle: FakePuzzle, state: FakeState): HintStep? = null

        override fun grade(puzzle: FakePuzzle): Grade = Grade(FAKE_TECHNIQUE, 1, puzzle.pack)

        override fun generate(rng: SplitMix64, pack: Pack): FakePuzzle {
            generationThreads += Thread.currentThread().name
            generationBarrier?.let { barrier ->
                barrier.countDown()
                check(barrier.await(5, TimeUnit.SECONDS)) { "Generation did not use concurrent workers" }
            }
            val token = rng.nextLong().toString(16)
            return FakePuzzle(
                id = "FAKE-${pack.name}-$token",
                pack = pack,
                payload = "payload-$token",
                solution = "solution-$token",
                storedGrade = "fake-step:1",
            )
        }

        override fun encodeState(state: FakeState): ByteArray = byteArrayOf()

        override fun decodeState(bytes: ByteArray): FakeState = FakeState(
            FakePuzzle("FAKE-RESTORED", Pack.EASY, "payload", "solution", "fake-step:1"),
        )

        private companion object {
            val FAKE_TECHNIQUE = Technique("fake-step", "Fake step", 0)
        }
    }
}
