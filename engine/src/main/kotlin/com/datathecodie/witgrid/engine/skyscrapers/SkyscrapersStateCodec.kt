package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.util.ArrayList

/** Compact binary serializer for saved player state of a Skyscrapers puzzle. */
object SkyscrapersStateCodec {
    private const val MAGIC = 0x57534B59 // 'WSKY'

    /** Serializes the puzzle and per-cell digits and notes into a versioned byte array. */
    fun encode(state: SkyscrapersState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(SkyscrapersCodec.encode(state.puzzle))
            output.writeInt(state.digits.size)
            state.digits.indices.forEach { index ->
                output.writeByte(state.digits[index] ?: 0)
                output.writeByte(state.notes[index])
            }
        }

    /** Deserializes a versioned byte array back into [SkyscrapersState]. */
    fun decode(bytes: ByteArray): SkyscrapersState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = SkyscrapersCodec.decode(input.readUTF())
            val cellCount = input.readInt()
            require(cellCount == puzzle.grid.cellCount) {
                "Skyscrapers save has the wrong cell count"
            }
            val digits = ArrayList<Int?>(cellCount)
            val notes = ArrayList<Int>(cellCount)
            val maxNoteMask = (1 shl puzzle.size) - 1
            repeat(cellCount) {
                val digit = input.readUnsignedByte()
                val note = input.readUnsignedByte()
                require(digit <= puzzle.size) {
                    "Skyscrapers save has a digit outside 0 through ${puzzle.size}"
                }
                require(note and maxNoteMask == note) {
                    "Skyscrapers save has note bits above digit ${puzzle.size}"
                }
                digits += digit.takeIf { it != 0 }
                notes += note
            }
            SkyscrapersState(puzzle, digits, notes)
        }
}
