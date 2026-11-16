package com.datathecodie.witgrid.engine.core

/**
 * Pure rules and codecs for one game, with lane-owned immutable puzzle [P] and state [S].
 * No shared grid-shaped superclass is required: puzzles can carry regions or line clues,
 * and states can carry marks, digits or notes. State must retain any puzzle context needed
 * by [applyMove] and [encodeState]. Operations never mutate their inputs, except [generate]
 * consumes its explicitly supplied random stream. Invalid inputs throw [IllegalArgumentException].
 */
interface GameEngine<P : Any, S : Any> {
    /** Stable game key used by the registry and bank filenames. */
    val id: String

    /** Decodes one complete [BankLine], checking lane payload, solution and metadata. */
    fun decodePuzzle(line: String): P

    /** Encodes a puzzle as one canonical [BankLine], including its stored solution and grade. */
    fun encodePuzzle(puzzle: P): String

    /** Creates the player state with givens and no player edits. */
    fun initialState(puzzle: P): S

    /**
     * Returns a state with an absolute edit applied; givens cannot be edited.
     * A rule-breaking player entry remains representable for [mistakes] to report.
     */
    fun applyMove(state: S, move: Move): S

    /** True only for a complete, correct board; pencil notes do not satisfy cells. */
    fun isSolved(puzzle: P, state: S): Boolean

    /** Returns incorrect committed entries in row-major order; empty cells and notes are ignored. */
    fun mistakes(puzzle: P, state: S): List<Cell>

    /**
     * Independently counts rule-valid solutions, stopping at the positive [limit].
     * Returns `min(actual count, limit)` and does not trust the stored solution or technique solver.
     */
    fun countSolutions(puzzle: P, limit: Int = 2): Int

    /**
     * Returns the easiest available guess-free deduction, breaking ties deterministically.
     * Returns null for a solved, stuck or mistaken board; callers highlight [mistakes] first.
     */
    fun nextStep(puzzle: P, state: S): HintStep?

    /** Grades a full technique solve from givens; rejects puzzles requiring a guess. */
    fun grade(puzzle: P): Grade

    /** Returns a unique, technique-solvable puzzle in [pack], deterministically consuming [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): P

    /** Encodes a versioned save including puzzle context, givens, player entries and notes. */
    fun encodeState(state: S): ByteArray

    /** Restores a self-contained state; rejects malformed data or unsupported save versions. */
    fun decodeState(bytes: ByteArray): S
}
