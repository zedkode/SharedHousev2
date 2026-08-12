package com.sharedhouse.android.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * UI-only calendar contract. Networking and persistence models should be mapped into this type at
 * the application boundary; the calendar never assumes that sample or cached events are real.
 */
data class CalendarUiState(
    val view: CalendarView = CalendarView.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = anchorDate,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val content: CalendarContent = CalendarContent.Loading,
    val canCreateEvents: Boolean = false,
    val isMutationInProgress: Boolean = false,
    /** Localizable category only; raw server titles must not be surfaced directly. */
    val mutationProblem: CalendarMutationProblem? = null,
)

enum class CalendarView {
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
}

sealed interface CalendarContent {
    data object Loading : CalendarContent

    data class Error(
        val message: String? = null,
    ) : CalendarContent

    data class Ready(
        val events: List<CalendarEventUi>,
    ) : CalendarContent
}

data class CalendarEventUi(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: CalendarEventType = CalendarEventType.OTHER,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val reminderMinutesBefore: Int? = null,
    /** Required optimistic-concurrency version returned by the authoritative service. */
    val version: Int,
    /** Derived from household role and authorship; the service still re-authorizes the request. */
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
)

enum class CalendarEventType {
    HOUSEHOLD,
    MAINTENANCE,
    APPOINTMENT,
    SHOPPING,
    MONEY,
    TASK,
    OTHER,
    ;

    val wireValue: String get() = name.lowercase()
    val userCreatable: Boolean get() = this != MONEY && this != TASK

    companion object {
        fun fromWireValue(value: String): CalendarEventType =
            entries.firstOrNull { it.wireValue == value.lowercase() } ?: OTHER
    }
}

enum class CalendarMutationProblem {
    CREATE_FAILED,
    UPDATE_FAILED,
    DELETE_FAILED,
    VERSION_CONFLICT,
}

data class CalendarEventDraft(
    val title: String,
    val description: String?,
    val type: CalendarEventType,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val reminderMinutesBefore: Int?,
)

sealed interface CalendarAction {
    data class ChangeView(val view: CalendarView) : CalendarAction

    data class MovePeriod(val direction: CalendarPeriodDirection) : CalendarAction

    data class SelectDate(val date: LocalDate) : CalendarAction

    data object GoToToday : CalendarAction

    data object Retry : CalendarAction

    data class CreateEvent(val draft: CalendarEventDraft) : CalendarAction

    data class UpdateEvent(
        val eventId: String,
        val expectedVersion: Int,
        val draft: CalendarEventDraft,
    ) : CalendarAction

    data class DeleteEvent(
        val eventId: String,
        val expectedVersion: Int,
    ) : CalendarAction

}

enum class CalendarPeriodDirection {
    PREVIOUS,
    NEXT,
}

/** Small mapping seam for an API/domain DTO without adding a networking dependency to the UI. */
fun interface CalendarEventAdapter<in T> {
    fun toCalendarEventUi(source: T): CalendarEventUi
}
