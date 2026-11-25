package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillerSudokuGeneratorTest {
    @Test
    fun sameSeedGivesTheSamePuzzleAndTheEngineDelegates() {
        Pack.values().forEach { pack ->
            val first = KillerSudokuGenerator.generate(SplitMix64(1), pack)
            val second = KillerSudokuGenerator.generate(SplitMix64(1), pack)
            assertEquals(first, second, "$pack seed 1 must repeat the same puzzle")
        }
        val direct = KillerSudokuGenerator.generate(SplitMix64(4), Pack.EASY)
        val throughEngine = KillerSudokuEngine.generate(SplitMix64(4), Pack.EASY)
        assertEquals(direct, throughEngine)
    }

    @Test
    fun fivePuzzlesPerPackAreUniqueSolvableAndGradedInBand() {
        Pack.values().forEach { pack ->
            val puzzles = List(SAMPLE_SIZE) { index ->
                KillerSudokuGenerator.generate(SplitMix64(20L + index), pack)
            }
            assertEquals(SAMPLE_SIZE, puzzles.map { cageLayout(it) }.toSet().size, "$pack cage layouts must differ")
            puzzles.forEach { puzzle ->
                assertEquals(pack, puzzle.pack)
                assertTrue(KillerSudokuRules.isValidSolution(puzzle))
                assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2), puzzle.id)
                if (pack == Pack.EASY) {
                    assertTrue(puzzle.givens.all { it == null }, "Easy uses cages instead of givens")
                }
                puzzle.cages.forEach { cage ->
                    assertTrue(cage.cells.size in 1..5, "Cage size ${cage.cells.size} in ${puzzle.id}")
                    assertTrue(isOrthogonallyConnected(cage.cells.map { it.index }), puzzle.id)
                    val digits = cage.cells.map { puzzle.solution[it.index] }
                    assertEquals(digits.toSet().size, digits.size, "Repeated cage digit in ${puzzle.id}")
                    assertEquals(digits.sum(), cage.sum, puzzle.id)
                }
                val grade = KillerSudokuTechniqueSolver.grade(puzzle)
                assertTrue(KillerSudokuPackBands.isGradeInPackBand(pack, grade), "$pack grade $grade")
                assertEquals(KillerSudokuGradeCodec.encode(grade), puzzle.storedGrade)
            }
        }
    }

    private fun cageLayout(puzzle: KillerSudokuPuzzle): List<List<Int>> =
        puzzle.cages.map { cage -> cage.cells.map { it.index } }

    private fun isOrthogonallyConnected(cells: List<Int>): Boolean {
        if (cells.size <= 1) return true
        val pending = cells.toMutableSet()
        val stack = ArrayList<Int>()
        stack.add(pending.first())
        pending.remove(stack[0])
        while (stack.isNotEmpty()) {
            val current = stack.removeAt(stack.lastIndex)
            val row = current / 9
            val column = current % 9
            val neighbors = ArrayList<Int>(4)
            if (row > 0) neighbors.add(current - 9)
            if (row < 8) neighbors.add(current + 9)
            if (column > 0) neighbors.add(current - 1)
            if (column < 8) neighbors.add(current + 1)
            for (neighbor in neighbors) {
                if (pending.remove(neighbor)) stack.add(neighbor)
            }
        }
        return pending.isEmpty()
    }

    private companion object {
        const val SAMPLE_SIZE = 5
    }
}
