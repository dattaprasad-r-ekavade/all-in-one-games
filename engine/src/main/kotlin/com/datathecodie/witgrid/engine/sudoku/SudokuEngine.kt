package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Sudoku rules and bank model exposed through the shared engine contract. */
object SudokuEngine : GameEngine<SudokuPuzzle, SudokuState> {
    override val id: String = "sudoku"

    override fun decodePuzzle(line: String): SudokuPuzzle = SudokuCodec.decode(line)

    override fun encodePuzzle(puzzle: SudokuPuzzle): String = SudokuCodec.encode(puzzle)

    override fun initialState(puzzle: SudokuPuzzle): SudokuState = SudokuState.initial(puzzle)

    override fun applyMove(state: SudokuState, move: Move): SudokuState = state.withMove(move)

    override fun isSolved(puzzle: SudokuPuzzle, state: SudokuState): Boolean =
        SudokuRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: SudokuPuzzle, state: SudokuState): List<Cell> =
        SudokuRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: SudokuPuzzle, limit: Int): Int =
        SudokuRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: SudokuPuzzle, state: SudokuState): HintStep? =
        SudokuTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: SudokuPuzzle): Grade = SudokuTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): SudokuPuzzle =
        throw NotImplementedError("sudoku")

    override fun encodeState(state: SudokuState): ByteArray = SudokuStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): SudokuState = SudokuStateCodec.decode(bytes)
}