package com.datathecodie.witgrid.games.mosaic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.theme.Spacing

/**
 * Custom layout that measures controls first to guarantee their height before sizing the board.
 * Avoids tool bar shrinking or loss of space on compact viewports.
 */
@Composable
internal fun MosaicBoardWithControls(
    modifier: Modifier = Modifier,
    board: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    val boardPadding = if (LocalBoardViewportMode.current.embedded) 0.dp else Spacing.screenMargin * 2
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
        val boardSpace = loose.offset(vertical = -measuredControls.height - gap)
        val boardSide = minOf(boardSpace.maxWidth, boardSpace.maxHeight)
        val measuredBoard = if (boardSide > boardPadding.roundToPx()) {
            measurables[0].measure(boardSpace.copy(maxWidth = boardSide, maxHeight = boardSide))
        } else {
            null
        }
        val controlsTop = if (measuredBoard == null) 0 else measuredBoard.height + gap
        val width = constraints.constrainWidth(maxOf(measuredBoard?.width ?: 0, measuredControls.width))
        val height = constraints.constrainHeight(controlsTop + measuredControls.height)
        layout(width, height) {
            measuredBoard?.place((width - measuredBoard.width) / 2, 0)
            measuredControls.place((width - measuredControls.width) / 2, controlsTop)
        }
    }
}
