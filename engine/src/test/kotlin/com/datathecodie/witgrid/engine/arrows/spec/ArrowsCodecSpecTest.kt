package com.datathecodie.witgrid.engine.arrows.spec

import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.CLEAR_RIGHT
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.EASY_SIZE
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.FACING_COLUMN
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.FACING_PAIR
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.HINT_CHAIN
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.easyLine
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.rows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Family B bank line is `id|size|payload|pack|difficulty`.
 * Payload is size*size cells: `.` empty, `^v<>` arrows.
 */
class ArrowsCodecSpecTest {

    @Test
    fun bankLineRoundTripKeepsLayoutAndMetadata() {
        val puzzle = easyPuzzle("AR-SPEC-ROUNDTRIP", *HINT_CHAIN)

        val encoded = ArrowsEngine.encodePuzzle(puzzle)
        val fields = encoded.split('|')
        val decoded = ArrowsEngine.decodePuzzle(encoded)

        assertEquals(5, fields.size)
        assertEquals("AR-SPEC-ROUNDTRIP", fields[0])
        assertEquals("5", fields[1])
        assertEquals("EASY", fields[3])
        assertEquals("easy", fields[4])
        assertEquals(puzzle, decoded)
        assertEquals(encoded, ArrowsEngine.encodePuzzle(decoded))
        assertEquals("AR-SPEC-ROUNDTRIP", decoded.bankId)
        assertEquals(EASY_SIZE, decoded.size)
        assertEquals(ArrowsDirection.RIGHT, decoded.arrowAt(1, 1))
        assertEquals(ArrowsDirection.DOWN, decoded.arrowAt(1, 3))
        assertEquals(ArrowsDirection.LEFT, decoded.arrowAt(2, 3))
    }

    @Test
    fun payloadUsesCanonicalArrowSymbols() {
        val puzzle = easyPuzzle("AR-SPEC-SYMBOLS", *CLEAR_RIGHT)
        val payload = ArrowsEngine.encodePuzzle(puzzle).split('|')[2]

        assertEquals(EASY_SIZE * EASY_SIZE, payload.length)
        assertEquals(rows(*CLEAR_RIGHT), payload)
        assertEquals('>', payload[6])
        assertTrue(payload.all { it == '.' || it == '>' })
    }

    @Test
    fun facingPairOnARowIsRejectedOnDecode() {
        val line = easyLine("AR-SPEC-DEADLOCK-ROW", rows(*FACING_PAIR))

        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle(line)
        }
    }

    @Test
    fun facingPairInAColumnIsRejectedOnDecode() {
        val line = easyLine("AR-SPEC-DEADLOCK-COL", rows(*FACING_COLUMN))

        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle(line)
        }
    }

    @Test
    fun lineWithTooFewFieldsThrows() {
        val payload = rows(*CLEAR_RIGHT)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-SHORT|5|$payload|EASY")
        }
    }

    @Test
    fun lineWithTooManyFieldsThrows() {
        val payload = rows(*CLEAR_RIGHT)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-LONG|5|$payload|EASY|easy|extra")
        }
    }

    @Test
    fun emptyPayloadThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-EMPTY|5||EASY|easy")
        }
    }

    @Test
    fun payloadLengthMustMatchSize() {
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-LEN|5|...>|EASY|easy")
        }
    }

    @Test
    fun invalidLayoutCharacterThrows() {
        val payload = rows(
            ".....",
            ".>x..",
            ".....",
            ".....",
            ".....",
        )
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle(easyLine("AR-SPEC-CHAR", payload))
        }
    }

    @Test
    fun packSizeMismatchThrows() {
        val payload = ".".repeat(6 * 6)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-PACK|6|$payload|EASY|easy")
        }
    }

    @Test
    fun unsupportedBoardSizeThrows() {
        val payload = ".".repeat(3 * 3)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-SIZE3|3|$payload|EASY|easy")
        }
    }

    @Test
    fun invalidPackNameThrows() {
        val payload = rows(*CLEAR_RIGHT)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-PACKNAME|5|$payload|easy|easy")
        }
    }

    @Test
    fun invalidSizeTokenThrows() {
        val payload = rows(*CLEAR_RIGHT)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.decodePuzzle("AR-SPEC-SIZE|xx|$payload|EASY|easy")
        }
    }
}
