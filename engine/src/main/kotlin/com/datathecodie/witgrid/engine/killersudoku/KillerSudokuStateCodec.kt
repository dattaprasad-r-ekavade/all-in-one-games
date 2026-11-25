package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.util.ArrayList

/**
 * Versioned Killer Sudoku save layout: shared version header, then UTF bank line, cell count,
 * and for each cell one byte (0 clears the digit) plus two bytes for the 9-bit note mask.
 */
object KillerSudokuStateCodec {
    private const val MAGIC = 0x574B5355 // "WKSU"

    /** Serializes puzzle context, entered digits, and pencil notes into a versioned save. */
    fun encode(state: KillerSudokuState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(KillerSudokuCodec.encode(state.puzzle))
            output.writeInt(state.digits.size)
            state.digits.indices.forEach { index ->
                output.writeByte(state.digits[index] ?: 0)
                output.writeShort(state.notes[index])
            }
        }

    /** Restores a self-contained state save, rejecting wrong magic, versions, or bounds. */
    fun decode(bytes: ByteArray): KillerSudokuState = VersionedStateCodec.decode(bytes, MAGIC) { input ->
        val puzzle = KillerSudokuCodec.decode(input.readUTF())
        val cellCount = input.readInt()
        require(cellCount == puzzle.grid.cellCount) { "Killer Sudoku save has the wrong cell count" }
        val digits = ArrayList<Int?>(cellCount)
        val notes = ArrayList<Int>(cellCount)
        repeat(cellCount) {
            val digit = input.readUnsignedByte()
            val note = input.readUnsignedShort()
            require(digit <= KILLER_SUDOKU_SIZE) {
                "Killer Sudoku save has a digit outside 0 through 9"
            }
            require(note and KILLER_SUDOKU_NOTE_MASK == note) {
                "Killer Sudoku save has note bits above digit 9"
            }
            digits += digit.takeIf { it != 0 }
            notes += note
        }
        KillerSudokuState(puzzle, digits, notes)
    }
}
