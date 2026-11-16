package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.io.DataInputStream
import java.io.DataOutputStream

object CrownsStateCodec {
    private const val MAGIC = 0x5743524E

    fun encode(state: CrownsState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(CrownsCodec.encode(state.puzzle))
            output.writeInt(state.marks.size)
            state.marks.forEach { output.writeByte(it.ordinal) }
        }

    fun decode(bytes: ByteArray): CrownsState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = CrownsCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Crowns save has the wrong cell count" }
        val marks = List(cellCount) {
            Mark.entries.getOrNull(input.readUnsignedByte())
                ?: throw IllegalArgumentException("Crowns save has an unknown mark")
        }
        CrownsState(puzzle, marks)
    }
}
