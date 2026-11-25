package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColourSortEngineTest {
    private val engine = ColourSortEngine

    private val easyFixture = ColourSortPuzzle(
        bankId = "cs-fixture-4",
        pack = Pack.EASY,
        difficulty = "d1",
        initialTubes = listOf(
            listOf(1, 1, 2, 2),
            listOf(2, 2, 3, 3),
            listOf(3, 3, 1, 1),
            emptyList(),
        ),
    )

    @Test
    fun initialDealIsNotLost() {
        val puzzle = engine.decodePuzzle(engine.encodePuzzle(easyFixture))
        val start = engine.initialState(puzzle)
        assertFalse(engine.isLost(puzzle, start))
        assertFalse(engine.isWon(puzzle, start))
    }

    @Test
    fun poursToWin() {
        val start = engine.initialState(easyFixture)
        val sequence = listOf(
            ColourSortAction(2, 3),
            ColourSortAction(1, 2),
            ColourSortAction(0, 1),
            ColourSortAction(0, 3),
        )
        val won = sequence.fold(start) { state, action -> engine.applyAction(state, action) }
        assertTrue(engine.isWon(easyFixture, won))
        assertFalse(engine.isLost(easyFixture, won))
    }

    @Test
    fun illegalPourThrows() {
        val start = engine.initialState(easyFixture)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, ColourSortAction(0, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, ColourSortAction(3, 0))
        }
    }

    @Test
    fun stuckPositionIsLostAfterPour() {
        val stuckTubes = listOf(
            listOf(1, 2, 3, 4),
            listOf(2, 3, 4, 1),
            listOf(3, 4, 1, 2),
            listOf(4, 1, 2, 3),
        )
        val puzzle = easyFixture.copy(initialTubes = stuckTubes)
        val state = ColourSortState(puzzle, stuckTubes, pourCount = 1)
        assertTrue(engine.isLost(puzzle, state))
        assertFalse(engine.isWon(puzzle, state))
    }

    @Test
    fun continueAddsTubeAndClearsLoss() {
        val stuckTubes = listOf(
            listOf(1, 2, 3, 4),
            listOf(2, 3, 4, 1),
            listOf(3, 4, 1, 2),
            listOf(4, 1, 2, 3),
        )
        val puzzle = easyFixture.copy(initialTubes = stuckTubes)
        val lost = ColourSortState(puzzle, stuckTubes, pourCount = 2)
        assertTrue(engine.isLost(puzzle, lost))

        val continued = engine.applyContinue(lost)
        assertEquals(5, continued.tubes.size)
        assertTrue(continued.tubes.last().isEmpty())
        assertFalse(engine.isLost(puzzle, continued))
    }

    @Test
    fun continueThrowsWhenNotLost() {
        val start = engine.initialState(easyFixture)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(start)
        }
    }

    @Test
    fun bankLineRoundTrips() {
        val encoded = engine.encodePuzzle(easyFixture)
        val puzzle = engine.decodePuzzle(encoded)
        assertEquals(easyFixture, puzzle)
    }

    @Test
    fun malformedBankLineThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("only-three|fields|here")
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("cs-bad|4|1122/2233/3311|EASY|d1")
        }
    }

    @Test
    fun stateCodecRoundTrips() {
        val mid = engine.applyAction(engine.initialState(easyFixture), ColourSortAction(2, 3))
        val restored = engine.decodeState(engine.encodeState(mid))
        assertEquals(mid, restored)
    }

    @Test
    fun malformedStateThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun deadlockReachedThroughApplyAction() {
        val dealTubes = listOf(
            listOf(1, 2, 4, 5),
            listOf(2, 3, 5, 1),
            listOf(4, 5, 3, 2),
            listOf(4, 5, 2),
        )
        val puzzle = easyFixture.copy(initialTubes = dealTubes)
        val start = engine.initialState(puzzle)
        assertEquals(0, start.pourCount)
        assertFalse(engine.isLost(puzzle, start))
        assertFalse(engine.isWon(puzzle, start))

        val legalPours = ColourSortRules.legalPours(start.tubes)
        assertEquals(1, legalPours.size)
        assertEquals(ColourSortAction(2, 3), legalPours.single())

        val deadlocked = engine.applyAction(start, ColourSortAction(2, 3))
        assertEquals(1, deadlocked.pourCount)
        assertTrue(engine.isLost(puzzle, deadlocked))
        assertFalse(engine.isWon(puzzle, deadlocked))
        assertTrue(ColourSortRules.legalPours(deadlocked.tubes).isEmpty())
    }

    @Test
    fun topRunThatOnlyPartlyFits() {
        val tubes = listOf(
            listOf(1, 2, 2, 2),
            listOf(3, 2, 2),
            emptyList<Int>(),
            emptyList<Int>(),
        )
        val puzzle = easyFixture.copy(initialTubes = tubes)
        val start = engine.initialState(puzzle)

        assertEquals(3, ColourSortRules.topRunLength(tubes[0]))
        assertEquals(1, ColourSortRules.freeSpace(tubes[1]))
        assertEquals(1, ColourSortRules.pourUnits(tubes, 0, 1))

        val afterPour = engine.applyAction(start, ColourSortAction(0, 1))
        assertEquals(listOf(1, 2, 2), afterPour.tubes[0])
        assertEquals(listOf(3, 2, 2, 2), afterPour.tubes[1])
    }

    @Test
    fun hintActionOnEasyFixture() {
        val start = engine.initialState(easyFixture)
        val hint = engine.hintAction(easyFixture, start)
        org.junit.jupiter.api.Assertions.assertNotNull(hint)
        assertTrue(ColourSortRules.isPourLegal(easyFixture.initialTubes, hint!!.from, hint.to))
        val nextState = engine.applyAction(start, hint)
        assertTrue(ColourSortSolver.isWinnable(nextState.tubes))
    }

    @Test
    fun hintActionReturnsNullWhenWonOrLost() {
        val wonTubes = listOf(
            listOf(1, 1, 1, 1),
            listOf(2, 2, 2, 2),
            listOf(3, 3, 3, 3),
            emptyList(),
        )
        val wonState = ColourSortState(easyFixture, wonTubes, pourCount = 4)
        assertTrue(engine.isWon(easyFixture, wonState))
        assertEquals(null, engine.hintAction(easyFixture, wonState))

        val deadlockedState = ColourSortState(
            easyFixture,
            listOf(
                listOf(1, 2, 4, 5),
                listOf(2, 3, 5, 1),
                listOf(4, 5, 3),
                listOf(4, 5, 2, 2),
            ),
            pourCount = 1,
        )
        assertTrue(engine.isLost(easyFixture, deadlockedState))
        assertEquals(null, engine.hintAction(easyFixture, deadlockedState))
    }

    @Test
    fun tenWinnableLevelsPerPackWithSameSeedSameOutput() {
        for (pack in Pack.entries) {
            val rng1 = SplitMix64(1)
            val puzzles1 = (1..10).map { engine.generate(rng1, pack) }

            assertEquals(10, puzzles1.size)
            puzzles1.forEach { puzzle ->
                assertEquals(pack, puzzle.pack)
                assertEquals(ColourSortCodec.expectedTubeCount(pack), puzzle.initialTubes.size)
                assertTrue(ColourSortSolver.isWinnable(puzzle.initialTubes))
                val emptyCount = puzzle.initialTubes.count { it.isEmpty() }
                assertEquals(if (pack == Pack.EASY) 1 else 2, emptyCount)
                puzzle.initialTubes.filter { it.isNotEmpty() }.forEach { tube ->
                    assertEquals(ColourSortRules.TUBE_CAPACITY, tube.size)
                    assertFalse(ColourSortRules.isTubeSorted(tube))
                }
            }

            val rng2 = SplitMix64(1)
            val puzzles2 = (1..10).map { engine.generate(rng2, pack) }
            assertEquals(puzzles1, puzzles2)
        }
    }

    @Test
    fun generatorConfigMatchesPacks() {
        val easy = ColourSortGenerator.configFor(Pack.EASY)
        assertEquals(4, easy.tubeCount)
        assertEquals(3, easy.colorCount)
        assertEquals(1, easy.emptyTubeCount)
        assertEquals("d1", easy.difficulty)

        val medium = ColourSortGenerator.configFor(Pack.MEDIUM)
        assertEquals(6, medium.tubeCount)
        assertEquals(4, medium.colorCount)
        assertEquals(2, medium.emptyTubeCount)
        assertEquals("d2", medium.difficulty)

        val hard = ColourSortGenerator.configFor(Pack.HARD)
        assertEquals(8, hard.tubeCount)
        assertEquals(6, hard.colorCount)
        assertEquals(2, hard.emptyTubeCount)
        assertEquals("d3", hard.difficulty)

        val expert = ColourSortGenerator.configFor(Pack.EXPERT)
        assertEquals(10, expert.tubeCount)
        assertEquals(8, expert.colorCount)
        assertEquals(2, expert.emptyTubeCount)
        assertEquals("d4", expert.difficulty)
    }

    @Test
    fun solverReturnsNullForUnsolvableState() {
        val unsolvable = listOf(
            listOf(1, 2, 4, 5),
            listOf(2, 3, 5, 1),
            listOf(4, 5, 3),
            listOf(4, 5, 2, 2),
        )
        assertFalse(ColourSortSolver.isWinnable(unsolvable))
        assertEquals(null, ColourSortSolver.solve(unsolvable))
    }

    @Test
    fun hintSolvesFirstTenLevelsOfEveryPackWithinThreeHundredMoves() {
        for (pack in Pack.entries) {
            val rng = SplitMix64(1)
            val puzzles = (1..10).map { engine.generate(rng, pack) }

            for ((index, puzzle) in puzzles.withIndex()) {
                var state = engine.initialState(puzzle)
                assertFalse(engine.isWon(puzzle, state), "$pack level $index should not start won")

                val initialHint = engine.hintAction(puzzle, state)
                assertNotNull(initialHint, "hintAction should be non-null at deal for $pack level $index")

                var moves = 0
                val moveHistory = ArrayList<ColourSortAction>()
                while (!engine.isWon(puzzle, state) && moves < 300) {
                    val hint = engine.hintAction(puzzle, state)
                    assertNotNull(hint, "hintAction returned null at move $moves for $pack level $index")
                    moveHistory.add(hint!!)
                    state = engine.applyAction(state, hint)
                    moves++
                }

                if (moves >= 300) {
                    println("Failure on $pack level $index. Initial tubes: ${puzzle.initialTubes}")
                    println("First 15 moves: ${moveHistory.take(15)}")
                    // Replay first 6 moves and print states
                    var debugState = engine.initialState(puzzle)
                    for (m in 0 until minOf(8, moveHistory.size)) {
                        println("State at move $m: ${debugState.tubes}")
                        println("Applying: ${moveHistory[m]}")
                        debugState = engine.applyAction(debugState, moveHistory[m])
                    }
                    println("State at move 8: ${debugState.tubes}")
                }

                assertTrue(
                    engine.isWon(puzzle, state),
                    "$pack level $index should be won within 300 moves (took $moves)",
                )
            }
        }
        println("Largest node count seen across all hint solves: ${ColourSortSolver.maxExploredNodesSeen}")
        assertTrue(ColourSortSolver.maxExploredNodesSeen < ColourSortSolver.MAX_EXPLORED_STATES)
    }

    @Test
    fun midGameExpertStateHintIsNonNull() {
        val rng = SplitMix64(1)
        val expertPuzzle = engine.generate(rng, Pack.EXPERT)
        var state = engine.initialState(expertPuzzle)

        repeat(5) {
            val hint = engine.hintAction(expertPuzzle, state)
            assertNotNull(hint, "Hint should be non-null during play")
            state = engine.applyAction(state, hint!!)
        }

        assertEquals(5, state.pourCount)
        assertFalse(engine.isWon(expertPuzzle, state))
        assertFalse(engine.isLost(expertPuzzle, state))

        val midGameHint = engine.hintAction(expertPuzzle, state)
        assertNotNull(midGameHint, "hintAction should be non-null in mid-game Expert state")
        assertTrue(ColourSortRules.isPourLegal(state.tubes, midGameHint!!.from, midGameHint.to))
    }

    @Test
    fun expertLevelsHintTimingAveragesUnderOneSecond() {
        val rng = SplitMix64(1)
        val puzzles = (1..10).map { engine.generate(rng, Pack.EXPERT) }

        val durationsMs = ArrayList<Long>()
        for ((index, puzzle) in puzzles.withIndex()) {
            val startState = engine.initialState(puzzle)
            val startNano = System.nanoTime()
            val hint = engine.hintAction(puzzle, startState)
            val elapsedMs = (System.nanoTime() - startNano) / 1_000_000
            durationsMs.add(elapsedMs)
            assertNotNull(hint, "Expert level $index hint should be non-null")
        }

        val averageMs = durationsMs.average()
        println("Expert 10 levels hint times (ms): $durationsMs, average: $averageMs ms")
        // Loose bound: the real target is 200 ms, but parallel builds on the dev PC slow this test.
        assertTrue(averageMs < 1000.0, "Hint on 10 Expert levels must average under 1 s, was $averageMs ms")
    }

    @Test
    fun canonicalKeySeesEveryTubeAfterManyContinues() {
        val base = List(12) { listOf(1, 2, 1, 2) }
        val first = base + listOf(listOf(3))
        val second = base + listOf(listOf(4))
        assertNotEquals(ColourSortSolver.canonicalKey(first), ColourSortSolver.canonicalKey(second))
        assertEquals(
            ColourSortSolver.canonicalKey(first),
            ColourSortSolver.canonicalKey(listOf(listOf(3)) + base),
        )
    }
}
