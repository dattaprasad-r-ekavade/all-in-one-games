package com.datathecodie.witgrid.engine.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

internal object VersionedStateCodec {
    private const val VERSION = 1
    private const val MAX_SAVE_BYTES = 1_048_576

    fun encode(magic: Int, writePayload: (DataOutputStream) -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(magic)
            output.writeByte(VERSION)
            writePayload(output)
        }
        return bytes.toByteArray()
    }

    fun <T> decode(
        bytes: ByteArray,
        magic: Int,
        readPayload: (DataInputStream) -> T,
    ): T {
        require(bytes.size <= MAX_SAVE_BYTES) { "State save is too large" }
        try {
            return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                require(input.readInt() == magic) { "State save belongs to another game" }
                val version = input.readUnsignedByte()
                require(version == VERSION) { "Unsupported state save version: $version" }
                val state = readPayload(input)
                require(input.available() == 0) { "State save contains trailing data" }
                state
            }
        } catch (exception: IllegalArgumentException) {
            throw exception
        } catch (exception: IOException) {
            throw IllegalArgumentException("Malformed state save", exception)
        }
    }
}
