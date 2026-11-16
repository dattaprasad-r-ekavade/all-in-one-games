package com.datathecodie.witgrid.engine.core

/**
 * Immutable undo/redo history of state snapshots; [S] must itself be immutable.
 * Snapshots also restore notes and multi-cell hints without inverse-move logic.
 *
 * @property present The current snapshot, including all player entries and notes.
 */
class History<S : Any> private constructor(
    private val past: List<S>,
    val present: S,
    private val future: List<S>,
) {
    /** Starts a history at [initialState] with no undo or redo entries. */
    constructor(initialState: S) : this(emptyList(), initialState, emptyList())

    /** Whether [undo] can restore an earlier snapshot. */
    val canUndo: Boolean get() = past.isNotEmpty()

    /** Whether [redo] can restore an undone snapshot. */
    val canRedo: Boolean get() = future.isNotEmpty()

    /**
     * Records a changed snapshot and discards the redo branch. Equal states are a no-op,
     * preserving redo; record once after all moves in a hint to make its application atomic.
     */
    fun record(state: S): History<S> =
        if (state == present) this else History(past + present, state, emptyList())

    /** Restores the previous snapshot, or returns this history when already at its start. */
    fun undo(): History<S> =
        if (!canUndo) this else History(past.dropLast(1), past.last(), future + present)

    /** Restores the next snapshot, or returns this history when no redo is available. */
    fun redo(): History<S> =
        if (!canRedo) this else History(past + present, future.last(), future.dropLast(1))
}
