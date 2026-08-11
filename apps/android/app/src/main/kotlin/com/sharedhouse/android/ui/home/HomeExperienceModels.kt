package com.sharedhouse.android.ui.home

import com.sharedhouse.android.ui.calendar.CalendarEventUi

/**
 * Authenticated dashboard data supplied by the application layer.
 *
 * This type deliberately contains no placeholder totals. Calendar events must come from the
 * authoritative household calendar flow before [DashboardCalendarContent.Ready] is emitted.
 */
data class HouseholdDashboardUiModel(
    val householdName: String,
    val accountDisplayName: String,
    val calendar: DashboardCalendarContent,
    val tasks: DashboardTasksContent,
)

sealed interface DashboardTasksContent {
    data object Loading : DashboardTasksContent
    data object Error : DashboardTasksContent
    data class Ready(val nextMineTitle: String?, val activeCount: Int, val pendingRequests: Int) : DashboardTasksContent
}

sealed interface DashboardCalendarContent {
    data object Loading : DashboardCalendarContent

    data object Error : DashboardCalendarContent

    data class Ready(
        val events: List<CalendarEventUi>,
    ) : DashboardCalendarContent
}

/** Authoritative household configuration projected for the household hub. */
data class HouseholdHubUiModel(
    val householdName: String,
    val accountDisplayName: String,
    val countryCode: String,
    val timezone: String,
    val currencyCode: String,
    val firstDayOfWeek: Int,
    val cycleType: String,
    val cycleAnchor: String,
    val householdRole: String,
    val membershipStatus: String,
    val households: List<HouseholdOptionUi>,
    val memberState: HouseholdMembersUiState,
)

data class HouseholdMembersUiState(
    val content: HouseholdMembersContent = HouseholdMembersContent.Loading,
    val mutatingMembershipId: String? = null,
    val problem: HouseholdMembersProblem? = null,
)

sealed interface HouseholdMembersContent {
    data object Loading : HouseholdMembersContent
    data object Error : HouseholdMembersContent
    data class Ready(
        val canInvite: Boolean,
        val canEditHousehold: Boolean,
        val members: List<HouseholdMemberUi>,
    ) : HouseholdMembersContent
}

data class HouseholdMemberUi(
    val membershipId: String,
    val displayName: String,
    val role: String,
    val status: String,
    val isCurrentUser: Boolean,
    val canChangeRole: Boolean,
    val canSuspend: Boolean,
    val canReactivate: Boolean,
    val canRemove: Boolean,
    val canTransferOwnership: Boolean,
    val assignableRoles: List<String>,
    val joinedAt: String,
    val version: Int,
)

data class HouseholdMemberCommand(
    val membershipId: String,
    val expectedVersion: Int,
    val action: String,
    val role: String? = null,
)

enum class HouseholdMembersProblem { LOAD_FAILED, ACTION_FAILED, VERSION_CONFLICT }

data class HouseholdOptionUi(
    val id: String,
    val name: String,
    val role: String,
    val selected: Boolean,
)

enum class UnavailableHouseholdFeature {
    MONEY,
    TASKS,
}
