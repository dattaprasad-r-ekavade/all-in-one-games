package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal data class ValidatedBankFile(
    val name: String,
    val gameId: String,
    val pack: Pack?,
    val entries: List<BankEntry>,
) {
    val gradeHistogram: Map<String, Int>
        get() = entries.groupingBy { it.grade }.eachCount().toSortedMap()
}

internal data class ValidationReport(val files: List<ValidatedBankFile>) {
    val puzzleCount: Int
        get() = files.sumOf { it.entries.size }
}

/** File and generation operations for the offline puzzle bank. */
internal class BankgenService(
    games: List<BankGame>,
    private val bankDirectory: Path,
    private val workerCount: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 8),
) {
    private val gamesById = games.associateBy { it.id }

    init {
        require(gamesById.size == games.size) { "Bank game IDs must be unique" }
        require(workerCount > 0) { "Bank generator needs at least one worker" }
    }

    fun generate(
        gameId: String,
        pack: Pack,
        count: Int,
        seed: Long,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): Path {
        require(count > 0) { "Puzzle count must be positive" }
        return generateFile(gameId, List(count) { pack }, "${gameId}-${pack.name.lowercase()}", seed, onProgress)
    }

    fun generateDaily(
        gameId: String,
        seed: Long,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): Path = generateFile(
        gameId = gameId,
        packs = List(DAILY_PUZZLE_COUNT) { Pack.values()[it % Pack.values().size] },
        fileStem = "$gameId-daily",
        seed = seed,
        onProgress = onProgress,
    )

    fun validateAll(): ValidationReport {
        require(Files.isDirectory(bankDirectory)) { "Bank directory does not exist: $bankDirectory" }
        val files = Files.list(bankDirectory).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(BANK_FILE_SUFFIX) }
                .sorted()
                .toList()
        }
        require(files.isNotEmpty()) { "No $BANK_FILE_SUFFIX files found in $bankDirectory" }

        val seenIds = HashSet<String>()
        val seenPuzzles = HashSet<String>()
        val validatedFiles = files.map { path ->
            val info = bankFileInfo(path.fileName.toString())
            val game = requireNotNull(gamesById[info.gameId]) {
                "No registered game engine for '${info.gameId}'"
            }
            val lines = readGzipLines(path)
            require(lines.isNotEmpty()) { "Bank file ${path.fileName} is empty" }
            if (info.pack == null) {
                require(lines.size == DAILY_PUZZLE_COUNT) {
                    "Daily file ${path.fileName} must contain $DAILY_PUZZLE_COUNT puzzles"
                }
            }
            val entries = lines.mapIndexed { index, line ->
                try {
                    val entry = game.validateLine(line, info.pack)
                    require(seenIds.add("${game.id}|${entry.id}")) {
                        "Duplicate puzzle ID '${entry.id}' in the bank"
                    }
                    require(seenPuzzles.add("${game.id}|${entry.fingerprint}")) {
                        "Duplicate ${game.id} puzzle in ${path.fileName}"
                    }
                    entry
                } catch (failure: IllegalArgumentException) {
                    throw BankValidationException(
                        "${path.fileName} line ${index + 1}: ${failure.message}",
                        failure,
                    )
                }
            }
            if (info.pack == null) {
                if (entries.map { it.pack }.toSet() != Pack.values().toSet()) {
                    throw BankValidationException("${path.fileName}: daily file must include every pack")
                }
            }
            ValidatedBankFile(path.fileName.toString(), game.id, info.pack, entries)
        }
        return ValidationReport(validatedFiles)
    }

    fun printStats(appendable: Appendable) {
        val report = validateAll()
        report.files.forEach { file ->
            appendable.append(file.name).append(": ").append(file.entries.size.toString()).append(" puzzles\n")
            file.gradeHistogram.forEach { (grade, count) ->
                appendable.append("  ").append(grade).append(": ").append(count.toString()).append('\n')
            }
        }
        appendable.append("Total: ").append(report.puzzleCount.toString()).append(" puzzles in ")
            .append(report.files.size.toString()).append(" files\n")
    }

    private fun generateFile(
        gameId: String,
        packs: List<Pack>,
        fileStem: String,
        seed: Long,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): Path {
        require(packs.isNotEmpty()) { "Puzzle count must be positive" }
        val game = requireNotNull(gamesById[gameId]) { "Unknown game '$gameId'" }
        val seedStream = SplitMix64(seed)
        val puzzleSeeds = List(packs.size) { seedStream.nextLong() }
        val completed = AtomicInteger()
        val executor = Executors.newFixedThreadPool(minOf(workerCount, packs.size))
        val lines = try {
            val work = packs.indices.map { index ->
                Callable {
                    val line = game.generateLine(puzzleSeeds[index], packs[index])
                    onProgress(completed.incrementAndGet(), packs.size)
                    line
                }
            }
            executor.invokeAll(work).map { future ->
                try {
                    future.get()
                } catch (failure: ExecutionException) {
                    val cause = failure.cause ?: failure
                    throw IllegalStateException("Puzzle generation failed: ${cause.message}", cause)
                }
            }
        } finally {
            executor.shutdownNow()
        }

        val seenIds = HashSet<String>()
        val seenPuzzles = HashSet<String>()
        lines.forEachIndexed { index, line ->
            val entry = game.validateLine(line, packs[index])
            require(seenIds.add(entry.id)) { "Generator returned duplicate puzzle ID '${entry.id}'" }
            require(seenPuzzles.add(entry.fingerprint)) { "Generator returned a duplicate puzzle" }
        }
        Files.createDirectories(bankDirectory)
        val output = bankDirectory.resolve("$fileStem$BANK_FILE_SUFFIX")
        writeGzipLinesAtomically(output, lines)
        return output
    }

    private fun bankFileInfo(fileName: String): BankFileInfo {
        require(fileName.endsWith(BANK_FILE_SUFFIX)) { "Unsupported bank file '$fileName'" }
        val stem = fileName.removeSuffix(BANK_FILE_SUFFIX)
        val delimiter = stem.lastIndexOf('-')
        require(delimiter > 0 && delimiter < stem.lastIndex) { "Invalid bank file name '$fileName'" }
        val gameId = stem.substring(0, delimiter)
        val packName = stem.substring(delimiter + 1)
        val pack = if (packName == "daily") null else {
            requireNotNull(Pack.values().firstOrNull { it.name.equals(packName, ignoreCase = true) }) {
                "Invalid pack in bank file '$fileName'"
            }
        }
        return BankFileInfo(gameId, pack)
    }

    private fun readGzipLines(path: Path): List<String> = try {
        GZIPInputStream(Files.newInputStream(path)).bufferedReader(StandardCharsets.UTF_8)
            .use(BufferedReader::readLines)
    } catch (failure: IOException) {
        throw BankValidationException("Could not read bank file ${path.fileName}: ${failure.message}", failure)
    }

    private fun writeGzipLinesAtomically(path: Path, lines: List<String>) {
        val temporaryPath = path.resolveSibling("${path.fileName}.tmp")
        try {
            GZIPOutputStream(Files.newOutputStream(temporaryPath)).use { compressed ->
                BufferedWriter(OutputStreamWriter(compressed, StandardCharsets.UTF_8)).use { writer ->
                    lines.forEach { line ->
                        writer.write(line)
                        writer.write('\n'.code)
                    }
                }
            }
            try {
                Files.move(temporaryPath, path, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporaryPath, path, REPLACE_EXISTING)
            }
        } catch (failure: Exception) {
            Files.deleteIfExists(temporaryPath)
            throw failure
        }
    }

    private data class BankFileInfo(val gameId: String, val pack: Pack?)

    companion object {
        const val DAILY_PUZZLE_COUNT: Int = 365
        private const val BANK_FILE_SUFFIX = ".txt.gz"
    }
}

internal class BankValidationException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
