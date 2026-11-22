package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.util.ArrayList

/** Save/restore codec for Path player states. */
object PathStateCodec {
    private const val MAGIC = 0x57504154 // "WPAT"

    fun encode(state: PathState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(PathCodec.encode(state.puzzle))
            output.writeInt(state.digits.size)
            state.digits.forEach { digit ->
                output.writeByte(digit ?: 0)
            }
        }

    fun decode(bytes: ByteArray): PathState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = PathCodec.decode(input.readUTF())
            val cellCount = input.readInt()
            require(cellCount == puzzle.grid.cellCount) { "Path save has the wrong cell count" }
            val digits = ArrayList<Int?>(cellCount)
            repeat(cellCount) {
                val digit = input.readUnsignedByte()
                require(digit <= puzzle.pairCount) {
                    "Path save has a digit outside 0..${puzzle.pairCount}: $digit"
                }
                digits += digit.takeIf { it != 0 }
            }
            PathState(puzzle, digits)
        }
}
