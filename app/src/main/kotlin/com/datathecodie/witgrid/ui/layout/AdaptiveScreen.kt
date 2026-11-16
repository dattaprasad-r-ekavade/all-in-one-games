package com.datathecodie.witgrid.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Centers readable screen content on wide windows while using the full phone width. */
@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = DEFAULT_CONTENT_WIDTH,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val contentWidth = maxWidth.coerceAtMost(maxContentWidth)
        Box(
            modifier = Modifier
                .width(contentWidth)
                .fillMaxHeight()
                .align(Alignment.TopCenter),
            content = content,
        )
    }
}

private val DEFAULT_CONTENT_WIDTH = 720.dp
