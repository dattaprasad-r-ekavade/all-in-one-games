package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsGeneratorTest {

    @Test
    fun seedOneGeneratesTenDeterministicPuzzlesForEveryPack() {
        for (pack in Pack.entries) {
            val firstRun = generateBatch(1L, pack, 10)
            val secondRun = generateBatch(1L, pack, 10)

            val firstEncoded = firstRun.map(TacticsEngine::encodePuzzle)
            val secondEncoded = secondRun.map(TacticsEngine::encodePuzzle)

            // Deterministic output on second run
            assertEquals(firstEncoded, secondEncoded, "Puzzles must be reproducible with the same seed for $pack")
            assertEquals(10, firstRun.size)
        }
    }

    @Test
    fun seedOneSuppliesAtLeast241DistinctSolvablePositionsForEveryPack() {
        for (pack in Pack.entries) {
            val puzzles = TacticsEngine.generatePack(SplitMix64(1L), pack, 241)
            assertEquals(241, puzzles.size, "Must generate exactly 241 puzzles for $pack")

            // Unique IDs and distinct board positions
            assertEquals(241, puzzles.map { it.id }.toSet().size, "All puzzle IDs must be distinct for $pack")
            assertEquals(241, puzzles.map { it.initialBoard }.toSet().size, "All board positions must be distinct for $pack")
            assertEquals(241, puzzles.map { it.initialBoard.toFen() }.toSet().size, "All FEN positions must be distinct for $pack")

            puzzles.forEach { puzzle ->
                // Unique solution under mate sequence rule
                assertUnique(TacticsEngine, puzzle)

                // Solvable by technique solver
                assertTechniqueSolves(TacticsEngine, puzzle)

                // Codec round-trip
                val encoded = TacticsEngine.encodePuzzle(puzzle)
                val decoded = TacticsEngine.decodePuzzle(encoded)
                assertEquals(puzzle, decoded)

                // Correct pack band and valid grade format
                val grade = TacticsEngine.grade(puzzle)
                assertEquals(pack, grade.pack, "Grade pack must match puzzle pack")
                assertEquals(TacticsGradeCodec.encode(grade), puzzle.storedGrade)
                TacticsGradeCodec.validate(puzzle.storedGrade)

                // Valid chess legality
                assertFalse(puzzle.initialBoard.isCheck(PieceColor.BLACK), "Side not to move must not be in check")
                assertEquals(1, puzzle.initialBoard.pieces.count { it?.color == PieceColor.WHITE && it.type == PieceType.KING })
                assertEquals(1, puzzle.initialBoard.pieces.count { it?.color == PieceColor.BLACK && it.type == PieceType.KING })

                // Pawns not on back ranks
                for (col in 0..7) {
                    assertFalse(puzzle.initialBoard.pieceAt(0, col)?.type == PieceType.PAWN, "No pawn on rank 8")
                    assertFalse(puzzle.initialBoard.pieceAt(7, col)?.type == PieceType.PAWN, "No pawn on rank 1")
                }
            }
        }
    }

    @Test
    fun generatorRejectsSeenPositionsAndEnsuresNovelStartingPosition() {
        for (pack in Pack.entries) {
            val baseline = TacticsGenerator.generate(SplitMix64(42L), pack)
            val filtered = TacticsGenerator.generate(
                SplitMix64(42L),
                pack,
                seenPositions = setOf(baseline.initialBoard),
            )
            assertNotEquals(
                baseline.initialBoard,
                filtered.initialBoard,
                "Generator must reject starting positions present in seenPositions for $pack",
            )
            assertUnique(TacticsEngine, filtered)
            assertTechniqueSolves(TacticsEngine, filtered)
        }
    }

    @Test
    fun regularAndDailyBankSeedsYieldDistinctStartingPositionsAtLaunchVolume() {
        // The bank forks one SplitMix64 stream per file, then seeds one generator per entry.
        val regular = Pack.entries.associateWith { pack ->
            val seeds = SplitMix64(1)
            List(150) { TacticsEngine.generate(SplitMix64(seeds.nextLong()), pack) }
        }
        val dailySeeds = SplitMix64(2)
        val daily = List(365) { index ->
            val pack = Pack.entries[index % Pack.entries.size]
            TacticsEngine.generate(SplitMix64(dailySeeds.nextLong()), pack)
        }.groupBy { it.pack }

        val seenBoards = hashSetOf<TacticsBoard>()
        val seenIds = hashSetOf<String>()
        for (pack in Pack.entries) {
            val puzzles = regular.getValue(pack) + daily.getValue(pack)
            val distinctPositions = puzzles.map { it.initialBoard }.toSet()
            assertEquals(puzzles.size, distinctPositions.size, "Distinct regular and daily positions for $pack")
            assertTrue(distinctPositions.all(seenBoards::add), "Positions must differ across packs")
            assertTrue(puzzles.all { seenIds.add(it.id) }, "IDs must differ across files")
        }
        assertEquals(965, seenBoards.size)
    }

    @Test
    fun boundarySeedsGenerateValidPuzzlesForEveryPack() {
        val boundarySeeds = listOf(0L, -1L, 2L, Long.MIN_VALUE, Long.MAX_VALUE)
        for (pack in Pack.entries) {
            val puzzles = boundarySeeds.map { TacticsEngine.generate(SplitMix64(it), pack) }
            assertEquals(boundarySeeds.size, puzzles.map { it.initialBoard.toFen() }.toSet().size)

            puzzles.forEach { puzzle ->
                assertUnique(TacticsEngine, puzzle)
                assertTechniqueSolves(TacticsEngine, puzzle)
                assertEquals(pack, TacticsEngine.grade(puzzle).pack)
            }
        }
    }

    @Test
    fun pieceCountBandsMatchPlanSpecifications() {
        // Medium <= 10 pieces, Hard > 10 pieces
        for (seed in 1L..5L) {
            val medium = TacticsEngine.generate(SplitMix64(seed), Pack.MEDIUM)
            val mediumPieces = medium.initialBoard.pieces.count { it != null }
            assertTrue(
                mediumPieces <= TacticsPackBands.MAX_MEDIUM_PIECES,
                "Medium puzzle must have <= ${TacticsPackBands.MAX_MEDIUM_PIECES} pieces, had $mediumPieces",
            )
            assertEquals(2, medium.mateIn)

            val hard = TacticsEngine.generate(SplitMix64(seed), Pack.HARD)
            val hardPieces = hard.initialBoard.pieces.count { it != null }
            assertTrue(
                hardPieces > TacticsPackBands.MAX_MEDIUM_PIECES,
                "Hard puzzle must have > ${TacticsPackBands.MAX_MEDIUM_PIECES} pieces, had $hardPieces",
            )
            assertEquals(2, hard.mateIn)
            assertTrue(openingChecks(medium) <= 2, "Medium should offer at most two checking moves")
            assertTrue(openingChecks(hard) >= 3, "Hard should offer at least three checking moves")

            val expert = TacticsEngine.generate(SplitMix64(seed), Pack.EXPERT)
            assertEquals(3, expert.mateIn)
        }
    }

    private fun openingChecks(puzzle: TacticsPuzzle): Int {
        val board = puzzle.initialBoard
        return board.legalMoves().count { board.applyMove(it).isCheck() }
    }

    private fun generateBatch(seed: Long, pack: Pack, count: Int): List<TacticsPuzzle> {
        val rng = SplitMix64(seed)
        return (0 until count).map { TacticsEngine.generate(rng, pack) }
    }
}
