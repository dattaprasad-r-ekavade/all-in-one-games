package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArrowsGeneratorTest {
    private val engine = ArrowsEngine

    @Test
    fun packConfigsMatchSizesAndDifficulty() {
        val easy = ArrowsGenerator.configFor(Pack.EASY)
        assertEquals(5, easy.size)
        assertEquals(8, easy.minArrows)
        assertEquals(16, easy.maxArrows)
        assertEquals(2, easy.minChainDepth)
        assertEquals("d1", easy.difficulty)

        val medium = ArrowsGenerator.configFor(Pack.MEDIUM)
        assertEquals(6, medium.size)
        assertEquals(14, medium.minArrows)
        assertEquals(24, medium.maxArrows)
        assertEquals(3, medium.minChainDepth)
        assertEquals("d2", medium.difficulty)

        val hard = ArrowsGenerator.configFor(Pack.HARD)
        assertEquals(8, hard.size)
        assertEquals(22, hard.minArrows)
        assertEquals(40, hard.maxArrows)
        assertEquals(4, hard.minChainDepth)
        assertEquals("d3", hard.difficulty)

        val expert = ArrowsGenerator.configFor(Pack.EXPERT)
        assertEquals(10, expert.size)
        assertEquals(30, expert.minArrows)
        assertEquals(60, expert.maxArrows)
        assertEquals(5, expert.minChainDepth)
        assertEquals("d4", expert.difficulty)

        for (pack in Pack.entries) {
            val config = ArrowsGenerator.configFor(pack)
            assertEquals(ArrowsPuzzle.expectedGridSize(pack), config.size)
            assertTrue(config.minArrows <= 4 * (config.size - 1))
            assertTrue(config.minChainDepth < config.size)
            assertTrue(config.minArrows <= config.maxArrows)
        }
    }

    @Test
    fun chainDepthFollowsBlockersOnTheRay() {
        val arrows = MutableList<ArrowsDirection?>(25) { null }
        arrows[0] = ArrowsDirection.UP
        arrows[5] = ArrowsDirection.UP
        arrows[10] = ArrowsDirection.UP
        arrows[1] = ArrowsDirection.RIGHT
        assertEquals(3, ArrowsGenerator.chainDepth(5, arrows))
        assertTrue(ArrowsRules.isClearable(5, arrows))
        assertEquals(listOf(0, 1), ArrowsRules.legalSlides(5, arrows).map { it.cell.index })
    }

    @Test
    fun sameSeedGivesTheSameTenLevels() {
        for (pack in Pack.entries) {
            val first = tenLevels(pack)
            val second = tenLevels(pack)
            assertEquals(10, first.size)
            assertEquals(first, second)
            assertEquals(
                first.map { engine.encodePuzzle(it) },
                second.map { engine.encodePuzzle(it) },
            )
        }
    }

    @Test
    fun tenLevelsPerPackAreClearableAndNotDeadlocked() {
        for (pack in Pack.entries) {
            val config = ArrowsGenerator.configFor(pack)
            val puzzles = tenLevels(pack)
            assertEquals(10, puzzles.size)
            assertEquals(10, puzzles.map { it.bankId }.toSet().size)
            for (puzzle in puzzles) {
                val arrowCount = puzzle.initialArrows.count { it != null }
                val hex = puzzle.bankId.substringAfterLast('-')
                assertEquals(pack, puzzle.pack)
                assertEquals(config.size, puzzle.size)
                assertEquals(ArrowsPuzzle.expectedGridSize(pack), puzzle.size)
                assertEquals(config.difficulty, puzzle.difficulty)
                assertTrue(puzzle.bankId.startsWith("AR-${pack.name}-"))
                assertEquals(16, hex.length)
                assertTrue(hex.all { it in '0'..'9' || it in 'a'..'f' })
                assertTrue(
                    arrowCount in config.minArrows..config.maxArrows,
                    "$pack arrow count $arrowCount outside ${config.minArrows}..${config.maxArrows}",
                )
                assertTrue(puzzle.initialArrows.filterNotNull().distinct().size >= 2)
                assertTrue(
                    ArrowsGenerator.chainDepth(puzzle.size, puzzle.initialArrows) >= config.minChainDepth,
                    "$pack chain is shorter than ${config.minChainDepth}",
                )
                assertTrue(ArrowsRules.isClearable(puzzle.size, puzzle.initialArrows))
                assertTrue(ArrowsRules.legalSlides(puzzle.size, puzzle.initialArrows).isNotEmpty())
                val start = engine.initialState(puzzle)
                assertFalse(engine.isWon(puzzle, start))
                assertFalse(engine.isLost(puzzle, start))
                assertEquals(0, start.mistakes)
                assertEquals(puzzle, engine.decodePuzzle(engine.encodePuzzle(puzzle)))
            }
        }
    }

    @Test
    fun followingHintsClearsTenLevelsWithZeroMistakes() {
        for (pack in Pack.entries) {
            val puzzles = tenLevels(pack)
            for ((index, puzzle) in puzzles.withIndex()) {
                val arrowCount = puzzle.initialArrows.count { it != null }
                var state = engine.initialState(puzzle)
                var slides = 0
                while (!engine.isWon(puzzle, state)) {
                    assertTrue(slides < arrowCount, "$pack level $index did not clear")
                    val hint = requireNotNull(engine.hintAction(puzzle, state)) {
                        "$pack level $index has no hint after $slides slides"
                    }
                    val legal = ArrowsRules.legalSlides(puzzle.size, state.arrows)
                    assertEquals(legal.first(), hint, "$pack level $index hint is not on the clearing order")
                    state = engine.applyAction(state, hint)
                    assertEquals(0, state.mistakes, "$pack level $index hint was a mistake")
                    assertFalse(engine.isLost(puzzle, state))
                    slides++
                }
                assertEquals(arrowCount, slides)
                assertEquals(arrowCount, state.slideCount)
                assertTrue(engine.isWon(puzzle, state))
                assertNull(engine.hintAction(puzzle, state))
            }
        }
    }

    @Test
    fun hintOnKnownFixtureFollowsTheClearingOrder() {
        val puzzle = knownFixture()
        val start = engine.initialState(puzzle)
        assertEquals(Slide(Cell(2)), engine.hintAction(puzzle, start))

        val expected = listOf(2, 7, 14, 12)
        var state = start
        for (index in expected) {
            val hint = requireNotNull(engine.hintAction(puzzle, state))
            assertEquals(Slide(Cell(index)), hint)
            state = engine.applyAction(state, hint)
            assertEquals(0, state.mistakes)
        }
        assertTrue(engine.isWon(puzzle, state))
        assertNull(engine.hintAction(puzzle, state))

        val blocked = engine.applyAction(start, Slide(Cell(7)))
        assertEquals(1, blocked.mistakes)
        assertEquals(Slide(Cell(2)), engine.hintAction(puzzle, blocked))
    }

    @Test
    fun hintIsNullWhenWonOrLostAndReturnsAfterContinue() {
        val puzzle = knownFixture()
        val start = engine.initialState(puzzle)
        val won = start.copy(
            arrows = List(puzzle.cellCount) { null },
            slideCount = 4,
        )
        assertTrue(engine.isWon(puzzle, won))
        assertNull(engine.hintAction(puzzle, won))

        val lost = start.copy(mistakes = ArrowsRules.MAX_MISTAKES)
        assertTrue(engine.isLost(puzzle, lost))
        assertTrue(ArrowsRules.canSlide(puzzle.size, lost.arrows, 2))
        assertNull(engine.hintAction(puzzle, lost))

        val continued = engine.applyContinue(lost)
        assertFalse(engine.isLost(puzzle, continued))
        assertEquals(2, continued.mistakes)
        assertEquals(Slide(Cell(2)), engine.hintAction(puzzle, continued))
    }

    @Test
    fun hintOffersTheOnlySlideWhenTheRestIsDeadlocked() {
        val puzzle = knownFixture()
        val stuck = ArrowsCodec.decodeLayout(
            "^...." +
                "....." +
                ".>.<." +
                "....." +
                ".....",
            5,
        )
        val state = ArrowsState(puzzle, stuck)
        assertFalse(ArrowsRules.isClearable(5, stuck))
        assertFalse(engine.isWon(puzzle, state))
        assertFalse(engine.isLost(puzzle, state))
        assertEquals(listOf(0), ArrowsRules.legalSlides(5, stuck).map { it.cell.index })
        assertEquals(Slide(Cell(0)), engine.hintAction(puzzle, state))
    }

    private fun tenLevels(pack: Pack): List<ArrowsPuzzle> {
        val rng = SplitMix64(1)
        return List(10) { engine.generate(rng, pack) }
    }

    private fun knownFixture(): ArrowsPuzzle = ArrowsPuzzle(
        bankId = "ar-hint-fixture",
        size = 5,
        pack = Pack.EASY,
        difficulty = "d1",
        initialArrows = ArrowsCodec.decodeLayout(
            "..^.." +
                "..^.." +
                "..>.v" +
                "....." +
                ".....",
            5,
        ),
    )
}
