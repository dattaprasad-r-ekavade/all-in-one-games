package com.datathecodie.witgrid.ui.screens.settings

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ads.flow.PremiumOfferDialog
import com.datathecodie.witgrid.billing.BillingNotice
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.ReminderSettings
import com.datathecodie.witgrid.data.settings.ThemeMode
import com.datathecodie.witgrid.reminder.ReminderEnableResult
import com.datathecodie.witgrid.ui.components.ListRow
import com.datathecodie.witgrid.ui.components.SectionHeader
import com.datathecodie.witgrid.ui.components.SegmentedControl
import com.datathecodie.witgrid.ui.components.WitgridCard
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.theme.Colourway
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: AppSettings,
    reminder: ReminderSettings,
    reminderActionMessage: Int?,
    privacyOptionsAvailable: Boolean,
    onUpdateSettings: suspend (AppSettings) -> Unit,
    onEnableReminder: suspend () -> ReminderEnableResult,
    onDisableReminder: suspend () -> Unit,
    onUpdateReminderTime: suspend (Int, Int) -> Unit,
    onShowPrivacyOptions: suspend () -> Unit,
    premiumEnabled: Boolean = false,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
) {
    var editedSettings by remember { mutableStateOf(settings) }
    var pendingSettings by remember { mutableStateOf<AppSettings?>(null) }
    var actionError by remember { mutableStateOf<Int?>(null) }
    var informationDialog by remember { mutableStateOf<Int?>(null) }
    var showPremium by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showLicences by remember { mutableStateOf(false) }
    var licenceText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val reminderTimeLabel = stringResource(R.string.settings_reminder_time)
    val licenceUnavailable = stringResource(R.string.settings_licence_unavailable)
    val privacyPolicyUrl = stringResource(R.string.settings_privacy_policy_url).trim()
    val privacyPolicyUri = remember(privacyPolicyUrl) {
        runCatching { privacyPolicyUrl.toUri() }
            .getOrNull()
            ?.takeIf { it.scheme == "https" && !it.host.isNullOrBlank() }
    }
    val scope = rememberCoroutineScope()
    val settingsWriteMutex = remember { Mutex() }

    LaunchedEffect(settings) {
        val pending = pendingSettings
        when {
            pending == null -> editedSettings = settings
            pending == settings -> {
                editedSettings = settings
                pendingSettings = null
            }
        }
    }

    fun updateSettings(updated: AppSettings) {
        editedSettings = updated
        pendingSettings = updated
        actionError = null
        scope.launch {
            runCatching { settingsWriteMutex.withLock { onUpdateSettings(updated) } }
                .onFailure { exception ->
                    if (exception is CancellationException) throw exception
                    actionError = R.string.settings_action_failed
                    if (pendingSettings == updated) {
                        pendingSettings = null
                        editedSettings = settings
                    }
                }
        }
    }

    fun enableReminder() {
        actionError = null
        scope.launch {
            try {
                onEnableReminder()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                actionError = R.string.settings_action_failed
            }
        }
    }

    fun showTimePicker() {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                scope.launch {
                    runCatching { onUpdateReminderTime(hour, minute) }
                        .onFailure { exception ->
                            if (exception is CancellationException) throw exception
                            actionError = R.string.settings_action_failed
                        }
                }
            },
            reminder.hourOfDay,
            reminder.minute,
            DateFormat.is24HourFormat(context),
        )
        dialog.setTitle(reminderTimeLabel)
        dialog.show()
    }

    if (informationDialog != null) {
        AlertDialog(
            onDismissRequest = { informationDialog = null },
            confirmButton = {
                TextButton(onClick = { informationDialog = null }) {
                    Text(stringResource(R.string.button_close))
                }
            },
            text = { Text(stringResource(requireNotNull(informationDialog))) },
        )
    }

    if (showLicences) {
        LaunchedEffect(Unit) {
            licenceText = withContext(Dispatchers.IO) {
                readInterFontLicence(context, licenceUnavailable)
            }
        }
        AlertDialog(
            onDismissRequest = { showLicences = false },
            title = { Text(stringResource(R.string.settings_licences)) },
            text = {
                Text(
                    licenceText ?: stringResource(R.string.settings_licence_loading),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicences = false }) {
                    Text(stringResource(R.string.button_close))
                }
            },
        )
    }

    if (showPremium) {
        PremiumOfferDialog(premiumEnabled, billingState, onPurchasePremium, { showPremium = false })
    }
    if (showReminder) {
        val onReminderEnabledChange: (Boolean) -> Unit = { enabled ->
            if (enabled) enableReminder() else scope.launch {
                runCatching { onDisableReminder() }.onFailure { exception ->
                    if (exception is CancellationException) throw exception
                    actionError = R.string.settings_action_failed
                }
            }
        }
        val reminderTime = formattedTime(context, reminder.hourOfDay, reminder.minute)
        AlertDialog(
            onDismissRequest = { showReminder = false },
            title = { Text(stringResource(R.string.settings_reminder)) },
            text = {
                Column {
                    ListRow(
                        title = stringResource(R.string.settings_daily_reminder),
                        checked = reminder.enabled,
                        onCheckedChange = onReminderEnabledChange,
                    )
                    if (LocalDensity.current.fontScale >= 1.8f) {
                        ListRow(
                            title = stringResource(R.string.settings_reminder_time),
                            subtitle = reminderTime,
                            onClick = ::showTimePicker,
                        )
                    } else {
                        ListRow(
                            title = stringResource(R.string.settings_reminder_time),
                            trailingValue = reminderTime,
                            onClick = ::showTimePicker,
                        )
                    }
                    (actionError ?: reminderActionMessage)?.let { Text(stringResource(it)) }
                }
            },
            confirmButton = { TextButton(onClick = { showReminder = false }) { Text(stringResource(R.string.button_close)) } },
        )
    }
    AdaptiveScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(stringResource(R.string.settings_appearance))
            WitgridCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                SegmentedControl(ThemeMode.entries.map { stringResource(themeModeLabel(it)) },
                    ThemeMode.entries.indexOf(editedSettings.themeMode),
                    { index -> updateSettings(editedSettings.copy(themeMode = ThemeMode.entries[index])) },
                    Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                val currentColourway = Colourway.entries.firstOrNull { it.id == editedSettings.colourwayId } ?: Colourway.INDIGO
                Text(stringResource(R.string.settings_colourway_selected, stringResource(colourwayLabel(currentColourway))),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Colourway.entries.forEach { colourway ->
                        ColourwaySwatch(colourway, editedSettings.colourwayId == colourway.id) {
                            updateSettings(editedSettings.copy(colourwayId = colourway.id))
                        }
                    }
                }
            }
            SectionHeader(stringResource(R.string.settings_assists))
            WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                AssistSetting(R.string.settings_crowns_region_aids, R.string.settings_region_aids_subtitle,
                    editedSettings.crownsShowRegionAids) { updateSettings(editedSettings.copy(crownsShowRegionAids = it)) }
                AssistSetting(R.string.settings_crowns_auto_x, R.string.settings_auto_x_subtitle,
                    editedSettings.crownsAutoXEnabled) { updateSettings(editedSettings.copy(crownsAutoXEnabled = it)) }
                AssistSetting(R.string.settings_nonogram_auto_cross, R.string.settings_auto_cross_subtitle,
                    editedSettings.nonogramAutoCrossEnabled) { updateSettings(editedSettings.copy(nonogramAutoCrossEnabled = it)) }
                AssistSetting(R.string.settings_auto_notes, R.string.settings_auto_notes_subtitle,
                    editedSettings.autoNotesEnabled) { updateSettings(editedSettings.copy(autoNotesEnabled = it)) }
                AssistSetting(R.string.settings_mistake_limit, R.string.settings_mistake_subtitle,
                    editedSettings.mistakeLimitEnabled) { updateSettings(editedSettings.copy(mistakeLimitEnabled = it)) }
                AssistSetting(R.string.settings_haptics, R.string.settings_haptics_subtitle,
                    editedSettings.hapticsEnabled) { updateSettings(editedSettings.copy(hapticsEnabled = it)) }
                AssistSetting(R.string.settings_reduce_motion, R.string.settings_motion_subtitle,
                    editedSettings.reduceMotion) { updateSettings(editedSettings.copy(reduceMotion = it)) }
            }
            SectionHeader(stringResource(R.string.settings_more))
            WitgridCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                ListRow(stringResource(R.string.settings_reminder),
                    trailingValue = if (reminder.enabled) formattedTime(context, reminder.hourOfDay, reminder.minute)
                        else stringResource(R.string.settings_off), onClick = { showReminder = true })
                SettingsDivider()
                ListRow(stringResource(R.string.home_premium_row),
                    trailingValue = if (premiumEnabled) stringResource(R.string.settings_premium_active) else billingState.productPrice,
                    onClick = { showPremium = true })
                SettingsDivider()
                ListRow(stringResource(R.string.settings_restore_purchases), onClick = onRestorePurchases,
                    enabled = !billingState.isLoading && !billingState.isProcessing)
                SettingsDivider()
                ListRow(stringResource(R.string.settings_privacy_policy), onClick = {
                    if (privacyPolicyUri != null) {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, privacyPolicyUri)) }
                        catch (_: ActivityNotFoundException) { informationDialog = R.string.settings_policy_open_failed }
                    } else informationDialog = R.string.settings_privacy_policy_pending
                })
                if (privacyOptionsAvailable) {
                    SettingsDivider()
                    ListRow(stringResource(R.string.settings_privacy_options), onClick = {
                        scope.launch {
                            runCatching { onShowPrivacyOptions() }.onFailure { exception ->
                                if (exception is CancellationException) throw exception
                                actionError = R.string.settings_action_failed
                            }
                        }
                    })
                }
                SettingsDivider()
                ListRow(stringResource(R.string.settings_licences), onClick = { licenceText = null; showLicences = true })
            }
            billingNoticeResource(billingState.notice)?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            (actionError ?: reminderActionMessage)?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ColourwaySwatch(colourway: Colourway, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(colourwayLabel(colourway))
    val ink = WitgridTheme.colors.ink
    val accent = if (WitgridTheme.colors.isDark) colourway.darkAccent else colourway.lightAccent
    Canvas(Modifier.size(48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = label }) {
        drawCircle(Color(0xFF000000 or accent.toLong()), radius = size.minDimension * 0.34f)
        if (selected) drawCircle(ink, radius = size.minDimension * 0.46f, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun AssistSetting(title: Int, subtitle: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListRow(stringResource(title), subtitle = stringResource(subtitle), checked = checked, onCheckedChange = onChange)
    SettingsDivider()
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = WitgridTheme.colors.cardEdge)
}

private fun billingNoticeResource(notice: BillingNotice): Int? = when (notice) {
    BillingNotice.NONE -> null
    BillingNotice.STORE_UNAVAILABLE -> R.string.premium_store_unavailable
    BillingNotice.PRODUCT_UNAVAILABLE -> R.string.premium_product_unavailable
    BillingNotice.PURCHASE_PENDING -> R.string.premium_purchase_pending
    BillingNotice.PURCHASE_CANCELLED -> R.string.premium_purchase_cancelled
    BillingNotice.PURCHASE_COMPLETE -> R.string.premium_purchase_complete
    BillingNotice.PURCHASE_FAILED -> R.string.premium_purchase_failed
    BillingNotice.RESTORE_COMPLETE -> R.string.premium_restore_complete
    BillingNotice.RESTORE_EMPTY -> R.string.premium_restore_empty
    BillingNotice.RESTORE_FAILED -> R.string.premium_restore_failed
}

private fun themeModeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

private fun colourwayLabel(colourway: Colourway): Int = when (colourway) {
    Colourway.INDIGO -> R.string.settings_colourway_indigo
    Colourway.FOREST -> R.string.settings_colourway_forest
    Colourway.PLUM -> R.string.settings_colourway_plum
    Colourway.RUST -> R.string.settings_colourway_rust
    Colourway.SLATE -> R.string.settings_colourway_slate
}

private fun formattedTime(context: Context, hour: Int, minute: Int): String {
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(time.time)
}

private fun readInterFontLicence(context: Context, unavailableText: String): String = try {
    context.assets.open("licences/inter-OFL.txt").bufferedReader().use { it.readText() }
} catch (_: Exception) {
    unavailableText
}
