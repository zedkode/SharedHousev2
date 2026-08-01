package com.sharedhouse.android.ui.app

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Currency

object AppFormValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun registration(form: AuthFormState): Map<FormField, UiMessage> = buildMap {
        if (form.displayName.trim().isEmpty() || form.displayName.trim().length > 80) {
            put(FormField.DisplayName, UiMessage.DisplayNameRequired)
        }
        if (!emailPattern.matches(form.email.trim())) {
            put(FormField.Email, UiMessage.EmailInvalid)
        }
        when {
            form.password.isEmpty() -> put(FormField.Password, UiMessage.PasswordRequired)
            form.password.length < 15 -> put(FormField.Password, UiMessage.PasswordTooShort)
        }
        if (!form.ageConfirmed) {
            put(FormField.AgeConfirmation, UiMessage.AgeConfirmationRequired)
        }
        if (!form.termsAccepted) {
            put(FormField.TermsConfirmation, UiMessage.TermsConfirmationRequired)
        }
    }

    fun signIn(form: AuthFormState): Map<FormField, UiMessage> = buildMap {
        if (!emailPattern.matches(form.email.trim())) {
            put(FormField.Email, UiMessage.EmailInvalid)
        }
        if (form.password.isEmpty()) {
            put(FormField.Password, UiMessage.PasswordRequired)
        }
    }

    fun verification(form: AuthFormState): Map<FormField, UiMessage> = buildMap {
        if (!Regex("^[0-9]{8}$").matches(form.verificationCode.trim())) {
            put(FormField.VerificationCode, UiMessage.VerificationCodeInvalidInput)
        }
    }

    fun household(form: HouseholdFormState): Map<FormField, UiMessage> = buildMap {
        if (form.name.trim().isEmpty() || form.name.trim().length > 100) {
            put(FormField.HouseholdName, UiMessage.HouseholdNameRequired)
        }
        if (!Regex("^[A-Za-z]{2}$").matches(form.countryCode.trim())) {
            put(FormField.CountryCode, UiMessage.CountryCodeInvalid)
        }
        if (form.timezone !in ZoneId.getAvailableZoneIds()) {
            put(FormField.Timezone, UiMessage.TimezoneInvalid)
        }
        val currencyValid = runCatching {
            Currency.getInstance(form.currency.trim().uppercase())
        }.isSuccess
        if (!currencyValid) {
            put(FormField.Currency, UiMessage.CurrencyInvalid)
        }
        try {
            LocalDate.parse(form.cycleAnchor.trim())
        } catch (_: DateTimeParseException) {
            put(FormField.CycleAnchor, UiMessage.CycleAnchorInvalid)
        }
    }
}

