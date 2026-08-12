package com.sharedhouse.android.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Remove
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.AlertDialog
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.FilterChip
import com.sharedhouse.android.ui.atmosphere.HorizontalDivider
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.atmosphere.OutlinedTextField
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Switch
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TextButton
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.platform.notifications.NotificationPermissionExplainer
import com.sharedhouse.android.platform.google.GoogleServicesStatus
import com.sharedhouse.android.preferences.AppLanguage
import com.sharedhouse.android.preferences.AppPreferences
import com.sharedhouse.android.preferences.AppearanceMode
import com.sharedhouse.android.preferences.NotificationCategory
import com.sharedhouse.android.preferences.ReminderLeadTime
import com.sharedhouse.android.preferences.TextScale
import com.sharedhouse.android.preferences.adjustedQuietTime
import com.sharedhouse.android.preferences.formatMinutesOfDay
import com.sharedhouse.android.ui.app.UiMessage
import com.sharedhouse.android.ui.components.localized

private data class Choice<T>(
    val value: T,
    @StringRes val label: Int,
)

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    notice: SettingsNotice?,
    onBack: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onAppearanceChanged: (AppearanceMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReducedMotionChanged: (Boolean) -> Unit,
    onHighContrastChanged: (Boolean) -> Unit,
    onTextScaleChanged: (TextScale) -> Unit,
    onNotificationCategoryChanged: (NotificationCategory, Boolean) -> Unit,
    onQuietHoursEnabledChanged: (Boolean) -> Unit,
    onQuietHoursChanged: (Int, Int) -> Unit,
    onReminderLeadTimeChanged: (ReminderLeadTime) -> Unit,
    onNotificationSoundChanged: (Boolean) -> Unit,
    onNotificationVibrationChanged: (Boolean) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onCrashReportingChanged: (Boolean) -> Unit,
    onAdsChanged: (Boolean) -> Unit,
    googleServicesStatus: GoogleServicesStatus,
    onShowAdPrivacyOptions: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenLegal: () -> Unit,
    onShowTutorial: () -> Unit,
    accountError: UiMessage?,
    accountOperationInProgress: Boolean,
    onDeleteAccount: (String) -> Unit,
    onExportAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notifications = preferences.notifications
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletionPassword by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AtmosphereTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_heading),
                        style = AtmosphereTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.settings_description),
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                        style = AtmosphereTheme.typography.bodyLarge,
                    )
                }
            }

            if (notice != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notice.isError) {
                                AtmosphereTheme.colorScheme.errorContainer
                            } else {
                                AtmosphereTheme.colorScheme.primaryContainer
                            },
                        ),
                    ) {
                        Text(
                            text = stringResource(notice.messageResource),
                            modifier = Modifier.padding(16.dp),
                            color = if (notice.isError) {
                                AtmosphereTheme.colorScheme.onErrorContainer
                            } else {
                                AtmosphereTheme.colorScheme.onPrimaryContainer
                            },
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Outlined.Palette,
                    title = R.string.settings_appearance_title,
                    description = R.string.settings_appearance_description,
                ) {
                    ChoiceRow(
                        choices = listOf(
                            Choice(AppearanceMode.SYSTEM, R.string.settings_appearance_system),
                            Choice(AppearanceMode.LIGHT, R.string.settings_appearance_light),
                            Choice(AppearanceMode.DARK, R.string.settings_appearance_dark),
                        ),
                        selected = preferences.appearanceMode,
                        onSelected = onAppearanceChanged,
                    )
                    Text(
                        text = stringResource(R.string.settings_atmospheric_theme_description),
                        style = AtmosphereTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_atmospheric_theme_note),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Outlined.Language,
                    title = R.string.settings_language_title,
                    description = R.string.settings_language_description,
                ) {
                    ChoiceRow(
                        choices = listOf(
                            Choice(AppLanguage.SYSTEM, R.string.settings_language_system),
                            Choice(AppLanguage.ENGLISH, R.string.settings_language_english),
                            Choice(AppLanguage.ROMANIAN, R.string.settings_language_romanian),
                        ),
                        selected = preferences.language,
                        onSelected = onLanguageChanged,
                    )
                    Text(
                        text = stringResource(R.string.settings_language_apply_note),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Outlined.AccessibilityNew,
                    title = R.string.settings_accessibility_title,
                    description = R.string.settings_accessibility_description,
                ) {
                    ToggleSetting(
                        title = R.string.settings_reduced_motion,
                        description = R.string.settings_reduced_motion_description,
                        checked = preferences.reducedMotion,
                        onCheckedChange = onReducedMotionChanged,
                    )
                    HorizontalDivider()
                    ToggleSetting(
                        title = R.string.settings_high_contrast,
                        description = R.string.settings_high_contrast_description,
                        checked = preferences.highContrast,
                        onCheckedChange = onHighContrastChanged,
                    )
                    HorizontalDivider()
                    SettingLabel(R.string.settings_text_size)
                    ChoiceRow(
                        choices = listOf(
                            Choice(TextScale.STANDARD, R.string.settings_text_standard),
                            Choice(TextScale.LARGE, R.string.settings_text_large),
                            Choice(TextScale.EXTRA_LARGE, R.string.settings_text_extra_large),
                        ),
                        selected = preferences.textScale,
                        onSelected = onTextScaleChanged,
                    )
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Outlined.PrivacyTip,
                    title = R.string.settings_optional_data_title,
                    description = R.string.settings_optional_data_description,
                ) {
                    ServiceStatusRow(
                        icon = if (googleServicesStatus.firebaseConfigured) {
                            Icons.Outlined.CloudDone
                        } else {
                            Icons.Outlined.CloudOff
                        },
                        title = R.string.settings_firebase_status,
                        status = if (googleServicesStatus.firebaseConfigured) {
                            R.string.settings_service_configured
                        } else {
                            R.string.settings_service_not_configured
                        },
                    )
                    ServiceStatusRow(
                        icon = if (googleServicesStatus.admobConfigured) {
                            Icons.Outlined.CloudDone
                        } else {
                            Icons.Outlined.CloudOff
                        },
                        title = R.string.settings_admob_status,
                        status = when {
                            !googleServicesStatus.admobConfigured -> R.string.settings_service_not_configured
                            googleServicesStatus.admobTestMode -> R.string.settings_service_test_mode
                            else -> R.string.settings_service_configured
                        },
                    )
                    HorizontalDivider()
                    ToggleSetting(
                        title = R.string.settings_analytics,
                        description = R.string.settings_analytics_description,
                        checked = preferences.privacy.analyticsEnabled,
                        onCheckedChange = onAnalyticsChanged,
                        enabled = googleServicesStatus.firebaseConfigured,
                    )
                    HorizontalDivider()
                    ToggleSetting(
                        title = R.string.settings_crash_reporting,
                        description = R.string.settings_crash_reporting_description,
                        checked = preferences.privacy.crashReportingEnabled,
                        onCheckedChange = onCrashReportingChanged,
                        enabled = googleServicesStatus.firebaseConfigured,
                    )
                    HorizontalDivider()
                    ToggleSetting(
                        title = R.string.settings_ads,
                        description = R.string.settings_ads_description,
                        checked = preferences.privacy.adsEnabled,
                        onCheckedChange = onAdsChanged,
                        enabled = googleServicesStatus.admobConfigured,
                    )
                    if (googleServicesStatus.consentRequestFailed) {
                        Text(
                            text = stringResource(R.string.settings_ad_consent_error),
                            style = AtmosphereTheme.typography.bodySmall,
                            color = AtmosphereTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = onShowAdPrivacyOptions,
                        enabled = googleServicesStatus.privacyOptionsRequired,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_ad_privacy_options))
                    }
                    Text(
                        text = stringResource(R.string.settings_optional_data_note),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Outlined.Notifications,
                    title = R.string.settings_notifications_title,
                    description = R.string.settings_notifications_description,
                ) {
                    NotificationPermissionExplainer()
                    Text(
                        text = stringResource(R.string.notification_categories_heading),
                        style = AtmosphereTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    NotificationCategory.entries.forEachIndexed { index, category ->
                        NotificationCategorySetting(
                            category = category,
                            checked = notifications.isEnabled(category),
                            onCheckedChange = { enabled ->
                                onNotificationCategoryChanged(category, enabled)
                            },
                        )
                        if (index != NotificationCategory.entries.lastIndex) HorizontalDivider()
                    }
                    HorizontalDivider()
                    SettingLabel(R.string.notification_lead_time)
                    ChoiceRow(
                        choices = listOf(
                            Choice(ReminderLeadTime.AT_TIME, R.string.notification_lead_at_time),
                            Choice(ReminderLeadTime.FIFTEEN_MINUTES, R.string.notification_lead_15_minutes),
                            Choice(ReminderLeadTime.ONE_HOUR, R.string.notification_lead_one_hour),
                            Choice(ReminderLeadTime.ONE_DAY, R.string.notification_lead_one_day),
                        ),
                        selected = notifications.leadTime,
                        onSelected = onReminderLeadTimeChanged,
                    )
                    Text(
                        text = stringResource(R.string.notification_lead_time_note),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    ToggleSetting(
                        title = R.string.notification_sound,
                        description = R.string.notification_sound_description,
                        checked = notifications.sound,
                        onCheckedChange = onNotificationSoundChanged,
                    )
                    ToggleSetting(
                        title = R.string.notification_vibration,
                        description = R.string.notification_vibration_description,
                        checked = notifications.vibration,
                        onCheckedChange = onNotificationVibrationChanged,
                    )
                    Text(
                        text = stringResource(R.string.notification_channel_authority_note),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    ToggleSetting(
                        title = R.string.notification_quiet_hours,
                        description = R.string.notification_quiet_hours_description,
                        checked = notifications.quietHours.enabled,
                        onCheckedChange = onQuietHoursEnabledChanged,
                    )
                    if (notifications.quietHours.enabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            QuietTimeControl(
                                label = R.string.notification_quiet_start,
                                value = notifications.quietHours.startMinutes,
                                otherValue = notifications.quietHours.endMinutes,
                                onValueChange = { start ->
                                    onQuietHoursChanged(start, notifications.quietHours.endMinutes)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            QuietTimeControl(
                                label = R.string.notification_quiet_end,
                                value = notifications.quietHours.endMinutes,
                                otherValue = notifications.quietHours.startMinutes,
                                onValueChange = { end ->
                                    onQuietHoursChanged(notifications.quietHours.startMinutes, end)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.notification_test_explanation),
                        style = AtmosphereTheme.typography.bodyMedium,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onSendTestNotification,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_send_test))
                    }
                }
            }

            item {
                SettingsSection(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = R.string.settings_help_title,
                    description = R.string.settings_help_description,
                ) {
                    NavigationSetting(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = R.string.settings_guides,
                        description = R.string.settings_guides_description,
                        onClick = onOpenGuides,
                    )
                    HorizontalDivider()
                    NavigationSetting(
                        icon = Icons.Outlined.Download,
                        title = R.string.settings_export_account,
                        description = R.string.settings_export_account_description,
                        onClick = { showExportDialog = true },
                    )
                    HorizontalDivider()
                    NavigationSetting(
                        icon = Icons.Outlined.AutoAwesome,
                        title = R.string.settings_show_tutorial,
                        description = R.string.settings_show_tutorial_description,
                        onClick = onShowTutorial,
                    )
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Outlined.Lock,
                    title = R.string.settings_trust_title,
                    description = R.string.settings_trust_description,
                ) {
                    NavigationSetting(
                        icon = Icons.Outlined.PrivacyTip,
                        title = R.string.settings_privacy,
                        description = R.string.settings_privacy_description,
                        onClick = onOpenPrivacy,
                    )
                    HorizontalDivider()
                    NavigationSetting(
                        icon = Icons.Outlined.Lock,
                        title = R.string.settings_security,
                        description = R.string.settings_security_description,
                        onClick = onOpenSecurity,
                    )
                    HorizontalDivider()
                    NavigationSetting(
                        icon = Icons.Outlined.Description,
                        title = R.string.settings_legal,
                        description = R.string.settings_legal_description,
                        onClick = onOpenLegal,
                    )
                    HorizontalDivider()
                    NavigationSetting(
                        icon = Icons.Outlined.DeleteForever,
                        title = R.string.settings_delete_account,
                        description = R.string.settings_delete_account_description,
                        onClick = { showDeleteDialog = true },
                    )
                    accountError?.let {
                        Text(
                            text = it.localized(),
                            style = AtmosphereTheme.typography.bodySmall,
                            color = AtmosphereTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!accountOperationInProgress) showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_delete_account_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_delete_account_confirm_description))
                    OutlinedTextField(
                        value = deletionPassword,
                        onValueChange = { deletionPassword = it },
                        label = { Text(stringResource(R.string.settings_current_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !accountOperationInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount(deletionPassword)
                        deletionPassword = ""
                        showDeleteDialog = false
                    },
                    enabled = deletionPassword.isNotBlank() && !accountOperationInProgress,
                ) { Text(stringResource(R.string.settings_delete_permanently)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !accountOperationInProgress,
                ) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!accountOperationInProgress) showExportDialog = false },
            icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_export_account_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_export_account_explanation))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text(stringResource(R.string.settings_current_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !accountOperationInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExportAccount(exportPassword)
                        exportPassword = ""
                        showExportDialog = false
                    },
                    enabled = exportPassword.isNotBlank() && !accountOperationInProgress,
                ) { Text(stringResource(R.string.settings_export_choose_location)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    enabled = !accountOperationInProgress,
                ) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes description: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AtmosphereTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(title),
                        style = AtmosphereTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(description),
                        style = AtmosphereTheme.typography.bodyMedium,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = AtmosphereTheme.typography.titleSmall,
        color = AtmosphereTheme.colorScheme.onSurface,
    )
}

@Composable
private fun <T> ChoiceRow(
    choices: List<Choice<T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(choices, key = { it.value.toString() }) { choice ->
            FilterChip(
                selected = choice.value == selected,
                onClick = { onSelected(choice.value) },
                label = { Text(stringResource(choice.label)) },
            )
        }
    }
}

@Composable
private fun ToggleSetting(
    @StringRes title: Int,
    @StringRes description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = stringResource(title), style = AtmosphereTheme.typography.bodyLarge)
            Text(
                text = stringResource(description),
                style = AtmosphereTheme.typography.bodySmall,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ServiceStatusRow(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes status: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AtmosphereTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(title), style = AtmosphereTheme.typography.bodyLarge)
            Text(
                text = stringResource(status),
                style = AtmosphereTheme.typography.labelMedium,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationCategorySetting(
    category: NotificationCategory,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val title = when (category) {
        NotificationCategory.CHAT -> R.string.notification_category_chat
        NotificationCategory.MONEY -> R.string.notification_category_money
        NotificationCategory.TASKS -> R.string.notification_category_tasks
        NotificationCategory.REQUESTS -> R.string.notification_category_requests
        NotificationCategory.ANNOUNCEMENTS -> R.string.notification_category_announcements
        NotificationCategory.SECURITY -> R.string.notification_category_security
        NotificationCategory.SUBSCRIPTION -> R.string.notification_category_subscription
    }
    val description = when (category) {
        NotificationCategory.CHAT -> R.string.notification_category_chat_description
        NotificationCategory.MONEY -> R.string.notification_category_money_description
        NotificationCategory.TASKS -> R.string.notification_category_tasks_description
        NotificationCategory.REQUESTS -> R.string.notification_category_requests_description
        NotificationCategory.ANNOUNCEMENTS -> R.string.notification_category_announcements_description
        NotificationCategory.SECURITY -> R.string.notification_category_security_description
        NotificationCategory.SUBSCRIPTION -> R.string.notification_category_subscription_description
    }
    ToggleSetting(title, description, checked, onCheckedChange)
}

@Composable
private fun QuietTimeControl(
    @StringRes label: Int,
    value: Int,
    otherValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = stringResource(label), style = AtmosphereTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick = { onValueChange(adjustedQuietTime(value, -30, otherValue)) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.notification_time_earlier),
                )
            }
            Text(
                text = formatMinutesOfDay(value),
                style = AtmosphereTheme.typography.titleMedium,
            )
            IconButton(
                onClick = { onValueChange(adjustedQuietTime(value, 30, otherValue)) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.notification_time_later),
                )
            }
        }
    }
}

@Composable
private fun NavigationSetting(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes description: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AtmosphereTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = stringResource(title), style = AtmosphereTheme.typography.bodyLarge)
            Text(
                text = stringResource(description),
                style = AtmosphereTheme.typography.bodySmall,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AtmosphereTheme.colorScheme.onSurfaceVariant,
        )
    }
}
