package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = WitgridTheme.colors.mutedInk,
    )
}
