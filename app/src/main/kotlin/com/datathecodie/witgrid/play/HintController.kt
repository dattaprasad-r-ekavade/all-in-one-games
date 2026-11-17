package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.HintStep
import java.security.MessageDigest

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
    suspend fun allowHint(hintKey: String): HintAllowanceDecision

    suspend fun allowCheck(): Boolean
}

sealed interface HintAllowanceDecision {
    data object Allowed : HintAllowanceDecision

    data object AlreadyCharged : HintAllowanceDecision

    data object Denied : HintAllowanceDecision
}

object AlwaysAllow : Allowance {
    override suspend fun allowHint(hintKey: String): HintAllowanceDecision = HintAllowanceDecision.Allowed

    override suspend fun allowCheck(): Boolean = true
}

class HintController(
    private val currentMistakes: () -> List<Cell>,
    private val nextStep: () -> HintStep?,
    private val applyStep: suspend (HintStep) -> Boolean,
    private val recordHintUse: suspend (String) -> Unit,
    private val allowance: Allowance = AlwaysAllow,
    private val boardKey: () -> String = { "" },
) {
    suspend fun requestHint(): HintRequest {
        val mistakes = currentMistakes().distinct()
        val hint = if (mistakes.isNotEmpty()) {
            HintUi.Mistake(mistakes)
        } else {
            nextStep()?.let(HintUi::Step) ?: return HintRequest.NoStep
        }

        val hintKey = stableHintKey(boardKey(), hint)
        return when (allowance.allowHint(hintKey)) {
            HintAllowanceDecision.Denied -> HintRequest.NotAllowed
            HintAllowanceDecision.AlreadyCharged -> HintRequest.Available(hint)
            HintAllowanceDecision.Allowed -> {
                recordHintUse(hintKey)
                HintRequest.Available(hint)
            }
        }
    }

    suspend fun apply(step: HintUi.Step): Boolean = applyStep(step.hintStep)

    suspend fun check(): CheckUi {
        if (!allowance.allowCheck()) return CheckUi.NotAllowed

        val mistakes = currentMistakes().distinct()
        return if (mistakes.isEmpty()) CheckUi.Clear else CheckUi.Mistakes(mistakes)
    }

    private fun stableHintKey(boardFingerprint: String, hint: HintUi): String {
        val advice = when (hint) {
            is HintUi.Mistake -> "mistake:${hint.cells.sortedBy(Cell::index).joinToString { it.index.toString() }}"
            is HintUi.Step -> buildString {
                append("step:")
                append(hint.hintStep.technique.id)
                append(':')
                append(hint.cells.sortedBy(Cell::index).joinToString { it.index.toString() })
                append(':')
                append(hint.hintStep.moves.joinToString())
            }
        }
        val fingerprint = "$boardFingerprint|$advice".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(fingerprint).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }
}
