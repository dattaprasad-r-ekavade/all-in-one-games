package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun WitgridCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WitgridTheme.colors
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = colors.card,
            contentColor = colors.ink,
            border = BorderStroke(1.dp, colors.cardEdge),
            content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = colors.card,
            contentColor = colors.ink,
            border = BorderStroke(1.dp, colors.cardEdge),
            content = body,
        )
    }
}
