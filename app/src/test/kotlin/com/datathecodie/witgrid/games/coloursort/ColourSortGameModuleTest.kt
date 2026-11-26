package com.datathecodie.witgrid.games.coloursort

import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortCodec
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.ColourSortPuzzle
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class ColourSortGameModuleTest {
    private val module = ColourSortGameModule

    @Test
    fun poursBankFixtureToWonThroughGameModuleAndBoardInput() {
        val fixture = ColourSortPuzzle(
            bankId = "cs-easy-1",
            pack = Pack.EASY,
            difficulty = "d1",
            initialTubes = listOf(
                listOf(1, 2, 1, 2),
                listOf(2, 1, 2, 1),
                listOf(3, 3, 3, 3),
                emptyList(),
            ),
        )
        val introPuzzle = fixture.copy(bankId = "CS-INTRO-01")
        val bank = FakeBankSource(
            mapOf(
                "bank/coloursort-intro.txt" to ColourSortCodec.encodePuzzle(introPuzzle),
                "bank/coloursort-easy.txt.gz" to ColourSortCodec.encodePuzzle(fixture),
            ),
        )

        val levelRef = module.levelRefs(Pack.EASY, bank)[1]
        val level = module.loadLevel(levelRef, bank)
        assertEquals(2, module.levelsPerInterstitial(level))

        var state = module.newState(level)
        var selectedTube: Int? = null
        assertFalse(module.isWon(level, state))
        assertFalse(module.isLost(level, state))

        // Solve using tap-source / tap-dest guided by engine hint actions.
        var steps = 0
        while (!module.isWon(level, state) && steps < 20) {
            val hint = module.hintAction(level, state)
            assertNotNull(hint)

            val select = ColourSortBoardInput.tapTube(state, selectedTube, hint!!.from)
            assertEquals(hint.from, select.selectedTube)
            selectedTube = select.selectedTube

            val pour = ColourSortBoardInput.tapTube(state, selectedTube, hint.to)
            assertEquals(hint, pour.action)
            selectedTube = pour.selectedTube

            state = module.applyAction(state, pour.action!!)
            steps++
        }

        assertTrue(module.isWon(level, state))
        assertFalse(module.isLost(level, state))

        val restored = module.decodeState(module.encodeState(state))
        assertTrue(module.stateMatchesLevel(level, restored))
        assertTrue(module.isWon(level, restored))
    }

    @Test
    fun poursGeneratedExpertPuzzleToWonThroughTapSequence() {
        val puzzle = ColourSortEngine.generate(SplitMix64(1L), Pack.EXPERT)
        val bank = FakeBankSource(
            mapOf(
                "bank/coloursort-expert.txt.gz" to ColourSortCodec.encodePuzzle(puzzle),
            ),
        )
        val level = module.loadLevel(module.firstLevelRef(Pack.EXPERT, bank), bank)
        var state = module.newState(level)
        var selectedTube: Int? = null

        var steps = 0
        while (!module.isWon(level, state) && steps < 200) {
            val hint = module.hintAction(level, state)
            assertNotNull(hint)
            val select = ColourSortBoardInput.tapTube(state, selectedTube, hint!!.from)
            val pour = ColourSortBoardInput.tapTube(state, select.selectedTube, hint.to)
            selectedTube = pour.selectedTube
            state = module.applyAction(state, pour.action!!)
            steps++
        }

        assertTrue(module.isWon(level, state))
    }

    @Test
    fun continueAddsEmptyTubeAndAllowsFurtherPours() {
        val deadlockDeal = listOf(
            listOf(1, 2, 4, 5),
            listOf(2, 3, 5, 1),
            listOf(4, 5, 3, 2),
            listOf(4, 5, 2),
        )
        val deadlockPuzzle = ColourSortPuzzle(
            bankId = "cs-deadlock-1",
            pack = Pack.EASY,
            difficulty = "d1",
            initialTubes = deadlockDeal,
        )
        val level = GameLevel(
            ref = LevelRef("coloursort", deadlockPuzzle.bankId, Pack.EASY, levelNumber = 1L),
            size = 4,
            puzzle = deadlockPuzzle,
        )

        val start = module.newState(level)
        assertFalse(module.isLost(level, start))

        val lost = module.applyAction(start, ColourSortAction(2, 3))
        assertTrue(module.isLost(level, lost))

        val continued = module.applyContinue(lost)
        assertFalse(module.isLost(level, continued))
        assertEquals(5, continued.tubes.size)
        assertTrue(continued.tubes.last().isEmpty())

        val select = ColourSortBoardInput.tapTube(continued, null, 0)
        val pour = ColourSortBoardInput.tapTube(continued, select.selectedTube, 4)
        assertEquals(ColourSortAction(0, 4), pour.action)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val text = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$text\n".toByteArray(Charsets.UTF_8))
        }
    }
}
