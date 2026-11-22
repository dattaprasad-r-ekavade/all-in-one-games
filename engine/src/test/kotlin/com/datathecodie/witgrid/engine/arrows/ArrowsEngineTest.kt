package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArrowsEngineTest {
    private val engine = ArrowsEngine

    /**
     * 5x5 Easy layout:
     * . . ^ . .  (row 0: cell 2 is ^)
     * . . ^ . .  (row 1: cell 7 is ^, blocked by cell 2)
     * . . > . v  (row 2: cell 12 is >, blocked by cell 14; cell 14 is v)
     * . . . . .  (row 3)
     * . . . . .  (row 4)
     *
     * Clearing sequence:
     * 1. Slide Cell(2) [UP off board] -> unblocks Cell(7)
     * 2. Slide Cell(7) [UP off board]
     * 3. Slide Cell(14) [DOWN off board] -> unblocks Cell(12)
     * 4. Slide Cell(12) [RIGHT off board] -> board empty, win!
     */
    private val easyFixture = ArrowsPuzzle(
        bankId = "ar-fixture-5",
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

    @Test
    fun initialDealIsNotLost() {
        val puzzle = engine.decodePuzzle(engine.encodePuzzle(easyFixture))
        val start = engine.initialState(puzzle)
        assertFalse(engine.isLost(puzzle, start))
        assertFalse(engine.isWon(puzzle, start))
    }

    @Test
    fun slideSequenceToWin() {
        val start = engine.initialState(easyFixture)
        val sequence = listOf(
            Slide(Cell(2)),
            Slide(Cell(7)),
            Slide(Cell(14)),
            Slide(Cell(12)),
        )
        val won = sequence.fold(start) { state, action -> engine.applyAction(state, action) }
        assertTrue(engine.isWon(easyFixture, won))
        assertFalse(engine.isLost(easyFixture, won))
    }

    @Test
    fun blockedArrowCannotSlide() {
        val start = engine.initialState(easyFixture)
        // Cell 7 is blocked by Cell 2
        assertFalse(ArrowsRules.isSlideLegal(easyFixture.size, start.arrows, Cell(7)))
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, Slide(Cell(7)))
        }

        // Cell 12 is blocked by Cell 14
        assertFalse(ArrowsRules.isSlideLegal(easyFixture.size, start.arrows, Cell(12)))
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, Slide(Cell(12)))
        }
    }

    @Test
    fun emptyCellSlideThrows() {
        val start = engine.initialState(easyFixture)
        assertFalse(ArrowsRules.isSlideLegal(easyFixture.size, start.arrows, Cell(0)))
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, Slide(Cell(0)))
        }
    }

    @Test
    fun outOfBoundsSlideThrows() {
        val start = engine.initialState(easyFixture)
        assertFalse(ArrowsRules.canSlide(easyFixture.size, start.arrows, -1))
        assertFalse(ArrowsRules.canSlide(easyFixture.size, start.arrows, 25))
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, Slide(Cell(25)))
        }
    }

    @Test
    fun directionalMovementAllFourDirections() {
        // Test a 5x5 board where one arrow of each direction slides to its corresponding edge
        // row 0 col 2 (cell 2): ^ (UP)
        // row 4 col 2 (cell 22): v (DOWN)
        // row 2 col 0 (cell 10): < (LEFT)
        // row 2 col 4 (cell 14): > (RIGHT)
        val layout = "..^.." +
                     "....." +
                     "<...>" +
                     "....." +
                     "..v.."
        val arrows = ArrowsCodec.decodeLayout(layout, 5)
        val puzzle = ArrowsPuzzle("all-dirs", 5, Pack.EASY, "d1", arrows)
        val state = engine.initialState(puzzle)

        assertTrue(ArrowsRules.isSlideLegal(5, state.arrows, Cell(2)))
        assertTrue(ArrowsRules.isSlideLegal(5, state.arrows, Cell(22)))
        assertTrue(ArrowsRules.isSlideLegal(5, state.arrows, Cell(10)))
        assertTrue(ArrowsRules.isSlideLegal(5, state.arrows, Cell(14)))

        // Verify path cells for each direction
        assertEquals(listOf(Cell(11), Cell(12), Cell(13), Cell(14)), ArrowsRules.pathCells(5, Cell(10), ArrowsDirection.RIGHT))
        assertEquals(listOf(Cell(13), Cell(12), Cell(11), Cell(10)), ArrowsRules.pathCells(5, Cell(14), ArrowsDirection.LEFT))
        assertEquals(listOf(Cell(17), Cell(12), Cell(7), Cell(2)), ArrowsRules.pathCells(5, Cell(22), ArrowsDirection.UP))
        assertEquals(listOf(Cell(7), Cell(12), Cell(17), Cell(22)), ArrowsRules.pathCells(5, Cell(2), ArrowsDirection.DOWN))
    }

    @Test
    fun stuckPositionIsLostAfterSlide() {
        // Cell 0 is ^ (can slide off top)
        // Cell 11 is > (pointing right towards cell 13)
        // Cell 13 is < (pointing left towards cell 11)
        // Cells 11 and 13 block each other.
        val layout = "^...." +
                     "....." +
                     ".>.<." +
                     "....." +
                     "....."
        val arrows = ArrowsCodec.decodeLayout(layout, 5)
        val puzzle = ArrowsPuzzle("stuck-demo", 5, Pack.EASY, "d1", arrows)
        val start = engine.initialState(puzzle)

        // Before any move, isLost must be false
        assertFalse(engine.isLost(puzzle, start))

        // After sliding Cell 0, only mutual-blocking arrows 11 and 13 remain
        val afterSlide = engine.applyAction(start, Slide(Cell(0)))
        assertTrue(engine.isLost(puzzle, afterSlide))
        assertFalse(engine.isWon(puzzle, afterSlide))
    }

    @Test
    fun continueRevivesBoardAndClearsLoss() {
        val layout = "^...." +
                     "....." +
                     ".>.<." +
                     "....." +
                     "....."
        val arrows = ArrowsCodec.decodeLayout(layout, 5)
        val puzzle = ArrowsPuzzle("stuck-demo", 5, Pack.EASY, "d1", arrows)
        val start = engine.initialState(puzzle)
        val lost = engine.applyAction(start, Slide(Cell(0)))
        assertTrue(engine.isLost(puzzle, lost))

        val continued = engine.applyContinue(lost)
        assertFalse(engine.isLost(puzzle, continued))
        assertEquals(start.arrows, continued.arrows)
        assertEquals(0, continued.slideCount)
    }

    @Test
    fun continueThrowsWhenNotLost() {
        val start = engine.initialState(easyFixture)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(start)
        }
    }

    @Test
    fun bankLineRoundTripsAllSupportedSizes() {
        val sizesAndPacks = listOf(
            Pair(5, Pack.EASY),
            Pair(6, Pack.MEDIUM),
            Pair(8, Pack.HARD),
            Pair(10, Pack.EXPERT),
        )

        for ((size, pack) in sizesAndPacks) {
            val emptyLayout = ".".repeat(size * size)
            val arrows = ArrowsCodec.decodeLayout(emptyLayout, size).toMutableList()
            // Place one arrow in each corner
            arrows[0] = ArrowsDirection.UP
            arrows[size - 1] = ArrowsDirection.RIGHT
            arrows[size * (size - 1)] = ArrowsDirection.LEFT
            arrows[size * size - 1] = ArrowsDirection.DOWN

            val puzzle = ArrowsPuzzle(
                bankId = "ar-test-$size",
                size = size,
                pack = pack,
                difficulty = "d-$size",
                initialArrows = arrows.toList(),
            )

            val encoded = engine.encodePuzzle(puzzle)
            val decoded = engine.decodePuzzle(encoded)
            assertEquals(puzzle, decoded)
        }
    }

    @Test
    fun bankLineDecodesLettersAndSymbols() {
        // Verify decode accepts U, D, L, R as well as ^, v, <, >
        val layoutWithLetters = "U.D.L.R.................."
        val arrows = ArrowsCodec.decodeLayout(layoutWithLetters, 5)
        assertEquals(ArrowsDirection.UP, arrows[0])
        assertEquals(null, arrows[1])
        assertEquals(ArrowsDirection.DOWN, arrows[2])
        assertEquals(null, arrows[3])
        assertEquals(ArrowsDirection.LEFT, arrows[4])
        assertEquals(null, arrows[5])
        assertEquals(ArrowsDirection.RIGHT, arrows[6])
    }

    @Test
    fun malformedBankLineThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("only-three|fields|here")
        }
        // Size 5 does not match MEDIUM
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-bad|5|${".".repeat(25)}|MEDIUM|d1")
        }
        // Unsupported size 7
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsPuzzle("bad-size", 7, Pack.EASY, "d1", List(49) { null })
        }
        // Payload length mismatch
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-bad|5|${".".repeat(24)}|EASY|d1")
        }
        // Invalid character
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-bad|5|${".".repeat(24)}X|EASY|d1")
        }
    }

    @Test
    fun stateCodecRoundTrips() {
        val start = engine.initialState(easyFixture)
        val step1 = engine.applyAction(start, Slide(Cell(2)))
        val step2 = engine.applyAction(step1, Slide(Cell(7)))

        val encoded = engine.encodeState(step2)
        val restored = engine.decodeState(encoded)

        assertEquals(step2, restored)
        assertEquals(2, restored.slideCount)
        assertEquals(2, restored.history.size)
    }

    @Test
    fun malformedStateThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun deferredFeaturesThrow() {
        val start = engine.initialState(easyFixture)
        assertThrows(NotImplementedError::class.java) {
            engine.hintAction(easyFixture, start)
        }
        assertThrows(NotImplementedError::class.java) {
            engine.generate(SplitMix64(1), Pack.EASY)
        }
    }

    @Test
    fun actionConstructorsAndSlideAlias() {
        val action1 = ArrowsAction(Cell(5))
        val action2 = ArrowsAction.fromIndex(5)
        val slide1 = Slide(Cell(5))

        assertEquals(action1, action2)
        assertEquals(action1, slide1)
        assertEquals(5, action1.cellIndex)
        assertEquals(Cell(5), action1.cell)
    }
}
