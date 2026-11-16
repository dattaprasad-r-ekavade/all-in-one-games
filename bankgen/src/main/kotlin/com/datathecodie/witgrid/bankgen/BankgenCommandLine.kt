package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Pack
import java.nio.file.Files
import java.nio.file.Path

internal class BankgenCommandLine(
    private val service: BankgenService,
    private val output: Appendable,
    private val error: Appendable,
) {
    fun run(arguments: Array<String>): Int = try {
        when (arguments.firstOrNull()) {
            "generate" -> generate(arguments.drop(1))
            "validate" -> validate(arguments.drop(1))
            "stats" -> stats(arguments.drop(1))
            "help", "--help", "-h", null -> {
                output.append(USAGE)
                0
            }

            else -> throw IllegalArgumentException("Unknown command '${arguments.first()}'")
        }
    } catch (failure: Exception) {
        error.append("Error: ").append(failure.message ?: failure.javaClass.simpleName).append('\n')
        1
    }

    private fun generate(arguments: List<String>): Int {
        val options = parseOptions(arguments)
        val daily = options.containsKey("daily")
        val allowed = if (daily) {
            setOf("game", "seed", "daily")
        } else {
            setOf("game", "pack", "count", "seed")
        }
        require(options.keys.all { it in allowed }) { "Unsupported generate option" }
        val gameId = requiredOption(options, "game")
        val seed = requiredOption(options, "seed").toLongOrNull()
            ?: throw IllegalArgumentException("--seed must be a signed 64-bit integer")
        val progress: (Int, Int) -> Unit = { completed, total ->
            if (completed == 1 || completed == total || completed % 25 == 0) {
                synchronized(error) {
                    error.append("Generated ").append(completed.toString()).append(" / ")
                        .append(total.toString()).append(" puzzles\n")
                }
            }
        }
        val outputPath = if (daily) {
            service.generateDaily(gameId, seed, progress)
        } else {
            val pack = parsePack(requiredOption(options, "pack"))
            val count = requiredOption(options, "count").toIntOrNull()
                ?: throw IllegalArgumentException("--count must be a positive integer")
            require(count > 0) { "--count must be a positive integer" }
            service.generate(gameId, pack, count, seed, progress)
        }
        output.append("Wrote ").append(outputPath.toString()).append('\n')
        return 0
    }

    private fun validate(arguments: List<String>): Int {
        require(arguments == listOf("--all")) { "Use: validate --all" }
        val report = service.validateAll()
        output.append("Validated ").append(report.puzzleCount.toString()).append(" puzzles in ")
            .append(report.files.size.toString()).append(" files\n")
        return 0
    }

    private fun stats(arguments: List<String>): Int {
        require(arguments.isEmpty()) { "Use: stats" }
        service.printStats(output)
        return 0
    }

    private fun parseOptions(arguments: List<String>): Map<String, String?> {
        val options = linkedMapOf<String, String?>()
        var index = 0
        while (index < arguments.size) {
            val option = arguments[index]
            require(option.startsWith("--")) { "Unexpected argument '$option'" }
            val name = option.removePrefix("--")
            require(name.isNotBlank() && name !in options) { "Unknown or repeated option '$option'" }
            if (name == "daily") {
                options[name] = null
                index++
            } else {
                require(index + 1 < arguments.size && !arguments[index + 1].startsWith("--")) {
                    "Option '$option' needs a value"
                }
                options[name] = arguments[index + 1]
                index += 2
            }
        }
        return options
    }

    private fun requiredOption(options: Map<String, String?>, name: String): String =
        requireNotNull(options[name]) { "Missing --$name" }

    private fun parsePack(value: String): Pack =
        Pack.values().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown pack '$value'")

    companion object {
        private const val USAGE = """
            bankgen generate --game <id> --pack <easy|medium|hard|expert> --count <n> --seed <s>
            bankgen generate --game <id> --daily --seed <s>
            bankgen validate --all
            bankgen stats
            Bank files are written to app/src/main/assets/bank/.

        """
    }
}

internal fun defaultBankDirectory(
    startingDirectory: Path = Path.of("").toAbsolutePath(),
): Path {
    var directory: Path? = startingDirectory.toAbsolutePath().normalize()
    while (directory != null) {
        if (Files.isRegularFile(directory.resolve("settings.gradle.kts")) &&
            Files.isDirectory(directory.resolve("app"))
        ) {
            return directory.resolve(Path.of("app", "src", "main", "assets", "bank"))
        }
        directory = directory.parent
    }
    throw IllegalStateException("Could not find the Witgrid repository root above $startingDirectory")
}
