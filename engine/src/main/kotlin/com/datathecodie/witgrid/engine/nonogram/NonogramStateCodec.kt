package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.VersionedStateCodec

object NonogramStateCodec {
    private const val MAGIC = 0x574E4F4E

    fun encode(state: NonogramState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(NonogramCodec.encode(state.puzzle))
            output.writeInt(state.marks.size)
            state.marks.forEach { output.writeByte(it.ordinal) }
        }

    fun decode(bytes: ByteArray): NonogramState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = NonogramCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Nonogram save has the wrong cell count" }
        val marks = List(cellCount) {
            Mark.entries.getOrNull(input.readUnsignedByte())
                ?: throw IllegalArgumentException("Nonogram save has an unknown mark")
        }
        NonogramState(puzzle, marks)
    }
}
