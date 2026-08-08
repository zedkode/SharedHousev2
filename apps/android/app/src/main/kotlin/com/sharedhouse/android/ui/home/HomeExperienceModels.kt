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
)

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
)

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
