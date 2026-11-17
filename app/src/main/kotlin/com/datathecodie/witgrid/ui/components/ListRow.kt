package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ui.theme.WitgridTheme

/** A switch row has one toggle target, including its title and subtitle. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingValue: String? = null,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    enabled: Boolean = true,
) {
    require((checked == null) == (onCheckedChange == null)) { "A switch needs a value and a callback" }
    require(checked == null || onClick == null) { "A row has one action" }
    val colors = WitgridTheme.colors
    val action = when {
        checked != null && onCheckedChange != null -> Modifier.toggleable(
            value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange,
        )
        onClick != null -> Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        else -> Modifier
    }
    Row(
        modifier = modifier.then(action).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.ink.copy(alpha = if (enabled) 1f else 0.38f))
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.mutedInk) }
        }
        trailingValue?.let {
            Text(it, modifier = Modifier.widthIn(max = 112.dp), style = MaterialTheme.typography.bodyMedium, color = colors.mutedInk)
        }
        if (checked != null) {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        } else if (showChevron) {
            Icon(
                painterResource(R.drawable.ic_back),
                contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(if (LocalLayoutDirection.current == LayoutDirection.Ltr) 180f else 0f),
                tint = colors.mutedInk,
            )
        }
    }
}
