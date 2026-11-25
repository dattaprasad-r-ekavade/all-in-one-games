package com.datathecodie.witgrid.games.coloursort

import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortRules
import com.datathecodie.witgrid.engine.coloursort.ColourSortState

internal enum class TapFeedback {
    NONE,
    SELECTED,
    DESELECTED,
    POURED,
}

internal data class TubeTapResult(
    val selectedTube: Int?,
    val action: ColourSortAction?,
    val feedback: TapFeedback,
)

/** Converts tube taps into selection state transitions and [ColourSortAction] pours. */
internal object ColourSortBoardInput {
    fun tapTube(state: ColourSortState, selectedTube: Int?, tappedTube: Int): TubeTapResult {
        require(tappedTube in state.tubes.indices) { "Tapped tube $tappedTube outside board" }
        if (ColourSortRules.isWon(state.tubes) ||
            ColourSortRules.isStuckAfterPlay(state.tubes, state.pourCount)
        ) {
            return TubeTapResult(selectedTube = null, action = null, feedback = TapFeedback.NONE)
        }

        val currentSelection = selectedTube?.takeIf { index ->
            index in state.tubes.indices && state.tubes[index].isNotEmpty()
        }

        if (currentSelection == null) {
            return if (state.tubes[tappedTube].isEmpty()) {
                TubeTapResult(selectedTube = null, action = null, feedback = TapFeedback.NONE)
            } else {
                TubeTapResult(selectedTube = tappedTube, action = null, feedback = TapFeedback.SELECTED)
            }
        }

        if (tappedTube == currentSelection) {
            return TubeTapResult(selectedTube = null, action = null, feedback = TapFeedback.DESELECTED)
        }

        if (ColourSortRules.isPourLegal(state.tubes, from = currentSelection, to = tappedTube)) {
            return TubeTapResult(
                selectedTube = null,
                action = ColourSortAction(from = currentSelection, to = tappedTube),
                feedback = TapFeedback.POURED,
            )
        }

        // Tapping another non-empty tube that cannot receive the pour switches selection to it.
        return if (state.tubes[tappedTube].isNotEmpty()) {
            TubeTapResult(selectedTube = tappedTube, action = null, feedback = TapFeedback.SELECTED)
        } else {
            TubeTapResult(selectedTube = null, action = null, feedback = TapFeedback.NONE)
        }
    }

    fun isLegalDestination(state: ColourSortState, selectedTube: Int?, candidateTube: Int): Boolean {
        if (selectedTube == null) return false
        if (selectedTube !in state.tubes.indices || candidateTube !in state.tubes.indices) return false
        if (ColourSortRules.isWon(state.tubes)) return false
        return ColourSortRules.isPourLegal(state.tubes, from = selectedTube, to = candidateTube)
    }

    fun isTubeSortedAndFull(tube: List<Int>): Boolean =
        tube.size == ColourSortRules.TUBE_CAPACITY && ColourSortRules.isTubeSorted(tube)

    fun colourLetter(colour: Int): Char {
        require(colour >= 1) { "Colour index must be positive, got $colour" }
        return ('A'.code + colour - 1).toChar()
    }
}
