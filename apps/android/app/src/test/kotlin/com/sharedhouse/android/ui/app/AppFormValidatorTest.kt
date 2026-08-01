package com.sharedhouse.android.ui.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppFormValidatorTest {
    @Test
    fun `registration requires real identity confirmations and strong password`() {
        val errors = AppFormValidator.registration(
            AuthFormState(
                displayName = "",
                email = "not-an-email",
                password = "short",
            ),
        )

        assertEquals(UiMessage.DisplayNameRequired, errors[FormField.DisplayName])
        assertEquals(UiMessage.EmailInvalid, errors[FormField.Email])
        assertEquals(UiMessage.PasswordTooShort, errors[FormField.Password])
        assertEquals(UiMessage.AgeConfirmationRequired, errors[FormField.AgeConfirmation])
        assertEquals(UiMessage.TermsConfirmationRequired, errors[FormField.TermsConfirmation])
    }

    @Test
    fun `household accepts documented API configuration`() {
        val errors = AppFormValidator.household(
            HouseholdFormState(
                name = "Oak House",
                countryCode = "GB",
                timezone = "Europe/London",
                currency = "GBP",
                firstDayOfWeek = 1,
                cycleType = "fourteen_day",
                cycleAnchor = "2026-08-01",
            ),
        )

        assertTrue(errors.isEmpty())
    }
}

