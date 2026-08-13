package com.sharedhouse.android.ui.settings

import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharedhouse.android.R
import com.sharedhouse.android.platform.notifications.LocalTestNotificationResult
import com.sharedhouse.android.platform.notifications.SharedHouseNotifications
import com.sharedhouse.android.platform.google.GoogleServicesStatus
import com.sharedhouse.android.preferences.AppLanguage
import com.sharedhouse.android.preferences.AppearanceMode
import com.sharedhouse.android.ui.app.UiMessage
import com.sharedhouse.network.AccountExportDto
import com.sharedhouse.network.AccountDto
import com.sharedhouse.android.preferences.AppPreferences
import com.sharedhouse.android.preferences.AppPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SettingsNotice(
    @StringRes val messageResource: Int,
    val isError: Boolean = false,
)

@Composable
fun SettingsRoute(
    repository: AppPreferencesRepository,
    onBack: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenLegal: () -> Unit,
    onTutorialRequested: () -> Unit,
    accountError: UiMessage?,
    accountOperationInProgress: Boolean,
    onDeleteAccount: (String) -> Unit,
    accountExport: AccountExportDto?,
    onExportAccount: (String) -> Unit,
    onAccountExportHandled: () -> Unit,
    account: AccountDto?,
    onUpdateDisplayName: (String) -> Unit,
    onChangePassword: (String,String,Boolean) -> Unit,
    onRequestEmailChange: (String,String) -> Unit,
    onConfirmEmailChange: (String) -> Unit,
    onEnableBiometric: () -> Unit,
    googleServicesStatus: GoogleServicesStatus,
    onShowAdPrivacyOptions: () -> Unit,
    modifier: Modifier = Modifier,
    onLanguageChanged: (AppLanguage) -> Unit = {},
) {
    val context = LocalContext.current
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<SettingsNotice?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { destination ->
        val content = pendingExport
        pendingExport = null
        if (destination != null && content != null) {
            try {
                context.contentResolver.openOutputStream(destination)?.bufferedWriter()?.use {
                    it.write(content)
                } ?: error("Unable to open export destination")
                notice = SettingsNotice(R.string.notice_account_export_saved)
            } catch (_: Exception) {
                notice = SettingsNotice(R.string.error_account_export_save, isError = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        SharedHouseNotifications.ensureChannels(context)
    }

    LaunchedEffect(accountExport) {
        accountExport?.let { export ->
            pendingExport = Json { prettyPrint = true }.encodeToString(export)
            onAccountExportHandled()
            exportLauncher.launch("SharedHouse-account-export.json")
        }
    }

    fun persist(update: suspend () -> Unit) {
        scope.launch {
            try {
                update()
                notice = null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                notice = SettingsNotice(R.string.settings_save_error, isError = true)
            }
        }
    }

    SettingsScreen(
        preferences = preferences,
        notice = notice,
        onBack = onBack,
        onLanguageChanged = { language ->
            persist {
                repository.setLanguage(language)
                onLanguageChanged(language)
            }
        },
        onAppearanceChanged = { mode -> persist { repository.setAppearanceMode(mode) } },
        onDynamicColorChanged = { enabled -> persist { repository.setDynamicColor(enabled) } },
        onReducedMotionChanged = { enabled -> persist { repository.setReducedMotion(enabled) } },
        onHighContrastChanged = { enabled -> persist { repository.setHighContrast(enabled) } },
        onTextScaleChanged = { scale -> persist { repository.setTextScale(scale) } },
        onNotificationCategoryChanged = { category, enabled ->
            persist { repository.setNotificationCategory(category, enabled) }
        },
        onQuietHoursEnabledChanged = { enabled -> persist { repository.setQuietHoursEnabled(enabled) } },
        onQuietHoursChanged = { start, end -> persist { repository.setQuietHours(start, end) } },
        onReminderLeadTimeChanged = { leadTime -> persist { repository.setReminderLeadTime(leadTime) } },
        onNotificationSoundChanged = { enabled -> persist { repository.setNotificationSound(enabled) } },
        onNotificationVibrationChanged = { enabled -> persist { repository.setNotificationVibration(enabled) } },
        onAnalyticsChanged = { enabled -> persist { repository.setAnalyticsEnabled(enabled) } },
        onCrashReportingChanged = { enabled -> persist { repository.setCrashReportingEnabled(enabled) } },
        onAdsChanged = { enabled -> persist { repository.setAdsEnabled(enabled) } },
        googleServicesStatus = googleServicesStatus,
        onShowAdPrivacyOptions = onShowAdPrivacyOptions,
        onSendTestNotification = {
            notice = when (SharedHouseNotifications.postLocalTest(context, preferences.notifications)) {
                LocalTestNotificationResult.POSTED -> SettingsNotice(R.string.notification_test_posted)
                LocalTestNotificationResult.RUNTIME_PERMISSION_REQUIRED -> {
                    SettingsNotice(R.string.notification_test_permission_required, isError = true)
                }
                LocalTestNotificationResult.APP_NOTIFICATIONS_BLOCKED -> {
                    SettingsNotice(R.string.notification_test_app_blocked, isError = true)
                }
                LocalTestNotificationResult.TASK_CATEGORY_DISABLED -> {
                    SettingsNotice(R.string.notification_test_category_disabled, isError = true)
                }
                LocalTestNotificationResult.TASK_CHANNEL_BLOCKED -> {
                    SettingsNotice(R.string.notification_test_channel_blocked, isError = true)
                }
            }
        },
        onOpenGuides = onOpenGuides,
        onOpenPrivacy = onOpenPrivacy,
        onOpenSecurity = onOpenSecurity,
        onOpenLegal = onOpenLegal,
        onShowTutorial = {
            persist {
                repository.showTutorialAgain()
                onTutorialRequested()
            }
        },
        accountError = accountError,
        accountOperationInProgress = accountOperationInProgress,
        onDeleteAccount = onDeleteAccount,
        onExportAccount = onExportAccount,
        account = account,
        biometricEnabled = preferences.biometricUnlockEnabled,
        onUpdateDisplayName = onUpdateDisplayName,
        onChangePassword = onChangePassword,
        onRequestEmailChange = onRequestEmailChange,
        onConfirmEmailChange = onConfirmEmailChange,
        onBiometricChanged = { enabled -> if (enabled) onEnableBiometric() else persist { repository.setBiometricUnlockEnabled(false) } },
        modifier = modifier,
    )
}
