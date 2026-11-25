package com.datathecodie.witgrid.games.futoshiki

import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiPuzzle
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FutoshikiCellRelationsTest {
    // 3x3 board; signs touch the centre cell (index 4) from all four sides.
    private val state: FutoshikiState = run {
        val size = 3
        val horizontal = MutableList(size * (size - 1)) { FutoshikiSign.NONE }
        val vertical = MutableList((size - 1) * size) { FutoshikiSign.NONE }
        horizontal[1 * (size - 1) + 0] = FutoshikiSign.LESS // (1,0) < (1,1): centre larger than left
        horizontal[1 * (size - 1) + 1] = FutoshikiSign.GREATER // (1,1) > (1,2): centre larger than right
        vertical[0 * size + 1] = FutoshikiSign.GREATER // (0,1) > (1,1): centre smaller than above
        vertical[1 * size + 1] = FutoshikiSign.LESS // (1,1) < (2,1): centre smaller than below
        val puzzle = FutoshikiPuzzle(
            id = "FU-TEST-REL",
            size = size,
            givens = List(size * size) { null },
            horizontalSigns = horizontal,
            verticalSigns = vertical,
            solution = List(size * size) { (it % size) + 1 },
            pack = Pack.EASY,
            storedGrade = "none:0",
        )
        FutoshikiState.initial(puzzle)
    }

    @Test
    fun centreCellNamesEveryInequalityFromItsOwnSide() {
        assertEquals(
            listOf(
                Pair(true, R.string.futoshiki_side_above),
                Pair(false, R.string.futoshiki_side_right),
                Pair(true, R.string.futoshiki_side_below),
                Pair(false, R.string.futoshiki_side_left),
            ),
            futoshikiCellRelations(state, Cell(4)),
        )
    }

    @Test
    fun neighbourSeesTheMirroredRelation() {
        // Left cell (1,0) is smaller than the centre, which is on its right.
        assertEquals(listOf(Pair(true, R.string.futoshiki_side_right)), futoshikiCellRelations(state, Cell(3)))
        // Top cell (0,1) is larger than the centre, which is below it.
        assertEquals(listOf(Pair(false, R.string.futoshiki_side_below)), futoshikiCellRelations(state, Cell(1)))
    }

    @Test
    fun cornerWithoutSignsHasNoRelations() {
        assertEquals(emptyList<Pair<Boolean, Int>>(), futoshikiCellRelations(state, Cell(0)))
    }
}
