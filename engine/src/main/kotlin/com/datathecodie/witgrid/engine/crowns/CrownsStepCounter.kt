package com.datathecodie.witgrid.engine.crowns


/** Counts the deductions a full technique solve needs, grouped by technique ID. */
internal object CrownsStepCounter {
    /** Returns step counts by technique ID, or null when the techniques cannot finish the board. */
    fun countSteps(puzzle: CrownsPuzzle): Map<String, Int>? {
        var state = CrownsState.initial(puzzle)
        val counts = mutableMapOf<String, Int>()
        while (!CrownsRules.isSolved(puzzle, state)) {
            val step = CrownsTechniqueSolver.nextStep(puzzle, state) ?: return null
            val nextState = step.moves.fold(state, CrownsState::withMove)
            if (nextState == state) return null
            state = nextState
            counts.merge(step.technique.id, 1, Int::plus)
        }
        return counts
    }
}
