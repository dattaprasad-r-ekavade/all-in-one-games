package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathCodecTest {
    private val sampleLine = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|EASY|easy:1"

    @Test
    fun decodesAndEncodesCanonicalBankLine() {
        val puzzle = PathCodec.decode(sampleLine)

        assertEquals("path-5-u", puzzle.id)
        assertEquals(5, puzzle.size)
        assertEquals(Pack.EASY, puzzle.pack)
        assertEquals("easy:1", puzzle.storedGrade)
        assertEquals(3, puzzle.pairCount)
        assertTrue(puzzle.isWall(12))
        assertEquals(1, puzzle.endpointAt(0))
        assertEquals(1, puzzle.endpointAt(24))

        val encoded = PathCodec.encode(puzzle)
        assertEquals(sampleLine, encoded)
    }

    @Test
    fun rejectsMalformedBankLines() {
        // Wrong field count
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("too|few|fields")
        }

        // Wrong size
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("id|3|payload|sol|EASY|grade")
        }

        // Wrong payload length
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("id|5|short|11111233312##312333122221|EASY|easy:1")
        }

        // Wrong pack for size
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("path-5-u|5|1....23....##...3......21|11111233312##312333122221|EXPERT|easy:1")
        }

        // Unsupported board sizes 7, 9, 11 rejected at bank boundary
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("path-7|7|${".".repeat(49)}|${"1".repeat(49)}|MEDIUM|medium:1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("path-9|9|${".".repeat(81)}|${"1".repeat(81)}|HARD|hard:1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode("path-11|11|${".".repeat(121)}|${"1".repeat(121)}|EXPERT|expert:1")
        }

        // Stored solution violating path rules is rejected on decode
        val invalidSelfTouchingLine = "path-5-invalid-selftouch|5|1....23.........3......21|1111123331233312333122221|EASY|easy:1"
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode(invalidSelfTouchingLine)
        }
    }
}
