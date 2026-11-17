package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = WitgridTheme.colors
    val background = if (highlighted) colors.accentSoft else colors.card
    val foreground = if (highlighted) colors.accent else colors.mutedInk
    val body: @Composable () -> Unit = {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
    }
    if (onClick == null) {
        Surface(modifier, RoundedCornerShape(50), background, foreground, border = BorderStroke(1.dp, colors.cardEdge), content = body)
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(50),
            color = background,
            contentColor = foreground,
            border = BorderStroke(1.dp, colors.cardEdge),
            content = body,
        )
    }
}
