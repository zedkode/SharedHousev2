package com.sharedhouse.android.ui.home

import com.sharedhouse.domain.ApiContract

/**
 * Honest initial state shown before identity and household setup are connected.
 *
 * No monetary or household records are synthesized in this state.
 */
data class HomeFoundationState(
    val contractVersion: String,
    val isHouseholdConfigured: Boolean,
    val scheduledItemCount: Int,
    val assignedTaskCount: Int,
    val pendingRequestCount: Int,
    val householdName: String? = null,
    val accountDisplayName: String? = null,
) {
    companion object {
        fun empty(): HomeFoundationState = HomeFoundationState(
            contractVersion = ApiContract.VERSION,
            isHouseholdConfigured = false,
            scheduledItemCount = 0,
            assignedTaskCount = 0,
            pendingRequestCount = 0,
        )

        fun authenticated(
            householdName: String,
            accountDisplayName: String,
        ): HomeFoundationState = HomeFoundationState(
            contractVersion = ApiContract.VERSION,
            isHouseholdConfigured = true,
            scheduledItemCount = 0,
            assignedTaskCount = 0,
            pendingRequestCount = 0,
            householdName = householdName,
            accountDisplayName = accountDisplayName,
        )
    }
}
