package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import java.util.ArrayList
import java.util.Collections

/** Immutable Nonogram marks tied to the clue puzzle. */
class NonogramState(
    val puzzle: NonogramPuzzle,
    marks: List<Mark>,
) {
    /** Row-major fill, X or empty marks, defensively copied. */
    val marks: List<Mark> = immutableMarks(marks)

    init {
        require(marks.size == puzzle.grid.cellCount) { "Nonogram state needs one mark per cell" }
    }

    /** Returns a new state with one absolute mark edit applied. */
    fun withMove(move: Move): NonogramState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Nonogram puzzle" }
        val setMark = move as? Move.SetMark
            ?: throw IllegalArgumentException("Nonogram supports mark moves only")
        val updated = marks.toMutableList()
        updated[move.cell.index] = setMark.mark
        return NonogramState(puzzle, updated)
    }

    override fun equals(other: Any?): Boolean = other is NonogramState &&
        puzzle == other.puzzle && marks == other.marks

    override fun hashCode(): Int = 31 * puzzle.hashCode() + marks.hashCode()

    companion object {
        /** Creates the empty board after checking that the stored pattern matches its clues. */
        fun initial(puzzle: NonogramPuzzle): NonogramState {
            require(NonogramRules.isValidSolution(puzzle)) {
                "Nonogram stored solution does not match its clues"
            }
            return NonogramState(puzzle, List(puzzle.grid.cellCount) { Mark.EMPTY })
        }
    }
}

private fun immutableMarks(values: List<Mark>): List<Mark> =
    Collections.unmodifiableList(ArrayList(values))
