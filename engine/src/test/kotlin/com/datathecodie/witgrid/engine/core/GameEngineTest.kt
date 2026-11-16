package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Compile-and-run examples of lane-owned models, without a common puzzle/state base class. */
class GameEngineTest {
    private data class RegionPuzzle(val grid: Grid, val regions: List<Int>)
    private data class RegionState(val puzzle: RegionPuzzle, val crowns: Set<Cell>)
    private data class BinaryPuzzle(val grid: Grid, val givens: Map<Cell, Mark>)
    private data class BinaryState(val puzzle: BinaryPuzzle, val symbols: Map<Cell, Mark>)
    private data class CluePuzzle(val rows: List<List<Int>>, val columns: List<List<Int>>)
    private data class ClueState(val puzzle: CluePuzzle, val marks: Map<Cell, Mark>)
    private data class DigitPuzzle(val grid: Grid, val boxRows: Int, val boxColumns: Int)
    private data class DigitState(val puzzle: DigitPuzzle, val digits: Map<Cell, Int>, val notes: Map<Cell, Int>)

    @Test
    fun regionGridFitsWithoutFlatteningItsRegionsIntoStateValues() {
        val puzzle = RegionPuzzle(Grid(1), listOf(0))
        val engine = FixtureEngine(
            puzzle,
            RegionState(puzzle, emptySet()),
            RegionState(puzzle, setOf(Cell(0))),
            listOf(Move.SetMark(Cell(0), Mark.FILLED)),
        ) { state, move ->
            when (move) {
                is Move.SetMark -> state.copy(
                    crowns = if (move.mark == Mark.FILLED) state.crowns + move.cell else state.crowns - move.cell,
                )
                is Move.SetDigit, is Move.SetNotes -> error("Unsupported fixture edit")
            }
        }
        assertEquals(setOf(Cell(0)), playThroughCodec(engine).crowns)
    }

    @Test
    fun binaryCanRetainItsGivensAndTwoSymbols() {
        val puzzle = BinaryPuzzle(Grid(2), mapOf(Cell(0) to Mark.FILLED))
        val engine = FixtureEngine(
            puzzle,
            BinaryState(puzzle, puzzle.givens),
            BinaryState(puzzle, puzzle.givens + (Cell(1) to Mark.CROSSED)),
            listOf(Move.SetMark(Cell(1), Mark.CROSSED)),
        ) { state, move ->
            when (move) {
                is Move.SetMark -> state.copy(symbols = state.symbols + (move.cell to move.mark))
                is Move.SetDigit, is Move.SetNotes -> error("Unsupported fixture edit")
            }
        }
        assertEquals(Mark.CROSSED, playThroughCodec(engine).symbols[Cell(1)])
    }

    @Test
    fun nonogramCanOwnSeparateRowAndColumnClues() {
        val puzzle = CluePuzzle(listOf(listOf(1)), listOf(listOf(1)))
        val engine = FixtureEngine(
            puzzle,
            ClueState(puzzle, emptyMap()),
            ClueState(puzzle, mapOf(Cell(0) to Mark.FILLED)),
            listOf(Move.SetMark(Cell(0), Mark.FILLED)),
        ) { state, move ->
            when (move) {
                is Move.SetMark -> state.copy(marks = state.marks + (move.cell to move.mark))
                is Move.SetDigit, is Move.SetNotes -> error("Unsupported fixture edit")
            }
        }
        assertEquals(puzzle.columns, playThroughCodec(engine).puzzle.columns)
    }

    @Test
    fun miniSudokuSupportsDigitsAndNotesInTheSameHint() {
        val puzzle = DigitPuzzle(Grid(6), 2, 3)
        val engine = FixtureEngine(
            puzzle,
            DigitState(puzzle, emptyMap(), mapOf(Cell(0) to 3)),
            DigitState(puzzle, mapOf(Cell(0) to 2), emptyMap()),
            listOf(Move.SetNotes(Cell(0), 0), Move.SetDigit(Cell(0), 2)),
        ) { state, move ->
            when (move) {
                is Move.SetDigit -> state.copy(digits = state.digits + (move.cell to move.digit))
                is Move.SetNotes -> state.copy(
                    notes = if (move.digitMask == 0) state.notes - move.cell else state.notes + (move.cell to move.digitMask),
                )
                is Move.SetMark -> error("Unsupported fixture edit")
            }
        }
        val state = playThroughCodec(engine)
        assertEquals(2, state.digits[Cell(0)])
        assertTrue(state.notes.isEmpty())
    }

    private fun <P : Any, S : Any> playThroughCodec(engine: GameEngine<P, S>): S {
        val puzzle = engine.decodePuzzle(engine.encodePuzzle(engine.generate(SplitMix64(1), Pack.EASY)))
        val initial = engine.initialState(puzzle)
        assertFalse(engine.isSolved(puzzle, initial))
        assertTrue(engine.mistakes(puzzle, initial).isEmpty())
        assertEquals(1, engine.countSolutions(puzzle))
        val step = requireNotNull(engine.nextStep(puzzle, initial))
        val solved = step.moves.fold(initial) { state, move -> engine.applyMove(state, move) }
        assertTrue(engine.isSolved(puzzle, solved))
        assertEquals(null, engine.nextStep(puzzle, solved))
        assertEquals(step.technique, engine.grade(puzzle).hardestTechnique)
        return engine.decodeState(engine.encodeState(solved))
    }

    /** Fixed fixtures test type composition, not any lane's puzzle rules or codecs. */
    private class FixtureEngine<P : Any, S : Any>(
        private val puzzle: P,
        private val initial: S,
        private val solved: S,
        private val moves: List<Move>,
        private val edit: (S, Move) -> S,
    ) : GameEngine<P, S> {
        override val id: String = "fixture"

        override fun decodePuzzle(line: String): P {
            require(line == "fixture")
            return puzzle
        }

        override fun encodePuzzle(puzzle: P): String {
            require(puzzle == this.puzzle)
            return "fixture"
        }

        override fun initialState(puzzle: P): S = initial

        override fun applyMove(state: S, move: Move): S = edit(state, move)

        override fun isSolved(puzzle: P, state: S): Boolean = state == solved

        override fun mistakes(puzzle: P, state: S): List<Cell> = emptyList()

        override fun countSolutions(puzzle: P, limit: Int): Int {
            require(limit > 0)
            return 1
        }

        override fun nextStep(puzzle: P, state: S): HintStep? =
            if (state == solved) null else HintStep(
                Technique("fixture", "Fixture deduction", 0),
                listOf(Cell(0)),
                "The fixture has one forced step.",
                moves,
            )

        override fun grade(puzzle: P): Grade = Grade(requireNotNull(nextStep(puzzle, initial)).technique, 1, Pack.EASY)

        override fun generate(rng: SplitMix64, pack: Pack): P = puzzle

        override fun encodeState(state: S): ByteArray {
            require(state == initial || state == solved)
            return byteArrayOf(if (state == initial) 0 else 1)
        }

        override fun decodeState(bytes: ByteArray): S {
            require(bytes.size == 1 && bytes[0] in 0..1)
            return if (bytes[0] == 0.toByte()) initial else solved
        }
    }
}
