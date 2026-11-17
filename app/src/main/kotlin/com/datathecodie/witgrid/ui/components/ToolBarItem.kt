package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun ToolBarItem(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = WitgridTheme.colors
    val foreground = if (!enabled) colors.mutedInk.copy(alpha = 0.38f) else if (selected) colors.accent else colors.ink
    Column(
        modifier = modifier
            .widthIn(min = 48.dp)
            .background(if (selected) colors.accentSoft else Color.Transparent, RoundedCornerShape(14.dp))
            .selectable(selected, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 6.dp).size(24.dp), tint = foreground)
            badge?.let {
                Text(
                    it,
                    modifier = Modifier.background(colors.accent, CircleShape).padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onAccent,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = foreground, textAlign = TextAlign.Center)
    }
}
