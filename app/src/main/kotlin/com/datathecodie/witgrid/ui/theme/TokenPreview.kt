package com.datathecodie.witgrid.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Shows every theme token. Used by the two previews below. */
@Composable
fun TokenScreen() {
    val colors = WitgridTheme.colors
    Column(
        modifier = Modifier
            .background(colors.paper)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Witgrid tokens", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
        Text("Region hues", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        colors.regions.forEach { region ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(region.color), contentAlignment = Alignment.Center) {
                    Text(region.letter.toString(), style = NumberStyles.cell, color = colors.ink)
                }
                Text(
                    "  hatch ${region.hatch.id} ${region.hatch.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                )
            }
        }
        Text("Colourways", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Colourway.entries.forEach { way ->
                Box(Modifier.size(48.dp).background(witgridColors(way, colors.isDark).accent))
            }
        }
        Text("Numbers (tabular)", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Text("0123456789 1111", style = NumberStyles.display, color = colors.ink)
        Text("Clue 14 sp: 12 / 34 / 56", style = NumberStyles.clue, color = colors.ink)
        Text("Timer 01:23", style = NumberStyles.timer, color = colors.ink)
        Text("Spacing", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        listOf(Spacing.xxs, Spacing.xs, Spacing.sm, Spacing.md, Spacing.lg, Spacing.xl, Spacing.xxl).forEach { gap ->
            Box(Modifier.width(gap * 4).height(Spacing.xs).background(colors.accent))
        }
        Box(Modifier.fillMaxWidth().padding(top = Spacing.sm).background(colors.accent).padding(Spacing.md)) {
            Text("On accent", style = MaterialTheme.typography.labelLarge, color = colors.onAccent)
        }
    }
}

@Preview(name = "Tokens light", showBackground = true, heightDp = 900)
@Composable
private fun TokensLightPreview() {
    WitgridTheme(darkTheme = false) { TokenScreen() }
}

@Preview(
    name = "Tokens dark",
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TokensDarkPreview() {
    WitgridTheme(darkTheme = true) { TokenScreen() }
}
