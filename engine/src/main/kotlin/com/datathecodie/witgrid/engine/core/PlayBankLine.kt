package com.datathecodie.witgrid.engine.core

/**
 * Family B bank envelope: `id|size|payload|pack|difficulty`, with no escaping.
 * [payload] is lane-owned text (layout, deal seed, or other generator input).
 * There is no stored solution field; many winning paths may exist.
 */
data class PlayBankLine(
    val id: String,
    val size: Int,
    val payload: String,
    val pack: Pack,
    val difficulty: String,
) {
    init {
        require(size >= 0) { "Bank size must not be negative" }
        for (field in listOf(id, payload, difficulty)) {
            require(field.isNotBlank() && field.none { it == '|' || it == '\n' || it == '\r' }) {
                "Bank fields must be non-blank single-line text without delimiters"
            }
        }
    }

    /** Encodes five fields with the stable uppercase [Pack] name, without a line terminator. */
    fun encode(): String = "$id|$size|$payload|${pack.name}|$difficulty"

    companion object {
        /** Decodes exactly five fields; malformed envelopes throw [IllegalArgumentException]. */
        fun decode(line: String): PlayBankLine {
            val fields = line.split('|')
            require(fields.size == 5) { "A Family B bank line must have exactly five fields" }
            val size = requireNotNull(fields[1].toIntOrNull()) { "Invalid bank size" }
            return PlayBankLine(fields[0], size, fields[2], Pack.valueOf(fields[3]), fields[4])
        }
    }
}