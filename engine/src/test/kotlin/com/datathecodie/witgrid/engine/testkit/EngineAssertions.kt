package com.datathecodie.witgrid.engine.testkit

import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/** Asserts that the engine finds exactly one solution when counting is capped at two. */
fun <P : Any, S : Any> assertUnique(engine: GameEngine<P, S>, puzzle: P) {
    assertEquals(1, engine.countSolutions(puzzle, limit = 2), "${engine.id} puzzle must be unique")
}

/** Asserts that deterministic hints solve the puzzle without requiring a guess. */
fun <P : Any, S : Any> assertTechniqueSolves(engine: GameEngine<P, S>, puzzle: P) {
    val state = solveWithHints(engine, puzzle)
    assertTrue(engine.isSolved(puzzle, state), "${engine.id} hint sequence must solve the puzzle")
}

/** Asserts that equal seeds and packs produce the same canonical puzzle line. */
fun <P : Any, S : Any> assertDeterministic(engine: GameEngine<P, S>, seed: Long, pack: Pack) {
    val first = engine.encodePuzzle(engine.generate(SplitMix64(seed), pack))
    val second = engine.encodePuzzle(engine.generate(SplitMix64(seed), pack))
    assertEquals(first, second, "${engine.id} generation must be deterministic for seed $seed and $pack")
}

/** Applies each next hint until the puzzle is solved, returning the resulting state. */
fun <P : Any, S : Any> solveWithHints(engine: GameEngine<P, S>, puzzle: P): S {
    var state = engine.initialState(puzzle)
    val visited = hashSetOf(state)

    while (!engine.isSolved(puzzle, state)) {
        val step = requireNotNull(engine.nextStep(puzzle, state)) {
            "${engine.id} is stuck before the puzzle is solved"
        }
        val nextState = step.moves.fold(state, engine::applyMove)
        assertTrue(nextState != state, "${engine.id} hint must change the state")
        assertTrue(visited.add(nextState), "${engine.id} hints must not repeat a previous state")
        state = nextState
    }
    return state
}
