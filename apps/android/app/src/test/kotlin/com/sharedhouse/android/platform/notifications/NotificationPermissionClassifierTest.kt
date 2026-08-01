package com.sharedhouse.android.platform.notifications

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationPermissionClassifierTest {
    @Test
    fun `missing runtime permission wins over app enabled state`() {
        assertEquals(
            NotificationPermissionStatus.NOT_GRANTED,
            classifyNotificationPermission(
                runtimePermissionRequired = true,
                runtimePermissionGranted = false,
                appNotificationsEnabled = false,
            ),
        )
    }

    @Test
    fun `system block is reported after runtime permission is granted`() {
        assertEquals(
            NotificationPermissionStatus.BLOCKED_BY_SYSTEM,
            classifyNotificationPermission(
                runtimePermissionRequired = true,
                runtimePermissionGranted = true,
                appNotificationsEnabled = false,
            ),
        )
    }

    @Test
    fun `enabled notifications are granted`() {
        assertEquals(
            NotificationPermissionStatus.GRANTED,
            classifyNotificationPermission(
                runtimePermissionRequired = true,
                runtimePermissionGranted = true,
                appNotificationsEnabled = true,
            ),
        )
    }
}
