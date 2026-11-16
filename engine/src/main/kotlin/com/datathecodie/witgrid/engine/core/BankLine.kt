package com.datathecodie.witgrid.engine.core

/**
 * Common bank envelope: `id|size|payload|solution|pack|grade`, with no escaping.
 * [id] identifies the level, not the game. [payload], [solution] and [grade] are opaque
 * lane-owned text; the lane codec validates their grammar and maps grade text to [Grade].
 * Fields cannot be blank or contain delimiters or line breaks. Whitespace is preserved.
 */
data class BankLine(
    val id: String,
    val size: Int,
    val payload: String,
    val solution: String,
    val pack: Pack,
    val grade: String,
) {
    init {
        Grid(size)
        for (field in listOf(id, payload, solution, grade)) {
            require(field.isNotBlank() && field.none { it == '|' || it == '\n' || it == '\r' }) {
                "Bank fields must be non-blank single-line text without delimiters"
            }
        }
    }

    /** Encodes six fields with the stable uppercase [Pack] name, without a line terminator. */
    fun encode(): String = "$id|$size|$payload|$solution|${pack.name}|$grade"

    companion object {
        /** Decodes exactly six fields; malformed envelopes throw [IllegalArgumentException]. */
        fun decode(line: String): BankLine {
            val fields = line.split('|')
            require(fields.size == 6) { "A bank line must have exactly six fields" }
            val size = requireNotNull(fields[1].toIntOrNull()) { "Invalid bank grid size" }
            return BankLine(fields[0], size, fields[2], fields[3], Pack.valueOf(fields[4]), fields[5])
        }
    }
}
