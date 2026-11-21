package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack

/**
 * Family B bank envelope: `id|size|layout-or-seed|pack|difficulty`, with no escaping.
 * There is no solution field; many winning paths are allowed.
 */
internal data class PlayBankLine(
    val id: String,
    val size: Int,
    val payload: String,
    val pack: Pack,
    val difficulty: String,
) {
    init {
        Grid(size)
        for (field in listOf(id, payload, difficulty)) {
            require(field.isNotBlank() && field.none { it == '|' || it == '\n' || it == '\r' }) {
                "Bank fields must be non-blank single-line text without delimiters"
            }
        }
    }

    fun encode(): String = "$id|$size|$payload|${pack.name}|$difficulty"

    companion object {
        fun decode(line: String): PlayBankLine {
            val fields = line.split('|')
            require(fields.size == 5) { "A Family B bank line must have exactly five fields" }
            val size = requireNotNull(fields[1].toIntOrNull()) { "Invalid bank grid size" }
            return PlayBankLine(fields[0], size, fields[2], Pack.valueOf(fields[3]), fields[4])
        }
    }
}