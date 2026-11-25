package com.datathecodie.witgrid.games.path

import com.datathecodie.witgrid.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PathIntroTest {
    @Test
    fun introUsesPathLevelIdAndCoachLine() {
        assertEquals("PA-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_path, Intro.definition.coachLineResourceId)
    }
}
