package com.datathecodie.witgrid.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EngineInfoTest {
    @Test
    fun nameIsStable() {
        assertEquals("witgrid-engine", EngineInfo.NAME)
    }
}
