package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.HintStep

sealed interface HintUi {
    val cells: List<Cell>

    data class Mistake(override val cells: List<Cell>) : HintUi

    data class Step(val hintStep: HintStep) : HintUi {
        override val cells: List<Cell> = hintStep.cells
    }
}

sealed interface CheckUi {
    data class Mistakes(val cells: List<Cell>) : CheckUi

    data object Clear : CheckUi

    data object NotAllowed : CheckUi
}

sealed interface HintRequest {
    data class Available(val hint: HintUi) : HintRequest

    data object NoStep : HintRequest

    data object NotAllowed : HintRequest
}

interface Allowance {
    suspend fun allowHint(): Boolean

    suspend fun allowCheck(): Boolean
}

object AlwaysAllow : Allowance {
    override suspend fun allowHint(): Boolean = true

    override suspend fun allowCheck(): Boolean = true
}

class HintController(
    private val currentMistakes: () -> List<Cell>,
    private val nextStep: () -> HintStep?,
    private val applyStep: suspend (HintStep) -> Boolean,
    private val recordMistakeHintUse: suspend () -> Unit,
    private val allowance: Allowance = AlwaysAllow,
) {
    suspend fun requestHint(): HintRequest {
        if (!allowance.allowHint()) return HintRequest.NotAllowed

        val mistakes = currentMistakes().distinct()
        if (mistakes.isNotEmpty()) {
            recordMistakeHintUse()
            return HintRequest.Available(HintUi.Mistake(mistakes))
        }

        val step = nextStep() ?: return HintRequest.NoStep
        return HintRequest.Available(HintUi.Step(step))
    }

    suspend fun apply(step: HintUi.Step): Boolean = applyStep(step.hintStep)

    suspend fun check(): CheckUi {
        if (!allowance.allowCheck()) return CheckUi.NotAllowed

        val mistakes = currentMistakes().distinct()
        return if (mistakes.isEmpty()) CheckUi.Clear else CheckUi.Mistakes(mistakes)
    }
}
