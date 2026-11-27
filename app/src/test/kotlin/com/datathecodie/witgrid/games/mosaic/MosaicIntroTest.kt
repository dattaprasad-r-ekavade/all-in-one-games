package com.datathecodie.witgrid.games.mosaic

import com.datathecodie.witgrid.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MosaicIntroTest {
    @Test
    fun introUsesMosaicLevelIdAndCoachLine() {
        assertEquals("MO-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_mosaic, Intro.definition.coachLineResourceId)
    }
}
