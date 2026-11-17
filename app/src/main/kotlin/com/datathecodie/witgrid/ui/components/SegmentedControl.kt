package com.datathecodie.witgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.ui.theme.WitgridTheme

/** Options wrap into rows as the available width shrinks or text grows. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    require(options.isNotEmpty()) { "A segmented control needs options" }
    require(selectedIndex in options.indices) { "Selection must name an option" }
    val colors = WitgridTheme.colors
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.background(colors.cardEdge, RoundedCornerShape(14.dp)).padding(4.dp)) {
        val columns = (maxWidth.value / (88 * fontScale)).toInt().coerceIn(1, options.size)
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.indices.chunked(columns).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { index ->
                        val selected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (selected) colors.card else colors.cardEdge, RoundedCornerShape(11.dp))
                                .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(index) })
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                options[index],
                                color = (if (selected) colors.ink else colors.mutedInk).copy(alpha = if (enabled) 1f else 0.38f),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
