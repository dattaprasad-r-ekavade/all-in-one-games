package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BankLineTest {
    @Test
    fun roundTripsEachPackAndOpaqueLanePayload() {
        for (pack in Pack.entries) {
            val line = "level-001|6|regions:aa,bb;clues:1/2|123456|${pack.name}|single:12"
            val expected = BankLine("level-001", 6, "regions:aa,bb;clues:1/2", "123456", pack, "single:12")
            assertEquals(expected, BankLine.decode(line))
            assertEquals(line, expected.encode())
            assertEquals(expected, BankLine.decode(expected.encode()))
        }
    }

    @Test
    fun preservesFieldWhitespaceAndUnicode() {
        val line = BankLine(" level ", 5, " ø ", " 1 ", Pack.EASY, " règle ")
        assertEquals(line, BankLine.decode(line.encode()))
    }

    @Test
    fun rejectsMalformedEnvelopes() {
        val malformed = listOf(
            "",
            "id|6|p|s|EASY",
            "id|6|p|s|EASY|g|extra",
            "id|6|p|s|EASY|",
            "|6|p|s|EASY|g",
            "id|0|p|s|EASY|g",
            "id|-1|p|s|EASY|g",
            "id|46341|p|s|EASY|g",
            "id|2147483648|p|s|EASY|g",
            "id|six|p|s|EASY|g",
            "id|6||s|EASY|g",
            "id|6|p||EASY|g",
            "id|6|p|s|easy|g",
            "id|6|p|s|UNKNOWN|g",
            "id|6|p|s|EASY|g\n",
            "id|6|p|s|EASY|g\r\n",
            "id|6|p\ns|s|EASY|g",
        )
        for (line in malformed) {
            assertThrows(IllegalArgumentException::class.java, { BankLine.decode(line) }, line)
        }
    }

    @Test
    fun constructorsAndCopiesRejectUnencodableFields() {
        val valid = BankLine("id", 6, "p", "s", Pack.HARD, "g")
        for (field in listOf("", " ", "a|b", "a\nb", "a\rb")) {
            assertThrows(IllegalArgumentException::class.java) { valid.copy(id = field) }
            assertThrows(IllegalArgumentException::class.java) { valid.copy(payload = field) }
            assertThrows(IllegalArgumentException::class.java) { valid.copy(solution = field) }
            assertThrows(IllegalArgumentException::class.java) { valid.copy(grade = field) }
        }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(size = 0) }
    }
}
