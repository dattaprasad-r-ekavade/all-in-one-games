package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Mini Sudoku rules and bank model exposed through the shared engine contract. */
object MiniSudokuEngine : GameEngine<MiniSudokuPuzzle, MiniSudokuState> {
    override val id: String = "minisudoku"

    override fun decodePuzzle(line: String): MiniSudokuPuzzle = MiniSudokuCodec.decode(line)

    override fun encodePuzzle(puzzle: MiniSudokuPuzzle): String = MiniSudokuCodec.encode(puzzle)

    override fun initialState(puzzle: MiniSudokuPuzzle): MiniSudokuState = MiniSudokuState.initial(puzzle)

    override fun applyMove(state: MiniSudokuState, move: Move): MiniSudokuState = state.withMove(move)

    override fun isSolved(puzzle: MiniSudokuPuzzle, state: MiniSudokuState): Boolean =
        MiniSudokuRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: MiniSudokuPuzzle, state: MiniSudokuState): List<Cell> =
        MiniSudokuRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: MiniSudokuPuzzle, limit: Int): Int =
        MiniSudokuRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: MiniSudokuPuzzle, state: MiniSudokuState): HintStep? =
        MiniSudokuTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: MiniSudokuPuzzle): Grade = MiniSudokuTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): MiniSudokuPuzzle =
        MiniSudokuGenerator.generate(rng, pack)

    override fun encodeState(state: MiniSudokuState): ByteArray = unavailable()

    override fun decodeState(bytes: ByteArray): MiniSudokuState = unavailable()

    private fun unavailable(): Nothing = throw NotImplementedError("MS")
}
