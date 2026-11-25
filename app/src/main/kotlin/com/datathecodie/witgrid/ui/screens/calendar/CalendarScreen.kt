package com.datathecodie.witgrid.ui.screens.calendar

import android.icu.text.DateFormatSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.ads.flow.RewardedActionButton
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletion
import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.domain.daily.DailyLocalDate
import com.datathecodie.witgrid.domain.daily.DailyProgressSnapshot
import com.datathecodie.witgrid.ui.components.ListRow
import com.datathecodie.witgrid.ui.components.SectionHeader
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun CalendarScreenRoute(
    dailyProgress: DailyProgressSnapshot?,
    completionRepository: DailyPuzzleCompletionRepository,
    gameIds: List<String>,
    onOpenDailyPuzzle: (String) -> Unit,
    premiumEnabled: Boolean = false,
    streakRepairCreditAvailable: Boolean = false,
    onRepairStreak: suspend () -> AdShowResult = { AdShowResult.Unavailable },
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
) {
    if (dailyProgress == null) {
        AdaptiveScreen {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val timeZone = remember(dailyProgress.today.timeZoneId) {
        TimeZone.getTimeZone(dailyProgress.today.timeZoneId)
    }
    val locale = requireNotNull(LocalConfiguration.current.locales[0])
    val currentMonthStart = remember(dailyProgress.today.epochDay, timeZone.id, locale) {
        monthStartEpochDay(dailyProgress.today.epochDay, timeZone, locale)
    }
    var visibleMonthStart by rememberSaveable(dailyProgress.today.timeZoneId) {
        mutableLongStateOf(currentMonthStart)
    }
    var previousCurrentMonthStart by rememberSaveable(dailyProgress.today.timeZoneId) {
        mutableLongStateOf(currentMonthStart)
    }
    var selectedEpochDay by rememberSaveable(dailyProgress.today.timeZoneId) {
        mutableLongStateOf(dailyProgress.today.epochDay)
    }
    var previousTodayEpochDay by rememberSaveable(dailyProgress.today.timeZoneId) {
        mutableLongStateOf(dailyProgress.today.epochDay)
    }
    LaunchedEffect(currentMonthStart, dailyProgress.today.epochDay) {
        val wasViewingCurrentMonth = visibleMonthStart == previousCurrentMonthStart
        if (currentMonthStart != previousCurrentMonthStart && wasViewingCurrentMonth) {
            visibleMonthStart = currentMonthStart
            selectedEpochDay = dailyProgress.today.epochDay
        } else if (selectedEpochDay == previousTodayEpochDay || selectedEpochDay > dailyProgress.today.epochDay) {
            selectedEpochDay = dailyProgress.today.epochDay
        }
        previousCurrentMonthStart = currentMonthStart
        previousTodayEpochDay = dailyProgress.today.epochDay
    }
    val monthRange = remember(visibleMonthStart, timeZone.id, locale) {
        monthRange(visibleMonthStart, timeZone, locale)
    }
    val completions by completionRepository.observeBetween(monthRange.first, monthRange.second)
        .collectAsState(initial = emptyList())

    CalendarScreen(
        todayEpochDay = dailyProgress.today.epochDay,
        timeZone = timeZone,
        locale = locale,
        visibleMonthStartEpochDay = visibleMonthStart,
        selectedEpochDay = selectedEpochDay,
        streak = dailyProgress.streak,
        repairAvailable = dailyProgress.repairableMiss != null,
        premiumEnabled = premiumEnabled,
        streakRepairCreditAvailable = streakRepairCreditAvailable,
        onRepairStreak = onRepairStreak,
        billingState = billingState,
        onPurchasePremium = onPurchasePremium,
        completions = completions,
        gameIds = gameIds,
        canAdvanceMonth = visibleMonthStart < currentMonthStart,
        onPreviousMonth = {
            visibleMonthStart = shiftMonth(visibleMonthStart, -1, timeZone, locale)
            selectedEpochDay = visibleMonthStart
        },
        onNextMonth = {
            if (visibleMonthStart < currentMonthStart) {
                visibleMonthStart = shiftMonth(visibleMonthStart, 1, timeZone, locale)
                selectedEpochDay = if (visibleMonthStart == currentMonthStart) {
                    dailyProgress.today.epochDay
                } else {
                    visibleMonthStart
                }
            }
        },
        onSelectDay = { selectedEpochDay = it },
        onOpenDailyPuzzle = onOpenDailyPuzzle,
    )
}

@Composable
fun CalendarScreen(
    todayEpochDay: Long,
    timeZone: TimeZone,
    locale: Locale,
    visibleMonthStartEpochDay: Long,
    selectedEpochDay: Long,
    streak: StreakData,
    repairAvailable: Boolean,
    premiumEnabled: Boolean,
    streakRepairCreditAvailable: Boolean,
    billingState: BillingUiState,
    onPurchasePremium: () -> Unit,
    completions: List<DailyPuzzleCompletion>,
    gameIds: List<String>,
    canAdvanceMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onOpenDailyPuzzle: (String) -> Unit,
    onRepairStreak: suspend () -> AdShowResult,
) {
    var showPremiumOffer by rememberSaveable { mutableStateOf(false) }
    if (showPremiumOffer) {
        PremiumOfferDialog(
            premiumEnabled = premiumEnabled,
            billingState = billingState,
            onPurchasePremium = onPurchasePremium,
            onDismiss = { showPremiumOffer = false },
        )
    }

    val completedByDay = completions.groupBy { it.dateEpochDay }
        .mapValues { (_, rows) -> rows.map { it.gameId }.toSet() }
    val visibleMonth = calendarForEpochDay(visibleMonthStartEpochDay, timeZone, locale)
    val selectedDate = calendarForEpochDay(selectedEpochDay, timeZone, locale)
    val selectedGames = completedByDay[selectedEpochDay].orEmpty()
    val isToday = selectedEpochDay == todayEpochDay

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.calendar_streak_status, streak.currentDays, streak.freezesAvailable),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    if (repairAvailable) R.string.calendar_repair_available else R.string.calendar_repair_unavailable,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (repairAvailable) {
                RewardedActionButton(
                    titleResource = when {
                        premiumEnabled -> R.string.calendar_repair_streak_premium
                        streakRepairCreditAvailable -> R.string.calendar_repair_streak_credit
                        else -> R.string.calendar_repair_streak_watch_ad
                    },
                    successResource = R.string.calendar_repair_streak_done,
                    onReward = onRepairStreak,
                    onOpenPremiumOffer = { showPremiumOffer = true },
                    alwaysShowPremiumOffer = !premiumEnabled,
                )
            }

            WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val previousMonthLabel = stringResource(R.string.calendar_previous_month)
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.semantics { contentDescription = previousMonthLabel },
                    ) {
                        Text(stringResource(R.string.calendar_previous_month_symbol))
                    }
                    Text(
                        SimpleDateFormat("LLLL yyyy", locale).apply { this.timeZone = timeZone }
                            .format(visibleMonth.time),
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val nextMonthLabel = stringResource(R.string.calendar_next_month)
                    IconButton(
                        onClick = onNextMonth,
                        enabled = canAdvanceMonth,
                        modifier = Modifier.semantics { contentDescription = nextMonthLabel },
                    ) {
                        Text(stringResource(R.string.calendar_next_month_symbol))
                    }
                }

                MonthGrid(
                    visibleMonthStartEpochDay = visibleMonthStartEpochDay,
                    todayEpochDay = todayEpochDay,
                    selectedEpochDay = selectedEpochDay,
                    timeZone = timeZone,
                    locale = locale,
                    completedByDay = completedByDay,
                    onSelectDay = onSelectDay,
                )

            }
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { this.timeZone = timeZone }
                    .format(selectedDate.time),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                pluralStringResource(R.plurals.calendar_completed_count, selectedGames.size, selectedGames.size),
                style = MaterialTheme.typography.bodyMedium,
                color = WitgridTheme.colors.mutedInk,
            )

            if (isToday) {
                SectionHeader(stringResource(R.string.calendar_today_puzzles))
                WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    gameIds.forEach { gameId ->
                        ListRow(
                            title = stringResource(gameNameResource(gameId)),
                            subtitle = stringResource(if (gameId in selectedGames) R.string.calendar_solved else R.string.calendar_ready),
                            onClick = { onOpenDailyPuzzle(gameId) },
                        )
                    }
                }
            } else if (selectedGames.isNotEmpty()) {
                selectedGames.sorted().forEach { gameId ->
                    Text(stringResource(gameNameResource(gameId)), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(stringResource(R.string.calendar_no_completions), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    visibleMonthStartEpochDay: Long,
    todayEpochDay: Long,
    selectedEpochDay: Long,
    timeZone: TimeZone,
    locale: Locale,
    completedByDay: Map<Long, Set<String>>,
    onSelectDay: (Long) -> Unit,
) {
    val firstDayOfMonth = calendarForEpochDay(visibleMonthStartEpochDay, timeZone, locale).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val weekdaySymbols = DateFormatSymbols(locale)
    val abbreviatedWeekdays = weekdaySymbols.getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.ABBREVIATED)
    val narrowWeekdays = weekdaySymbols.getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.NARROW)
    val fullWeekdays = weekdaySymbols.getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.WIDE)
    val weekdays = (0 until DAYS_PER_WEEK).map { offset ->
        (firstDayOfMonth.firstDayOfWeek - 1 + offset) % DAYS_PER_WEEK + 1
    }
    val monthDays = monthDates(firstDayOfMonth, timeZone)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val useNarrowWeekdays = LocalDensity.current.fontScale >= 1.8f && maxWidth / DAYS_PER_WEEK < 52.dp
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { weekday ->
                    Text(
                        if (useNarrowWeekdays) narrowWeekdays[weekday] else abbreviatedWeekdays[weekday],
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = fullWeekdays[weekday]
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            monthDays.chunked(DAYS_PER_WEEK).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { epochDay ->
                        if (epochDay == null) {
                            Text("", modifier = Modifier.weight(1f).heightIn(min = DAY_CELL_MIN_HEIGHT))
                        } else {
                            val completedCount = completedByDay[epochDay].orEmpty().size
                            val canSelect = epochDay <= todayEpochDay
                            val dayNumber = calendarForEpochDay(epochDay, timeZone, locale)
                                .get(Calendar.DAY_OF_MONTH)
                            val status = when {
                                completedCount > 0 -> R.string.calendar_day_done
                                epochDay < todayEpochDay -> R.string.calendar_day_missed
                                else -> R.string.calendar_day_ready
                            }
                            val description = stringResource(R.string.calendar_day_accessibility, dayNumber,
                                stringResource(status), completedCount)
                            val colors = WitgridTheme.colors
                            Column(
                                modifier = Modifier.weight(1f).heightIn(min = DAY_CELL_MIN_HEIGHT)
                                    .background(
                                        if (epochDay == selectedEpochDay) colors.accent.copy(alpha = 0.15f)
                                        else colors.card,
                                        CircleShape,
                                    )
                                    .clickable(enabled = canSelect, role = Role.Button) { onSelectDay(epochDay) }
                                    .semantics(mergeDescendants = true) { contentDescription = description }
                                    .padding(vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(dayNumber.toString(), color = if (canSelect) colors.ink else colors.mutedInk.copy(alpha = 0.5f))
                                Text(stringResource(when {
                                    completedCount > 0 -> R.string.calendar_done_mark
                                    epochDay < todayEpochDay -> R.string.calendar_missed_mark
                                    else -> R.string.calendar_empty_mark
                                }), color = if (completedCount > 0) colors.accent else colors.mutedInk,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun monthDates(firstDayOfMonth: Calendar, timeZone: TimeZone): List<Long?> {
    val firstEpochDay = DailyLocalDate.epochDayAt(firstDayOfMonth.timeInMillis, timeZone)
    val leadingEmptyCells = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - firstDayOfMonth.firstDayOfWeek + DAYS_PER_WEEK)
        .rem(DAYS_PER_WEEK)
    val numberOfDays = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = buildList<Long?> {
        repeat(leadingEmptyCells) { add(null) }
        repeat(numberOfDays) { dayOffset -> add(firstEpochDay + dayOffset) }
    }
    val trailingEmptyCells = (DAYS_PER_WEEK - cells.size % DAYS_PER_WEEK) % DAYS_PER_WEEK
    return cells + List(trailingEmptyCells) { null }
}

private fun monthStartEpochDay(epochDay: Long, timeZone: TimeZone, locale: Locale): Long =
    calendarForEpochDay(epochDay, timeZone, locale).apply { set(Calendar.DAY_OF_MONTH, 1) }
        .let { DailyLocalDate.epochDayAt(it.timeInMillis, timeZone) }

private fun monthRange(monthStartEpochDay: Long, timeZone: TimeZone, locale: Locale): Pair<Long, Long> {
    val month = calendarForEpochDay(monthStartEpochDay, timeZone, locale).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val first = DailyLocalDate.epochDayAt(month.timeInMillis, timeZone)
    month.set(Calendar.DAY_OF_MONTH, month.getActualMaximum(Calendar.DAY_OF_MONTH))
    return first to DailyLocalDate.epochDayAt(month.timeInMillis, timeZone)
}

private fun shiftMonth(monthStartEpochDay: Long, months: Int, timeZone: TimeZone, locale: Locale): Long {
    val month = calendarForEpochDay(monthStartEpochDay, timeZone, locale).apply { set(Calendar.DAY_OF_MONTH, 1) }
    month.add(Calendar.MONTH, months)
    return DailyLocalDate.epochDayAt(month.timeInMillis, timeZone)
}

private fun calendarForEpochDay(epochDay: Long, timeZone: TimeZone, locale: Locale): Calendar {
    val utc = Calendar.getInstance(UTC, Locale.ROOT).apply {
        timeInMillis = epochDay * MILLIS_PER_DAY
    }
    return Calendar.getInstance(timeZone, locale).apply {
        clear()
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            12,
            0,
            0,
        )
    }
}

private fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    "skyscrapers" -> R.string.game_skyscrapers
    else -> R.string.game_unknown
}

private const val DAYS_PER_WEEK = 7
private val DAY_CELL_MIN_HEIGHT = 52.dp
private const val MILLIS_PER_DAY = 86_400_000L
private val UTC = TimeZone.getTimeZone("UTC")
