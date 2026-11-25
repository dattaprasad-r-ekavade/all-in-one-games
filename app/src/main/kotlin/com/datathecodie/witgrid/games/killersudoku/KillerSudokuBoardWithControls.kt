package com.datathecodie.witgrid.games.killersudoku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import com.datathecodie.witgrid.ui.theme.Spacing

@Composable
internal fun KillerSudokuBoardWithControls(
    modifier: Modifier,
    board: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box(contentAlignment = Alignment.TopCenter) { board() }
            Box { controls() }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val measuredControls = measurables[1].measure(loose)
        val gap = Spacing.sm.roundToPx()
        val measuredBoard = measurables[0].measure(loose.offset(vertical = -measuredControls.height - gap))
        val controlsTop = if (measuredBoard.height == 0) 0 else measuredBoard.height + gap
        val width = constraints.constrainWidth(maxOf(measuredBoard.width, measuredControls.width))
        val height = constraints.constrainHeight(controlsTop + measuredControls.height)
        layout(width, height) {
            measuredBoard.place((width - measuredBoard.width) / 2, 0)
            measuredControls.place((width - measuredControls.width) / 2, controlsTop)
        }
    }
}
