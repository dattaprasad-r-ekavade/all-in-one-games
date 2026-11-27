package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.PlayEngine
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.games.arrows.ArrowsGameModule
import com.datathecodie.witgrid.games.coloursort.ColourSortGameModule
import com.datathecodie.witgrid.games.klondike.KlondikeGameModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class PlayGameModuleTest {
    @Test
    fun levelRefsLoadWinAndContinueThroughModule() {
        val bank = FakeBankSource()
        val module = testModule()
        val refs = module.levelRefs(Pack.EASY, bank)

        assertEquals(listOf("pl-easy-1", "pl-easy-2"), refs.map { it.levelId })
        val level = module.loadLevel(refs.first(), bank)
        var state = module.newState(level)
        assertFalse(module.isWon(level, state))
        assertFalse(module.isLost(level, state))

        state = module.applyAction(state, TapAction.Advance)
        state = module.applyAction(state, TapAction.Advance)
        assertTrue(module.isWon(level, state))
        assertEquals(TapAction.Advance, module.hintAction(level, module.newState(module.loadLevel(refs[1], bank))))

        val restarted = module.decodeState(module.encodeState(state))
        assertTrue(module.stateMatchesLevel(level, restarted))

        var failed = module.applyAction(module.newState(level), TapAction.Trap)
        assertTrue(module.isLost(level, failed))
        failed = module.applyContinue(failed)
        assertFalse(module.isLost(level, failed))
    }

    @Test
    fun nextLevelRefLoopsLikeFamilyAModule() {
        val bank = FakeBankSource()
        val module = testModule()
        val refs = module.levelRefs(Pack.EASY, bank)

        assertEquals(refs[1], module.nextLevelRef(refs[0], bank))
        assertEquals(refs.first().copy(levelNumber = 3), module.nextLevelRef(refs.last(), bank))
    }

    @Test
    fun interstitialRuleMustBePositive() {
        val module = EnginePlayGameModule(FakePlayEngine, FakeState::puzzle) { 0 }
        val level = GameLevel(LevelRef(FakePlayEngine.id, "pl-easy-1", Pack.EASY), 2, FakePuzzle(2, Pack.EASY))

        assertThrows(IllegalArgumentException::class.java) { module.levelsPerInterstitial(level) }
    }

    @Test
    fun playRegistryFindsRegisteredLanes() {
        assertEquals(null, PlayRegistry.find("not-registered"))
        assertEquals(ColourSortGameModule, PlayRegistry.find("coloursort"))
        assertEquals(ArrowsGameModule, PlayRegistry.find("arrows"))
        assertEquals(KlondikeGameModule, PlayRegistry.find("klondike"))
        assertTrue(PlayRegistry.games.contains(ColourSortGameModule))
        assertTrue(PlayRegistry.games.contains(ArrowsGameModule))
        assertTrue(PlayRegistry.games.contains(KlondikeGameModule))
    }

    private fun testModule() = EnginePlayGameModule(
        engine = FakePlayEngine,
        puzzleOf = FakeState::puzzle,
        interstitialRule = { 2 },
    )

    private class FakeBankSource : BankSource {
        override fun open(path: String): InputStream {
            val content = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream(content)
        }

        private companion object {
            val files: Map<String, ByteArray> = mapOf(
                "bank/play-fixture-easy.txt.gz" to textFile(
                    listOf(
                        line("pl-easy-1", 2),
                        line("pl-easy-2", 3),
                    ),
                ),
            )

            fun line(id: String, steps: Int): String =
                PlayBankLine(id, steps, "steps=$steps", Pack.EASY, "d$steps").encode()

            fun textFile(lines: List<String>): ByteArray =
                lines.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        }
    }

    private data class FakePuzzle(val target: Int, val pack: Pack)

    private data class FakeState(val puzzle: FakePuzzle, val progress: Int, val failed: Boolean, val revived: Boolean)

    private enum class TapAction {
        Advance,
        Trap,
    }

    private object FakePlayEngine : PlayEngine<FakePuzzle, FakeState, TapAction> {
        override val id: String = "play-fixture"

        override fun decodePuzzle(line: String): FakePuzzle {
            val bank = PlayBankLine.decode(line)
            return FakePuzzle(bank.payload.removePrefix("steps=").toInt(), bank.pack)
        }

        override fun encodePuzzle(puzzle: FakePuzzle): String =
            PlayBankLine("pl-easy-1", puzzle.target, "steps=${puzzle.target}", puzzle.pack, "d${puzzle.target}").encode()

        override fun initialState(puzzle: FakePuzzle): FakeState = FakeState(puzzle, 0, false, false)

        override fun applyAction(state: FakeState, action: TapAction): FakeState = when (action) {
            TapAction.Advance -> {
                require(!state.failed) { "Cannot play after failure" }
                state.copy(progress = state.progress + 1)
            }
            TapAction.Trap -> state.copy(failed = true)
        }

        override fun isWon(puzzle: FakePuzzle, state: FakeState): Boolean =
            !state.failed && state.progress >= puzzle.target

        override fun isLost(puzzle: FakePuzzle, state: FakeState): Boolean = state.failed && !state.revived

        override fun hintAction(puzzle: FakePuzzle, state: FakeState): TapAction? =
            if (isWon(puzzle, state) || isLost(puzzle, state)) null else TapAction.Advance

        override fun generate(rng: SplitMix64, pack: Pack): FakePuzzle = FakePuzzle(2, pack)

        override fun encodeState(state: FakeState): ByteArray = byteArrayOf(
            state.progress.toByte(),
            (if (state.failed) 1 else 0).toByte(),
            (if (state.revived) 1 else 0).toByte(),
            state.puzzle.target.toByte(),
        )

        override fun decodeState(bytes: ByteArray): FakeState {
            require(bytes.size == 4)
            val puzzle = FakePuzzle(bytes[3].toInt(), Pack.EASY)
            return FakeState(
                puzzle = puzzle,
                progress = bytes[0].toInt(),
                failed = bytes[1].toInt() == 1,
                revived = bytes[2].toInt() == 1,
            )
        }

        override fun applyContinue(state: FakeState): FakeState {
            require(state.failed && !state.revived) { "Continue only applies after failure" }
            return state.copy(failed = false, revived = true)
        }
    }
}