package com.sharedhouse.android.platform.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val Authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
    BiometricManager.Authenticators.DEVICE_CREDENTIAL

fun biometricAvailability(context: Context): Int =
    BiometricManager.from(context).canAuthenticate(Authenticators)

fun FragmentActivity.requestBiometricUnlock(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onUnavailable: (Int) -> Unit,
) {
    val availability = biometricAvailability(this)
    if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
        onUnavailable(availability)
        return
    }
    BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT || errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) onUnavailable(errorCode)
            }
        },
    ).authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(Authenticators)
            .build(),
    )
}
