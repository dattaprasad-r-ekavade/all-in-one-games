package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.datathecodie.witgrid.engine.core.Cell

internal const val INTRO_SPOTLIGHT_FREE_AFTER_MOVES = 3

internal fun introSpotlightCells(isIntro: Boolean, successfulMoves: Int, nextStepCells: Set<Cell>): Set<Cell> =
    if (isIntro && successfulMoves < INTRO_SPOTLIGHT_FREE_AFTER_MOVES) nextStepCells else emptySet()

@Composable
internal fun IntroSpotlight(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "intro-spotlight")
    val alpha by transition.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "intro-spotlight-pulse",
    )
    androidx.compose.foundation.layout.Box(modifier.alpha(alpha)) { content() }
}
