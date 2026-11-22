package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PathTechniquesTest {

    @Test
    fun registersAllTechniquesWithExpectedRanks() {
        assertEquals(4, PathTechniques.all.size)
        assertEquals(PathTechniques.ENDPOINT_EXTENSION, PathTechniques.byId("endpoint-extension"))
        assertEquals(PathTechniques.FORCED_NEIGHBOUR, PathTechniques.byId("forced-neighbour"))
        assertEquals(PathTechniques.DEAD_END_AVOIDANCE, PathTechniques.byId("dead-end-avoidance"))
        assertEquals(PathTechniques.LAST_EMPTY_CELL, PathTechniques.byId("last-empty-cell"))
        assertNull(PathTechniques.byId("unknown"))

        assertEquals(0, PathTechniques.ENDPOINT_EXTENSION.difficultyRank)
        assertEquals(1, PathTechniques.FORCED_NEIGHBOUR.difficultyRank)
        assertEquals(2, PathTechniques.DEAD_END_AVOIDANCE.difficultyRank)
        assertEquals(3, PathTechniques.LAST_EMPTY_CELL.difficultyRank)
    }

    @Test
    fun gradeCodecEncodesAndValidatesSuccessfully() {
        val grade = Grade(PathTechniques.FORCED_NEIGHBOUR, 12, Pack.EASY)
        val encoded = PathGradeCodec.encode(grade)
        assertEquals("forced-neighbour:12", encoded)

        PathGradeCodec.validate(encoded)

        val zeroGrade = Grade(null, 0, Pack.EASY)
        assertEquals("none:0", PathGradeCodec.encode(zeroGrade))
        PathGradeCodec.validate("none:0")
    }

    @Test
    fun gradeCodecRejectsInvalidStrings() {
        assertThrows(IllegalArgumentException::class.java) {
            PathGradeCodec.validate("forced-neighbour")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathGradeCodec.validate("forced-neighbour:notanumber")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathGradeCodec.validate("forced-neighbour:-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathGradeCodec.validate("unknown-technique:5")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathGradeCodec.validate("forced-neighbour:0")
        }
    }
}
