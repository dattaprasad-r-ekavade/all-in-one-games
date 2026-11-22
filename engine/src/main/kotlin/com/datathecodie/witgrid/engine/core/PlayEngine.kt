package com.datathecodie.witgrid.engine.core

/**
 * Pure rules and codecs for one Family B game, with lane-owned immutable puzzle [P],
 * state [S], and player action [A] (for example pour, slide or card move).
 *
 * Family B levels need not have a unique solution. A level is fair when it is winnable
 * from the deal, or when failure only appears after a legal-but-wrong player action.
 * Operations never mutate their inputs; [generate] consumes its explicitly supplied random stream.
 * Invalid inputs throw [IllegalArgumentException].
 */
interface PlayEngine<P : Any, S : Any, A : Any> {
    /** Stable game key used by the registry and bank filenames. */
    val id: String

    /** Decodes one complete Family B bank line, checking lane payload and metadata. */
    fun decodePuzzle(line: String): P

    /** Encodes a puzzle as one canonical bank line. */
    fun encodePuzzle(puzzle: P): String

    /** Creates the player state at level start. [isLost] must be false for this state. */
    fun initialState(puzzle: P): S

    /** Returns a state with [action] applied. Illegal actions throw [IllegalArgumentException]. */
    fun applyAction(state: S, action: A): S

    /** True only when the win condition for this level is satisfied. */
    fun isWon(puzzle: P, state: S): Boolean

    /**
     * True only after a legal player action has entered a failed end state.
     * Must be false on [initialState] and on a fresh deal before any action.
     */
    fun isLost(puzzle: P, state: S): Boolean

    /**
     * One legal action that improves toward a win, if any exists.
     * Returns null when the level is already won, already lost, or has no improving move.
     */
    fun hintAction(puzzle: P, state: S): A?

    /**
     * Returns a winnable (or legally failable) puzzle in [pack], deterministically consuming [rng].
     * Lanes may retry generation internally; they must not use [kotlin.random.Random] or [java.util.Random].
     */
    fun generate(rng: SplitMix64, pack: Pack): P

    /** Encodes a versioned save including puzzle context and in-progress play. */
    fun encodeState(state: S): ByteArray

    /** Restores a self-contained state; rejects malformed data or unsupported save versions. */
    fun decodeState(bytes: ByteArray): S

    /**
     * Applies one rewarded Continue after failure (plan 8.4): extra tube, revive, or extra undo.
     * The returned state must clear [isLost] while preserving valid in-progress progress.
     * The engine may apply Continue more than once; app policy (lives / rewarded ad) is the limiter.
     * Illegal calls throw [IllegalArgumentException].
     */
    fun applyContinue(state: S): S
}