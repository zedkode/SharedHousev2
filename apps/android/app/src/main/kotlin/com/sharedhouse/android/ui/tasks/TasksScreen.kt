@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sharedhouse.android.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TasksScreen(state: TasksUiState, onAction: (TasksAction) -> Unit, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.MY_TASKS.name) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var command by remember { mutableStateOf<Pair<HouseholdTaskUi, TaskCommand>?>(null) }
    var requestDecision by remember { mutableStateOf<Triple<HouseholdTaskUi, TaskRequestUi, Boolean>?>(null) }
    val message = state.problem?.let { stringResource(it.messageRes) }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it) } }
    val tasks = (state.content as? TasksContent.Ready)?.tasks.orEmpty()
    val filter = runCatching { TaskFilter.valueOf(filterName) }.getOrDefault(TaskFilter.MY_TASKS)
    val visible = tasks.filter {
        when (filter) {
            TaskFilter.MY_TASKS -> it.isMine && it.status in setOf(TaskStatus.OPEN, TaskStatus.IN_PROGRESS)
            TaskFilter.ACTIVE -> it.status in setOf(TaskStatus.OPEN, TaskStatus.IN_PROGRESS)
            TaskFilter.REQUESTS -> it.requests.any { request -> request.status == TaskRequestStatus.PENDING }
            TaskFilter.COMPLETED -> it.status == TaskStatus.COMPLETED
            TaskFilter.ALL -> true
        }
    }
    val selected = tasks.firstOrNull { it.id == selectedId }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Column { Text(stringResource(R.string.tasks_title)); Text(stringResource(R.string.tasks_subtitle), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.canCreate && state.content is TasksContent.Ready) ExtendedFloatingActionButton(
                onClick = { createOpen = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(stringResource(R.string.tasks_add)) },
            )
        },
    ) { padding ->
        when (state.content) {
            TasksContent.Loading -> Column(Modifier.fillMaxSize().padding(padding), Arrangement.Center, Alignment.CenterHorizontally) { CircularProgressIndicator() }
            TasksContent.Error -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp)); Text(stringResource(R.string.tasks_load_failed), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp)); Button(onClick = { onAction(TasksAction.Retry) }) { Text(stringResource(R.string.action_retry)) }
            }
            is TasksContent.Ready -> LazyColumn(
                Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { TaskSummary(tasks) }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskFilter.entries.forEach { option -> FilterChip(selected = option == filter, onClick = { filterName = option.name }, label = { Text(stringResource(option.labelRes)) }) }
                    }
                }
                if (visible.isEmpty()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Outlined.Assignment, null); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.tasks_empty_title), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(if (state.canCreate) R.string.tasks_empty_admin else R.string.tasks_empty_member), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(visible, key = HouseholdTaskUi::id) { task -> TaskCard(task) { selectedId = task.id } }
            }
        }
    }

    if (createOpen) TaskCreateDialog(state.members, state.isMutationInProgress, onDismiss = { createOpen = false }) { draft -> createOpen = false; onAction(TasksAction.Create(draft)) }
    if (selected != null) TaskDetailsSheet(
        task = selected, members = state.members, busy = state.isMutationInProgress, onDismiss = { selectedId = null },
        onCommand = { command = selected to it }, onDecision = { request, approve -> requestDecision = Triple(selected, request, approve) },
    )
    command?.let { (task, taskCommand) -> TaskActionDialog(task, taskCommand, state.members, onDismiss = { command = null }) { draft -> command = null; onAction(TasksAction.Execute(task.id, task.version, draft)) } }
    requestDecision?.let { (task, request, approve) -> DecisionDialog(approve, onDismiss = { requestDecision = null }) { note -> requestDecision = null; onAction(TasksAction.Execute(task.id, task.version, TaskCommandDraft(if (approve) TaskCommand.APPROVE_REQUEST else TaskCommand.REJECT_REQUEST, note, request.id))) } }
}

@Composable
private fun TaskSummary(tasks: List<HouseholdTaskUi>) {
    val mine = tasks.count { it.isMine && it.status in setOf(TaskStatus.OPEN, TaskStatus.IN_PROGRESS) }
    val overdue = tasks.count { it.status in setOf(TaskStatus.OPEN, TaskStatus.IN_PROGRESS) && it.dueDate < LocalDate.now() }
    val requests = tasks.sumOf { it.requests.count { request -> request.status == TaskRequestStatus.PENDING } }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryTile(Icons.AutoMirrored.Outlined.Assignment, stringResource(R.string.tasks_summary_mine), mine, Modifier.weight(1f))
        SummaryTile(Icons.Outlined.Flag, stringResource(R.string.tasks_summary_overdue), overdue, Modifier.weight(1f))
        SummaryTile(Icons.Outlined.PendingActions, stringResource(R.string.tasks_summary_requests), requests, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(12.dp)) { Icon(icon, null); Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelMedium) } }
}

@Composable
private fun TaskCard(task: HouseholdTaskUi, onClick: () -> Unit) {
    val overdue = task.status in setOf(TaskStatus.OPEN, TaskStatus.IN_PROGRESS) && task.dueDate < LocalDate.now()
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = if (task.isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(task.assigneeDisplayName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                AssistChip(onClick = onClick, label = { Text(stringResource(task.status.labelRes)) }, leadingIcon = { Icon(if (task.status == TaskStatus.COMPLETED) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.Assignment, null) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatDue(task), color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal)
                task.zone?.let { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (task.requests.any { it.status == TaskRequestStatus.PENDING }) Text(stringResource(R.string.tasks_pending_count, task.requests.count { it.status == TaskRequestStatus.PENDING }), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TaskDetailsSheet(task: HouseholdTaskUi, members: List<TaskMemberUi>, busy: Boolean, onDismiss: () -> Unit, onCommand: (TaskCommand) -> Unit, onDecision: (TaskRequestUi, Boolean) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(stringResource(R.string.tasks_assigned_to, task.assigneeDisplayName)); Text(formatDue(task)); task.zone?.let { Text(stringResource(R.string.tasks_zone, it)) }; task.estimatedMinutes?.let { Text(stringResource(R.string.tasks_estimate, it)) }; task.instructions?.let { Spacer(Modifier.height(4.dp)); Text(it) } }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.canStart) Button(onClick = { onCommand(TaskCommand.START) }, enabled = !busy) { Icon(Icons.Outlined.PlayArrow, null); Text(stringResource(R.string.tasks_start)) }
                    if (task.canComplete) Button(onClick = { onCommand(TaskCommand.COMPLETE) }, enabled = !busy) { Icon(Icons.Outlined.CheckCircle, null); Text(stringResource(R.string.tasks_complete)) }
                    if (task.canRequest) { OutlinedButton(onClick = { onCommand(TaskCommand.REQUEST_HELP) }) { Icon(Icons.Outlined.Groups, null); Text(stringResource(R.string.tasks_help)) }; OutlinedButton(onClick = { onCommand(TaskCommand.REQUEST_SWAP) }) { Text(stringResource(R.string.tasks_swap)) }; OutlinedButton(onClick = { onCommand(TaskCommand.REQUEST_POSTPONE) }) { Icon(Icons.Outlined.MoreTime, null); Text(stringResource(R.string.tasks_postpone)) }; OutlinedButton(onClick = { onCommand(TaskCommand.REPORT_ISSUE) }) { Text(stringResource(R.string.tasks_issue)) } }
                    if (task.canManage && task.status in setOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED)) OutlinedButton(onClick = { onCommand(TaskCommand.REOPEN) }) { Text(stringResource(R.string.tasks_reopen)) }
                    if (task.canManage && task.status != TaskStatus.CANCELLED) OutlinedButton(onClick = { onCommand(TaskCommand.CANCEL) }) { Text(stringResource(R.string.tasks_cancel)) }
                }
            }
            if (task.completionNote != null) item { HorizontalDivider(); Text(stringResource(R.string.tasks_completion_note), fontWeight = FontWeight.SemiBold); Text(task.completionNote) }
            if (task.requests.isNotEmpty()) { item { HorizontalDivider(); Text(stringResource(R.string.tasks_requests_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; items(task.requests, key = TaskRequestUi::id) { request -> RequestCard(request, task.canManage, members, onDecision) } }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun RequestCard(request: TaskRequestUi, canManage: Boolean, members: List<TaskMemberUi>, onDecision: (TaskRequestUi, Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(request.type.labelRes), fontWeight = FontWeight.SemiBold); Text(request.reason); Text(stringResource(R.string.tasks_requested_by, request.createdByDisplayName), style = MaterialTheme.typography.labelMedium)
        request.requestedAssigneeMembershipId?.let { id -> Text(stringResource(R.string.tasks_swap_to, members.firstOrNull { it.membershipId == id }?.displayName ?: stringResource(R.string.tasks_member_unavailable))) }
        request.requestedDueDate?.let { Text(stringResource(R.string.tasks_postpone_to, it.toString(), request.requestedDueTime.orEmpty())) }
        Text(stringResource(request.status.labelRes), color = MaterialTheme.colorScheme.primary)
        request.resolutionNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (canManage && request.status == TaskRequestStatus.PENDING) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onDecision(request, true) }) { Text(stringResource(R.string.tasks_approve)) }; TextButton(onClick = { onDecision(request, false) }) { Text(stringResource(R.string.tasks_reject)) } }
    } }
}

@Composable
private fun TaskCreateDialog(members: List<TaskMemberUi>, busy: Boolean, onDismiss: () -> Unit, onCreate: (TaskDraft) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }; var instructions by rememberSaveable { mutableStateOf("") }; var zone by rememberSaveable { mutableStateOf("") }; var dueDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }; var dueTime by rememberSaveable { mutableStateOf("") }; var estimate by rememberSaveable { mutableStateOf("30") }; var priority by rememberSaveable { mutableStateOf(TaskPriority.NORMAL) }; var memberId by rememberSaveable { mutableStateOf(members.firstOrNull()?.membershipId.orEmpty()) }; var menu by remember { mutableStateOf(false) }
    val valid = title.trim().isNotEmpty() && runCatching { LocalDate.parse(dueDate) }.isSuccess && (dueTime.isBlank() || Regex("^(?:[01][0-9]|2[0-3]):[0-5][0-9]$").matches(dueTime)) && memberId.isNotEmpty()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.tasks_create_title)) }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_title)) }, singleLine = true) }
        item { OutlinedTextField(instructions, { instructions = it.take(2000) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_instructions)) }, minLines = 2) }
        item { OutlinedTextField(zone, { zone = it.take(80) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_zone)) }, singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TaskPriority.entries.forEach { item -> FilterChip(selected = item == priority, onClick = { priority = item }, label = { Text(stringResource(item.labelRes)) }) } } }
        item { OutlinedTextField(dueDate, { dueDate = it.take(10) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_date)) }, supportingText = { Text("YYYY-MM-DD") }, singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(dueTime, { dueTime = it.take(5) }, Modifier.weight(1f), label = { Text(stringResource(R.string.tasks_field_time)) }, placeholder = { Text("18:30") }, singleLine = true); OutlinedTextField(estimate, { estimate = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), label = { Text(stringResource(R.string.tasks_field_minutes)) }, singleLine = true) } }
        item { Column { OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text(members.firstOrNull { it.membershipId == memberId }?.displayName ?: stringResource(R.string.tasks_choose_member)) }; DropdownMenu(menu, { menu = false }) { members.forEach { member -> DropdownMenuItem(text = { Text(member.displayName) }, onClick = { memberId = member.membershipId; menu = false }) } } } }
    } }, confirmButton = { Button(enabled = valid && !busy, onClick = { onCreate(TaskDraft(title.trim(), instructions.trim().ifEmpty { null }, zone.trim().ifEmpty { null }, priority, LocalDate.parse(dueDate), dueTime.ifBlank { null }, estimate.toIntOrNull()?.takeIf { it in 5..1440 }, memberId)) }) { Text(stringResource(R.string.tasks_create_action)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.calendar_cancel)) } })
}

@Composable
private fun TaskActionDialog(task: HouseholdTaskUi, command: TaskCommand, members: List<TaskMemberUi>, onDismiss: () -> Unit, onSubmit: (TaskCommandDraft) -> Unit) {
    var note by rememberSaveable(command) { mutableStateOf("") }; var memberId by rememberSaveable(command) { mutableStateOf(members.firstOrNull { it.membershipId != task.assigneeMembershipId }?.membershipId.orEmpty()) }; var dueDate by rememberSaveable(command) { mutableStateOf(task.dueDate.plusDays(1).toString()) }; var dueTime by rememberSaveable(command) { mutableStateOf(task.dueTime.orEmpty()) }; var menu by remember { mutableStateOf(false) }
    val noteRequired = command in setOf(TaskCommand.COMPLETE, TaskCommand.CANCEL, TaskCommand.REQUEST_HELP, TaskCommand.REQUEST_SWAP, TaskCommand.REQUEST_POSTPONE, TaskCommand.REPORT_ISSUE)
    val valid = (!noteRequired || note.trim().length >= 3) && (command != TaskCommand.REQUEST_SWAP || memberId.isNotEmpty()) && (command != TaskCommand.REQUEST_POSTPONE || runCatching { LocalDate.parse(dueDate) }.getOrNull()?.isAfter(task.dueDate) == true)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(command.titleRes)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(command.explanationRes)); if (noteRequired) OutlinedTextField(note, { note = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_reason)) }, minLines = 2)
        if (command == TaskCommand.REQUEST_SWAP) Column { OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text(members.firstOrNull { it.membershipId == memberId }?.displayName ?: stringResource(R.string.tasks_choose_member)) }; DropdownMenu(menu, { menu = false }) { members.filter { it.membershipId != task.assigneeMembershipId }.forEach { member -> DropdownMenuItem(text = { Text(member.displayName) }, onClick = { memberId = member.membershipId; menu = false }) } } }
        if (command == TaskCommand.REQUEST_POSTPONE) { OutlinedTextField(dueDate, { dueDate = it.take(10) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_date)) }); OutlinedTextField(dueTime, { dueTime = it.take(5) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_time)) }) }
    } }, confirmButton = { Button(enabled = valid, onClick = { onSubmit(TaskCommandDraft(command, note.trim().ifEmpty { null }, requestedAssigneeMembershipId = memberId.takeIf { command == TaskCommand.REQUEST_SWAP }, requestedDueDate = dueDate.takeIf { command == TaskCommand.REQUEST_POSTPONE }?.let(LocalDate::parse), requestedDueTime = dueTime.ifBlank { null }.takeIf { command == TaskCommand.REQUEST_POSTPONE })) }) { Text(stringResource(R.string.tasks_confirm_action)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.calendar_cancel)) } })
}

@Composable
private fun DecisionDialog(approve: Boolean, onDismiss: () -> Unit, onSubmit: (String?) -> Unit) { var note by rememberSaveable { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (approve) R.string.tasks_approve_title else R.string.tasks_reject_title)) }, text = { OutlinedTextField(note, { note = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tasks_field_decision_note)) }, supportingText = { Text(stringResource(if (approve) R.string.tasks_note_optional else R.string.tasks_note_required)) }) }, confirmButton = { Button(enabled = approve || note.trim().length >= 3, onClick = { onSubmit(note.trim().ifEmpty { null }) }) { Text(stringResource(if (approve) R.string.tasks_approve else R.string.tasks_reject)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.calendar_cancel)) } }) }

private fun formatDue(task: HouseholdTaskUi): String = task.dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + task.dueTime?.let { " · $it" }.orEmpty()
private val TaskFilter.labelRes get() = when (this) { TaskFilter.MY_TASKS -> R.string.tasks_filter_mine; TaskFilter.ACTIVE -> R.string.tasks_filter_active; TaskFilter.REQUESTS -> R.string.tasks_filter_requests; TaskFilter.COMPLETED -> R.string.tasks_filter_completed; TaskFilter.ALL -> R.string.tasks_filter_all }
private val TaskPriority.labelRes get() = when (this) { TaskPriority.LOW -> R.string.tasks_priority_low; TaskPriority.NORMAL -> R.string.tasks_priority_normal; TaskPriority.HIGH -> R.string.tasks_priority_high }
private val TaskStatus.labelRes get() = when (this) { TaskStatus.OPEN -> R.string.tasks_status_open; TaskStatus.IN_PROGRESS -> R.string.tasks_status_progress; TaskStatus.COMPLETED -> R.string.tasks_status_completed; TaskStatus.CANCELLED -> R.string.tasks_status_cancelled }
private val TaskRequestType.labelRes get() = when (this) { TaskRequestType.HELP -> R.string.tasks_help; TaskRequestType.SWAP -> R.string.tasks_swap; TaskRequestType.POSTPONE -> R.string.tasks_postpone; TaskRequestType.ISSUE -> R.string.tasks_issue }
private val TaskRequestStatus.labelRes get() = when (this) { TaskRequestStatus.PENDING -> R.string.tasks_request_pending; TaskRequestStatus.APPROVED -> R.string.tasks_request_approved; TaskRequestStatus.REJECTED -> R.string.tasks_request_rejected; TaskRequestStatus.CANCELLED -> R.string.tasks_request_cancelled }
private val TasksProblem.messageRes get() = when (this) { TasksProblem.LOAD_FAILED -> R.string.tasks_load_failed; TasksProblem.CREATE_FAILED -> R.string.tasks_create_failed; TasksProblem.ACTION_FAILED -> R.string.tasks_action_failed; TasksProblem.VERSION_CONFLICT -> R.string.tasks_version_conflict; TasksProblem.REQUEST_CONFLICT -> R.string.tasks_request_conflict; TasksProblem.INVALID_TRANSITION -> R.string.tasks_invalid_transition }
private val TaskCommand.titleRes get() = when (this) { TaskCommand.START -> R.string.tasks_start_title; TaskCommand.COMPLETE -> R.string.tasks_complete_title; TaskCommand.REOPEN -> R.string.tasks_reopen_title; TaskCommand.CANCEL -> R.string.tasks_cancel_title; TaskCommand.REQUEST_HELP -> R.string.tasks_help_title; TaskCommand.REQUEST_SWAP -> R.string.tasks_swap_title; TaskCommand.REQUEST_POSTPONE -> R.string.tasks_postpone_title; TaskCommand.REPORT_ISSUE -> R.string.tasks_issue_title; else -> R.string.tasks_requests_title }
private val TaskCommand.explanationRes get() = when (this) { TaskCommand.START -> R.string.tasks_start_explanation; TaskCommand.COMPLETE -> R.string.tasks_complete_explanation; TaskCommand.REOPEN -> R.string.tasks_reopen_explanation; TaskCommand.CANCEL -> R.string.tasks_cancel_explanation; TaskCommand.REQUEST_HELP -> R.string.tasks_help_explanation; TaskCommand.REQUEST_SWAP -> R.string.tasks_swap_explanation; TaskCommand.REQUEST_POSTPONE -> R.string.tasks_postpone_explanation; TaskCommand.REPORT_ISSUE -> R.string.tasks_issue_explanation; else -> R.string.tasks_requests_title }
