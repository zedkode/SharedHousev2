package com.sharedhouse.android.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import java.time.LocalDate

/**
 * Material 3 calendar surface. It owns only transient overlay state; all calendar data and
 * mutations flow through [CalendarUiState] and [CalendarAction].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var agendaDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    var editorEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    var detailEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteEventId by rememberSaveable { mutableStateOf<String?>(null) }

    val mutationProblemMessage = state.mutationProblem?.let { problem ->
        stringResource(problem.messageResource)
    }
    LaunchedEffect(mutationProblemMessage) {
        mutationProblemMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val events = (state.content as? CalendarContent.Ready)?.events.orEmpty()
    val agendaDate = agendaDateIso?.let(LocalDate::parse)
    val agendaEvents = agendaDate?.let { date ->
        events.filter { it.occursOn(date) }.sortedWith(calendarEventOrdering)
    }.orEmpty()
    val editorEvent = editorEventId?.let { id -> events.firstOrNull { it.id == id } }
    val detailEvent = detailEventId?.let { id -> events.firstOrNull { it.id == id } }
    val deleteEvent = deleteEventId?.let { id -> events.firstOrNull { it.id == id } }
    val editorAllowed = when {
        editorDateIso == null -> false
        editorEventId == null -> state.canCreateEvents
        else -> editorEvent?.canEdit == true
    }
    val actionNotAllowedMessage = stringResource(R.string.calendar_action_not_allowed)

    LaunchedEffect(editorAllowed, editorDateIso, editorEventId, state.content) {
        if (editorDateIso != null && state.content is CalendarContent.Ready && !editorAllowed) {
            editorDateIso = null
            editorEventId = null
            snackbarHostState.showSnackbar(actionNotAllowedMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.calendar_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = calendarPeriodTitle(
                                view = state.view,
                                anchorDate = state.anchorDate,
                                firstDayOfWeek = state.firstDayOfWeek,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.content is CalendarContent.Ready && state.canCreateEvents) {
                FloatingActionButton(
                    onClick = {
                        editorDateIso = state.selectedDate.toString()
                        editorEventId = null
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.calendar_add_event),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CalendarControls(state = state, onAction = onAction)
            HorizontalDivider()
            when (val content = state.content) {
                CalendarContent.Loading -> CalendarLoadingState()
                is CalendarContent.Error -> CalendarErrorState(
                    message = content.message,
                    onRetry = { onAction(CalendarAction.Retry) },
                )

                is CalendarContent.Ready -> CalendarReadyContent(
                    state = state,
                    events = content.events,
                    onDateSelected = { date ->
                        onAction(CalendarAction.SelectDate(date))
                        agendaDateIso = date.toString()
                    },
                )
            }
        }
    }

    if (agendaDate != null) {
        CalendarDayAgendaSheet(
            date = agendaDate,
            events = agendaEvents,
            isMutationInProgress = state.isMutationInProgress,
            canCreateEvents = state.canCreateEvents,
            onDismiss = { agendaDateIso = null },
            onAdd = {
                if (state.canCreateEvents) {
                    editorDateIso = agendaDate.toString()
                    editorEventId = null
                    agendaDateIso = null
                }
            },
            onDetails = { event ->
                detailEventId = event.id
                agendaDateIso = null
            },
            onEdit = { event ->
                if (event.canEdit) {
                    editorEventId = event.id
                    editorDateIso = event.date.toString()
                    agendaDateIso = null
                }
            },
            onDelete = { event ->
                if (event.canDelete) deleteEventId = event.id
            },
        )
    }

    if (editorDateIso != null && editorAllowed) {
        CalendarEventEditorDialog(
            initialDate = LocalDate.parse(editorDateIso),
            event = editorEvent,
            isSaving = state.isMutationInProgress,
            onDismiss = {
                editorDateIso = null
                editorEventId = null
            },
            onSubmit = { draft ->
                if (editorEventId == null && state.canCreateEvents) {
                    onAction(CalendarAction.CreateEvent(draft))
                } else if (editorEvent?.canEdit == true) {
                    onAction(
                        CalendarAction.UpdateEvent(
                            eventId = editorEvent.id,
                            expectedVersion = editorEvent.version,
                            draft = draft,
                        ),
                    )
                }
                editorDateIso = null
                editorEventId = null
            },
        )
    }

    if (detailEvent != null) {
        CalendarEventDetailsDialog(
            event = detailEvent,
            onDismiss = { detailEventId = null },
            onEdit = {
                if (detailEvent.canEdit) {
                    editorEventId = detailEvent.id
                    editorDateIso = detailEvent.date.toString()
                    detailEventId = null
                }
            },
            onDelete = {
                if (detailEvent.canDelete) {
                    deleteEventId = detailEvent.id
                    detailEventId = null
                }
            },
        )
    }

    if (deleteEvent != null && deleteEvent.canDelete) {
        CalendarDeleteConfirmationDialog(
            eventTitle = deleteEvent.title,
            isDeleting = state.isMutationInProgress,
            onDismiss = { deleteEventId = null },
            onConfirm = {
                onAction(
                    CalendarAction.DeleteEvent(
                        eventId = deleteEvent.id,
                        expectedVersion = deleteEvent.version,
                    ),
                )
                deleteEventId = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarControls(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CalendarView.entries.forEachIndexed { index, view ->
                val viewDescription = stringResource(view.descriptionResource)
                SegmentedButton(
                    modifier = Modifier.semantics {
                        contentDescription = viewDescription
                    },
                    selected = state.view == view,
                    onClick = { onAction(CalendarAction.ChangeView(view)) },
                    shape = SegmentedButtonDefaults.itemShape(index, CalendarView.entries.size),
                    label = {
                        Text(
                            text = stringResource(view.labelResource),
                            maxLines = 1,
                        )
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onAction(CalendarAction.MovePeriod(CalendarPeriodDirection.PREVIOUS))
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.calendar_previous_period),
                )
            }
            TextButton(onClick = { onAction(CalendarAction.GoToToday) }) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.calendar_today),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            IconButton(
                onClick = { onAction(CalendarAction.MovePeriod(CalendarPeriodDirection.NEXT)) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.calendar_next_period),
                )
            }
        }
    }
}

@Composable
private fun CalendarLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.calendar_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarErrorState(
    message: String?,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.calendar_error_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = message?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.calendar_error_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.calendar_retry))
                }
            }
        }
    }
}

@Composable
private fun CalendarReadyContent(
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (events.isEmpty()) {
            CalendarEmptyBanner(
                canCreateEvents = state.canCreateEvents,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )
        }
        CalendarPeriodView(
            state = state,
            events = events,
            onDateSelected = onDateSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CalendarEmptyBanner(
    canCreateEvents: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.calendar_empty_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (canCreateEvents) {
                        R.string.calendar_empty_description
                    } else {
                        R.string.calendar_empty_read_only_description
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val CalendarView.labelResource: Int
    get() = when (this) {
        CalendarView.WEEK -> R.string.calendar_view_week
        CalendarView.MONTH -> R.string.calendar_view_month
        CalendarView.QUARTER -> R.string.calendar_view_quarter
        CalendarView.YEAR -> R.string.calendar_view_year
    }

private val CalendarView.descriptionResource: Int
    get() = when (this) {
        CalendarView.WEEK -> R.string.calendar_view_week_description
        CalendarView.MONTH -> R.string.calendar_view_month_description
        CalendarView.QUARTER -> R.string.calendar_view_quarter_description
        CalendarView.YEAR -> R.string.calendar_view_year_description
    }

private val CalendarMutationProblem.messageResource: Int
    get() = when (this) {
        CalendarMutationProblem.CREATE_FAILED -> R.string.calendar_create_failed
        CalendarMutationProblem.UPDATE_FAILED -> R.string.calendar_update_failed
        CalendarMutationProblem.DELETE_FAILED -> R.string.calendar_delete_failed
        CalendarMutationProblem.VERSION_CONFLICT -> R.string.calendar_version_conflict
    }

internal val calendarEventOrdering = compareBy<CalendarEventUi> { it.startTime != null }
    .thenBy { it.startTime }
    .thenBy { it.title.lowercase() }
