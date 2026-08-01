package com.sharedhouse.android.ui.home

import com.sharedhouse.domain.ApiContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HomeFoundationStateTest {
    @Test
    fun `empty state does not fabricate household activity`() {
        val state = HomeFoundationState.empty()

        assertFalse(state.isHouseholdConfigured)
        assertEquals(0, state.scheduledItemCount)
        assertEquals(0, state.assignedTaskCount)
        assertEquals(0, state.pendingRequestCount)
    }

    @Test
    fun `empty state uses the shared API contract`() {
        assertEquals(ApiContract.VERSION, HomeFoundationState.empty().contractVersion)
    }
}
