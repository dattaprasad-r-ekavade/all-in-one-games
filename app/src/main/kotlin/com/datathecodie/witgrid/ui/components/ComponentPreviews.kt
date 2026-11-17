package com.datathecodie.witgrid.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Preview(name = "Light", group = "U3 components", widthDp = 360, showBackground = true)
@Preview(name = "Dark", group = "U3 components", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light 1.3", group = "U3 components", widthDp = 360, fontScale = 1.3f, showBackground = true)
@Preview(name = "Dark 1.3", group = "U3 components", widthDp = 360, fontScale = 1.3f, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light 2.0", group = "U3 components", widthDp = 360, fontScale = 2f, showBackground = true)
@Preview(name = "Dark 2.0", group = "U3 components", widthDp = 360, fontScale = 2f, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class ComponentPreviews

/** The same samples are rendered by Studio previews and the component screenshot test. */
enum class ComponentSample {
    CARD, PRIMARY_BUTTON, SECONDARY_BUTTON, LIST_ROW, SECTION_HEADER,
    STAT_TILE, CHIP, SEGMENTED_CONTROL, TOOL_BAR_ITEM, ICON_ACTION_BUTTON,
}

@Composable
fun ComponentSampleContent(sample: ComponentSample, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (sample) {
            ComponentSample.CARD -> {
                WitgridCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.game_crowns), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.welcome_point_endless), color = WitgridTheme.colors.mutedInk)
                }
                WitgridCard(Modifier.fillMaxWidth(), onClick = {}) {
                    Text(stringResource(R.string.game_daily_entry))
                }
            }
            ComponentSample.PRIMARY_BUTTON -> {
                PrimaryButton(stringResource(R.string.button_next), {}, Modifier.fillMaxWidth())
                PrimaryButton(stringResource(R.string.button_next), {}, Modifier.fillMaxWidth(), enabled = false)
            }
            ComponentSample.SECONDARY_BUTTON -> {
                SecondaryButton(stringResource(R.string.button_back), {}, Modifier.fillMaxWidth())
                SecondaryButton(stringResource(R.string.button_back), {}, Modifier.fillMaxWidth(), enabled = false)
            }
            ComponentSample.LIST_ROW -> {
                var checked by remember { mutableStateOf(true) }
                WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    ListRow(stringResource(R.string.settings_reminder), trailingValue = stringResource(R.string.stats_duration_minutes_seconds, 8, 0), onClick = {})
                    HorizontalDivider(color = WitgridTheme.colors.cardEdge)
                    ListRow(
                        stringResource(R.string.settings_haptics),
                        subtitle = stringResource(R.string.welcome_point_offline),
                        checked = checked,
                        onCheckedChange = { checked = it },
                    )
                    HorizontalDivider(color = WitgridTheme.colors.cardEdge)
                    ListRow(stringResource(R.string.settings_sound), checked = false, onCheckedChange = {})
                }
            }
            ComponentSample.SECTION_HEADER -> SectionHeader(stringResource(R.string.settings_appearance))
            ComponentSample.STAT_TILE -> StatTile("2", stringResource(R.string.button_hint), Modifier.fillMaxWidth())
            ComponentSample.CHIP -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.pack_easy), highlighted = true)
                Chip(stringResource(R.string.game_crowns))
                Chip(stringResource(R.string.play_elapsed_time, "4:07"), onClick = {})
            }
            ComponentSample.SEGMENTED_CONTROL -> {
                var theme by remember { mutableIntStateOf(0) }
                var mode by remember { mutableIntStateOf(0) }
                SegmentedControl(
                    listOf(stringResource(R.string.settings_theme_system), stringResource(R.string.settings_theme_light), stringResource(R.string.settings_theme_dark)),
                    theme, { theme = it },
                )
                SegmentedControl(
                    listOf(stringResource(R.string.nonogram_mode_fill), stringResource(R.string.nonogram_mode_cross)),
                    mode, { mode = it },
                )
            }
            ComponentSample.TOOL_BAR_ITEM -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolBarItem(painterResource(R.drawable.ic_back), stringResource(R.string.button_undo), {})
                ToolBarItem(painterResource(R.drawable.ic_info), stringResource(R.string.button_hint), {}, badge = "2", selected = true)
                ToolBarItem(painterResource(R.drawable.ic_close), stringResource(R.string.button_redo), {}, enabled = false)
            }
            ComponentSample.ICON_ACTION_BUTTON -> FlowRow {
                IconActionButton(painterResource(R.drawable.ic_back), stringResource(R.string.button_back), {})
                IconActionButton(painterResource(R.drawable.ic_calendar), stringResource(R.string.action_calendar), {})
                IconActionButton(painterResource(R.drawable.ic_settings), stringResource(R.string.action_settings), {}, enabled = false)
            }
        }
    }
}

@Composable
private fun ComponentPreviewFrame(sample: ComponentSample) {
    WitgridTheme {
        Surface(color = WitgridTheme.colors.paper, contentColor = WitgridTheme.colors.ink) {
            ComponentSampleContent(sample, Modifier.padding(16.dp))
        }
    }
}

@ComponentPreviews
@Composable
private fun CardPreview() = ComponentPreviewFrame(ComponentSample.CARD)

@ComponentPreviews
@Composable
private fun PrimaryButtonPreview() = ComponentPreviewFrame(ComponentSample.PRIMARY_BUTTON)

@ComponentPreviews
@Composable
private fun SecondaryButtonPreview() = ComponentPreviewFrame(ComponentSample.SECONDARY_BUTTON)

@ComponentPreviews
@Composable
private fun ListRowPreview() = ComponentPreviewFrame(ComponentSample.LIST_ROW)

@ComponentPreviews
@Composable
private fun SectionHeaderPreview() = ComponentPreviewFrame(ComponentSample.SECTION_HEADER)

@ComponentPreviews
@Composable
private fun StatTilePreview() = ComponentPreviewFrame(ComponentSample.STAT_TILE)

@ComponentPreviews
@Composable
private fun ChipPreview() = ComponentPreviewFrame(ComponentSample.CHIP)

@ComponentPreviews
@Composable
private fun SegmentedControlPreview() = ComponentPreviewFrame(ComponentSample.SEGMENTED_CONTROL)

@ComponentPreviews
@Composable
private fun ToolBarItemPreview() = ComponentPreviewFrame(ComponentSample.TOOL_BAR_ITEM)

@ComponentPreviews
@Composable
private fun IconActionButtonPreview() = ComponentPreviewFrame(ComponentSample.ICON_ACTION_BUTTON)
