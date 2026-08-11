package com.sharedhouse.android.platform.google

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sharedhouse.android.BuildConfig
import com.sharedhouse.android.preferences.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoogleServicesStatus(
    val firebaseConfigured: Boolean = BuildConfig.FIREBASE_CONFIGURED,
    val admobConfigured: Boolean = BuildConfig.ADMOB_CONFIGURED,
    val admobTestMode: Boolean = BuildConfig.ADMOB_TEST_MODE,
    val adsReady: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val consentRequestFailed: Boolean = false,
)

/**
 * Applies optional Google service choices without attaching account, email or household identifiers.
 * Collection is disabled in the manifest and remains disabled until the user opts in locally.
 */
class GoogleServicesCoordinator(
    private val activity: Activity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val mutableStatus = MutableStateFlow(GoogleServicesStatus())
    private var consentRequestStarted = false
    private var adsInitializationStarted = false

    val status: StateFlow<GoogleServicesStatus> = mutableStatus.asStateFlow()

    fun apply(preferences: PrivacyPreferences) {
        applyFirebasePreferences(preferences)
        if (!preferences.adsEnabled || !mutableStatus.value.admobConfigured) {
            mutableStatus.value = mutableStatus.value.copy(adsReady = false)
            return
        }
        requestConsentAndPrepareAds()
    }

    fun showPrivacyOptions() {
        if (!mutableStatus.value.privacyOptionsRequired) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            updateConsentStatus(failed = formError != null)
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun applyFirebasePreferences(preferences: PrivacyPreferences) {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        val firebaseApp = FirebaseApp.getApps(activity).firstOrNull()
            ?: FirebaseApp.initializeApp(activity)
            ?: return
        FirebaseAnalytics.getInstance(firebaseApp.applicationContext)
            .setAnalyticsCollectionEnabled(preferences.analyticsEnabled)
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(preferences.crashReportingEnabled)
    }

    private fun requestConsentAndPrepareAds() {
        if (consentRequestStarted) {
            if (consentInformation.canRequestAds()) initializeAds()
            return
        }
        consentRequestStarted = true
        val request = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            request,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    updateConsentStatus(failed = formError != null)
                    if (consentInformation.canRequestAds()) initializeAds()
                }
            },
            {
                updateConsentStatus(failed = true)
                if (consentInformation.canRequestAds()) initializeAds()
            },
        )
    }

    private fun updateConsentStatus(failed: Boolean) {
        mutableStatus.value = mutableStatus.value.copy(
            adsReady = consentInformation.canRequestAds() && adsInitializationStarted,
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            consentRequestFailed = failed,
        )
    }

    private fun initializeAds() {
        if (adsInitializationStarted) {
            mutableStatus.value = mutableStatus.value.copy(adsReady = true)
            return
        }
        adsInitializationStarted = true
        scope.launch {
            MobileAds.initialize(
                activity.applicationContext,
                InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build(),
            ) {
                mutableStatus.value = mutableStatus.value.copy(adsReady = true)
            }
        }
    }
}
