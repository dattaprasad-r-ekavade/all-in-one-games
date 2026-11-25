package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class KillerSudokuTechniqueSolverTest {
    @Test
    fun solvesZeroGivenPuzzleAndGradesEasyPack() {
        val zeroGiven = zeroGivenCageCombinationPuzzle()
        assertEquals(1, KillerSudokuRules.countSolutions(zeroGiven, limit = 2))
        val zeroSolved = solveWithHints(KillerSudokuEngine, zeroGiven)
        assertTrue(KillerSudokuRules.isSolved(zeroGiven, zeroSolved))

        val zeroGrade = KillerSudokuTechniqueSolver.grade(zeroGiven)
        assertEquals(KillerSudokuTechniques.CAGE_COMBINATIONS, zeroGrade.hardestTechnique)
        assertEquals(81, zeroGrade.stepCount)
        assertTrue(KillerSudokuPackBands.isGradeInPackBand(Pack.EASY, zeroGrade))
    }

    @Test
    fun solvesUniqueFixturesForEveryPublishedTechniqueAndGradesTheirPack() {
        val fixtures = listOf(
            TechniqueFixture(Pack.EASY, KillerSudokuTechniques.NAKED_SINGLE, nakedSinglePuzzle()),
            TechniqueFixture(Pack.EASY, KillerSudokuTechniques.HIDDEN_SINGLE, hiddenSinglePuzzle()),
            TechniqueFixture(Pack.EASY, KillerSudokuTechniques.CAGE_COMBINATIONS, zeroGivenCageCombinationPuzzle()),
            TechniqueFixture(Pack.MEDIUM, KillerSudokuTechniques.RULE_45, rule45InniePuzzle()),
            TechniqueFixture(Pack.MEDIUM, KillerSudokuTechniques.RULE_45, rule45OutiePuzzle()),
            TechniqueFixture(Pack.HARD, KillerSudokuTechniques.LOCKED_CANDIDATES, lockedCandidatesPuzzle()),
            TechniqueFixture(Pack.HARD, KillerSudokuTechniques.NAKED_PAIR, nakedPairPuzzle()),
            TechniqueFixture(Pack.EXPERT, KillerSudokuTechniques.INNIE_OUTIE, multiHouseInnieOutiePuzzle()),
            TechniqueFixture(Pack.EXPERT, KillerSudokuTechniques.INNIE_OUTIE, virtualCageInnieOutiePuzzle()),
            TechniqueFixture(Pack.EXPERT, KillerSudokuTechniques.INNIE_OUTIE, differenceInnieOutiePuzzle()),
            TechniqueFixture(Pack.EXPERT, KillerSudokuTechniques.HIDDEN_PAIR, hiddenPairPuzzle()),
            TechniqueFixture(Pack.EXPERT, KillerSudokuTechniques.NAKED_TRIPLE, nakedTriplePuzzle()),
            TechniqueFixture(Pack.EXPERT, KillerSudokuTechniques.X_WING, xWingPuzzle()),
        )

        assertAll(
            fixtures.map { fixture ->
                Executable {
                    val puzzle = fixture.puzzle
                    assertEquals(1, KillerSudokuRules.countSolutions(puzzle), "${fixture.id} must be unique")

                    val solvedByKit = solveWithHints(KillerSudokuEngine, puzzle)
                    assertTrue(KillerSudokuEngine.isSolved(puzzle, solvedByKit), "${fixture.id} must solve with testkit")

                    val (solved, steps) = collectHintSolve(puzzle)
                    assertTrue(KillerSudokuRules.isSolved(puzzle, solved), "${fixture.id} must solve")
                    assertTrue(steps.any { it.technique == fixture.technique }) {
                        "${fixture.id} must use ${fixture.technique.displayName}, used ${steps.map { it.technique.id }.toSet()}"
                    }
                    steps.forEach { step ->
                        assertOneSentence(step)
                        assertThrows(UnsupportedOperationException::class.java) {
                            (step.cells as MutableList<Cell>).add(Cell(0))
                        }
                        assertThrows(UnsupportedOperationException::class.java) {
                            (step.moves as MutableList<Move>).add(Move.SetDigit(Cell(0), 1))
                        }
                    }
                    val techniqueStep = steps.first { it.technique == fixture.technique }
                    val placement = techniqueStep.moves.single() as Move.SetDigit
                    assertEquals(puzzle.solution[placement.cell.index], placement.digit)
                    assertTrue(techniqueStep.cells.contains(placement.cell))
                    if (fixture.technique.difficultyRank >= KillerSudokuTechniques.LOCKED_CANDIDATES.difficultyRank) {
                        assertTrue(techniqueStep.cells.size >= 3, "Advanced hints must highlight their source cells")
                    }

                    val expected = Grade(fixture.technique, steps.size, fixture.pack)
                    val grade = KillerSudokuTechniqueSolver.grade(puzzle)
                    assertEquals(expected, grade, "Hardest technique for ${fixture.id} must match")
                    assertTrue(KillerSudokuPackBands.isGradeInPackBand(fixture.pack, grade))
                    val storedGrade = KillerSudokuGradeCodec.encode(grade)
                    KillerSudokuGradeCodec.validate(storedGrade)
                }
            },
        )
    }

    @Test
    fun rejectsUniquePuzzleThatIsStuckWithSupportedTechniques() {
        val stuck = uniqueStuckPuzzle()

        assertEquals(1, KillerSudokuRules.countSolutions(stuck, limit = 2))
        assertNull(KillerSudokuTechniqueSolver.nextStep(stuck, KillerSudokuState.initial(stuck)))
        val error = assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuTechniqueSolver.grade(stuck)
        }
        assertTrue(error.message.orEmpty().contains("stuck", ignoreCase = true))
    }

    @Test
    fun gradeRequiresSingleValidSolutionMatchingGivensAndPackBand() {
        val multiple = KillerSudokuPuzzle(
            id = "KS-MULTI",
            givens = List(CELL_COUNT) { null },
            cages = rowCages(solution),
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        val zero = KillerSudokuPuzzle(
            id = "KS-ZERO",
            givens = List(CELL_COUNT) { index -> if (index == 0 || index == 10) 1 else null },
            cages = rowCages(solution),
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        val easyInMediumBand = nakedSinglePuzzle(Pack.MEDIUM)
        val mismatchedGiven = nakedSinglePuzzle(Pack.EASY).let { source ->
            val givens = source.givens.toMutableList()
            givens[0] = if (solution[0] == 9) 1 else solution[0] + 1
            KillerSudokuPuzzle(source.id, givens, source.cages, source.solution, source.pack, source.storedGrade)
        }
        val invalidSolution = nakedSinglePuzzle(Pack.EASY).let { source ->
            val badSolution = source.solution.toMutableList()
            badSolution[0] = badSolution[1]
            KillerSudokuPuzzle(source.id, source.givens, source.cages, badSolution, source.pack, source.storedGrade)
        }

        assertEquals(2, KillerSudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(0, KillerSudokuRules.countSolutions(zero, limit = 2))
        assertNull(KillerSudokuTechniqueSolver.nextStep(zero, KillerSudokuState.initial(zero)))
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuTechniqueSolver.grade(multiple) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuTechniqueSolver.grade(zero) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuTechniqueSolver.grade(easyInMediumBand) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuTechniqueSolver.grade(mismatchedGiven) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuTechniqueSolver.grade(invalidSolution) }

        assertFalse(
            KillerSudokuPackBands.isGradeInPackBand(
                Pack.MEDIUM,
                Grade(KillerSudokuTechniques.CAGE_COMBINATIONS, 20, Pack.MEDIUM),
            ),
        )
        assertFalse(
            KillerSudokuPackBands.isGradeInPackBand(
                Pack.HARD,
                Grade(KillerSudokuTechniques.RULE_45, 20, Pack.HARD),
            ),
        )
        assertFalse(
            KillerSudokuPackBands.isGradeInPackBand(
                Pack.EXPERT,
                Grade(KillerSudokuTechniques.NAKED_PAIR, 20, Pack.EXPERT),
            ),
        )
        assertFalse(
            KillerSudokuPackBands.isGradeInPackBand(
                Pack.EASY,
                Grade(
                    KillerSudokuTechniques.CAGE_COMBINATIONS,
                    KillerSudokuPackBands.MINIMUM_HINT_STEP_COUNT - 1,
                    Pack.EASY,
                ),
            ),
        )
        assertFalse(
            KillerSudokuPackBands.isGradeInPackBand(Pack.EASY, Grade(null, 0, Pack.EASY)),
        )
    }

    @Test
    fun nextStepHandlesMistakesNotesSolvedAndDeterminismAndEngineDelegates() {
        val puzzle = zeroGivenCageCombinationPuzzle()
        val initial = KillerSudokuEngine.initialState(puzzle)

        val step1 = KillerSudokuEngine.nextStep(puzzle, initial)
        val step2 = KillerSudokuTechniqueSolver.nextStep(puzzle, initial)
        assertEquals(step2, step1)
        assertEquals(KillerSudokuTechniqueSolver.grade(puzzle), KillerSudokuEngine.grade(puzzle))

        val noted = initial.withMove(Move.SetNotes(Cell(0), (1 shl 0) or (1 shl 1)))
        assertEquals(step2, KillerSudokuTechniqueSolver.nextStep(puzzle, noted))

        val mistaken = initial.withMove(
            Move.SetDigit(Cell(0), if (puzzle.solution[0] == 9) 1 else puzzle.solution[0] + 1),
        )
        assertNull(KillerSudokuTechniqueSolver.nextStep(puzzle, mistaken))

        val solved = solveWithHints(KillerSudokuEngine, puzzle)
        assertNull(KillerSudokuTechniqueSolver.nextStep(puzzle, solved))

        val otherPuzzle = nakedSinglePuzzle()
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuTechniqueSolver.nextStep(otherPuzzle, initial)
        }
    }

    @Test
    fun cageCombinationsCoversLockedCageDigitsWithinHouse() {
        val puzzle = lockedCageDigitsPuzzle()
        assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2))
        val (solved, steps) = collectHintSolve(puzzle)
        assertTrue(KillerSudokuRules.isSolved(puzzle, solved))
        assertTrue(
            steps.any {
                it.technique == KillerSudokuTechniques.CAGE_COMBINATIONS &&
                    it.explanation.contains("must appear in cage", ignoreCase = true)
            },
            "Expected locked cage digit step, got: ${steps.map { "${it.technique.id}:${it.explanation}" }}",
        )
    }

    @Test
    fun gradeCodecRejectsUnknownTechniquesAndMalformedCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuGradeCodec.encode(Grade(Technique("outside", "Outside", 1), 1, Pack.MEDIUM))
        }
        listOf(
            "outside:1",
            "none:1",
            "cage-combinations:0",
            "cage-combinations:-1",
            "rule-45:many",
            "rule-45:1:extra",
        ).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { KillerSudokuGradeCodec.validate(value) }
        }
        KillerSudokuGradeCodec.validate("none:0")
        KillerSudokuGradeCodec.validate("cage-combinations:81")
        KillerSudokuGradeCodec.validate("rule-45:40")
        KillerSudokuGradeCodec.validate("innie-outie:60")
        KillerSudokuGradeCodec.validate("x-wing:25")
    }

    private fun collectHintSolve(puzzle: KillerSudokuPuzzle): Pair<KillerSudokuState, List<HintStep>> {
        var state = KillerSudokuState.initial(puzzle)
        val steps = mutableListOf<HintStep>()
        while (!KillerSudokuRules.isSolved(puzzle, state)) {
            val step = KillerSudokuTechniqueSolver.nextStep(puzzle, state)
                ?: throw AssertionError("${puzzle.id} (${puzzle.pack}) stuck after ${steps.size} steps")
            assertOneSentence(step)
            steps += step
            state = step.moves.fold(state, KillerSudokuState::withMove)
            assertTrue(steps.size <= CELL_COUNT)
        }
        return state to steps
    }

    private fun assertOneSentence(step: HintStep) {
        assertTrue(step.explanation.isNotBlank(), "Explanation must not be blank")
        assertTrue(step.explanation.endsWith('.'), "Explanation must end with a period: ${step.explanation}")
        assertEquals(1, step.explanation.count { it == '.' }, "Explanation must have one sentence: ${step.explanation}")
    }

    private fun nakedSinglePuzzle(pack: Pack = Pack.EASY): KillerSudokuPuzzle =
        sudokuWithDistinctCages("KS-NAKED-SINGLE", nakedSingleClues, solution, pack)

    private fun hiddenSinglePuzzle(): KillerSudokuPuzzle =
        sudokuWithDistinctCages("KS-HIDDEN-SINGLE", hiddenSingleClues, solution, Pack.EASY)

    private fun zeroGivenCageCombinationPuzzle(): KillerSudokuPuzzle {
        // Staggered 1-cell and 2-cell cages; breaking the col 7/8 cycle at row 7 (70, 71 as 1-cell cages).
        val groups = listOf(
            listOf(0), listOf(1, 10), listOf(2), listOf(3, 4), listOf(5), listOf(6, 15), listOf(7), listOf(8),
            listOf(9), listOf(11, 12), listOf(13), listOf(14, 23), listOf(16, 17),
            listOf(18, 19), listOf(20), listOf(21, 22), listOf(24), listOf(25, 26),
            listOf(27), listOf(28, 37), listOf(29), listOf(30, 31), listOf(32), listOf(33, 34), listOf(35),
            listOf(36), listOf(38, 39), listOf(40), listOf(41, 42), listOf(43, 44),
            listOf(45, 46), listOf(47), listOf(48), listOf(49, 50), listOf(51), listOf(52, 53),
            listOf(54), listOf(55, 56), listOf(57, 66), listOf(58), listOf(59, 60), listOf(61), listOf(62),
            listOf(63, 64), listOf(65), listOf(67, 68), listOf(69), listOf(70), listOf(71),
            listOf(72), listOf(73, 74), listOf(75), listOf(76, 77), listOf(78), listOf(79, 80),
        )
        return KillerSudokuPuzzle(
            id = "KS-ZERO-CAGE-COMBO",
            givens = List(CELL_COUNT) { null },
            cages = cagesFromGroups(groups, solution),
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "pending",
        )
    }

    private fun lockedCageDigitsPuzzle(): KillerSudokuPuzzle {
        val emptyCells = (36..80).toSet()
        val givens = solution.mapIndexed { index, digit -> if (index in emptyCells) null else digit }
        val custom = listOf(
            listOf(54, 55, 56), // 5+2+1=8 -> locks {1,2,5} in row 6 and box 6
            listOf(58, 67), // 7+2=9
            listOf(36, 37),
            listOf(38, 39, 40),
            listOf(41, 42),
            listOf(43, 44, 53, 62),
            listOf(45, 46, 47, 48),
            listOf(49, 50),
            listOf(51, 60),
            listOf(52, 61, 70, 79),
            listOf(57, 66),
            listOf(59, 68, 69),
            listOf(63, 64, 72),
            listOf(65, 73, 74),
            listOf(71, 80),
            listOf(75, 76),
            listOf(77, 78),
        )
        return KillerSudokuPuzzle(
            id = "KS-LOCKED-CAGE-DIGITS",
            givens = givens,
            cages = fillRemainingWithSingletons(custom, emptyCells, solution),
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "pending",
        )
    }

    private fun rule45InniePuzzle(): KillerSudokuPuzzle {
        val emptyCells = (0..53).toSet()
        val givens = rulesSolution.mapIndexed { index, digit -> if (index in emptyCells) null else digit }
        val custom = listOf(
            listOf(0, 1, 2, 3), // 5+3+4+6=18 (wholly in row 0)
            listOf(4, 5, 6, 7), // 7+8+9+1=25 (wholly in row 0)
            listOf(8, 17, 26), // 2+8+7=17 -> single innie 8 in row 0 = 45 - (18+25) = 2
            listOf(9, 10),
            listOf(11, 12, 13),
            listOf(14, 23),
            listOf(15, 16, 24, 25),
            listOf(18, 19, 27, 36),
            listOf(20, 21, 22, 31),
            listOf(28, 29, 30, 37),
            listOf(32, 33, 42),
            listOf(34, 35, 43, 44),
            listOf(38, 47, 48),
            listOf(39, 40, 49),
            listOf(41, 50, 51),
            listOf(45, 46),
            listOf(52, 53),
        )
        return KillerSudokuPuzzle(
            id = "KS-RULE45-INNIE",
            givens = givens,
            cages = fillRemainingWithSingletons(custom, emptyCells, rulesSolution),
            solution = rulesSolution,
            pack = Pack.MEDIUM,
            storedGrade = "pending",
        )
    }

    private fun rule45OutiePuzzle(): KillerSudokuPuzzle {
        val emptyCells = (0..53).toSet()
        val givens = rulesSolution.mapIndexed { index, digit -> if (index in emptyCells) null else digit }
        val custom = listOf(
            listOf(0, 1, 2), // 5+3+4=12 (wholly in row 0)
            listOf(3, 4, 5), // 6+7+8=21 (wholly in row 0)
            listOf(6, 7, 8, 17), // 9+1+2+8=20 -> single outie 17 of row 0 = (12+21+20) - 45 = 8
            listOf(9, 18, 27),
            listOf(10, 11, 19),
            listOf(12, 13, 14),
            listOf(15, 24, 33),
            listOf(16, 25, 26),
            listOf(20, 21, 22),
            listOf(23, 31, 32, 41),
            listOf(28, 29, 37, 38),
            listOf(30, 39, 40),
            listOf(34, 35, 43),
            listOf(36, 45, 46),
            listOf(42, 49, 50, 51),
            listOf(44, 52, 53),
            listOf(47, 48),
        )
        return KillerSudokuPuzzle(
            id = "KS-RULE45-OUTIE",
            givens = givens,
            cages = fillRemainingWithSingletons(custom, emptyCells, rulesSolution),
            solution = rulesSolution,
            pack = Pack.MEDIUM,
            storedGrade = "pending",
        )
    }

    private fun multiHouseInnieOutiePuzzle(): KillerSudokuPuzzle {
        val emptyCells = (0..53).toSet()
        val givens = rulesSolution.mapIndexed { index, digit -> if (index in emptyCells) null else digit }
        val custom = listOf(
            listOf(0, 9, 10), // 5+6+7=18 (wholly in rows 0..1)
            listOf(1, 2, 3), // 3+4+6=13
            listOf(11, 12, 13), // 2+1+9=12
            listOf(4, 5, 6), // 7+8+9=24
            listOf(14, 15, 16), // 5+3+4=12
            listOf(7, 8, 17, 26), // 1+2+8+7=18 -> outie 26 of rows 1 and 2 = (18+13+12+24+12+18) - 90 = 7
            listOf(18, 27, 28),
            listOf(19, 20, 21, 22),
            listOf(23, 24, 32),
            listOf(25, 34, 35),
            listOf(29, 30, 31),
            listOf(33, 42, 43),
            listOf(36, 37, 38, 39),
            listOf(40, 41, 50),
            listOf(44, 51, 52, 53),
            listOf(45, 46),
            listOf(47, 48, 49),
        )
        return KillerSudokuPuzzle(
            id = "KS-INNIE-OUTIE-MULTIHOUSE",
            givens = givens,
            cages = fillRemainingWithSingletons(custom, emptyCells, rulesSolution),
            solution = rulesSolution,
            pack = Pack.EXPERT,
            storedGrade = "pending",
        )
    }

    private fun virtualCageInnieOutiePuzzle(): KillerSudokuPuzzle {
        val emptyCells = (27..80).toSet() - 54
        val givens = solution.mapIndexed { index, digit -> if (index in emptyCells) null else digit }
        val custom = listOf(
            listOf(57, 58, 59), // 9+7+4=20 (wholly in row 6)
            listOf(60, 61, 62), // 3+6+8=17 (wholly in row 6)
            listOf(55, 64, 73), // 2+3+9=14 (innie 55)
            listOf(56, 65, 74), // 1+8+6=15 (innie 56) -> virtual cage {55,56}=3
            listOf(27, 36, 37),
            listOf(28, 29, 38),
            listOf(30, 39, 40, 48),
            listOf(31, 32, 33),
            listOf(34, 35, 44),
            listOf(41, 42, 49, 50),
            listOf(43, 51, 52, 53),
            listOf(45, 46, 47),
            listOf(63, 72),
            listOf(66, 75, 76),
            listOf(67, 68, 69),
            listOf(70, 71, 80),
            listOf(77, 78, 79),
        )
        return KillerSudokuPuzzle(
            id = "KS-INNIE-OUTIE-VIRTUAL",
            givens = givens,
            cages = fillRemainingWithSingletons(custom, emptyCells, solution),
            solution = solution,
            pack = Pack.EXPERT,
            storedGrade = "pending",
        )
    }

    private fun differenceInnieOutiePuzzle(): KillerSudokuPuzzle {
        val groups = listOf(
            listOf(30, 31, 39), // 2+3+8=13 (wholly inside box 4)
            listOf(40, 48, 49), // 4+1+6=11 (wholly inside box 4)
            listOf(23, 32, 41), // 1+7+5=13 (outie 23 of box 4)
            listOf(50, 51, 52), // 9+5+3=17 (innie 50 of box 4) -> r6c6 - r3c6 = 8
            listOf(0, 1),
            listOf(2, 3, 4),
            listOf(5, 6, 7, 16),
            listOf(8, 17, 26),
            listOf(9, 10),
            listOf(11, 12),
            listOf(13, 21, 22),
            listOf(14, 15),
            listOf(18, 27),
            listOf(19, 28, 37),
            listOf(20, 29, 38),
            listOf(24, 25),
            listOf(33, 34, 35),
            listOf(36, 45),
            listOf(42, 43, 44),
            listOf(46, 47),
            listOf(53, 61, 62, 70),
            listOf(54, 55, 63, 72),
            listOf(56, 57, 65),
            listOf(58, 67, 68),
            listOf(59, 60),
            listOf(64, 73),
            listOf(66, 74, 75, 76),
            listOf(69, 77, 78),
            listOf(71, 79, 80),
        )
        return KillerSudokuPuzzle(
            id = "KS-INNIE-OUTIE-DIFF",
            givens = List(CELL_COUNT) { null },
            cages = cagesFromGroups(groups, solution),
            solution = solution,
            pack = Pack.EXPERT,
            storedGrade = "pending",
        )
    }

    private fun lockedCandidatesPuzzle(): KillerSudokuPuzzle =
        sudokuWithHouseCages("KS-LOCKED-CANDIDATES", lockedCandidatesClues, solution, Pack.HARD, byBox = false)

    private fun nakedPairPuzzle(): KillerSudokuPuzzle =
        sudokuWithHouseCages("KS-NAKED-PAIR", nakedPairClues, solution, Pack.HARD, byBox = true)

    private fun hiddenPairPuzzle(): KillerSudokuPuzzle =
        sudokuWithHouseCages("KS-HIDDEN-PAIR", hiddenPairClues, solution, Pack.EXPERT, byBox = false)

    private fun nakedTriplePuzzle(): KillerSudokuPuzzle =
        sudokuWithHouseCages("KS-NAKED-TRIPLE", nakedTripleClues, solution, Pack.EXPERT, byBox = true)

    private fun xWingPuzzle(): KillerSudokuPuzzle =
        sudokuWithHouseCages("KS-X-WING", xWingClues, solution, Pack.EXPERT, byBox = true)

    private fun uniqueStuckPuzzle(): KillerSudokuPuzzle =
        sudokuWithHouseCages("KS-STUCK", stuckClues, solution, Pack.EASY, byBox = true)

    /** Groups empty cells within each 3x3 box or column so cages never add cross-house restrictions. */
    private fun sudokuWithHouseCages(
        id: String,
        clueIndices: List<Int>,
        gridSolution: List<Int>,
        pack: Pack,
        byBox: Boolean,
    ): KillerSudokuPuzzle {
        val clues = clueIndices.toSet()
        val givens = gridSolution.mapIndexed { index, digit -> if (index in clues) digit else null }
        val houseCells = if (byBox) KillerSudokuCageMath.BOX_CELLS else KillerSudokuCageMath.COLUMN_CELLS
        val custom = houseCells.map { unit -> unit.filter { givens[it] == null } }.filter { it.isNotEmpty() }
        val emptyCells = givens.indices.filter { givens[it] == null }.toSet()
        return KillerSudokuPuzzle(
            id = id,
            givens = givens,
            cages = fillRemainingWithSingletons(custom, emptyCells, gridSolution),
            solution = gridSolution,
            pack = pack,
            storedGrade = "pending",
        )
    }

    private fun sudokuWithDistinctCages(
        id: String,
        clueIndices: List<Int>,
        gridSolution: List<Int>,
        pack: Pack,
        avoidSingletons: Boolean = false,
    ): KillerSudokuPuzzle {
        val clues = clueIndices.toSet()
        val givens = gridSolution.mapIndexed { index, digit -> if (index in clues) digit else null }
        val cages = ArrayList<KillerCage>()
        givens.indices.forEach { index ->
            if (givens[index] != null) {
                cages += KillerCage(listOf(Cell(index)), gridSolution[index])
            }
        }
        val remaining = givens.indices.filter { givens[it] == null }.toMutableList()
        val groups = ArrayList<MutableList<Int>>()
        while (remaining.isNotEmpty()) {
            val group = ArrayList<Int>()
            var usedMask = 0
            val iterator = remaining.iterator()
            while (iterator.hasNext() && group.size < 9) {
                val cell = iterator.next()
                val bit = 1 shl (gridSolution[cell] - 1)
                if (usedMask and bit == 0) {
                    usedMask = usedMask or bit
                    group += cell
                    iterator.remove()
                }
            }
            groups += group
        }
        if (avoidSingletons) {
            // Merge any 1-cell empty group into a prior group lacking that digit.
            val singletons = groups.filter { it.size == 1 }
            groups.removeAll(singletons)
            for (singleton in singletons) {
                val cell = singleton.single()
                val digit = gridSolution[cell]
                val target = groups.firstOrNull { grp ->
                    grp.size < 9 && grp.none { gridSolution[it] == digit }
                }
                if (target != null) {
                    target += cell
                } else {
                    groups += singleton
                }
            }
        }
        groups.forEach { group ->
            cages += KillerCage(group.map(::Cell), group.sumOf { gridSolution[it] })
        }
        return KillerSudokuPuzzle(id, givens, cages, gridSolution, pack, "pending")
    }

    private fun fillRemainingWithSingletons(
        customGroups: List<List<Int>>,
        emptyCells: Set<Int>,
        gridSolution: List<Int>,
    ): List<KillerCage> {
        val covered = customGroups.flatten().toSet()
        require(covered == emptyCells) {
            "Custom groups must cover exact empty set; diff=${emptyCells - covered} extra=${covered - emptyCells}"
        }
        val cages = ArrayList<KillerCage>()
        customGroups.forEach { group ->
            cages += KillerCage(group.map(::Cell), group.sumOf { gridSolution[it] })
        }
        for (index in 0 until CELL_COUNT) {
            if (index !in covered) {
                cages += KillerCage(listOf(Cell(index)), gridSolution[index])
            }
        }
        return cages
    }

    private fun cagesFromGroups(groups: List<List<Int>>, gridSolution: List<Int>): List<KillerCage> =
        groups.map { indices -> KillerCage(indices.map(::Cell), indices.sumOf { gridSolution[it] }) }

    private fun rowCages(gridSolution: List<Int>): List<KillerCage> =
        (0 until BOARD_SIZE).map { row ->
            val indices = (0 until BOARD_SIZE).map { col -> row * BOARD_SIZE + col }
            KillerCage(indices.map(::Cell), indices.sumOf { gridSolution[it] })
        }

    private data class TechniqueFixture(
        val pack: Pack,
        val technique: Technique,
        val puzzle: KillerSudokuPuzzle,
    ) {
        val id: String get() = puzzle.id
    }

    private companion object {
        const val BOARD_SIZE = 9
        const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE

        val solution = listOf(
            "812753649",
            "943682175",
            "675491283",
            "154237896",
            "369845721",
            "287169534",
            "521974368",
            "438526917",
            "796318452",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val rulesSolution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val cyclicGrid = listOf(
            "123456789",
            "456789123",
            "789123456",
            "231564897",
            "564897231",
            "897231564",
            "312645978",
            "645978312",
            "978312645",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val nakedSingleClues = listOf(
            0, 2, 11, 12, 15, 18, 19, 21, 22, 24, 28, 30, 32, 38, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78, 80,
        )
        val hiddenSingleClues = listOf(
            0, 11, 12, 18, 19, 21, 22, 24, 28, 30, 32, 38, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78, 80,
        )
        val lockedCandidatesClues = listOf(
            0, 11, 12, 18, 19, 21, 22, 24, 28, 32, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78,
        )
        val nakedPairClues = listOf(
            0, 11, 12, 15, 19, 22, 24, 28, 29, 31, 32, 40, 41, 42, 48, 52, 56, 60, 61, 62, 65, 66, 70, 71, 73, 78,
        )
        val hiddenPairClues = listOf(
            0, 8, 11, 12, 19, 22, 24, 28, 32, 40, 41, 42, 47, 48, 52, 56, 61, 62, 63, 65, 66, 68, 70, 73, 78,
        )
        val nakedTripleClues = listOf(
            0, 11, 12, 19, 22, 24, 28, 32, 34, 37, 40, 41, 42, 46, 48, 52, 56, 61, 62, 64, 65, 66, 70, 72, 73, 78,
        )
        val xWingClues = listOf(
            0, 1, 11, 12, 13, 19, 22, 24, 28, 32, 40, 41, 42, 44, 48, 52, 56, 61, 62, 64, 65, 66, 70, 73, 78,
        )
        val stuckClues = listOf(
            0, 11, 12, 19, 22, 24, 28, 32, 40, 41, 42, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78,
        )
    }
}
