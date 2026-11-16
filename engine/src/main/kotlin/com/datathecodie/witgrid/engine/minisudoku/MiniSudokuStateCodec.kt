package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.VersionedStateCodec

object MiniSudokuStateCodec {
    private const val MAGIC = 0x574D5355

    fun encode(state: MiniSudokuState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(MiniSudokuCodec.encode(state.puzzle))
            output.writeInt(state.digits.size)
            state.digits.indices.forEach { index ->
                output.writeByte(state.digits[index] ?: 0)
                output.writeByte(state.notes[index])
            }
        }

    fun decode(bytes: ByteArray): MiniSudokuState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = MiniSudokuCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Mini Sudoku save has the wrong cell count" }
        val digits = ArrayList<Int?>(cellCount)
        val notes = ArrayList<Int>(cellCount)
        repeat(cellCount) {
            val digit = input.readUnsignedByte()
            val note = input.readUnsignedByte()
            // Check the bytes here, so a bad save fails with a clear message, not a state rule.
            require(digit <= MINI_SUDOKU_SIZE) { "Mini Sudoku save has a digit outside 0 through 6" }
            require(note and MINI_SUDOKU_NOTE_MASK == note) {
                "Mini Sudoku save has note bits above digit 6"
            }
            digits += digit.takeIf { it != 0 }
            notes += note
        }
        MiniSudokuState(puzzle, digits, notes)
    }
}
