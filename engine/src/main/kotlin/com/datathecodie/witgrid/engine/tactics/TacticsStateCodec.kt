package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.util.ArrayList

/**
 * Binary state encoder and decoder for Tactics player progress.
 */
object TacticsStateCodec {
    private const val MAGIC = 0x57544143 // "WTAC"

    fun encode(state: TacticsState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(TacticsCodec.encode(state.puzzle))
            output.writeInt(state.picks.size)
            for (cell in state.picks) {
                output.writeByte(cell.index)
            }
        }

    fun decode(bytes: ByteArray): TacticsState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = TacticsCodec.decode(input.readUTF())
            val pickCount = input.readInt()
            require(pickCount in 0..1000) { "Tactics save has invalid pick count: $pickCount" }
            val picks = ArrayList<Cell>(pickCount)
            repeat(pickCount) {
                val index = input.readUnsignedByte()
                require(index in 0 until 64) { "Tactics save has cell outside board: $index" }
                picks.add(Cell(index))
            }
            TacticsState(puzzle, picks)
        }
}
