package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.engine.tactics.PieceType
import org.junit.Assert.assertEquals
import org.junit.Test

class TacticsPieceDrawingTest {
    @Test
    fun everyPieceTypeHasANonEmptySilhouette() {
        assertEquals(
            PieceType.values().toSet(),
            tacticsPieceTypesWithSilhouettes,
        )
    }
}
