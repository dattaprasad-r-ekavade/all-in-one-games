package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.VersionedStateCodec

object ColourSortStateCodec {
    private const val MAGIC = 0x57434F53 // "WCOS"

    fun encode(state: ColourSortState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(ColourSortCodec.encodePuzzle(state.puzzle))
            output.writeInt(state.tubes.size)
            state.tubes.forEach { tube ->
                output.writeInt(tube.size)
                tube.forEach { colour -> output.writeByte(colour) }
            }
            output.writeInt(state.pourCount)
        }

    fun decode(bytes: ByteArray): ColourSortState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = ColourSortCodec.decodeLine(input.readUTF())
            val tubeCount = input.readInt()
            require(tubeCount >= puzzle.initialTubes.size) { "Save has fewer tubes than the deal" }
            val tubes = ArrayList<List<Int>>(tubeCount)
            repeat(tubeCount) {
                val depth = input.readInt()
                require(depth in 0..ColourSortRules.TUBE_CAPACITY) { "Tube depth out of range" }
                val stack = ArrayList<Int>(depth)
                repeat(depth) {
                    val colour = input.readUnsignedByte()
                    require(colour in 1..9) { "Invalid colour in save" }
                    stack += colour
                }
                tubes += stack.toList()
            }
            val pourCount = input.readInt()
            require(pourCount >= 0) { "Invalid pour count" }
            ColourSortState(puzzle, tubes, pourCount)
        }
}