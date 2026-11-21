package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.VersionedStateCodec

/** Binary serializer for Mosaic saved player state. */
object MosaicStateCodec {
    private const val MAGIC = 0x574D4F53

    fun encode(state: MosaicState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(MosaicCodec.encode(state.puzzle))
            output.writeInt(state.marks.size)
            state.marks.forEach { output.writeByte(it.ordinal) }
        }

    fun decode(bytes: ByteArray): MosaicState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = MosaicCodec.decode(input.readUTF())
            val cellCount = input.readInt()
            require(cellCount == puzzle.grid.cellCount) { "Mosaic save has the wrong cell count" }
            val marks = List(cellCount) {
                Mark.entries.getOrNull(input.readUnsignedByte())
                    ?: throw IllegalArgumentException("Mosaic save has an unknown mark")
            }
            MosaicState(puzzle, marks)
        }
}
