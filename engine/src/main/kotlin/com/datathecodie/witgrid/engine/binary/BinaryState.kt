package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move

/** The two player-entered symbols plus the empty state. */
enum class BinarySymbol {
    EMPTY,
    A,
    B,
}

/** Immutable player symbols tied to the puzzle so givens remain fixed. */
class BinaryState(
    val puzzle: BinaryPuzzle,
    symbols: List<BinarySymbol>,
) {
    /** Row-major player symbols, defensively copied. */
    val symbols: List<BinarySymbol> = immutableList(symbols)

    init {
        require(symbols.size == puzzle.grid.cellCount) { "Binary state needs one symbol per cell" }
        puzzle.givens.forEachIndexed { index, given ->
            require(given == null || symbols[index].toDigit() == given) {
                "Binary state must retain every given"
            }
        }
    }

    /**
     * Returns a state with one absolute symbol edit applied. `FILLED` means A (zero), `CROSSED`
     * means B (one), and `EMPTY` clears a non-given cell.
     */
    fun withMove(move: Move): BinaryState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Binary puzzle" }
        val setMark = move as? Move.SetMark
            ?: throw IllegalArgumentException("Binary supports symbol mark moves only")
        val updated = symbols.toMutableList()
        updated[move.cell.index] = setMark.mark.toSymbol()
        return BinaryState(puzzle, updated)
    }

    override fun equals(other: Any?): Boolean = other is BinaryState &&
        puzzle == other.puzzle && symbols == other.symbols

    override fun hashCode(): Int = 31 * puzzle.hashCode() + symbols.hashCode()

    companion object {
        /** Creates the player board with the puzzle's givens already entered. */
        fun initial(puzzle: BinaryPuzzle): BinaryState = BinaryState(
            puzzle,
            puzzle.givens.map { given -> given?.toBinarySymbol() ?: BinarySymbol.EMPTY },
        )
    }
}

internal fun Mark.toSymbol(): BinarySymbol = when (this) {
    Mark.EMPTY -> BinarySymbol.EMPTY
    Mark.FILLED -> BinarySymbol.A
    Mark.CROSSED -> BinarySymbol.B
}

internal fun BinarySymbol.toDigit(): Int? = when (this) {
    BinarySymbol.EMPTY -> null
    BinarySymbol.A -> 0
    BinarySymbol.B -> 1
}

internal fun Int.toBinarySymbol(): BinarySymbol = when (this) {
    0 -> BinarySymbol.A
    1 -> BinarySymbol.B
    else -> throw IllegalArgumentException("Binary symbol must be zero or one")
}
