package com.sharedhouse.android.ui.tasks

import java.time.Instant
import java.time.LocalDate

data class TasksUiState(
    val content: TasksContent = TasksContent.Loading,
    val canCreate: Boolean = false,
    val members: List<TaskMemberUi> = emptyList(),
    val isMutationInProgress: Boolean = false,
    val problem: TasksProblem? = null,
)

sealed interface TasksContent {
    data object Loading : TasksContent
    data object Error : TasksContent
    data class Ready(val tasks: List<HouseholdTaskUi>) : TasksContent
}

data class TaskMemberUi(
    val membershipId: String,
    val displayName: String,
    val isCurrentUser: Boolean,
)

data class HouseholdTaskUi(
    val id: String,
    val title: String,
    val instructions: String?,
    val zone: String?,
    val priority: TaskPriority,
    val dueDate: LocalDate,
    val dueTime: String?,
    val estimatedMinutes: Int?,
    val assigneeMembershipId: String,
    val assigneeDisplayName: String,
    val isMine: Boolean,
    val status: TaskStatus,
    val completionNote: String?,
    val completedAt: Instant?,
    val requests: List<TaskRequestUi>,
    val canManage: Boolean,
    val canStart: Boolean,
    val canComplete: Boolean,
    val canRequest: Boolean,
    val version: Int,
)

data class TaskRequestUi(
    val id: String,
    val type: TaskRequestType,
    val status: TaskRequestStatus,
    val reason: String,
    val requestedAssigneeMembershipId: String?,
    val requestedDueDate: LocalDate?,
    val requestedDueTime: String?,
    val createdByDisplayName: String,
    val resolutionNote: String?,
)

enum class TaskPriority(val wireValue: String) { LOW("low"), NORMAL("normal"), HIGH("high") }
enum class TaskStatus { OPEN, IN_PROGRESS, COMPLETED, CANCELLED }
enum class TaskRequestType { HELP, SWAP, POSTPONE, ISSUE }
enum class TaskRequestStatus { PENDING, APPROVED, REJECTED, CANCELLED }
enum class TaskFilter { MY_TASKS, ACTIVE, REQUESTS, COMPLETED, ALL }
enum class TaskCommand(val wireValue: String) {
    START("start"), COMPLETE("complete"), REOPEN("reopen"), CANCEL("cancel"),
    REQUEST_HELP("request_help"), REQUEST_SWAP("request_swap"), REQUEST_POSTPONE("request_postpone"),
    REPORT_ISSUE("report_issue"), APPROVE_REQUEST("approve_request"), REJECT_REQUEST("reject_request"),
}

data class TaskDraft(
    val title: String,
    val instructions: String?,
    val zone: String?,
    val priority: TaskPriority,
    val dueDate: LocalDate,
    val dueTime: String?,
    val estimatedMinutes: Int?,
    val assigneeMembershipId: String,
)

data class TaskCommandDraft(
    val command: TaskCommand,
    val note: String? = null,
    val requestId: String? = null,
    val requestedAssigneeMembershipId: String? = null,
    val requestedDueDate: LocalDate? = null,
    val requestedDueTime: String? = null,
)

sealed interface TasksAction {
    data object Retry : TasksAction
    data class Create(val draft: TaskDraft) : TasksAction
    data class Execute(val taskId: String, val expectedVersion: Int, val draft: TaskCommandDraft) : TasksAction
}

enum class TasksProblem {
    LOAD_FAILED, CREATE_FAILED, ACTION_FAILED, VERSION_CONFLICT, REQUEST_CONFLICT, INVALID_TRANSITION,
}
