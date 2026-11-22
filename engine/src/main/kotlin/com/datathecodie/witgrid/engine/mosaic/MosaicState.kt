package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import java.util.ArrayList
import java.util.Collections

/** Immutable player marks on a Mosaic grid, tied to the clue puzzle. */
class MosaicState(
    val puzzle: MosaicPuzzle,
    marks: List<Mark>,
) {
    /** Row-major fill, cross or empty marks, defensively copied. */
    val marks: List<Mark> = Collections.unmodifiableList(ArrayList(marks))

    init {
        require(marks.size == puzzle.grid.cellCount) { "Mosaic state needs one mark per cell" }
    }

    /** Returns a new state with one absolute mark edit applied. */
    fun withMove(move: Move): MosaicState {
        require(move.cell.index in 0 until puzzle.grid.cellCount) { "Move cell outside Mosaic puzzle" }
        val setMark = move as? Move.SetMark
            ?: throw IllegalArgumentException("Mosaic supports mark moves only")
        val updated = marks.toMutableList()
        updated[move.cell.index] = setMark.mark
        return MosaicState(puzzle, updated)
    }

    override fun equals(other: Any?): Boolean = other is MosaicState &&
        puzzle == other.puzzle && marks == other.marks

    override fun hashCode(): Int = 31 * puzzle.hashCode() + marks.hashCode()

    companion object {
        /** Creates the empty board after checking that the stored solution matches its clues. */
        fun initial(puzzle: MosaicPuzzle): MosaicState {
            require(MosaicRules.isValidSolution(puzzle)) {
                "Mosaic stored solution does not match its clues"
            }
            return MosaicState(puzzle, List(puzzle.grid.cellCount) { Mark.EMPTY })
        }
    }
}
