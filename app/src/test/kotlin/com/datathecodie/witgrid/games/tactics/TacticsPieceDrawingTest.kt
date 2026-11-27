package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.engine.tactics.PieceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TacticsPieceDrawingTest {
    @Test
    fun everyPieceTypeHasACodeDrawnSilhouette() {
        assertEquals(PieceType.entries.toSet(), tacticsDrawnPieceTypes)
    }
}
