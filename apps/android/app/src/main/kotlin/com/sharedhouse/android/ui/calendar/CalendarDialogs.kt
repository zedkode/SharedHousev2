package com.sharedhouse.android.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sharedhouse.android.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarDayAgendaSheet(
    date: LocalDate,
    events: List<CalendarEventUi>,
    isMutationInProgress: Boolean,
    canCreateEvents: Boolean,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onDetails: (CalendarEventUi) -> Unit,
    onEdit: (CalendarEventUi) -> Unit,
    onDelete: (CalendarEventUi) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.calendar_day_agenda),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = localizedDate(date, FormatStyle.FULL),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                if (canCreateEvents) {
                    IconButton(
                        onClick = onAdd,
                        enabled = !isMutationInProgress,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.calendar_add_event_on_day),
                        )
                    }
                }
            }

            if (events.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.calendar_day_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                if (canCreateEvents) {
                                    R.string.calendar_day_empty_description
                                } else {
                                    R.string.calendar_day_empty_read_only_description
                                },
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (canCreateEvents) {
                            Button(onClick = onAdd) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.calendar_add_event),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                events.forEach { event ->
                    AgendaEventCard(
                        event = event,
                        enabled = !isMutationInProgress,
                        onDetails = { onDetails(event) },
                        onEdit = { onEdit(event) },
                        onDelete = { onDelete(event) },
                    )
                }
                if (canCreateEvents) {
                    OutlinedButton(
                        onClick = onAdd,
                        enabled = !isMutationInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.calendar_add_another_event),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaEventCard(
    event: CalendarEventUi,
    enabled: Boolean,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = event.type.icon,
                    contentDescription = null,
                    tint = eventTypeColor(event.type),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = calendarEventTimeRange(event),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            event.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDetails, enabled = enabled) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Text(
                        text = stringResource(R.string.calendar_details),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                if (event.canEdit) {
                    IconButton(onClick = onEdit, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.calendar_edit_event),
                        )
                    }
                }
                if (event.canDelete) {
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.calendar_delete_event),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarEventEditorDialog(
    initialDate: LocalDate,
    event: CalendarEventUi?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CalendarEventDraft) -> Unit,
) {
    val identity = event?.id ?: "new-${initialDate}"
    var title by rememberSaveable(identity) { mutableStateOf(event?.title.orEmpty()) }
    var description by rememberSaveable(identity) { mutableStateOf(event?.description.orEmpty()) }
    var dateIso by rememberSaveable(identity) {
        mutableStateOf((event?.date ?: initialDate).toString())
    }
    var startHour by rememberSaveable(identity) {
        mutableStateOf(event?.startTime?.hour ?: 9)
    }
    var startMinute by rememberSaveable(identity) {
        mutableStateOf(event?.startTime?.minute ?: 0)
    }
    var endHour by rememberSaveable(identity) {
        mutableStateOf(event?.endTime?.hour ?: 10)
    }
    var endMinute by rememberSaveable(identity) {
        mutableStateOf(event?.endTime?.minute ?: 0)
    }
    var allDay by rememberSaveable(identity) { mutableStateOf(event?.startTime == null) }
    var typeName by rememberSaveable(identity) {
        mutableStateOf((event?.type ?: CalendarEventType.HOUSEHOLD).name)
    }
    var reminderMinutes by rememberSaveable(identity) {
        mutableStateOf(event?.reminderMinutesBefore)
    }
    var errors by remember(identity) { mutableStateOf<Set<CalendarDraftError>>(emptySet()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }

    val date = LocalDate.parse(dateIso)
    val startTime = LocalTime.of(startHour, startMinute)
    val endTime = LocalTime.of(endHour, endMinute)
    val selectedType = CalendarEventType.valueOf(typeName)

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(
                        if (event == null) {
                            R.string.calendar_new_event_title
                        } else {
                            R.string.calendar_edit_event_title
                        },
                    ),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.calendar_event_form_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(CalendarDraftValidator.MAX_TITLE_LENGTH + 1) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = CalendarDraftError.TITLE_REQUIRED in errors ||
                        CalendarDraftError.TITLE_TOO_LONG in errors,
                    label = { Text(stringResource(R.string.calendar_event_title_label)) },
                    supportingText = {
                        Text(
                            text = when {
                                CalendarDraftError.TITLE_REQUIRED in errors -> {
                                    stringResource(R.string.calendar_error_title_required)
                                }
                                CalendarDraftError.TITLE_TOO_LONG in errors -> {
                                    stringResource(R.string.calendar_error_title_too_long)
                                }
                                else -> "${title.length}/${CalendarDraftValidator.MAX_TITLE_LENGTH}"
                            },
                        )
                    },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(CalendarDraftValidator.MAX_DESCRIPTION_LENGTH + 1)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = CalendarDraftError.DESCRIPTION_TOO_LONG in errors,
                    label = { Text(stringResource(R.string.calendar_event_description_label)) },
                    supportingText = {
                        Text(
                            text = if (CalendarDraftError.DESCRIPTION_TOO_LONG in errors) {
                                stringResource(R.string.calendar_error_description_too_long)
                            } else {
                                "${description.length}/${CalendarDraftValidator.MAX_DESCRIPTION_LENGTH}"
                            },
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                )

                FormSectionLabel(stringResource(R.string.calendar_event_type_label))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CalendarEventType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { typeName = type.name },
                            enabled = !isSaving,
                            label = { Text(calendarEventTypeLabel(type)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }

                FormSectionLabel(stringResource(R.string.calendar_event_when_label))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Text(
                        text = localizedDate(date, FormatStyle.LONG),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.calendar_all_day),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.calendar_all_day_support),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = allDay,
                        onCheckedChange = { allDay = it },
                        enabled = !isSaving,
                    )
                }

                if (!allDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TimeButton(
                            label = stringResource(R.string.calendar_start_time),
                            time = startTime,
                            onClick = { timeTarget = TimeTarget.START },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                        )
                        TimeButton(
                            label = stringResource(R.string.calendar_end_time),
                            time = endTime,
                            onClick = { timeTarget = TimeTarget.END },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (CalendarDraftError.END_NOT_AFTER_START in errors) {
                        Text(
                            text = stringResource(R.string.calendar_error_end_after_start),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                FormSectionLabel(stringResource(R.string.calendar_reminder_label))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reminderOptions.forEach { option ->
                        FilterChip(
                            selected = reminderMinutes == option,
                            onClick = { reminderMinutes = option },
                            enabled = !isSaving,
                            label = { Text(reminderLabel(option)) },
                            leadingIcon = if (option == null) {
                                null
                            } else {
                                {
                                    Icon(
                                        Icons.Outlined.Alarm,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.calendar_reminder_support),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text(stringResource(R.string.calendar_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val validation = CalendarDraftValidator.validate(
                                input = CalendarDraftInput(
                                    title = title,
                                    description = description,
                                    date = date,
                                    startTime = startTime,
                                    endTime = endTime,
                                    isAllDay = allDay,
                                    type = selectedType,
                                    reminderMinutesBefore = reminderMinutes,
                                ),
                            )
                            errors = validation.errors
                            validation.draft?.let(onSubmit)
                        },
                        enabled = !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            stringResource(
                                if (event == null) {
                                    R.string.calendar_create_event
                                } else {
                                    R.string.calendar_save_event
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CalendarDatePickerDialog(
            selectedDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedDate ->
                dateIso = selectedDate.toString()
                showDatePicker = false
            },
        )
    }

    timeTarget?.let { target ->
        CalendarTimePickerDialog(
            initialTime = if (target == TimeTarget.START) startTime else endTime,
            onDismiss = { timeTarget = null },
            onConfirm = { selectedTime ->
                if (target == TimeTarget.START) {
                    startHour = selectedTime.hour
                    startMinute = selectedTime.minute
                } else {
                    endHour = selectedTime.hour
                    endMinute = selectedTime.minute
                }
                timeTarget = null
            },
        )
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun TimeButton(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = time.format(
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(currentJavaLocale()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val date = state.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    } ?: selectedDate
                    onConfirm(date)
                },
            ) {
                Text(stringResource(R.string.calendar_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.calendar_choose_time),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.calendar_cancel))
                    }
                    TextButton(
                        onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
                    ) {
                        Text(stringResource(R.string.calendar_confirm))
                    }
                }
            }
        }
    }
}

@Composable
internal fun CalendarEventDetailsDialog(
    event: CalendarEventUi,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = event.type.icon,
                contentDescription = null,
                tint = eventTypeColor(event.type),
            )
        },
        title = { Text(event.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailRow(
                    label = stringResource(R.string.calendar_event_type_label),
                    value = calendarEventTypeLabel(event.type),
                )
                DetailRow(
                    label = stringResource(R.string.calendar_event_when_label),
                    value = buildString {
                        append(localizedDate(event.date, FormatStyle.FULL))
                        append(" · ")
                        append(calendarEventTimeRange(event))
                    },
                )
                DetailRow(
                    label = stringResource(R.string.calendar_reminder_label),
                    value = reminderLabel(event.reminderMinutesBefore),
                )
                event.description?.takeIf(String::isNotBlank)?.let { description ->
                    DetailRow(
                        label = stringResource(R.string.calendar_event_description_label),
                        value = description,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_close))
            }
        },
        dismissButton = {
            if (event.canEdit || event.canDelete) {
                Row {
                    if (event.canEdit) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Text(
                                text = stringResource(R.string.calendar_edit_event),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    if (event.canDelete) {
                        TextButton(onClick = onDelete) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = stringResource(R.string.calendar_delete_event),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun CalendarDeleteConfirmationDialog(
    eventTitle: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.calendar_delete_confirmation_title)) },
        text = {
            Text(stringResource(R.string.calendar_delete_confirmation_description, eventTitle))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.calendar_delete_event))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.calendar_cancel))
            }
        },
    )
}

@Composable
internal fun calendarEventTypeLabel(type: CalendarEventType): String = stringResource(
    when (type) {
        CalendarEventType.HOUSEHOLD -> R.string.calendar_type_household
        CalendarEventType.MAINTENANCE -> R.string.calendar_type_maintenance
        CalendarEventType.APPOINTMENT -> R.string.calendar_type_appointment
        CalendarEventType.SHOPPING -> R.string.calendar_type_shopping
        CalendarEventType.OTHER -> R.string.calendar_type_other
    },
)

@Composable
internal fun calendarEventTimeRange(event: CalendarEventUi): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(currentJavaLocale())
    val start = event.startTime ?: return stringResource(R.string.calendar_all_day)
    val end = event.endTime
    return if (end == null) {
        start.format(formatter)
    } else {
        stringResource(
            R.string.calendar_time_range,
            start.format(formatter),
            end.format(formatter),
        )
    }
}

@Composable
internal fun reminderLabel(minutes: Int?): String = when (minutes) {
    null -> stringResource(R.string.calendar_reminder_none)
    0 -> stringResource(R.string.calendar_reminder_at_time)
    15 -> stringResource(R.string.calendar_reminder_15_minutes)
    60 -> stringResource(R.string.calendar_reminder_1_hour)
    1_440 -> stringResource(R.string.calendar_reminder_1_day)
    else -> stringResource(R.string.calendar_reminder_minutes, minutes)
}

private val reminderOptions = listOf(null, 0, 15, 60, 1_440)

private enum class TimeTarget {
    START,
    END,
}
