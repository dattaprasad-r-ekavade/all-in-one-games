package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.VersionedStateCodec

object ArrowsStateCodec {
    private const val MAGIC = 0x57415257 // "WARW"

    fun encode(state: ArrowsState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(ArrowsCodec.encodePuzzle(state.puzzle))
            output.writeUTF(ArrowsCodec.encodeLayout(state.arrows, state.puzzle.size))
            output.writeInt(state.slideCount)
            output.writeInt(state.history.size)
            state.history.forEach { previousArrows ->
                output.writeUTF(ArrowsCodec.encodeLayout(previousArrows, state.puzzle.size))
            }
        }

    fun decode(bytes: ByteArray): ArrowsState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = ArrowsCodec.decodeLine(input.readUTF())
            val arrowsLayout = input.readUTF()
            val arrows = ArrowsCodec.decodeLayout(arrowsLayout, puzzle.size)
            val slideCount = input.readInt()
            require(slideCount >= 0) { "Invalid slide count: $slideCount" }
            val historySize = input.readInt()
            require(historySize >= 0) { "Invalid history size: $historySize" }
            val history = ArrayList<List<ArrowsDirection?>>(historySize)
            repeat(historySize) {
                val previousLayout = input.readUTF()
                history.add(ArrowsCodec.decodeLayout(previousLayout, puzzle.size))
            }
            ArrowsState(puzzle, arrows, slideCount, history)
        }
}
