package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Compile-and-run examples of lane-owned action types, without a shared action base class. */
class PlayEngineTest {
    @Test
    fun playBankLineRoundTripsFiveFields() {
        val original = PlayBankLine("fb-test-1", 3, "steps=2", Pack.EASY, "d1")
        assertEquals(original, PlayBankLine.decode(original.encode()))
    }

    @Test
    fun counterEngineWinLoseHintContinueAndCodec() {
        val engine = CounterEngine
        val puzzle = engine.decodePuzzle(engine.encodePuzzle(CounterPuzzle(2, Pack.EASY)))
        val start = engine.initialState(puzzle)

        assertFalse(engine.isWon(puzzle, start))
        assertFalse(engine.isLost(puzzle, start))

        val afterOne = engine.applyAction(start, CounterAction.Step)
        assertFalse(engine.isWon(puzzle, afterOne))
        assertEquals(CounterAction.Step, engine.hintAction(puzzle, afterOne))

        val won = engine.applyAction(afterOne, CounterAction.Step)
        assertTrue(engine.isWon(puzzle, won))
        assertNull(engine.hintAction(puzzle, won))

        val restarted = engine.decodeState(engine.encodeState(won))
        assertTrue(engine.isWon(puzzle, restarted))

        val doomed = engine.applyAction(start, CounterAction.Fail)
        assertTrue(engine.isLost(puzzle, doomed))
        assertNull(engine.hintAction(puzzle, doomed))

        val continued = engine.applyContinue(doomed)
        assertFalse(engine.isLost(puzzle, continued))
        assertEquals(CounterAction.Step, engine.hintAction(puzzle, continued))
    }

    @Test
    fun illegalActionThrows() {
        val engine = CounterEngine
        val puzzle = CounterPuzzle(2, Pack.EASY)
        val start = engine.initialState(puzzle)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(start)
        }
    }

    @Test
    fun generateUsesSuppliedRngDeterministically() {
        val first = CounterEngine.generate(SplitMix64(4), Pack.MEDIUM)
        val second = CounterEngine.generate(SplitMix64(4), Pack.MEDIUM)
        assertEquals(first, second)
        assertEquals(3, first.targetSteps)
    }

    private data class CounterPuzzle(val targetSteps: Int, val pack: Pack)

    private data class CounterState(
        val puzzle: CounterPuzzle,
        val steps: Int,
        val failed: Boolean,
        val revived: Boolean,
    )

    private enum class CounterAction {
        Step,
        Fail,
    }

    private object CounterEngine : PlayEngine<CounterPuzzle, CounterState, CounterAction> {
        override val id: String = "fixture-counter"

        override fun decodePuzzle(line: String): CounterPuzzle {
            val bank = PlayBankLine.decode(line)
            val target = bank.payload.removePrefix("steps=").toInt()
            return CounterPuzzle(target, bank.pack)
        }

        override fun encodePuzzle(puzzle: CounterPuzzle): String =
            PlayBankLine(
                id = "fb-${puzzle.targetSteps}",
                size = puzzle.targetSteps,
                payload = "steps=${puzzle.targetSteps}",
                pack = puzzle.pack,
                difficulty = "d${puzzle.targetSteps}",
            ).encode()

        override fun initialState(puzzle: CounterPuzzle): CounterState =
            CounterState(puzzle, steps = 0, failed = false, revived = false)

        override fun applyAction(state: CounterState, action: CounterAction): CounterState = when (action) {
            CounterAction.Step -> {
                require(!state.failed) { "Cannot play after failure" }
                val next = state.steps + 1
                state.copy(steps = next)
            }
            CounterAction.Fail -> {
                require(!state.failed) { "Already failed" }
                state.copy(failed = true)
            }
        }

        override fun isWon(puzzle: CounterPuzzle, state: CounterState): Boolean =
            !state.failed && state.steps >= puzzle.targetSteps

        override fun isLost(puzzle: CounterPuzzle, state: CounterState): Boolean = state.failed && !state.revived

        override fun hintAction(puzzle: CounterPuzzle, state: CounterState): CounterAction? {
            if (isWon(puzzle, state) || isLost(puzzle, state)) return null
            return CounterAction.Step
        }

        override fun generate(rng: SplitMix64, pack: Pack): CounterPuzzle {
            val targetSteps = 2 + rng.nextInt(4)
            return CounterPuzzle(targetSteps, pack)
        }

        override fun encodeState(state: CounterState): ByteArray =
            byteArrayOf(
                state.steps.toByte(),
                (if (state.failed) 1 else 0).toByte(),
                (if (state.revived) 1 else 0).toByte(),
                state.puzzle.targetSteps.toByte(),
            )

        override fun decodeState(bytes: ByteArray): CounterState {
            require(bytes.size == 4) { "Malformed counter save" }
            val target = bytes[3].toInt()
            val puzzle = CounterPuzzle(target, Pack.EASY)
            return CounterState(
                puzzle = puzzle,
                steps = bytes[0].toInt(),
                failed = bytes[1].toInt() == 1,
                revived = bytes[2].toInt() == 1,
            )
        }

        override fun applyContinue(state: CounterState): CounterState {
            require(state.failed && !state.revived) { "Continue only applies after failure" }
            return state.copy(failed = false, revived = true)
        }
    }
}