package com.datathecodie.witgrid.ui.screens.settings

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.billing.BillingUiState
import com.datathecodie.witgrid.ads.flow.PremiumPurchaseControls
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.ReminderSettings
import com.datathecodie.witgrid.data.settings.ThemeMode
import com.datathecodie.witgrid.reminder.ReminderEnableResult
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen
import com.datathecodie.witgrid.ui.theme.Colourway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

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
    onHome: () -> Unit,
    premiumEnabled: Boolean = false,
    billingState: BillingUiState = BillingUiState(),
    onPurchasePremium: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
) {
    var editedSettings by remember { mutableStateOf(settings) }
    var pendingSettings by remember { mutableStateOf<AppSettings?>(null) }
    var actionError by remember { mutableStateOf<Int?>(null) }
    var informationDialog by remember { mutableStateOf<Int?>(null) }
    var showLicences by remember { mutableStateOf(false) }
    var licenceText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
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

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.screen_settings), style = MaterialTheme.typography.headlineMedium)
            SettingsSectionTitle(R.string.settings_appearance)
            ThemeMode.entries.forEach { mode ->
                RadioSetting(
                    title = stringResource(themeModeLabel(mode)),
                    selected = editedSettings.themeMode == mode,
                    onClick = { updateSettings(editedSettings.copy(themeMode = mode)) },
                )
            }
            Text(stringResource(R.string.settings_colourway), style = MaterialTheme.typography.titleMedium)
            Colourway.entries.forEach { colourway ->
                RadioSetting(
                    title = stringResource(colourwayLabel(colourway)),
                    selected = editedSettings.colourwayId == colourway.id,
                    onClick = { updateSettings(editedSettings.copy(colourwayId = colourway.id)) },
                )
            }

            HorizontalDivider()
            SettingsSectionTitle(R.string.settings_assists)
            SwitchSetting(
                title = stringResource(R.string.settings_crowns_region_aids),
                checked = editedSettings.crownsShowRegionAids,
                onCheckedChange = { updateSettings(editedSettings.copy(crownsShowRegionAids = it)) },
            )
            SwitchSetting(
                title = stringResource(R.string.settings_crowns_auto_x),
                checked = editedSettings.crownsAutoXEnabled,
                onCheckedChange = { updateSettings(editedSettings.copy(crownsAutoXEnabled = it)) },
            )
            SwitchSetting(
                title = stringResource(R.string.settings_nonogram_auto_cross),
                checked = editedSettings.nonogramAutoCrossEnabled,
                onCheckedChange = { updateSettings(editedSettings.copy(nonogramAutoCrossEnabled = it)) },
            )

            HorizontalDivider()
            SettingsSectionTitle(R.string.settings_play)
            SwitchSetting(
                title = stringResource(R.string.settings_mistake_limit),
                checked = editedSettings.mistakeLimitEnabled,
                onCheckedChange = { updateSettings(editedSettings.copy(mistakeLimitEnabled = it)) },
            )
            SwitchSetting(
                title = stringResource(R.string.settings_haptics),
                checked = editedSettings.hapticsEnabled,
                onCheckedChange = { updateSettings(editedSettings.copy(hapticsEnabled = it)) },
            )
            SwitchSetting(
                title = stringResource(R.string.settings_sound),
                checked = editedSettings.soundEnabled,
                onCheckedChange = { updateSettings(editedSettings.copy(soundEnabled = it)) },
            )
            HorizontalDivider()
            SettingsSectionTitle(R.string.settings_accessibility)
            SwitchSetting(
                title = stringResource(R.string.settings_reduce_motion),
                checked = editedSettings.reduceMotion,
                onCheckedChange = { updateSettings(editedSettings.copy(reduceMotion = it)) },
            )

            HorizontalDivider()
            SettingsSectionTitle(R.string.settings_reminder)
            SwitchSetting(
                title = stringResource(R.string.settings_daily_reminder),
                checked = reminder.enabled,
                onCheckedChange = { enabled ->
                    if (enabled) enableReminder()
                    else scope.launch {
                        runCatching { onDisableReminder() }
                            .onFailure { exception ->
                                if (exception is CancellationException) throw exception
                                actionError = R.string.settings_action_failed
                            }
                    }
                },
            )
            ActionSetting(
                title = stringResource(R.string.settings_reminder_time),
                value = remember(configuration, reminder.hourOfDay, reminder.minute) {
                    formattedTime(context, reminder.hourOfDay, reminder.minute)
                },
                onClick = ::showTimePicker,
            )
            (actionError ?: reminderActionMessage)?.let { resourceId ->
                Text(stringResource(resourceId), color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider()
            SettingsSectionTitle(R.string.settings_account)
            PremiumPurchaseControls(
                premiumEnabled = premiumEnabled,
                billingState = billingState,
                onPurchasePremium = onPurchasePremium,
                onRestorePurchases = onRestorePurchases,
            )

            HorizontalDivider()
            SettingsSectionTitle(R.string.settings_privacy_and_licences)
            ActionSetting(
                title = stringResource(R.string.settings_privacy_policy),
                value = stringResource(
                    if (privacyPolicyUri != null) R.string.settings_open
                    else R.string.settings_privacy_policy_pending,
                ),
                onClick = {
                    if (privacyPolicyUri != null) {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, privacyPolicyUri),
                            )
                        } catch (_: ActivityNotFoundException) {
                            informationDialog = R.string.settings_policy_open_failed
                        }
                    } else {
                        informationDialog = R.string.settings_privacy_policy_pending
                    }
                },
            )
            if (privacyOptionsAvailable) {
                ActionSetting(
                    title = stringResource(R.string.settings_privacy_options),
                    value = stringResource(R.string.settings_open),
                    onClick = {
                        scope.launch {
                            runCatching { onShowPrivacyOptions() }
                                .onFailure { exception ->
                                    if (exception is CancellationException) throw exception
                                    actionError = R.string.settings_action_failed
                                }
                        }
                    },
                )
            }
            ActionSetting(
                title = stringResource(R.string.settings_licences),
                value = stringResource(R.string.settings_open),
                onClick = {
                    licenceText = null
                    showLicences = true
                },
            )
            Button(onClick = onHome) { Text(stringResource(R.string.button_home)) }
        }
    }
}

@Composable
private fun SettingsSectionTitle(resourceId: Int) {
    Text(stringResource(resourceId), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun RadioSetting(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun ActionSetting(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
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
