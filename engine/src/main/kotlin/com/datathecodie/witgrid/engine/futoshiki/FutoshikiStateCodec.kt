package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.util.ArrayList

object FutoshikiStateCodec {
    private const val MAGIC = 0x57465554 // "WFUT"

    fun encode(state: FutoshikiState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(FutoshikiCodec.encode(state.puzzle))
            output.writeInt(state.digits.size)
            state.digits.indices.forEach { index ->
                output.writeByte(state.digits[index] ?: 0)
                output.writeShort(state.notes[index])
            }
        }

    fun decode(bytes: ByteArray): FutoshikiState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = FutoshikiCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Futoshiki save has the wrong cell count" }
        val digits = ArrayList<Int?>(cellCount)
        val notes = ArrayList<Int>(cellCount)
        val maxNoteMask = (1 shl puzzle.size) - 1
        repeat(cellCount) {
            val digit = input.readUnsignedByte()
            val note = input.readUnsignedShort()
            require(digit <= puzzle.size) { "Futoshiki save has a digit outside 0 through ${puzzle.size}" }
            require(note and maxNoteMask == note) {
                "Futoshiki save has note bits above digit ${puzzle.size}"
            }
            digits += digit.takeIf { it != 0 }
            notes += note
        }
        FutoshikiState(puzzle, digits, notes)
    }
}
