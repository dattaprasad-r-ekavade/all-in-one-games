package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HintControllerTest {
    @Test
    fun mistakeHintTakesPriorityAndHasNoTechnique() = runBlocking {
        val mistakeCell = Cell(2)
        var nextStepCalls = 0
        var recordedMistakes = 0
        val controller = controller(
            mistakes = listOf(mistakeCell, mistakeCell),
            nextStep = { nextStepCalls += 1; sampleStep() },
            recordMistakeHintUse = { recordedMistakes += 1 },
        )

        val hint = controller.requestHint()

        assertEquals(HintRequest.Available(HintUi.Mistake(listOf(mistakeCell))), hint)
        assertEquals(0, nextStepCalls)
        assertEquals(1, recordedMistakes)
    }

    @Test
    fun logicalHintReturnsEvidenceAndCanBeApplied() = runBlocking {
        val step = sampleStep()
        var applied: HintStep? = null
        val controller = controller(
            nextStep = { step },
            applyStep = { applied = it; true },
        )

        val hint = controller.requestHint()

        assertEquals(HintRequest.Available(HintUi.Step(step)), hint)
        assertTrue(controller.apply((hint as HintRequest.Available).hint as HintUi.Step))
        assertEquals(step, applied)
    }

    @Test
    fun requestReturnsNullWhenNoStepRemains() = runBlocking {
        val controller = controller(nextStep = { null })

        assertEquals(HintRequest.NoStep, controller.requestHint())
    }

    @Test
    fun checkReportsMistakesOrClearBoard() = runBlocking {
        var mistakes = listOf(Cell(4))
        val controller = controller(mistakes = { mistakes })

        assertEquals(CheckUi.Mistakes(listOf(Cell(4))), controller.check())
        mistakes = emptyList()
        assertEquals(CheckUi.Clear, controller.check())
    }

    @Test
    fun deniedAllowanceDoesNotReadBoardOrApplyHints() = runBlocking {
        var readCount = 0
        val controller = controller(
            mistakes = { readCount += 1; emptyList() },
            allowance = DenyAll,
        )

        assertEquals(HintRequest.NotAllowed, controller.requestHint())
        assertEquals(CheckUi.NotAllowed, controller.check())
        assertEquals(0, readCount)
    }

    private fun controller(
        mistakes: List<Cell> = emptyList(),
        nextStep: () -> HintStep? = { null },
        applyStep: suspend (HintStep) -> Boolean = { false },
        recordMistakeHintUse: suspend () -> Unit = {},
        allowance: Allowance = AlwaysAllow,
    ) = controller(
        mistakes = { mistakes },
        nextStep = nextStep,
        applyStep = applyStep,
        recordMistakeHintUse = recordMistakeHintUse,
        allowance = allowance,
    )

    private fun controller(
        mistakes: () -> List<Cell>,
        nextStep: () -> HintStep? = { null },
        applyStep: suspend (HintStep) -> Boolean = { false },
        recordMistakeHintUse: suspend () -> Unit = {},
        allowance: Allowance = AlwaysAllow,
    ) = HintController(mistakes, nextStep, applyStep, recordMistakeHintUse, allowance)

    private fun sampleStep() = HintStep(
        technique = Technique("test", "Single cell", 0),
        cells = listOf(Cell(0)),
        explanation = "Only one value fits this cell.",
        moves = listOf(Move.SetMark(Cell(0), Mark.FILLED)),
    )

    private object DenyAll : Allowance {
        override suspend fun allowHint(): Boolean = false

        override suspend fun allowCheck(): Boolean = false
    }
}
