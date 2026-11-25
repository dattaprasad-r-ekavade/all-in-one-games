package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.tactics.ChessMove
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.TacticsBoard
import com.datathecodie.witgrid.engine.tactics.TacticsEngine
import com.datathecodie.witgrid.engine.tactics.TacticsPuzzle
import com.datathecodie.witgrid.engine.tactics.TacticsState
import com.datathecodie.witgrid.engine.tactics.squareToCell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsBoardInputTest {

    @Test
    fun mateInOneFixtureSolvesThroughTapPicks() {
        val puzzle = mate1UniquePuzzle()
        var state = TacticsState.initial(puzzle)
        assertFalse(TacticsEngine.isSolved(puzzle, state))

        state = applyTap(state, squareToCell("d7"))
        assertEquals(squareToCell("d7"), state.pendingFrom)

        state = applyTap(state, squareToCell("g7"))
        assertNull(state.pendingFrom)
        assertTrue(TacticsEngine.isSolved(puzzle, state))
    }

    @Test
    fun deselectsWhenTappingSelectedFromSquareAgain() {
        val puzzle = mate1UniquePuzzle()
        var state = TacticsState.initial(puzzle)

        state = applyTap(state, squareToCell("d7"))
        assertEquals(squareToCell("d7"), state.pendingFrom)

        state = applyTap(state, squareToCell("d7"))
        assertNull(state.pendingFrom)
        assertEquals(
            listOf(Move.SetMark(squareToCell("d7"), Mark.EMPTY)),
            TacticsBoardInput.tapMoves(
                apply(TacticsState.initial(puzzle), listOf(Move.SetMark(squareToCell("d7"), Mark.FILLED))),
                squareToCell("d7"),
            ),
        )
    }

    @Test
    fun emptySquareTapIsIgnoredByEngine() {
        val puzzle = mate1UniquePuzzle()
        val state = TacticsState.initial(puzzle)
        val emptySquare = squareToCell("a8")

        val moves = TacticsBoardInput.tapMoves(state, emptySquare)
        assertEquals(listOf(Move.SetMark(emptySquare, Mark.FILLED)), moves)

        val next = apply(state, emptySquare)
        assertEquals(state, next)
        assertNull(next.pendingFrom)
    }

    private fun applyTap(state: TacticsState, cell: com.datathecodie.witgrid.engine.core.Cell): TacticsState =
        apply(state, TacticsBoardInput.tapMoves(state, cell))

    private fun apply(state: TacticsState, moves: List<Move>): TacticsState =
        moves.fold(state) { current, move -> TacticsEngine.applyMove(current, move) }

    private fun apply(state: TacticsState, cell: com.datathecodie.witgrid.engine.core.Cell): TacticsState =
        apply(state, TacticsBoardInput.tapMoves(state, cell))

    private fun mate1UniquePuzzle(): TacticsPuzzle = TacticsPuzzle(
        id = "mate1-unique",
        initialBoard = TacticsBoard.fromFen("7k/3Q4/5K2/8/8/8/8/8 w - - 0 1"),
        sideToMove = PieceColor.WHITE,
        mateIn = 1,
        solutionLine = listOf(ChessMove(squareToCell("d7"), squareToCell("g7"))),
        pack = Pack.EASY,
        storedGrade = "checkmate:1",
    )
}