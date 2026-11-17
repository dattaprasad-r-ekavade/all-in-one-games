package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    WitgridCard(modifier.semantics(mergeDescendants = true) {}) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = NumberStyles.display, color = WitgridTheme.colors.ink, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = WitgridTheme.colors.mutedInk, textAlign = TextAlign.Center)
        }
    }
}
