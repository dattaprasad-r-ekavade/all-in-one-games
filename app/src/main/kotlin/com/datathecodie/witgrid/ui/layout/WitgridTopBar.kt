package com.datathecodie.witgrid.ui.layout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ui.theme.Spacing

/** One action icon in a top bar. */
data class TopBarAction(
    @DrawableRes val icon: Int,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * The bar at the top of every non-play screen: back arrow (or the logo on Home), title and icons.
 * The bar grows with the font scale instead of clipping the title.
 */
@Composable
fun WitgridTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showLogo: Boolean = false,
    actions: List<TopBarAction> = emptyList(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TOP_BAR_HEIGHT)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TopBarIconButton(TopBarAction(R.drawable.ic_back, stringResource(R.string.button_back), onBack))
        } else {
            Spacer(Modifier.width(Spacing.md))
        }
        if (showLogo) {
            WitgridLogo(modifier = Modifier.size(LOGO_SIZE))
            Spacer(Modifier.width(Spacing.md))
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Spacing.sm)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (showLogo) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        actions.forEach { TopBarIconButton(it) }
    }
}

@Composable
private fun TopBarIconButton(action: TopBarAction) {
    IconButton(onClick = action.onClick, modifier = Modifier.size(Spacing.minTouchTarget)) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = action.label,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

/** A screen with a top bar and content below it. */
@Composable
fun WitgridScreenFrame(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        topBar()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

private val TOP_BAR_HEIGHT = 64.dp
private val ICON_SIZE = 22.dp
private val LOGO_SIZE = 26.dp
