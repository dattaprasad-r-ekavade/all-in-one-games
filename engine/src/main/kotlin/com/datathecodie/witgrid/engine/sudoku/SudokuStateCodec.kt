package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.util.ArrayList

/**
 * Versioned Sudoku save layout: shared version header, then UTF bank line, cell count,
 * and for each cell one byte (0 clears the digit) plus two bytes for the 9-bit note mask.
 */
object SudokuStateCodec {
    private const val MAGIC = 0x57555344

    fun encode(state: SudokuState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(SudokuCodec.encode(state.puzzle))
            output.writeInt(state.digits.size)
            state.digits.indices.forEach { index ->
                output.writeByte(state.digits[index] ?: 0)
                output.writeShort(state.notes[index])
            }
        }

    fun decode(bytes: ByteArray): SudokuState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = SudokuCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Sudoku save has the wrong cell count" }
        val digits = ArrayList<Int?>(cellCount)
        val notes = ArrayList<Int>(cellCount)
        repeat(cellCount) {
            val digit = input.readUnsignedByte()
            val note = input.readUnsignedShort()
            require(digit <= SUDOKU_SIZE) { "Sudoku save has a digit outside 0 through 9" }
            require(note and SUDOKU_NOTE_MASK == note) {
                "Sudoku save has note bits above digit 9"
            }
            digits += digit.takeIf { it != 0 }
            notes += note
        }
        SudokuState(puzzle, digits, notes)
    }
}