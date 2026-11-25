package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Killer Sudoku rules and bank model exposed through the shared engine contract. */
object KillerSudokuEngine : GameEngine<KillerSudokuPuzzle, KillerSudokuState> {
    override val id: String = "killersudoku"

    override fun decodePuzzle(line: String): KillerSudokuPuzzle = KillerSudokuCodec.decode(line)

    override fun encodePuzzle(puzzle: KillerSudokuPuzzle): String = KillerSudokuCodec.encode(puzzle)

    override fun initialState(puzzle: KillerSudokuPuzzle): KillerSudokuState =
        KillerSudokuState.initial(puzzle)

    override fun applyMove(state: KillerSudokuState, move: Move): KillerSudokuState =
        state.withMove(move)

    override fun isSolved(puzzle: KillerSudokuPuzzle, state: KillerSudokuState): Boolean =
        KillerSudokuRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: KillerSudokuPuzzle, state: KillerSudokuState): List<Cell> =
        KillerSudokuRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: KillerSudokuPuzzle, limit: Int): Int =
        KillerSudokuRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: KillerSudokuPuzzle, state: KillerSudokuState): HintStep? =
        KillerSudokuTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: KillerSudokuPuzzle): Grade =
        KillerSudokuTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): KillerSudokuPuzzle =
        throw NotImplementedError("killersudoku")

    override fun encodeState(state: KillerSudokuState): ByteArray =
        KillerSudokuStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): KillerSudokuState =
        KillerSudokuStateCodec.decode(bytes)
}
