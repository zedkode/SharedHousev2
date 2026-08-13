package com.sharedhouse.android

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharedhouse.android.platform.notifications.SharedHouseNotifications
import com.sharedhouse.android.platform.google.GoogleServicesCoordinator
import com.sharedhouse.android.platform.security.AndroidKeystoreSessionStore
import com.sharedhouse.android.platform.security.requestBiometricUnlock
import com.sharedhouse.android.preferences.AppLanguage
import com.sharedhouse.android.preferences.AppearanceMode
import com.sharedhouse.android.preferences.AppPreferences
import com.sharedhouse.android.preferences.AppPreferencesRepository
import com.sharedhouse.android.ui.app.ApiSharedHouseGateway
import com.sharedhouse.android.ui.app.HouseholdFormState
import com.sharedhouse.android.ui.app.SharedHouseApp
import com.sharedhouse.android.ui.app.SharedHouseViewModel
import com.sharedhouse.android.ui.startup.SharedHouseStartupScreen
import com.sharedhouse.android.ui.startup.StartupCopyKind
import com.sharedhouse.android.ui.security.BiometricLockScreen
import com.sharedhouse.android.ui.theme.SharedHouseTheme
import com.sharedhouse.android.ui.tutorial.TutorialRoute
import com.sharedhouse.network.SharedHouseApiClient
import com.sharedhouse.network.createSharedHouseHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var lastBackgroundAt: Long? = null
    private var biometricUnlocked by mutableStateOf(false)
    private val gateway by lazy {
        ApiSharedHouseGateway(
            SharedHouseApiClient(
                client = createSharedHouseHttpClient(OkHttp.create()),
                baseUrl = BuildConfig.API_BASE_URL,
            ),
        )
    }
    private val preferencesRepository by lazy { AppPreferencesRepository(applicationContext) }
    private val sessionStore by lazy { AndroidKeystoreSessionStore(applicationContext) }
    private val googleServicesCoordinator by lazy { GoogleServicesCoordinator(this) }
    private val nullablePreferences by lazy {
        preferencesRepository.preferences.map<AppPreferences, AppPreferences?> { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SharedHouseNotifications.ensureChannels(this)

        setContent {
            val preferenceSnapshot by nullablePreferences.collectAsStateWithLifecycle(initialValue = null)

            if (preferenceSnapshot == null) {
                SharedHouseTheme {
                    SharedHouseStartupScreen(
                        copyKind = StartupCopyKind.RESOLVING_SESSION,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                return@setContent
            }

            val preferences = requireNotNull(preferenceSnapshot)
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (preferences.appearanceMode) {
                AppearanceMode.SYSTEM -> systemDark
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            val googleServicesStatus by googleServicesCoordinator.status.collectAsStateWithLifecycle()
            val systemDensity = LocalDensity.current
            val scaledDensity = remember(systemDensity, preferences.textScale) {
                Density(
                    density = systemDensity.density,
                    fontScale = systemDensity.fontScale * preferences.textScale.multiplier,
                )
            }

            LaunchedEffect(preferences.language) {
                applyAppLanguage(preferences.language)
            }
            LaunchedEffect(preferences.privacy) {
                googleServicesCoordinator.apply(preferences.privacy)
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                SharedHouseTheme(
                    darkTheme = useDarkTheme,
                    dynamicColor = preferences.dynamicColor,
                    highContrast = preferences.highContrast,
                    reducedMotion = preferences.reducedMotion,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        if (preferences.biometricUnlockEnabled && !biometricUnlocked) {
                            BiometricLockScreen(
                                onUnlock = { promptForBiometric() },
                                onUsePassword = {
                                    scope.launch {
                                        sessionStore.clear()
                                        preferencesRepository.setBiometricUnlockEnabled(false)
                                        biometricUnlocked = true
                                    }
                                },
                            )
                            LaunchedEffect(Unit) { promptForBiometric() }
                            return@Box
                        }
                        if (!preferences.tutorialCompleted) {
                            TutorialRoute(
                                repository = preferencesRepository,
                                onFinished = {},
                            )
                        } else {
                            val appViewModel: SharedHouseViewModel = viewModel(
                                factory = sharedHouseViewModelFactory(),
                            )
                            val locale = LocalConfiguration.current.locales[0]
                            LaunchedEffect(locale.language) {
                                appViewModel.updatePreferredLocale(locale.language)
                            }
                            SharedHouseApp(
                                viewModel = appViewModel,
                                preferencesRepository = preferencesRepository,
                                appPreferences = preferences,
                                googleServicesStatus = googleServicesStatus,
                                onShowAdPrivacyOptions = googleServicesCoordinator::showPrivacyOptions,
                                onLanguageChanged = ::applyAppLanguage,
                                onEnableBiometric = {
                                    requestBiometricUnlock(
                                        title=getString(R.string.biometric_prompt_title),
                                        subtitle=getString(R.string.biometric_prompt_subtitle),
                                        onSuccess={ scope.launch { preferencesRepository.setBiometricUnlockEnabled(true); biometricUnlocked=true } },
                                        onUnavailable={},
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        lastBackgroundAt = System.currentTimeMillis()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (lastBackgroundAt?.let { System.currentTimeMillis() - it >= 120_000L } == true) biometricUnlocked = false
    }

    private fun promptForBiometric() {
        requestBiometricUnlock(
            title = getString(R.string.biometric_prompt_title),
            subtitle = getString(R.string.biometric_prompt_subtitle),
            onSuccess = { biometricUnlocked = true },
            onUnavailable = {},
        )
    }

    override fun onDestroy() {
        googleServicesCoordinator.close()
        super.onDestroy()
    }

    private fun applyAppLanguage(language: AppLanguage) {
        val desiredTags = language.languageTag.orEmpty()
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == desiredTags) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(desiredTags))
    }

    private fun sharedHouseViewModelFactory(): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SharedHouseViewModel::class.java))
                val locale = resources.configuration.locales[0] ?: Locale.getDefault()
                return SharedHouseViewModel(
                    gateway = gateway,
                    sessionStore = sessionStore,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80),
                    preferredLocale = if (locale.language == "ro") "ro" else "en",
                    initialHousehold = defaultHouseholdForm(locale),
                ) as T
            }
        }

    private fun defaultHouseholdForm(locale: Locale): HouseholdFormState {
        val countryCode = locale.country.takeIf { it.length == 2 }?.uppercase() ?: "GB"
        val currency = runCatching { Currency.getInstance(locale).currencyCode }
            .getOrDefault(if (countryCode == "RO") "RON" else "GBP")
        val firstDay = WeekFields.of(locale).firstDayOfWeek.value.takeIf { it in setOf(1, 6, 7) } ?: 1
        return HouseholdFormState(
            countryCode = countryCode,
            timezone = ZoneId.systemDefault().id,
            currency = currency,
            firstDayOfWeek = firstDay,
            cycleAnchor = LocalDate.now().toString(),
        )
    }
}
