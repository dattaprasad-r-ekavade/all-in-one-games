package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Test
import java.time.Duration

class NonogramCounterReviewTest {
    @Test
    fun countsZeroOneAndTwoAtEveryLaunchSize() {
        for ((size, pack) in listOf(5 to Pack.EASY, 10 to Pack.MEDIUM, 15 to Pack.EXPERT)) {
            val onePerLine = List(size) { listOf(1) }
            val impossible = puzzle(size, pack, onePerLine, onePerLine.dropLast(1) + listOf(listOf(2)))
            val multiple = puzzle(size, pack, onePerLine, onePerLine)
            val unique = puzzle(
                size,
                pack,
                List(size) { row -> if (row % 2 == 0) listOf(size) else emptyList() },
                List(size) { List((size + 1) / 2) { 1 } },
            )

            assertTimeout(Duration.ofSeconds(2)) {
                assertEquals(0, NonogramRules.countSolutions(impossible), "size=$size")
                assertEquals(1, NonogramRules.countSolutions(unique), "size=$size")
                assertEquals(2, NonogramRules.countSolutions(multiple), "size=$size")
                assertEquals(1, NonogramRules.countSolutions(multiple, 1), "size=$size")
            }
        }
    }

    @Test
    fun agreesWithIndependentExhaustiveFiveByFiveEnumeration() {
        val rng = SplitMix64(612739L)
        repeat(200) { fixture ->
            val values = List(25) { rng.nextInt(2) }
            val secondValues = if (fixture % 2 == 0) values else {
                // Preserve the total while creating unrelated row/column constraints.
                val shuffled = values.toMutableList()
                for (index in shuffled.lastIndex downTo 1) {
                    val other = rng.nextInt(index + 1)
                    val value = shuffled[index]
                    shuffled[index] = shuffled[other]
                    shuffled[other] = value
                }
                shuffled
            }
            val rows = List(5) { row -> runs(values.subList(row * 5, row * 5 + 5)) }
            val columns = List(5) { column -> runs(List(5) { row -> secondValues[row * 5 + column] }) }
            val puzzle = puzzle(5, Pack.EASY, rows, columns)
            val expected = exhaustiveCount(rows, columns)

            for (limit in listOf(1, 2, 3, Int.MAX_VALUE)) {
                assertEquals(minOf(expected, limit), NonogramRules.countSolutions(puzzle, limit),
                    "fixture=$fixture limit=$limit rows=$rows columns=$columns")
            }
        }
    }

    private fun exhaustiveCount(rows: List<List<Int>>, columns: List<List<Int>>): Int {
        val possibleRows = rows.map { clues ->
            (0 until 32).map { mask -> List(5) { column -> (mask ushr column) and 1 } }
                .filter { runs(it) == clues }
        }
        val board = Array(5) { emptyList<Int>() }
        var count = 0
        fun enumerate(row: Int) {
            if (row == 5) {
                if ((0 until 5).all { column -> runs(List(5) { board[it][column] }) == columns[column] }) {
                    count++
                }
                return
            }
            for (candidate in possibleRows[row]) {
                board[row] = candidate
                enumerate(row + 1)
            }
        }
        enumerate(0)
        return count
    }

    private fun runs(values: List<Int>): List<Int> =
        values.joinToString("").split('0').filter { it.isNotEmpty() }.map { it.length }

    private fun puzzle(
        size: Int,
        pack: Pack,
        rows: List<List<Int>>,
        columns: List<List<Int>>,
    ): NonogramPuzzle = NonogramPuzzle(
        id = "NO-COUNTER-REVIEW",
        size = size,
        rowClues = rows,
        columnClues = columns,
        // A counter must not trust the stored solution.
        solution = List(size * size) { 0 },
        pack = pack,
        storedGrade = "pending",
    )
}
