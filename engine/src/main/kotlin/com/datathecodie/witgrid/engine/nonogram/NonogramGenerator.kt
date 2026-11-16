package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Seeded random-pattern generator with uniqueness and technique-grade rejection. */
object NonogramGenerator {
    private const val MAX_PATTERN_ATTEMPTS = 20_000

    /** Builds one unique puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): NonogramPuzzle {
        val size = NonogramPackBands.sizeForPack(pack)
        val densityPercent = densityTargetPercent(pack)
        repeat(MAX_PATTERN_ATTEMPTS) {
            val solution = List(size * size) {
                if (rng.nextInt(100) < densityPercent) 1 else 0
            }
            val clues = NonogramRules.cluesForSolution(size, solution)
            val candidate = NonogramPuzzle(
                id = "NO-CANDIDATE-${pack.name}",
                size = size,
                rowClues = clues.first,
                columnClues = clues.second,
                solution = solution,
                pack = pack,
                storedGrade = "pending",
            )
            val grade = try {
                NonogramTechniqueSolver.grade(candidate)
            } catch (_: IllegalArgumentException) {
                return@repeat
            }
            return NonogramPuzzle(
                id = "NO-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}",
                size = candidate.size,
                rowClues = candidate.rowClues,
                columnClues = candidate.columnClues,
                solution = candidate.solution,
                pack = candidate.pack,
                storedGrade = NonogramGradeCodec.encode(grade),
            )
        }
        throw IllegalStateException(
            "Could not generate a technique-solvable Nonogram puzzle in $MAX_PATTERN_ATTEMPTS attempts",
        )
    }

    private fun densityTargetPercent(pack: Pack): Int = when (pack) {
        Pack.EASY -> 40
        Pack.MEDIUM -> 45
        Pack.HARD, Pack.EXPERT -> 50
    }
}
