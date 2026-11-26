package com.datathecodie.witgrid.engine.arrows.spec

import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsPuzzle
import com.datathecodie.witgrid.engine.core.Cell

/**
 * Hand-made Arrows boards for plan 12.4.
 *
 * Easy pack size is 5x5, so every playable fixture is 5x5. The live arrows sit in a
 * 3x3 or 4x4 cluster; empty padding keeps the path-to-edge short enough to check by hand.
 */
internal object ArrowsSpecFixtures {

    const val EASY_SIZE = 5

    /** Plan 12.4: three blocked taps fail the level. */
    const val MISTAKE_LIMIT = 3

    /**
     * One right arrow. Path to the right edge is empty.
     *
     *     . . . . .
     *     . > . . .
     *     . . . . .
     *     . . . . .
     *     . . . . .
     *
     * (1,1) `>` index 6: path (1,2) (1,3) (1,4) all empty. Slide is legal.
     * After that slide the board is empty (win).
     */
    val CLEAR_RIGHT = arrayOf(
        ".....",
        ".>...",
        ".....",
        ".....",
        ".....",
    )

    /**
     * Up arrow already on the top edge. Path to the edge has no cells.
     *
     *     ^ . . . .
     *     . . . . .
     *     . . . . .
     *     . . . . .
     *     . . . . .
     *
     * (0,0) `^` index 0: next cell is off the board. Slide is legal. Board then empty.
     */
    val EDGE_UP = arrayOf(
        "^....",
        ".....",
        ".....",
        ".....",
        ".....",
    )

    /**
     * One blocked arrow and one free arrow. Used for mistakes, Continue, and a two-slide win.
     *
     *     . . . . .
     *     . > v . .
     *     . . . . .
     *     . . . . .
     *     . . . . .
     *
     * (1,1) `>` index 6: path hits `v` at (1,2). Blocked. A tap is a mistake.
     * (1,2) `v` index 7: path (2,2) (3,2) (4,2) all empty. Slide is legal.
     * After sliding 7, index 6 has an empty path and then slides. Clearable.
     */
    val BLOCKED_AND_FREE = arrayOf(
        ".....",
        ".>v..",
        ".....",
        ".....",
        ".....",
    )

    /**
     * Distant blocker: empty cells sit between the arrow and the occupant on its path.
     *
     *     . . . . .
     *     . > . . v
     *     . . . . .
     *     . . . . .
     *     . . . . .
     *
     * (1,1) `>` index 6: path (1,2) empty, (1,3) empty, (1,4) `v`. Blocked.
     * (1,4) `v` index 9: path (2,4) (3,4) (4,4) all empty. Slide is legal.
     */
    val DISTANT_BLOCKER = arrayOf(
        ".....",
        ".>..v",
        ".....",
        ".....",
        ".....",
    )

    /**
     * Three-arrow order used by the hint-follow test.
     *
     *     . . . . .
     *     . > . v .
     *     . . . < .
     *     . . . . .
     *     . . . . .
     *
     * (1,1) `>` index 6: path hits `v` at (1,3). Blocked.
     * (1,3) `v` index 8: path hits `<` at (2,3). Blocked.
     * (2,3) `<` index 13: path (2,2) (2,1) (2,0) all empty. Only legal slide.
     * After 13, index 8 is free; after 8, index 6 is free. Three slides, board empty.
     */
    val HINT_CHAIN = arrayOf(
        ".....",
        ".>.v.",
        "...<.",
        ".....",
        ".....",
    )

    /**
     * Two arrows face each other on a row. Neither path to the edge is empty.
     * Bank decode must reject this layout.
     *
     *     . . . . .
     *     . . > < .
     *     . . . . .
     *     . . . . .
     *     . . . . .
     *
     * (1,2) `>` index 7: path hits `<` at (1,3). Blocked.
     * (1,3) `<` index 8: path hits `>` at (1,2). Blocked.
     * No legal slide, so the board cannot clear.
     */
    val FACING_PAIR = arrayOf(
        ".....",
        "..><.",
        ".....",
        ".....",
        ".....",
    )

    /**
     * Two arrows face each other in a column. Same deadlock, other axis.
     *
     *     . . . . .
     *     . . v . .
     *     . . ^ . .
     *     . . . . .
     *     . . . . .
     *
     * (1,2) `v` index 7: path hits `^` at (2,2). Blocked.
     * (2,2) `^` index 12: path hits `v` at (1,2). Blocked.
     */
    val FACING_COLUMN = arrayOf(
        ".....",
        "..v..",
        "..^..",
        ".....",
        ".....",
    )

    fun rows(vararg lines: String): String {
        require(lines.size == EASY_SIZE) { "Easy fixture has $EASY_SIZE rows" }
        require(lines.all { it.length == EASY_SIZE }) { "Easy fixture rows are $EASY_SIZE cells" }
        return lines.joinToString("")
    }

    fun easyLine(bankId: String, payload: String): String =
        "$bankId|$EASY_SIZE|$payload|EASY|easy"

    fun easyPuzzle(bankId: String, vararg lines: String): ArrowsPuzzle =
        ArrowsEngine.decodePuzzle(easyLine(bankId, rows(*lines)))

    fun slide(row: Int, column: Int, size: Int = EASY_SIZE): ArrowsAction {
        require(row in 0 until size && column in 0 until size) {
            "Slide cell ($row, $column) is outside a ${size}x${size} board"
        }
        return ArrowsAction(Cell(row * size + column))
    }

    /**
     * Plan 12.4 oracle: an arrow slides only when every cell from it to the edge
     * in its pointing direction is empty. Integer steps only.
     */
    fun pathToEdgeIsEmpty(size: Int, arrows: List<ArrowsDirection?>, index: Int): Boolean {
        val direction = arrows.getOrNull(index) ?: return false
        var row = index / size + direction.rowDelta
        var column = index % size + direction.columnDelta
        while (row in 0 until size && column in 0 until size) {
            if (arrows[row * size + column] != null) return false
            row += direction.rowDelta
            column += direction.columnDelta
        }
        return true
    }

    fun isBoardEmpty(arrows: List<ArrowsDirection?>): Boolean = arrows.all { it == null }
}
