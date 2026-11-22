package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.VersionedStateCodec

object ArrowsStateCodec {
    private const val MAGIC = 0x57415257 // "WARW"

    fun encode(state: ArrowsState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(ArrowsCodec.encodePuzzle(state.puzzle))
            output.writeUTF(ArrowsCodec.encodeLayout(state.arrows, state.puzzle.size))
            output.writeInt(state.mistakes)
            output.writeInt(state.slideCount)
            output.writeInt(state.history.size)
            state.history.forEach { previousArrows ->
                output.writeUTF(ArrowsCodec.encodeLayout(previousArrows, state.puzzle.size))
            }
        }

    fun decode(bytes: ByteArray): ArrowsState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzleLine = input.readUTF()
            val puzzle = ArrowsCodec.decodeLine(puzzleLine)
            val arrowsLayout = input.readUTF()
            val arrows = ArrowsCodec.decodeLayout(arrowsLayout, puzzle.size)
            val mistakes = input.readInt()
            require(mistakes in 0..ArrowsRules.MAX_MISTAKES) {
                "Invalid mistake count: $mistakes"
            }
            val slideCount = input.readInt()
            val maxPossibleSlides = puzzle.initialArrows.count { it != null }
            require(slideCount in 0..maxPossibleSlides) {
                "Slide count $slideCount out of bounds (max $maxPossibleSlides)"
            }
            val historySize = input.readInt()
            require(historySize in 0..maxPossibleSlides) {
                "History size $historySize out of bounds (max $maxPossibleSlides)"
            }
            require(historySize == slideCount) {
                "History size $historySize does not match slide count $slideCount"
            }
            val currentArrowsCount = arrows.count { it != null }
            require(currentArrowsCount == maxPossibleSlides - slideCount) {
                "Current arrow count $currentArrowsCount does not match slide count $slideCount"
            }
            val history = ArrayList<List<ArrowsDirection?>>(historySize)
            repeat(historySize) {
                val previousLayout = input.readUTF()
                history.add(ArrowsCodec.decodeLayout(previousLayout, puzzle.size))
            }

            // Validate history progression and transition to current arrows
            if (history.isNotEmpty()) {
                require(history[0] == puzzle.initialArrows) {
                    "History root does not match puzzle initial deal"
                }
                for (i in 0 until history.size - 1) {
                    require(ArrowsRules.isValidSlideTransition(puzzle.size, history[i], history[i + 1])) {
                        "Invalid slide transition in history at step $i"
                    }
                }
                require(ArrowsRules.isValidSlideTransition(puzzle.size, history.last(), arrows)) {
                    "Current board does not follow legally from last history step"
                }
            } else {
                require(arrows == puzzle.initialArrows) {
                    "Current board does not match initial deal with 0 slides"
                }
            }

            ArrowsState(
                puzzle = puzzle,
                arrows = arrows,
                mistakes = mistakes,
                slideCount = slideCount,
                history = history,
            )
        }
}
