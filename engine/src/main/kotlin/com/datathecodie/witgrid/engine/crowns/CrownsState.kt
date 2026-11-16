package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move

/** Immutable player marks tied to their puzzle so moves and saves retain the board context. */
class CrownsState(
    val puzzle: CrownsPuzzle,
    marks: List<Mark>,
) {
    /** Row-major cell marks, defensively copied from the constructor input. */
    val marks: List<Mark> = immutableList(marks)

    init {
        require(marks.size == puzzle.grid.cellCount) { "Crowns state needs one mark per cell" }
    }

    /** Returns a state with one absolute mark edit applied. */
    fun withMove(move: Move): CrownsState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Crowns puzzle" }
        require(move is Move.SetMark) { "Crowns supports mark moves only" }
        val updated = marks.toMutableList()
        updated[move.cell.index] = move.mark
        return CrownsState(puzzle, updated)
    }

    override fun equals(other: Any?): Boolean = other is CrownsState &&
        puzzle == other.puzzle && marks == other.marks

    override fun hashCode(): Int = 31 * puzzle.hashCode() + marks.hashCode()

    companion object {
        /** Creates an empty board for a puzzle whose stored solution obeys all Crowns rules. */
        fun initial(puzzle: CrownsPuzzle): CrownsState {
            require(CrownsRules.isValidSolution(puzzle)) { "Crowns puzzle solution is not rule-valid" }
            return CrownsState(puzzle, List(puzzle.grid.cellCount) { Mark.EMPTY })
        }
    }
}
