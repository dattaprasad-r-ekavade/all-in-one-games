package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.core.VersionedStateCodec
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
        assertEquals(0, start.mistakes)
        assertEquals(0, start.slideCount)
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
        assertEquals(0, won.mistakes)
        assertEquals(4, won.slideCount)
        assertEquals(4, won.history.size)
    }

    @Test
    fun blockedArrowRuleAndMistakeMechanic() {
        val start = engine.initialState(easyFixture)
        // Cell 7 is blocked by Cell 2
        assertFalse(ArrowsRules.isSlideLegal(easyFixture.size, start.arrows, Cell(7)))
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsRules.applySlide(easyFixture.size, start.arrows, Cell(7))
        }

        // Tapping blocked arrow in engine is a mistake (mistakes = 1), board unchanged
        val mistake1 = engine.applyAction(start, Slide(Cell(7)))
        assertEquals(1, mistake1.mistakes)
        assertEquals(start.arrows, mistake1.arrows)
        assertEquals(0, mistake1.slideCount)
        assertEquals(0, mistake1.history.size)
        assertFalse(engine.isLost(easyFixture, mistake1))

        // Tap blocked arrow again -> mistake 2
        val mistake2 = engine.applyAction(mistake1, Slide(Cell(7)))
        assertEquals(2, mistake2.mistakes)
        assertEquals(start.arrows, mistake2.arrows)
        assertFalse(engine.isLost(easyFixture, mistake2))

        // Tap blocked arrow third time -> mistake 3 -> level is lost!
        val mistake3 = engine.applyAction(mistake2, Slide(Cell(7)))
        assertEquals(3, mistake3.mistakes)
        assertEquals(start.arrows, mistake3.arrows)
        assertTrue(engine.isLost(easyFixture, mistake3))
        assertFalse(engine.isWon(easyFixture, mistake3))

        // Attempting another action when lost throws
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(mistake3, Slide(Cell(2)))
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

        assertEquals(listOf(Cell(11), Cell(12), Cell(13), Cell(14)), ArrowsRules.pathCells(5, Cell(10), ArrowsDirection.RIGHT))
        assertEquals(listOf(Cell(13), Cell(12), Cell(11), Cell(10)), ArrowsRules.pathCells(5, Cell(14), ArrowsDirection.LEFT))
        assertEquals(listOf(Cell(17), Cell(12), Cell(7), Cell(2)), ArrowsRules.pathCells(5, Cell(22), ArrowsDirection.UP))
        assertEquals(listOf(Cell(7), Cell(12), Cell(17), Cell(22)), ArrowsRules.pathCells(5, Cell(2), ArrowsDirection.DOWN))
    }

    @Test
    fun deadlockedDealsAreRejectedOnDecode() {
        // Mutual blocking: two arrows pointing directly at each other
        val mutualBlocking = "....." +
                             "....." +
                             ".>.<." +
                             "....." +
                             "....."
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-deadlock|5|$mutualBlocking|EASY|d1")
        }

        // Circular blocking: 3 arrows in a cycle
        val cycle = ".v..." +
                    "..<.." +
                    ".^..." +
                    "....." +
                    "....."
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-cycle|5|$cycle|EASY|d1")
        }

        // Partial clearable with deadlocked residue: Cell 0 can slide UP, but 11 and 13 block each other
        val partialDeadlock = "^...." +
                              "....." +
                              ".>.<." +
                              "....." +
                              "....."
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-partial|5|$partialDeadlock|EASY|d1")
        }

        // Empty layout has no arrows to clear
        val empty = ".".repeat(25)
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("ar-empty|5|$empty|EASY|d1")
        }
    }

    @Test
    fun multiStepLossRoundTripAndContinue() {
        val start = engine.initialState(easyFixture)

        // Slide two arrows legally
        val step1 = engine.applyAction(start, Slide(Cell(2)))
        val step2 = engine.applyAction(step1, Slide(Cell(7)))
        assertEquals(2, step2.slideCount)
        assertEquals(2, step2.history.size)
        assertEquals(0, step2.mistakes)

        // Now cell 12 is blocked by cell 14. Make 3 mistakes tapping cell 12.
        val mist1 = engine.applyAction(step2, Slide(Cell(12)))
        val mist2 = engine.applyAction(mist1, Slide(Cell(12)))
        val lost = engine.applyAction(mist2, Slide(Cell(12)))

        assertTrue(engine.isLost(easyFixture, lost))
        assertEquals(3, lost.mistakes)
        assertEquals(2, lost.slideCount)
        assertEquals(2, lost.history.size)

        // Round-trip the lost state through the codec before Continue
        val encoded = engine.encodeState(lost)
        val restored = engine.decodeState(encoded)

        assertEquals(lost, restored)
        assertTrue(engine.isLost(easyFixture, restored))
        assertEquals(3, restored.mistakes)
        assertEquals(2, restored.slideCount)

        // Apply Continue: mistakes drops by 1 (extra life), board stays as it is
        val continued = engine.applyContinue(restored)
        assertFalse(engine.isLost(easyFixture, continued))
        assertEquals(2, continued.mistakes)
        assertEquals(restored.arrows, continued.arrows)
        assertEquals(2, continued.slideCount)
        assertEquals(2, continued.history.size)

        // Slide remaining arrows to complete the level and win
        val step3 = engine.applyAction(continued, Slide(Cell(14)))
        val step4 = engine.applyAction(step3, Slide(Cell(12)))

        assertTrue(engine.isWon(easyFixture, step4))
        assertFalse(engine.isLost(easyFixture, step4))
        assertEquals(2, step4.mistakes)
        assertEquals(4, step4.slideCount)
    }

    @Test
    fun continueThrowsWhenNotLost() {
        val start = engine.initialState(easyFixture)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(start)
        }

        val step1 = engine.applyAction(start, Slide(Cell(2)))
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(step1)
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
            // Place one arrow in each corner (all slide outward off edges)
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
        assertEquals(0, restored.mistakes)
        assertEquals(2, restored.slideCount)
        assertEquals(2, restored.history.size)
    }

    @Test
    fun malformedStateThrows() {
        // Truncated / garbage bytes
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(byteArrayOf(1, 2, 3))
        }

        val magic = 0x57415257

        // Excess history size: must throw without OutOfMemoryError
        val largeHistoryBytes = VersionedStateCodec.encode(magic) { out ->
            out.writeUTF(engine.encodePuzzle(easyFixture))
            out.writeUTF(ArrowsCodec.encodeLayout(easyFixture.initialArrows, 5))
            out.writeInt(0) // mistakes
            out.writeInt(0) // slideCount
            out.writeInt(1_000_000) // historySize out of bounds
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(largeHistoryBytes)
        }

        // Negative mistakes
        val negMistakesBytes = VersionedStateCodec.encode(magic) { out ->
            out.writeUTF(engine.encodePuzzle(easyFixture))
            out.writeUTF(ArrowsCodec.encodeLayout(easyFixture.initialArrows, 5))
            out.writeInt(-1) // negative mistakes
            out.writeInt(0)
            out.writeInt(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(negMistakesBytes)
        }

        // Mistakes exceeding MAX_MISTAKES
        val excessMistakesBytes = VersionedStateCodec.encode(magic) { out ->
            out.writeUTF(engine.encodePuzzle(easyFixture))
            out.writeUTF(ArrowsCodec.encodeLayout(easyFixture.initialArrows, 5))
            out.writeInt(4) // mistakes > 3
            out.writeInt(0)
            out.writeInt(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(excessMistakesBytes)
        }

        // SlideCount / historySize mismatch
        val mismatchBytes = VersionedStateCodec.encode(magic) { out ->
            out.writeUTF(engine.encodePuzzle(easyFixture))
            out.writeUTF(ArrowsCodec.encodeLayout(easyFixture.initialArrows, 5))
            out.writeInt(0)
            out.writeInt(1) // slideCount 1
            out.writeInt(0) // historySize 0
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(mismatchBytes)
        }

        // History root does not match puzzle initial deal
        val badRootBytes = VersionedStateCodec.encode(magic) { out ->
            out.writeUTF(engine.encodePuzzle(easyFixture))
            // Current arrows has 3 arrows (one removed)
            val current = easyFixture.initialArrows.toMutableList()
            current[2] = null
            out.writeUTF(ArrowsCodec.encodeLayout(current, 5))
            out.writeInt(0)
            out.writeInt(1)
            out.writeInt(1)
            // But history root has wrong layout
            out.writeUTF(ArrowsCodec.encodeLayout(current, 5))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(badRootBytes)
        }

        // Illegal slide transition in history
        val illegalTransitionBytes = VersionedStateCodec.encode(magic) { out ->
            out.writeUTF(engine.encodePuzzle(easyFixture))
            // Suppose board cleared cell 7 (which was blocked by cell 2)
            val badStep = easyFixture.initialArrows.toMutableList()
            badStep[7] = null // Cell 7 cannot slide while Cell 2 exists!
            out.writeUTF(ArrowsCodec.encodeLayout(badStep, 5))
            out.writeInt(0)
            out.writeInt(1)
            out.writeInt(1)
            out.writeUTF(ArrowsCodec.encodeLayout(easyFixture.initialArrows, 5))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(illegalTransitionBytes)
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
