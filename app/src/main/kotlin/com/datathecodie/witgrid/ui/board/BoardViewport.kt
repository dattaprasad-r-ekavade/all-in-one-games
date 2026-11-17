package com.datathecodie.witgrid.ui.board

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BoardViewportMode(val enlarged: Boolean = false, val embedded: Boolean = false)

val LocalBoardViewportMode = staticCompositionLocalOf { BoardViewportMode() }

@Composable
internal fun PannableBoardViewport(
    modifier: Modifier = Modifier,
    contentWidth: Dp,
    contentHeight: Dp,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    require(contentWidth > 0.dp && contentHeight > 0.dp) {
        "Board content dimensions must be positive"
    }
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    BoxWithConstraints(modifier.clipToBounds()) {
        val viewportWidth = if (maxWidth == Dp.Infinity) contentWidth else minOf(maxWidth, contentWidth)
        val viewportHeight = if (maxHeight == Dp.Infinity) contentHeight else minOf(maxHeight, contentHeight)
        val canPan = enabled && (contentWidth > viewportWidth || contentHeight > viewportHeight)

        if (canPan) {
            Box(
                modifier = Modifier
                    .width(viewportWidth)
                    .height(viewportHeight)
                    .verticalScroll(verticalScrollState),
            ) {
                Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    Box(
                        modifier = Modifier.width(contentWidth).height(contentHeight),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        content()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.width(viewportWidth).height(viewportHeight),
                contentAlignment = Alignment.TopStart,
            ) {
                content()
            }
        }
    }
}

internal val MIN_BOARD_CELL_SIZE = 48.dp
