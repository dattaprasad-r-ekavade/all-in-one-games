package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.engine.arrows.ArrowsCodec
import com.datathecodie.witgrid.engine.arrows.ArrowsPuzzle
import com.datathecodie.witgrid.engine.core.Pack

/**
 * 5x5 layout from the engine fixture.
 * Cell 2 points up and can slide. Cell 7 points up and is blocked by cell 2.
 * Cell 14 points down and can slide. Cell 12 points right and is blocked by cell 14.
 */
internal object ArrowsBoardFixtures {
    const val BLOCKED_CELL = 7
    const val LAYOUT =
        "..^.." +
            "..^.." +
            "..>.v" +
            "....." +
            "....."

    val easyPuzzle = ArrowsPuzzle(
        bankId = "ar-easy-1",
        size = 5,
        pack = Pack.EASY,
        difficulty = "d1",
        initialArrows = ArrowsCodec.decodeLayout(LAYOUT, 5),
    )
}
