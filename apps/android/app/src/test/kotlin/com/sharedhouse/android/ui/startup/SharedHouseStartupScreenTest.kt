package com.sharedhouse.android.ui.startup

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedHouseStartupScreenTest {
    @Test
    fun `restoring session takes priority over an existing name`() {
        assertEquals(
            StartupCopyKind.RESOLVING_SESSION,
            resolveStartupCopyKind(isRestoringSession = true, displayName = "Andrei"),
        )
    }

    @Test
    fun `authenticated session uses the personalised welcome`() {
        assertEquals(
            StartupCopyKind.AUTHENTICATED,
            resolveStartupCopyKind(isRestoringSession = false, displayName = "Andrei"),
        )
    }

    @Test
    fun `missing account name uses the guest welcome`() {
        assertEquals(
            StartupCopyKind.GUEST,
            resolveStartupCopyKind(isRestoringSession = false, displayName = "  "),
        )
        assertEquals(
            StartupCopyKind.GUEST,
            resolveStartupCopyKind(isRestoringSession = false, displayName = null),
        )
    }
}
