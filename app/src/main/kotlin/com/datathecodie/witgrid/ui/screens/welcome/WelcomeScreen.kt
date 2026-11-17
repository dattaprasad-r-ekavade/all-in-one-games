package com.datathecodie.witgrid.ui.screens.welcome

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.layout.LARGE_PATTERN
import com.datathecodie.witgrid.ui.layout.WitgridLogo
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import kotlinx.coroutines.launch

private data class WelcomePage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val points: List<Int>,
)

private val welcomePages = listOf(
    WelcomePage(
        title = R.string.welcome_offline_title,
        body = R.string.welcome_offline_body,
        points = listOf(
            R.string.welcome_point_offline,
            R.string.welcome_point_daily,
            R.string.welcome_point_endless,
        ),
    ),
    WelcomePage(
        title = R.string.welcome_hints_title,
        body = R.string.welcome_hints_body,
        points = emptyList(),
    ),
    WelcomePage(
        title = R.string.welcome_ads_title,
        body = R.string.welcome_ads_body,
        points = emptyList(),
    ),
)

/** First-run pages. They show once; Skip and the last button both call [onFinish]. */
@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { welcomePages.size })
    val scope = rememberCoroutineScope()
    val colors = WitgridTheme.colors
    val lastPage = pagerState.currentPage == welcomePages.lastIndex
    AdaptiveScreen(maxContentWidth = 560.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                    Text(stringResource(R.string.welcome_skip), color = colors.mutedInk)
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { index ->
                WelcomePageContent(welcomePages[index], showLogo = index == 0)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                PageDots(count = welcomePages.size, current = pagerState.currentPage)
                Button(
                    onClick = {
                        if (lastPage) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Text(
                        stringResource(if (lastPage) R.string.welcome_start else R.string.welcome_next),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePageContent(page: WelcomePage, showLogo: Boolean) {
    val colors = WitgridTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl, Alignment.CenterVertically),
    ) {
        if (showLogo) WitgridLogo(modifier = Modifier.size(132.dp), pattern = LARGE_PATTERN)
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(page.title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(page.body),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedInk,
                textAlign = TextAlign.Center,
            )
        }
        if (page.points.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xxs),
            ) {
                page.points.forEach { point -> WelcomePoint(stringResource(point)) }
            }
        }
    }
}

@Composable
private fun WelcomePoint(text: String) {
    val colors = WitgridTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, shape)
            .border(1.dp, colors.cardEdge, shape)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(modifier = Modifier.size(8.dp).background(colors.accent, CircleShape))
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val colors = WitgridTheme.colors
    val description = stringResource(R.string.welcome_page_position, current + 1, count)
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .size(width = if (selected) 22.dp else 8.dp, height = 8.dp)
                    .background(if (selected) colors.accent else colors.cardEdge, CircleShape),
            )
        }
    }
}
