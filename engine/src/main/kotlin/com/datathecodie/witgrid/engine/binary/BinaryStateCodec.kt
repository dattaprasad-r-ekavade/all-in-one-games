package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.VersionedStateCodec

object BinaryStateCodec {
    private const val MAGIC = 0x5742494E

    fun encode(state: BinaryState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(BinaryCodec.encode(state.puzzle))
            output.writeInt(state.symbols.size)
            state.symbols.forEach { output.writeByte(it.ordinal) }
        }

    fun decode(bytes: ByteArray): BinaryState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = BinaryCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Binary save has the wrong cell count" }
        val symbols = List(cellCount) {
            BinarySymbol.entries.getOrNull(input.readUnsignedByte())
                ?: throw IllegalArgumentException("Binary save has an unknown symbol")
        }
        BinaryState(puzzle, symbols)
    }
}
