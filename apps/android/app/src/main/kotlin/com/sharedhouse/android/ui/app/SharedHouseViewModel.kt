package com.sharedhouse.android.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharedhouse.android.platform.security.SessionLoadResult
import com.sharedhouse.android.platform.security.SessionSaveResult
import com.sharedhouse.android.platform.security.SessionStore
import com.sharedhouse.android.ui.calendar.CalendarAction
import com.sharedhouse.android.ui.calendar.CalendarContent
import com.sharedhouse.android.ui.calendar.CalendarEventDraft
import com.sharedhouse.android.ui.calendar.CalendarEventType
import com.sharedhouse.android.ui.calendar.CalendarEventUi
import com.sharedhouse.android.ui.calendar.CalendarMutationProblem
import com.sharedhouse.android.ui.calendar.CalendarPeriodCalculator
import com.sharedhouse.android.ui.calendar.CalendarUiReducer
import com.sharedhouse.android.ui.calendar.CalendarUiState
import com.sharedhouse.android.ui.money.ExpenseAllocationUi
import com.sharedhouse.android.ui.money.ExpenseAllocationStatus
import com.sharedhouse.android.ui.money.ExpenseDraft
import com.sharedhouse.android.ui.money.ExpensePaymentDraft
import com.sharedhouse.android.ui.money.ExpensePaymentMethod
import com.sharedhouse.android.ui.money.ExpensePaymentStatus
import com.sharedhouse.android.ui.money.ExpensePaymentUi
import com.sharedhouse.android.ui.money.ExpenseStatus
import com.sharedhouse.android.ui.money.ExpenseTemplateDraft
import com.sharedhouse.android.ui.money.ExpenseTemplateUi
import com.sharedhouse.android.ui.money.ExpenseUi
import com.sharedhouse.android.ui.money.MoneyAction
import com.sharedhouse.android.ui.money.MoneyContent
import com.sharedhouse.android.ui.money.MoneyProblem
import com.sharedhouse.android.ui.money.MoneyUiState
import com.sharedhouse.android.ui.tasks.HouseholdTaskUi
import com.sharedhouse.android.ui.tasks.TaskCommandDraft
import com.sharedhouse.android.ui.tasks.TaskDraft
import com.sharedhouse.android.ui.tasks.TaskMemberUi
import com.sharedhouse.android.ui.tasks.TaskPriority
import com.sharedhouse.android.ui.tasks.TaskRequestStatus
import com.sharedhouse.android.ui.tasks.TaskRequestType
import com.sharedhouse.android.ui.tasks.TaskRequestUi
import com.sharedhouse.android.ui.tasks.TaskStatus
import com.sharedhouse.android.ui.tasks.TasksAction
import com.sharedhouse.android.ui.tasks.TasksContent
import com.sharedhouse.android.ui.tasks.TasksProblem
import com.sharedhouse.android.ui.tasks.TasksUiState
import com.sharedhouse.android.ui.home.HouseholdMemberCommand
import com.sharedhouse.android.ui.home.HouseholdMemberUi
import com.sharedhouse.android.ui.home.HouseholdMembersContent
import com.sharedhouse.android.ui.home.HouseholdMembersProblem
import com.sharedhouse.android.ui.home.HouseholdMembersUiState
import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.CalendarEventConfigurationDto
import com.sharedhouse.network.CalendarEventDto
import com.sharedhouse.network.CreateHouseholdInvitationPayload
import com.sharedhouse.network.ExpenseConfigurationDto
import com.sharedhouse.network.ExpenseDto
import com.sharedhouse.network.ExpensePaymentDeclarationDto
import com.sharedhouse.network.ExpenseTemplateConfigurationDto
import com.sharedhouse.network.ExpenseTemplateDto
import com.sharedhouse.network.FieldViolationDto
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.HouseholdMemberActionDto
import com.sharedhouse.network.HouseholdMemberDto
import com.sharedhouse.network.HouseholdTaskActionDto
import com.sharedhouse.network.HouseholdTaskConfigurationDto
import com.sharedhouse.network.HouseholdTaskDto
import com.sharedhouse.network.HouseholdTaskMemberDto
import com.sharedhouse.network.MoneyDto
import com.sharedhouse.network.RegisterPayload
import com.sharedhouse.network.ResendVerificationPayload
import com.sharedhouse.network.SessionDto
import com.sharedhouse.network.SignInPayload
import com.sharedhouse.network.VerifyEmailPayload
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates the first real account and household vertical slice.
 *
 * Session activation is accepted locally only after the rotating credentials are safely persisted.
 */
class SharedHouseViewModel(
    private val gateway: SharedHouseGateway,
    private val sessionStore: SessionStore,
    private val deviceName: String,
    preferredLocale: String,
    initialHousehold: HouseholdFormState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(household = initialHousehold))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var session: SessionDto? = null
    private var preferredLocale = preferredLocale
    private var householdCreationIdempotencyKey: String? = null
    private var sessionRestoreJob: Job? = null
    private var calendarLoadJob: Job? = null
    private var calendarCreationIdempotencyKey: String? = null
    private var calendarCreationDraft: CalendarEventDraft? = null
    private var moneyLoadJob: Job? = null
    private var tasksLoadJob: Job? = null
    private var householdMembersLoadJob: Job? = null
    private var expenseCreationIdempotencyKey: String? = null
    private var expenseCreationDraft: ExpenseDraft? = null
    private var paymentDeclarationIdempotencyKey: String? = null
    private var paymentDeclarationDraft: Pair<String, ExpensePaymentDraft>? = null
    private var templateCreationIdempotencyKey: String? = null
    private var templateCreationDraft: ExpenseTemplateDraft? = null
    private var taskCreationIdempotencyKey: String? = null
    private var taskCreationDraft: TaskDraft? = null

    init {
        retrySessionRestore()
    }

    fun openWelcome() = moveTo(AppRoute.Welcome)

    fun openRegister() = moveTo(AppRoute.Register)

    fun openSignIn() = moveTo(AppRoute.SignIn)

    fun retrySessionRestore() {
        if (session != null || sessionRestoreJob?.isActive == true) return
        _uiState.update {
            it.copy(
                route = AppRoute.Welcome,
                isRestoringSession = true,
                canRetrySessionRestore = false,
                error = null,
                notice = null,
                correlationId = null,
            )
        }
        sessionRestoreJob = viewModelScope.launch { restoreSession() }
    }

    fun updatePreferredLocale(value: String) {
        preferredLocale = if (value == "ro") "ro" else "en"
    }

    fun updateDisplayName(value: String) = updateAuth(FormField.DisplayName) {
        copy(displayName = value)
    }

    fun updateEmail(value: String) = updateAuth(FormField.Email) { copy(email = value) }

    fun updatePassword(value: String) = updateAuth(FormField.Password) { copy(password = value) }

    fun updateVerificationCode(value: String) = updateAuth(FormField.VerificationCode) {
        copy(verificationCode = value.filter(Char::isDigit).take(8))
    }

    fun updateAgeConfirmed(value: Boolean) = updateAuth(FormField.AgeConfirmation) {
        copy(ageConfirmed = value)
    }

    fun updateTermsAccepted(value: Boolean) = updateAuth(FormField.TermsConfirmation) {
        copy(termsAccepted = value)
    }

    fun updateMarketingConsent(value: Boolean) {
        _uiState.update { state ->
            state.copy(auth = state.auth.copy(marketingConsent = value), error = null)
        }
    }

    fun updateHouseholdName(value: String) = updateHousehold(FormField.HouseholdName) {
        copy(name = value)
    }

    fun updateCountryCode(value: String) = updateHousehold(FormField.CountryCode) {
        copy(countryCode = value.uppercase().take(2))
    }

    fun updateTimezone(value: String) = updateHousehold(FormField.Timezone) {
        copy(timezone = value)
    }

    fun updateCurrency(value: String) = updateHousehold(FormField.Currency) {
        copy(currency = value.uppercase().take(3))
    }

    fun updateFirstDayOfWeek(value: Int) {
        updateHousehold { copy(firstDayOfWeek = value) }
    }

    fun updateCycleType(value: String) {
        updateHousehold { copy(cycleType = value) }
    }

    fun updateCycleAnchor(value: String) = updateHousehold(FormField.CycleAnchor) {
        copy(cycleAnchor = value.take(10))
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun handleCalendarAction(action: CalendarAction) {
        val snapshot = _uiState.value
        if (snapshot.route != AppRoute.Home || snapshot.selectedHousehold == null) return

        when (action) {
            is CalendarAction.ChangeView,
            is CalendarAction.MovePeriod,
            is CalendarAction.SelectDate,
            CalendarAction.GoToToday,
            -> {
                val previousRange = snapshot.calendar.visibleRange()
                val reduced = CalendarUiReducer.reduce(snapshot.calendar, action)
                _uiState.update {
                    it.copy(calendar = reduced.copy(mutationProblem = null))
                }
                if (reduced.visibleRange() != previousRange) {
                    loadCalendar()
                }
            }

            CalendarAction.Retry -> loadCalendar()
            is CalendarAction.CreateEvent -> createCalendarEvent(action.draft)
            is CalendarAction.UpdateEvent -> updateCalendarEvent(action)
            is CalendarAction.DeleteEvent -> deleteCalendarEvent(action)
        }
    }

    fun refreshCalendar() {
        loadCalendar()
    }

    fun handleMoneyAction(action: MoneyAction) {
        if (_uiState.value.route != AppRoute.Home || _uiState.value.selectedHousehold == null) return
        when (action) {
            MoneyAction.Retry -> loadMoney()
            is MoneyAction.Create -> createExpense(action.draft)
            is MoneyAction.Approve -> transitionExpense(action.expenseId, action.expectedVersion, true, null)
            is MoneyAction.Reverse -> transitionExpense(action.expenseId, action.expectedVersion, false, action.reason)
            is MoneyAction.DeclarePayment -> declareExpensePayment(action.expenseId, action.draft)
            is MoneyAction.ConfirmPayment -> transitionExpensePayment(
                action.expenseId,
                action.paymentId,
                action.expectedVersion,
                PaymentTransition.CONFIRM,
                null,
            )
            is MoneyAction.DisputePayment -> transitionExpensePayment(
                action.expenseId,
                action.paymentId,
                action.expectedVersion,
                PaymentTransition.DISPUTE,
                action.reason,
            )
            is MoneyAction.ReversePayment -> transitionExpensePayment(
                action.expenseId,
                action.paymentId,
                action.expectedVersion,
                PaymentTransition.REVERSE,
                action.reason,
            )
            is MoneyAction.CreateTemplate -> createExpenseTemplate(action.draft)
            is MoneyAction.UpdateTemplate -> updateExpenseTemplate(action.templateId, action.expectedVersion, action.draft)
            is MoneyAction.ArchiveTemplate -> archiveExpenseTemplate(action.templateId, action.expectedVersion, action.reason)
        }
    }

    fun refreshMoney() = loadMoney()

    fun handleTasksAction(action: TasksAction) {
        if (_uiState.value.route != AppRoute.Home || _uiState.value.selectedHousehold == null) return
        when (action) {
            TasksAction.Retry -> loadTasks()
            is TasksAction.Create -> createHouseholdTask(action.draft)
            is TasksAction.Execute -> executeTaskAction(action.taskId, action.expectedVersion, action.draft)
        }
    }

    private fun loadTasks(preserveProblem: Boolean = false) {
        val household = _uiState.value.selectedHousehold ?: return
        tasksLoadJob?.cancel()
        householdMembersLoadJob?.cancel()
        _uiState.update { state -> state.copy(tasks = state.tasks.copy(
            content = TasksContent.Loading,
            problem = if (preserveProblem) state.tasks.problem else null,
        )) }
        tasksLoadJob = viewModelScope.launch {
            when (val result = authorized { gateway.listHouseholdTasks(it, household.id) }) {
                is ApiResult.Success -> if (_uiState.value.selectedHousehold?.id == household.id) {
                    val mapped = runCatching { result.value.tasks.map { it.toTaskUi(result.value.members) } }.getOrNull()
                    _uiState.update { state -> state.copy(tasks = state.tasks.copy(
                        content = mapped?.let { TasksContent.Ready(it.sortedWith(compareBy<HouseholdTaskUi> { task -> task.status.ordinal }.thenBy { task -> task.dueDate })) } ?: TasksContent.Error,
                        canCreate = result.value.canCreate,
                        members = result.value.members.map { TaskMemberUi(it.membershipId, it.displayName, it.isCurrentUser) },
                        isMutationInProgress = false,
                        problem = if (mapped == null) TasksProblem.LOAD_FAILED else state.tasks.problem,
                    )) }
                }
                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    _uiState.update { it.copy(tasks = it.tasks.copy(content = TasksContent.Error, isMutationInProgress = false, problem = TasksProblem.LOAD_FAILED)) }
                }
            }
        }
    }

    private fun createHouseholdTask(draft: TaskDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (!snapshot.tasks.canCreate || snapshot.tasks.isMutationInProgress) return
        val key = (if (taskCreationDraft == draft) taskCreationIdempotencyKey else null) ?: UUID.randomUUID().toString().also {
            taskCreationDraft = draft; taskCreationIdempotencyKey = it
        }
        beginTaskMutation()
        viewModelScope.launch {
            when (val result = authorized { gateway.createHouseholdTask(it, household.id, key, HouseholdTaskConfigurationDto(
                title = draft.title, instructions = draft.instructions, zone = draft.zone,
                priority = draft.priority.wireValue, dueDate = draft.dueDate.toString(), dueTime = draft.dueTime,
                estimatedMinutes = draft.estimatedMinutes, assigneeMembershipId = draft.assigneeMembershipId,
            )) }) {
                is ApiResult.Success -> { taskCreationDraft = null; taskCreationIdempotencyKey = null; applyTask(result.value) }
                is ApiResult.Failure -> finishTaskMutation(result, TasksProblem.CREATE_FAILED)
            }
        }
    }

    private fun executeTaskAction(taskId: String, version: Int, draft: TaskCommandDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val task = (snapshot.tasks.content as? TasksContent.Ready)?.tasks?.firstOrNull { it.id == taskId } ?: return
        if (snapshot.tasks.isMutationInProgress || task.version != version) return
        beginTaskMutation()
        viewModelScope.launch {
            when (val result = authorized { gateway.actOnHouseholdTask(
                it, household.id, taskId, version, UUID.randomUUID().toString(), HouseholdTaskActionDto(
                    action = draft.command.wireValue, note = draft.note, requestId = draft.requestId,
                    requestedAssigneeMembershipId = draft.requestedAssigneeMembershipId,
                    requestedDueDate = draft.requestedDueDate?.toString(), requestedDueTime = draft.requestedDueTime,
                ),
            ) }) {
                is ApiResult.Success -> applyTask(result.value)
                is ApiResult.Failure -> finishTaskMutation(result, TasksProblem.ACTION_FAILED)
            }
        }
    }

    private fun beginTaskMutation() = _uiState.update { it.copy(tasks = it.tasks.copy(isMutationInProgress = true, problem = null)) }

    private fun applyTask(dto: HouseholdTaskDto) {
        val mapped = runCatching { dto.toTaskUi(emptyList()) }.getOrElse {
            loadTasks(); return
        }
        _uiState.update { state ->
            val current = (state.tasks.content as? TasksContent.Ready)?.tasks.orEmpty().filterNot { it.id == mapped.id }.plus(mapped)
            state.copy(tasks = state.tasks.copy(content = TasksContent.Ready(current.sortedBy(HouseholdTaskUi::dueDate)), isMutationInProgress = false, problem = null))
        }
    }

    private fun finishTaskMutation(failure: ApiResult.Failure, fallback: TasksProblem) {
        if (_uiState.value.route == AppRoute.SignIn) return
        val problem = when (failure.code) {
            "HOUSEHOLD_TASK_VERSION_CONFLICT" -> TasksProblem.VERSION_CONFLICT
            "HOUSEHOLD_TASK_REQUEST_CONFLICT" -> TasksProblem.REQUEST_CONFLICT
            "HOUSEHOLD_TASK_INVALID_TRANSITION" -> TasksProblem.INVALID_TRANSITION
            else -> fallback
        }
        _uiState.update { it.copy(tasks = it.tasks.copy(isMutationInProgress = false, problem = problem)) }
        if (problem != fallback) loadTasks(true)
    }

    private fun loadMoney(preserveProblem: Boolean = false) {
        val household = _uiState.value.selectedHousehold ?: return
        moneyLoadJob?.cancel()
        _uiState.update { state ->
            state.copy(money = state.money.copy(
                content = MoneyContent.Loading,
                problem = if (preserveProblem) state.money.problem else null,
            ))
        }
        moneyLoadJob = viewModelScope.launch {
            val expensesResult = authorized { gateway.listExpenses(it, household.id) }
            val templatesResult = if (expensesResult is ApiResult.Success) {
                authorized { gateway.listExpenseTemplates(it, household.id) }
            } else null
            if (_uiState.value.selectedHousehold?.id != household.id) return@launch
            if (expensesResult is ApiResult.Success && templatesResult is ApiResult.Success) {
                val mapped = runCatching {
                    expensesResult.value.map { it.toMoneyUi() } to templatesResult.value.map { it.toTemplateUi() }
                }.getOrNull()
                _uiState.update { state ->
                    state.copy(money = state.money.copy(
                        content = mapped?.first?.let { MoneyContent.Ready(it.sortedBy(ExpenseUi::dueDate)) }
                            ?: MoneyContent.Error,
                        templates = mapped?.second?.sortedBy(ExpenseTemplateUi::nextDueDate).orEmpty(),
                        isMutationInProgress = false,
                        problem = if (mapped == null) MoneyProblem.LOAD_FAILED else state.money.problem,
                    ))
                }
            } else if (_uiState.value.route != AppRoute.SignIn) {
                _uiState.update { it.copy(money = it.money.copy(
                    content = MoneyContent.Error,
                    templates = emptyList(),
                    isMutationInProgress = false,
                    problem = MoneyProblem.LOAD_FAILED,
                )) }
            }
        }
    }

    private fun createExpense(draft: ExpenseDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (!snapshot.money.canCreate || snapshot.money.isMutationInProgress) return
        val key = (if (expenseCreationDraft == draft) expenseCreationIdempotencyKey else null)
            ?: UUID.randomUUID().toString().also {
                expenseCreationDraft = draft
                expenseCreationIdempotencyKey = it
            }
        beginMoneyMutation()
        viewModelScope.launch {
            when (val result = authorized {
                gateway.createExpense(
                    it,
                    household.id,
                    key,
                    ExpenseConfigurationDto(
                        title = draft.title,
                        category = draft.category.wireValue,
                        customCategoryName = draft.customCategoryName,
                        amount = MoneyDto(draft.amountMinor, household.currency),
                        dueDate = draft.dueDate.toString(),
                        notes = draft.notes,
                    ),
                )
            }) {
                is ApiResult.Success -> {
                    expenseCreationDraft = null
                    expenseCreationIdempotencyKey = null
                    applyExpense(result.value)
                }
                is ApiResult.Failure -> finishMoneyMutation(result, MoneyProblem.CREATE_FAILED)
            }
        }
    }

    private fun transitionExpense(id: String, version: Int, approve: Boolean, reason: String?) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val expense = (snapshot.money.content as? MoneyContent.Ready)?.expenses?.firstOrNull { it.id == id }
            ?: return
        if (snapshot.money.isMutationInProgress || (approve && !expense.canApprove) || (!approve && !expense.canReverse)) return
        beginMoneyMutation()
        viewModelScope.launch {
            val result = authorized { token ->
                if (approve) gateway.approveExpense(token, household.id, id, version)
                else gateway.reverseExpense(token, household.id, id, version, requireNotNull(reason))
            }
            when (result) {
                is ApiResult.Success -> applyExpense(result.value)
                is ApiResult.Failure -> finishMoneyMutation(
                    result,
                    if (approve) MoneyProblem.APPROVE_FAILED else MoneyProblem.REVERSE_FAILED,
                )
            }
        }
    }

    private fun declareExpensePayment(expenseId: String, draft: ExpensePaymentDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val expense = (snapshot.money.content as? MoneyContent.Ready)?.expenses
            ?.firstOrNull { it.id == expenseId } ?: return
        if (
            snapshot.money.isMutationInProgress ||
            expense.allocations.none { it.isCurrentUser && it.canDeclarePayment }
        ) return
        val declaration = expenseId to draft
        val key = (if (paymentDeclarationDraft == declaration) paymentDeclarationIdempotencyKey else null)
            ?: UUID.randomUUID().toString().also {
                paymentDeclarationDraft = declaration
                paymentDeclarationIdempotencyKey = it
            }
        beginMoneyMutation()
        viewModelScope.launch {
            when (val result = authorized { token ->
                gateway.declareExpensePayment(
                    token,
                    household.id,
                    expenseId,
                    key,
                    ExpensePaymentDeclarationDto(
                        method = draft.method.wireValue,
                        paidAt = draft.paidAt.toString(),
                        reference = draft.reference,
                        note = draft.note,
                    ),
                )
            }) {
                is ApiResult.Success -> {
                    paymentDeclarationDraft = null
                    paymentDeclarationIdempotencyKey = null
                    applyExpense(result.value)
                }
                is ApiResult.Failure -> finishMoneyMutation(
                    result,
                    MoneyProblem.PAYMENT_DECLARE_FAILED,
                )
            }
        }
    }

    private fun transitionExpensePayment(
        expenseId: String,
        paymentId: String,
        version: Int,
        transition: PaymentTransition,
        reason: String?,
    ) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val payment = (snapshot.money.content as? MoneyContent.Ready)?.expenses
            ?.firstOrNull { it.id == expenseId }
            ?.allocations
            ?.flatMap(ExpenseAllocationUi::paymentDeclarations)
            ?.firstOrNull { it.id == paymentId } ?: return
        val permitted = when (transition) {
            PaymentTransition.CONFIRM -> payment.canConfirm
            PaymentTransition.DISPUTE -> payment.canDispute
            PaymentTransition.REVERSE -> payment.canReverse
        }
        if (snapshot.money.isMutationInProgress || !permitted) return
        beginMoneyMutation()
        viewModelScope.launch {
            val result = authorized { token ->
                when (transition) {
                    PaymentTransition.CONFIRM -> gateway.confirmExpensePayment(
                        token,
                        household.id,
                        expenseId,
                        paymentId,
                        version,
                    )
                    PaymentTransition.DISPUTE -> gateway.disputeExpensePayment(
                        token,
                        household.id,
                        expenseId,
                        paymentId,
                        version,
                        requireNotNull(reason),
                    )
                    PaymentTransition.REVERSE -> gateway.reverseExpensePayment(
                        token,
                        household.id,
                        expenseId,
                        paymentId,
                        version,
                        requireNotNull(reason),
                    )
                }
            }
            when (result) {
                is ApiResult.Success -> applyExpense(result.value)
                is ApiResult.Failure -> finishMoneyMutation(
                    result,
                    when (transition) {
                        PaymentTransition.CONFIRM -> MoneyProblem.PAYMENT_CONFIRM_FAILED
                        PaymentTransition.DISPUTE -> MoneyProblem.PAYMENT_DISPUTE_FAILED
                        PaymentTransition.REVERSE -> MoneyProblem.PAYMENT_REVERSE_FAILED
                    },
                )
            }
        }
    }

    private fun createExpenseTemplate(draft: ExpenseTemplateDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (!snapshot.money.canManageTemplates || snapshot.money.isMutationInProgress) return
        val key = (if (templateCreationDraft == draft) templateCreationIdempotencyKey else null)
            ?: UUID.randomUUID().toString().also {
                templateCreationDraft = draft
                templateCreationIdempotencyKey = it
            }
        beginMoneyMutation()
        viewModelScope.launch {
            when (val result = authorized { token ->
                gateway.createExpenseTemplate(token, household.id, key, draft.toDto(household.currency))
            }) {
                is ApiResult.Success -> {
                    templateCreationDraft = null
                    templateCreationIdempotencyKey = null
                    applyExpenseTemplate(result.value)
                }
                is ApiResult.Failure -> finishMoneyMutation(result, MoneyProblem.TEMPLATE_FAILED)
            }
        }
    }

    private fun updateExpenseTemplate(id: String, version: Int, draft: ExpenseTemplateDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val template = snapshot.money.templates.firstOrNull { it.id == id } ?: return
        if (!snapshot.money.canManageTemplates || !template.canManage || snapshot.money.isMutationInProgress) return
        beginMoneyMutation()
        viewModelScope.launch {
            when (val result = authorized { token ->
                gateway.updateExpenseTemplate(token, household.id, id, version, draft.toDto(household.currency))
            }) {
                is ApiResult.Success -> applyExpenseTemplate(result.value)
                is ApiResult.Failure -> finishMoneyMutation(result, MoneyProblem.TEMPLATE_FAILED)
            }
        }
    }

    private fun archiveExpenseTemplate(id: String, version: Int, reason: String) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val template = snapshot.money.templates.firstOrNull { it.id == id } ?: return
        if (!snapshot.money.canManageTemplates || !template.canManage || snapshot.money.isMutationInProgress) return
        beginMoneyMutation()
        viewModelScope.launch {
            when (val result = authorized { token ->
                gateway.archiveExpenseTemplate(token, household.id, id, version, reason)
            }) {
                is ApiResult.Success -> applyExpenseTemplate(result.value)
                is ApiResult.Failure -> finishMoneyMutation(result, MoneyProblem.TEMPLATE_FAILED)
            }
        }
    }

    private fun applyExpenseTemplate(dto: ExpenseTemplateDto) {
        val mapped = runCatching { dto.toTemplateUi() }.getOrElse {
            finishMoneyMutation(ApiResult.Failure("INVALID_RESPONSE", "Invalid template response"), MoneyProblem.TEMPLATE_FAILED)
            return
        }
        _uiState.update { state ->
            state.copy(money = state.money.copy(
                templates = state.money.templates.filterNot { it.id == mapped.id }.plus(mapped)
                    .sortedBy(ExpenseTemplateUi::nextDueDate),
                isMutationInProgress = false,
                problem = null,
            ))
        }
    }

    private fun beginMoneyMutation() = _uiState.update {
        it.copy(money = it.money.copy(isMutationInProgress = true, problem = null))
    }

    private fun applyExpense(dto: ExpenseDto) {
        val mapped = runCatching { dto.toMoneyUi() }.getOrElse {
            _uiState.update { state -> state.copy(money = state.money.copy(
                isMutationInProgress = false,
                problem = MoneyProblem.LOAD_FAILED,
            )) }
            loadMoney(true)
            return
        }
        _uiState.update { state ->
            val current = (state.money.content as? MoneyContent.Ready)?.expenses.orEmpty()
                .filterNot { it.id == mapped.id }
                .plus(mapped)
                .sortedBy(ExpenseUi::dueDate)
            state.copy(money = state.money.copy(
                content = MoneyContent.Ready(current),
                isMutationInProgress = false,
                problem = null,
            ))
        }
    }

    private fun finishMoneyMutation(failure: ApiResult.Failure, fallback: MoneyProblem) {
        if (_uiState.value.route == AppRoute.SignIn) return
        val conflict = failure.status == 412 || failure.code == "EXPENSE_VERSION_CONFLICT" ||
            failure.code == "EXPENSE_TEMPLATE_VERSION_CONFLICT" ||
            failure.code == "PAYMENT_VERSION_CONFLICT"
        _uiState.update { it.copy(money = it.money.copy(
            isMutationInProgress = false,
            problem = if (conflict) MoneyProblem.VERSION_CONFLICT else fallback,
        )) }
        if (
            conflict ||
            failure.code == "EXPENSE_NOT_FOUND" ||
            failure.code == "EXPENSE_TEMPLATE_NOT_FOUND" ||
            failure.code == "PAYMENT_NOT_FOUND"
        ) loadMoney(true)
    }

    private fun loadCalendar(preserveMutationProblem: Boolean = false) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (snapshot.route != AppRoute.Home) return
        val range = snapshot.calendar.visibleRange()

        calendarLoadJob?.cancel()
        _uiState.update { state ->
            state.copy(
                calendar = state.calendar.copy(
                    content = CalendarContent.Loading,
                    mutationProblem = if (preserveMutationProblem) {
                        state.calendar.mutationProblem
                    } else {
                        null
                    },
                ),
            )
        }
        calendarLoadJob = viewModelScope.launch {
            when (
                val result = authorized { token ->
                    gateway.listCalendarEvents(
                        accessToken = token,
                        householdId = household.id,
                        from = range.start.toString(),
                        to = range.endInclusive.toString(),
                    )
                }
            ) {
                is ApiResult.Success -> {
                    val current = _uiState.value
                    if (current.selectedHousehold?.id != household.id || current.route != AppRoute.Home) {
                        return@launch
                    }
                    val mapped = runCatching {
                        result.value.map { event -> event.toCalendarUi(current) }
                    }.getOrElse {
                        _uiState.update { state ->
                            state.copy(
                                calendar = state.calendar.copy(
                                    content = CalendarContent.Error(),
                                    isMutationInProgress = false,
                                ),
                            )
                        }
                        return@launch
                    }
                    _uiState.update { state ->
                        state.copy(
                            calendar = state.calendar.copy(
                                content = CalendarContent.Ready(mapped.sortedForCalendar()),
                                isMutationInProgress = false,
                            ),
                        )
                    }
                }

                is ApiResult.Failure -> {
                    if (_uiState.value.route != AppRoute.SignIn) {
                        _uiState.update { state ->
                            state.copy(
                                calendar = state.calendar.copy(
                                    content = CalendarContent.Error(),
                                    isMutationInProgress = false,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createCalendarEvent(draft: CalendarEventDraft) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (snapshot.calendar.isMutationInProgress || !snapshot.calendar.canCreateEvents) return

        val idempotencyKey = if (calendarCreationDraft == draft) {
            calendarCreationIdempotencyKey
        } else {
            null
        } ?: UUID.randomUUID().toString().also { key ->
            calendarCreationDraft = draft
            calendarCreationIdempotencyKey = key
        }
        beginCalendarMutation()
        viewModelScope.launch {
            when (
                val result = authorized { token ->
                    gateway.createCalendarEvent(
                        accessToken = token,
                        householdId = household.id,
                        idempotencyKey = idempotencyKey,
                        configuration = draft.toDto(),
                    )
                }
            ) {
                is ApiResult.Success -> {
                    calendarCreationDraft = null
                    calendarCreationIdempotencyKey = null
                    applyCalendarMutation(result.value)
                }

                is ApiResult.Failure -> finishCalendarMutation(
                    failure = result,
                    fallback = CalendarMutationProblem.CREATE_FAILED,
                )
            }
        }
    }

    private fun updateCalendarEvent(action: CalendarAction.UpdateEvent) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val currentEvent = snapshot.calendar.readyEvents().firstOrNull { it.id == action.eventId }
        if (snapshot.calendar.isMutationInProgress || currentEvent?.canEdit != true) return

        beginCalendarMutation()
        viewModelScope.launch {
            when (
                val result = authorized { token ->
                    gateway.updateCalendarEvent(
                        accessToken = token,
                        householdId = household.id,
                        eventId = action.eventId,
                        expectedVersion = action.expectedVersion,
                        configuration = action.draft.toDto(),
                    )
                }
            ) {
                is ApiResult.Success -> applyCalendarMutation(result.value)
                is ApiResult.Failure -> finishCalendarMutation(
                    failure = result,
                    fallback = CalendarMutationProblem.UPDATE_FAILED,
                )
            }
        }
    }

    private fun deleteCalendarEvent(action: CalendarAction.DeleteEvent) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val currentEvent = snapshot.calendar.readyEvents().firstOrNull { it.id == action.eventId }
        if (snapshot.calendar.isMutationInProgress || currentEvent?.canDelete != true) return

        beginCalendarMutation()
        viewModelScope.launch {
            when (
                val result = authorized { token ->
                    gateway.deleteCalendarEvent(
                        accessToken = token,
                        householdId = household.id,
                        eventId = action.eventId,
                        expectedVersion = action.expectedVersion,
                    )
                }
            ) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val remaining = state.calendar.readyEvents()
                            .filterNot { it.id == action.eventId }
                        state.copy(
                            calendar = state.calendar.copy(
                                content = CalendarContent.Ready(remaining),
                                isMutationInProgress = false,
                                mutationProblem = null,
                            ),
                        )
                    }
                }

                is ApiResult.Failure -> finishCalendarMutation(
                    failure = result,
                    fallback = CalendarMutationProblem.DELETE_FAILED,
                )
            }
        }
    }

    private fun beginCalendarMutation() {
        _uiState.update { state ->
            state.copy(
                calendar = state.calendar.copy(
                    isMutationInProgress = true,
                    mutationProblem = null,
                ),
            )
        }
    }

    private fun applyCalendarMutation(event: CalendarEventDto) {
        val snapshot = _uiState.value
        val mapped = runCatching { event.toCalendarUi(snapshot) }.getOrElse {
            finishCalendarMutation(
                failure = ApiResult.Failure(
                    code = "CALENDAR_RESPONSE_INVALID",
                    title = "Calendar response could not be read.",
                ),
                fallback = CalendarMutationProblem.UPDATE_FAILED,
            )
            return
        }
        _uiState.update { state ->
            val range = state.calendar.visibleRange()
            val updated = state.calendar.readyEvents()
                .filterNot { it.id == mapped.id }
                .plus(if (mapped.date in range) listOf(mapped) else emptyList())
                .sortedForCalendar()
            state.copy(
                calendar = state.calendar.copy(
                    content = CalendarContent.Ready(updated),
                    isMutationInProgress = false,
                    mutationProblem = null,
                ),
            )
        }
    }

    private fun finishCalendarMutation(
        failure: ApiResult.Failure,
        fallback: CalendarMutationProblem,
    ) {
        if (_uiState.value.route == AppRoute.SignIn) return
        val conflict = failure.code == "CALENDAR_EVENT_VERSION_CONFLICT" || failure.status == 412
        _uiState.update { state ->
            state.copy(
                calendar = state.calendar.copy(
                    isMutationInProgress = false,
                    mutationProblem = if (conflict) {
                        CalendarMutationProblem.VERSION_CONFLICT
                    } else {
                        fallback
                    },
                ),
            )
        }
        if (conflict || failure.code == "CALENDAR_EVENT_NOT_FOUND") {
            loadCalendar(preserveMutationProblem = true)
        }
    }

    fun register() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || snapshot.isRestoringSession) return
        val errors = AppFormValidator.registration(snapshot.auth)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null, correlationId = null) }
            return
        }

        submit {
            when (
                val result = gateway.register(
                    RegisterPayload(
                        email = snapshot.auth.email.trim(),
                        password = snapshot.auth.password,
                        displayName = snapshot.auth.displayName.trim(),
                        preferredLocale = preferredLocale,
                        ageConfirmed = snapshot.auth.ageConfirmed,
                        termsAccepted = snapshot.auth.termsAccepted,
                        marketingConsent = snapshot.auth.marketingConsent,
                    ),
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            route = AppRoute.VerifyEmail,
                            auth = it.auth.copy(password = "", verificationCode = ""),
                            isSubmitting = false,
                            error = null,
                            notice = UiMessage.RegistrationAccepted,
                            fieldErrors = emptyMap(),
                            developmentVerificationCode = result.value.developmentVerificationCode,
                        )
                    }
                }

                is ApiResult.Failure -> showFailure(result)
            }
        }
    }

    fun verifyEmail() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || snapshot.isRestoringSession) return
        val errors = AppFormValidator.verification(snapshot.auth)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null, correlationId = null) }
            return
        }

        submit {
            when (
                val result = gateway.verifyEmail(
                    VerifyEmailPayload(
                        email = snapshot.auth.email.trim(),
                        code = snapshot.auth.verificationCode.trim(),
                        deviceName = deviceName,
                    ),
                )
            ) {
                is ApiResult.Success -> establishSession(result.value)
                is ApiResult.Failure -> showFailure(result)
            }
        }
    }

    fun resendVerification() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || snapshot.isRestoringSession) return
        val email = snapshot.auth.email.trim()
        if (email.isEmpty()) return

        submit {
            when (
                val result = gateway.resendVerification(
                    ResendVerificationPayload(email = email),
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = null,
                            notice = UiMessage.VerificationCodeSent,
                            correlationId = null,
                            developmentVerificationCode =
                                result.value.developmentVerificationCode,
                        )
                    }
                }

                is ApiResult.Failure -> showFailure(result)
            }
        }
    }

    fun signIn() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || snapshot.isRestoringSession) return
        val errors = AppFormValidator.signIn(snapshot.auth)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null, correlationId = null) }
            return
        }

        submit {
            when (
                val result = gateway.signIn(
                    SignInPayload(
                        email = snapshot.auth.email.trim(),
                        password = snapshot.auth.password,
                        deviceName = deviceName,
                    ),
                )
            ) {
                is ApiResult.Success -> establishSession(result.value)
                is ApiResult.Failure -> showFailure(result)
            }
        }
    }

    fun retryHouseholds() {
        if (_uiState.value.isSubmitting || session === null) return
        submit { loadHouseholds() }
    }

    fun openCreateHousehold() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                route = AppRoute.HouseholdSetup,
                household = it.household.copy(name = ""),
                householdEditorMode = HouseholdEditorMode.Create,
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
    }

    fun openHouseholdEditor() {
        val selected = _uiState.value.selectedHousehold ?: return
        if (_uiState.value.isSubmitting || selected.role !in setOf("owner", "admin")) return
        householdCreationIdempotencyKey = null
        _uiState.update {
            it.copy(
                route = AppRoute.HouseholdSetup,
                household = selected.toForm(),
                householdEditorMode = HouseholdEditorMode.Edit,
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
    }

    fun closeHouseholdEditor() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                route = if (it.selectedHousehold === null) {
                    AppRoute.HouseholdChoice
                } else {
                    AppRoute.Home
                },
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
    }

    fun createHousehold() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) return
        val errors = AppFormValidator.household(snapshot.household)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null, correlationId = null) }
            return
        }

        val configuration = snapshot.household.toDto()
        submit {
            val result = if (snapshot.householdEditorMode == HouseholdEditorMode.Edit) {
                val selected = snapshot.selectedHousehold
                if (selected === null) {
                    ApiResult.Failure(
                        code = "HOUSEHOLD_NOT_FOUND",
                        title = "No household is selected.",
                    )
                } else {
                    authorized { token ->
                        gateway.updateHousehold(
                            accessToken = token,
                            householdId = selected.id,
                            expectedVersion = selected.version,
                            configuration = configuration,
                        )
                    }
                }
            } else {
                val key = householdCreationIdempotencyKey ?: UUID.randomUUID().toString().also {
                    householdCreationIdempotencyKey = it
                }
                authorized { token -> gateway.createHousehold(token, key, configuration) }
            }
            when (result) {
                is ApiResult.Success -> {
                    householdCreationIdempotencyKey = null
                    _uiState.update {
                        val updatedHouseholds = if (snapshot.householdEditorMode == HouseholdEditorMode.Edit) {
                            it.households.map { household ->
                                if (household.id == result.value.id) result.value else household
                            }
                        } else {
                            listOf(result.value)
                        }
                        it.copy(
                            route = AppRoute.Home,
                            isSubmitting = false,
                            error = null,
                            notice = null,
                            fieldErrors = emptyMap(),
                            households = updatedHouseholds,
                            selectedHousehold = result.value,
                            householdEditorMode = HouseholdEditorMode.Edit,
                            calendar = it.calendar.forHousehold(result.value),
                            money = it.money.forHousehold(result.value),
                            tasks = it.tasks.forHousehold(result.value),
                        )
                    }
                    loadCalendar()
                    loadMoney()
                    loadTasks()
                }

                is ApiResult.Failure -> {
                    if (_uiState.value.route != AppRoute.SignIn) showFailure(result)
                }
            }
        }
    }

    fun reloadHouseholdEditor() {
        val selectedId = _uiState.value.selectedHousehold?.id ?: return
        if (_uiState.value.isSubmitting) return
        submit {
            when (val result = authorized { token -> gateway.listHouseholds(token) }) {
                is ApiResult.Success -> {
                    val refreshed = result.value.firstOrNull { it.id == selectedId && it.status == "active" }
                    if (refreshed === null) {
                        showFailure(
                            ApiResult.Failure(
                                code = "HOUSEHOLD_NOT_FOUND",
                                title = "The household is no longer available.",
                                status = 404,
                            ),
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                route = AppRoute.HouseholdSetup,
                                household = refreshed.toForm(),
                                households = result.value.filter { item -> item.status == "active" },
                                selectedHousehold = refreshed,
                                householdEditorMode = HouseholdEditorMode.Edit,
                                isSubmitting = false,
                                error = null,
                                notice = UiMessage.HouseholdReloaded,
                                fieldErrors = emptyMap(),
                                correlationId = null,
                            )
                        }
                    }
                }

                is ApiResult.Failure -> showFailure(result)
            }
        }
    }

    fun openInvitationJoin() {
        if (_uiState.value.isSubmitting || session === null) return
        _uiState.update {
            it.copy(
                route = AppRoute.InvitationJoin,
                invitation = InvitationUiState(),
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
    }

    fun updateInvitationToken(value: String) {
        _uiState.update {
            it.copy(
                invitation = it.invitation.copy(
                    tokenInput = value.trim().take(64),
                    preview = null,
                ),
                error = null,
                fieldErrors = it.fieldErrors - FormField.InvitationToken,
                correlationId = null,
            )
        }
    }

    fun previewInvitation() {
        val token = _uiState.value.invitation.tokenInput.trim()
        if (_uiState.value.isSubmitting) return
        if (!INVITATION_TOKEN.matches(token)) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors +
                        (FormField.InvitationToken to UiMessage.InvitationTokenInvalid),
                    error = null,
                )
            }
            return
        }
        submit {
            when (val result = gateway.previewHouseholdInvitation(token)) {
                is ApiResult.Success -> {
                    val availabilityError = when (result.value.status) {
                        "expired" -> UiMessage.InvitationExpired
                        "unavailable" -> UiMessage.InvitationUnavailable
                        else -> null
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            invitation = it.invitation.copy(preview = result.value),
                            error = availabilityError,
                            correlationId = null,
                        )
                    }
                }

                is ApiResult.Failure -> showFailure(result)
            }
        }
    }

    fun acceptInvitation() {
        val snapshot = _uiState.value
        val token = snapshot.invitation.tokenInput.trim()
        if (snapshot.isSubmitting || snapshot.invitation.preview?.status != "pending") return
        submit {
            when (
                val result = authorized { accessToken ->
                    gateway.acceptHouseholdInvitation(accessToken, token)
                }
            ) {
                is ApiResult.Success -> {
                    val accepted = result.value.household
                    _uiState.update {
                        it.copy(
                            route = AppRoute.Home,
                            isSubmitting = false,
                            households = (it.households + accepted).distinctBy { item -> item.id },
                            selectedHousehold = accepted,
                            household = accepted.toForm(),
                            householdEditorMode = HouseholdEditorMode.Edit,
                            invitation = InvitationUiState(),
                            error = null,
                            notice = UiMessage.InvitationAccepted,
                            fieldErrors = emptyMap(),
                            correlationId = null,
                            calendar = it.calendar.forHousehold(accepted),
                            money = it.money.forHousehold(accepted),
                            tasks = it.tasks.forHousehold(accepted),
                        )
                    }
                    loadCalendar()
                    loadMoney()
                    loadTasks()
                }

                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    showFailure(result)
                }
            }
        }
    }

    fun closeInvitationFlow() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                route = if (it.selectedHousehold === null) {
                    AppRoute.HouseholdChoice
                } else {
                    AppRoute.Home
                },
                invitation = InvitationUiState(),
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
    }

    fun openInvitationManager() {
        val household = _uiState.value.selectedHousehold ?: return
        if (_uiState.value.isSubmitting || household.role !in setOf("owner", "admin")) return
        _uiState.update {
            it.copy(
                route = AppRoute.InvitationManage,
                invitation = InvitationUiState(),
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
        loadInvitations()
    }

    fun selectHousehold(householdId: String) {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || snapshot.selectedHousehold?.id == householdId) return
        val selected = snapshot.households.firstOrNull { it.id == householdId } ?: return
        calendarLoadJob?.cancel()
        moneyLoadJob?.cancel()
        tasksLoadJob?.cancel()
        _uiState.update {
            it.copy(
                route = AppRoute.Home,
                selectedHousehold = selected,
                household = selected.toForm(),
                householdEditorMode = HouseholdEditorMode.Edit,
                invitation = InvitationUiState(),
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
                calendar = it.calendar.forHousehold(selected),
                money = it.money.forHousehold(selected),
                tasks = it.tasks.forHousehold(selected),
                householdMembers = HouseholdMembersUiState(),
            )
        }
        loadCalendar()
        loadMoney()
        loadTasks()
        loadHouseholdMembers()
    }

    fun retryHouseholdMembers() = loadHouseholdMembers()

    fun actOnHouseholdMember(command: HouseholdMemberCommand) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        val ready = snapshot.householdMembers.content as? HouseholdMembersContent.Ready ?: return
        val target = ready.members.firstOrNull { it.membershipId == command.membershipId } ?: return
        if (snapshot.householdMembers.mutatingMembershipId != null || target.version != command.expectedVersion) return
        val allowed = when (command.action) {
            "change_role" -> target.canChangeRole && command.role in target.assignableRoles
            "suspend" -> target.canSuspend
            "reactivate" -> target.canReactivate
            "remove" -> target.canRemove
            "transfer_ownership" -> target.canTransferOwnership
            else -> false
        }
        if (!allowed) return
        _uiState.update {
            it.copy(householdMembers = it.householdMembers.copy(
                mutatingMembershipId = target.membershipId,
                problem = null,
            ))
        }
        viewModelScope.launch {
            val result = authorized { token ->
                gateway.actOnHouseholdMember(
                    accessToken = token,
                    householdId = household.id,
                    membershipId = target.membershipId,
                    expectedVersion = target.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                    action = HouseholdMemberActionDto(command.action, command.role),
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    if (command.action == "transfer_ownership") {
                        _uiState.update { state ->
                            val selected = state.selectedHousehold?.copy(role = "admin")
                            state.copy(
                                selectedHousehold = selected,
                                households = state.households.map { option ->
                                    if (option.id == household.id) option.copy(role = "admin") else option
                                },
                            )
                        }
                    }
                    loadHouseholdMembers()
                }
                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    if (result.code == "HOUSEHOLD_MEMBER_VERSION_CONFLICT") {
                        loadHouseholdMembers(HouseholdMembersProblem.VERSION_CONFLICT)
                    } else {
                        _uiState.update {
                            it.copy(householdMembers = it.householdMembers.copy(
                                mutatingMembershipId = null,
                                problem = HouseholdMembersProblem.ACTION_FAILED,
                            ))
                        }
                    }
                }
            }
        }
    }

    private fun loadHouseholdMembers(problemAfterLoad: HouseholdMembersProblem? = null) {
        val household = _uiState.value.selectedHousehold ?: return
        householdMembersLoadJob?.cancel()
        _uiState.update {
            it.copy(householdMembers = it.householdMembers.copy(
                content = HouseholdMembersContent.Loading,
                mutatingMembershipId = null,
                problem = problemAfterLoad,
            ))
        }
        householdMembersLoadJob = viewModelScope.launch {
            when (val result = authorized { token -> gateway.listHouseholdMembers(token, household.id) }) {
                is ApiResult.Success -> if (_uiState.value.selectedHousehold?.id == household.id) {
                    _uiState.update {
                        it.copy(householdMembers = HouseholdMembersUiState(
                            content = HouseholdMembersContent.Ready(
                                canInvite = result.value.canInvite,
                                canEditHousehold = result.value.canEditHousehold,
                                members = result.value.members.map { member -> member.toMemberUi() },
                            ),
                            problem = problemAfterLoad,
                        ))
                    }
                }
                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn &&
                    _uiState.value.selectedHousehold?.id == household.id) {
                    _uiState.update {
                        it.copy(householdMembers = HouseholdMembersUiState(
                            content = HouseholdMembersContent.Error,
                            problem = HouseholdMembersProblem.LOAD_FAILED,
                        ))
                    }
                }
            }
        }
    }

    fun updateInvitationEmail(value: String) {
        _uiState.update {
            it.copy(
                invitation = it.invitation.copy(email = value.take(254), createdToken = null),
                error = null,
                fieldErrors = it.fieldErrors - FormField.InvitationEmail,
                correlationId = null,
            )
        }
    }

    fun updateInvitationRole(value: String) {
        if (value !in setOf("admin", "member", "read_only")) return
        _uiState.update {
            it.copy(
                invitation = it.invitation.copy(role = value, createdToken = null),
                error = null,
                notice = null,
                correlationId = null,
            )
        }
    }

    fun createInvitation() {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (snapshot.isSubmitting || snapshot.route != AppRoute.InvitationManage) return
        val email = snapshot.invitation.email.trim()
        if (email.isNotEmpty() && !EMAIL.matches(email)) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors +
                        (FormField.InvitationEmail to UiMessage.EmailInvalid),
                )
            }
            return
        }
        submit {
            when (
                val result = authorized { token ->
                    gateway.createHouseholdInvitation(
                        accessToken = token,
                        householdId = household.id,
                        payload = CreateHouseholdInvitationPayload(
                            role = snapshot.invitation.role,
                            email = email.ifBlank { null },
                        ),
                    )
                }
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            invitation = it.invitation.copy(
                                email = "",
                                invitations = listOf(result.value.copy(token = null)) +
                                    it.invitation.invitations,
                                createdToken = result.value.token,
                            ),
                            error = null,
                            notice = UiMessage.InvitationCreated,
                            fieldErrors = emptyMap(),
                            correlationId = null,
                        )
                    }
                }

                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    showFailure(result)
                }
            }
        }
    }

    fun revokeInvitation(invitationId: String) {
        val snapshot = _uiState.value
        val household = snapshot.selectedHousehold ?: return
        if (snapshot.isSubmitting || snapshot.route != AppRoute.InvitationManage) return
        submit {
            when (
                val result = authorized { token ->
                    gateway.revokeHouseholdInvitation(token, household.id, invitationId)
                }
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            invitation = it.invitation.copy(
                                invitations = it.invitation.invitations.map { invitation ->
                                    if (invitation.id == invitationId) {
                                        invitation.copy(status = "revoked")
                                    } else {
                                        invitation
                                    }
                                },
                                createdToken = null,
                            ),
                            error = null,
                            notice = UiMessage.InvitationRevoked,
                            correlationId = null,
                        )
                    }
                }

                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    showFailure(result)
                }
            }
        }
    }

    private fun loadInvitations() {
        val household = _uiState.value.selectedHousehold ?: return
        submit {
            when (
                val result = authorized { token ->
                    gateway.listHouseholdInvitations(token, household.id)
                }
            ) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        invitation = it.invitation.copy(invitations = result.value),
                        error = null,
                        correlationId = null,
                    )
                }

                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    showFailure(result)
                }
            }
        }
    }

    fun signOut() {
        if (_uiState.value.isSubmitting) return
        val active = session
        if (active === null) {
            submit {
                val cleared = sessionStore.clear()
                clearSession(
                    if (cleared) UiMessage.SignedOut else UiMessage.SecureStorageUnavailable,
                )
            }
            return
        }
        submit {
            val result = gateway.signOut(active.accessToken)
            val cleared = sessionStore.clear()
            clearSession(
                if (!cleared) {
                    UiMessage.SecureStorageUnavailable
                } else if (result is ApiResult.Success) {
                    UiMessage.SignedOut
                } else {
                    UiMessage.SessionRevocationUnconfirmed
                },
            )
        }
    }

    fun deleteAccount(password: String) {
        if (_uiState.value.isSubmitting || password.isBlank()) return
        submit {
            when (val result = authorized { accessToken -> gateway.deleteAccount(accessToken, password) }) {
                is ApiResult.Success -> {
                    sessionStore.clear()
                    clearSession(UiMessage.AccountDeleted)
                }
                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    val message = when (result.code) {
                        "ACCOUNT_DELETION_OWNER_TRANSFER_REQUIRED" ->
                            UiMessage.AccountDeletionOwnerTransferRequired
                        "RECENT_AUTHENTICATION_REQUIRED" -> UiMessage.RecentAuthenticationRequired
                        else -> UiMessage.AccountDeletionFailed
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = message,
                            correlationId = result.correlationId,
                        )
                    }
                }
            }
        }
    }

    fun exportAccount(password: String) {
        if (_uiState.value.isSubmitting || password.isBlank()) return
        submit {
            when (val result = authorized { accessToken -> gateway.exportAccount(accessToken, password) }) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        accountExport = result.value,
                        notice = UiMessage.AccountExportReady,
                        error = null,
                        correlationId = null,
                    )
                }
                is ApiResult.Failure -> if (_uiState.value.route != AppRoute.SignIn) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = if (result.code == "RECENT_AUTHENTICATION_REQUIRED") {
                                UiMessage.RecentAuthenticationRequired
                            } else {
                                UiMessage.AccountExportFailed
                            },
                            correlationId = result.correlationId,
                        )
                    }
                }
            }
        }
    }

    fun accountExportHandled() {
        _uiState.update { it.copy(accountExport = null) }
    }

    private suspend fun restoreSession() {
        when (val stored = sessionStore.load()) {
            SessionLoadResult.Missing -> {
                _uiState.update {
                    it.copy(
                        isRestoringSession = false,
                        canRetrySessionRestore = false,
                    )
                }
            }

            SessionLoadResult.Invalid -> {
                _uiState.update {
                    it.copy(
                        route = AppRoute.Welcome,
                        isRestoringSession = false,
                        canRetrySessionRestore = false,
                        notice = UiMessage.SecureSessionReset,
                    )
                }
            }

            SessionLoadResult.Unavailable -> showSessionRestoreFailure(
                message = UiMessage.SecureStorageUnavailable,
            )

            is SessionLoadResult.Restored -> restoreSession(stored.session)
        }
    }

    private suspend fun restoreSession(stored: SessionDto) {
        when (val refreshed = gateway.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                if (sessionStore.save(refreshed.value) == SessionSaveResult.SAVED) {
                    activateSession(refreshed.value)
                } else {
                    gateway.signOut(refreshed.value.accessToken)
                    sessionStore.clear()
                    showSessionRestoreFailure(
                        message = UiMessage.SecureStorageUnavailable,
                        retryAllowed = false,
                    )
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.code == "NETWORK_UNAVAILABLE") {
                    showSessionRestoreFailure(UiMessage.SessionRestoreNetworkUnavailable)
                } else {
                    sessionStore.clear()
                    _uiState.update {
                        it.copy(
                            route = AppRoute.Welcome,
                            isRestoringSession = false,
                            canRetrySessionRestore = false,
                            account = null,
                            notice = UiMessage.SessionExpired,
                        )
                    }
                }
            }
        }
    }

    private fun showSessionRestoreFailure(
        message: UiMessage,
        retryAllowed: Boolean = true,
    ) {
        _uiState.update {
            it.copy(
                route = AppRoute.Welcome,
                isRestoringSession = false,
                canRetrySessionRestore = retryAllowed,
                account = null,
                error = null,
                notice = message,
                correlationId = null,
            )
        }
    }

    private suspend fun establishSession(newSession: SessionDto) {
        if (sessionStore.save(newSession) != SessionSaveResult.SAVED) {
            gateway.signOut(newSession.accessToken)
            sessionStore.clear()
            _uiState.update {
                it.copy(
                    isRestoringSession = false,
                    canRetrySessionRestore = false,
                    isSubmitting = false,
                    auth = it.auth.copy(password = "", verificationCode = ""),
                    account = null,
                    error = UiMessage.SecureStorageUnavailable,
                    notice = null,
                    correlationId = null,
                )
            }
            return
        }
        activateSession(newSession)
    }

    private suspend fun activateSession(newSession: SessionDto) {
        session = newSession
        _uiState.update {
            it.copy(
                route = AppRoute.HouseholdGate,
                isRestoringSession = false,
                canRetrySessionRestore = false,
                auth = it.auth.copy(password = "", verificationCode = ""),
                account = newSession.account,
                isSubmitting = true,
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
                developmentVerificationCode = null,
            )
        }
        loadHouseholds()
    }

    private suspend fun loadHouseholds() {
        _uiState.update {
            it.copy(route = AppRoute.HouseholdGate, isSubmitting = true, error = null, correlationId = null)
        }
        when (val result = authorized { token -> gateway.listHouseholds(token) }) {
            is ApiResult.Success -> {
                val active = result.value.filter { it.status == "active" }
                val selected = active.firstOrNull()
                _uiState.update {
                    it.copy(
                        route = if (active.isEmpty()) AppRoute.HouseholdChoice else AppRoute.Home,
                        households = active,
                        selectedHousehold = selected,
                        household = selected?.toForm() ?: it.household,
                        householdEditorMode = if (active.isEmpty()) {
                            HouseholdEditorMode.Create
                        } else {
                            HouseholdEditorMode.Edit
                        },
                        isSubmitting = false,
                        error = null,
                        fieldErrors = emptyMap(),
                        correlationId = null,
                        calendar = selected?.let { household ->
                            it.calendar.forHousehold(household)
                        } ?: CalendarUiState(),
                        money = selected?.let { household ->
                            it.money.forHousehold(household)
                        } ?: MoneyUiState(),
                        tasks = selected?.let { household ->
                            it.tasks.forHousehold(household)
                        } ?: TasksUiState(),
                        householdMembers = HouseholdMembersUiState(),
                    )
                }
                if (selected != null) {
                    loadCalendar()
                    loadMoney()
                    loadTasks()
                    loadHouseholdMembers()
                }
            }

            is ApiResult.Failure -> {
                if (_uiState.value.route != AppRoute.SignIn) {
                    _uiState.update {
                        it.copy(
                            route = AppRoute.HouseholdGate,
                            isSubmitting = false,
                            error = mapFailure(result, UiMessage.HouseholdLoadFailed),
                            correlationId = result.correlationId,
                        )
                    }
                }
            }
        }
    }

    private suspend fun <T> authorized(
        request: suspend (accessToken: String) -> ApiResult<T>,
    ): ApiResult<T> {
        val active = session ?: return ApiResult.Failure(
            code = "SESSION_INVALID",
            title = "No active session.",
            status = 401,
        )
        val first = request(active.accessToken)
        if (first !is ApiResult.Failure || (first.status != 401 && first.code != "SESSION_INVALID")) {
            return first
        }

        return when (val refreshed = gateway.refresh(active.refreshToken)) {
            is ApiResult.Success -> {
                if (sessionStore.save(refreshed.value) == SessionSaveResult.SAVED) {
                    session = refreshed.value
                    _uiState.update { it.copy(account = refreshed.value.account) }
                    request(refreshed.value.accessToken)
                } else {
                    gateway.signOut(refreshed.value.accessToken)
                    expireSession(UiMessage.SecureStorageUnavailable)
                    ApiResult.Failure(
                        code = "LOCAL_SECURE_STORAGE_UNAVAILABLE",
                        title = "Secure session storage is unavailable.",
                    )
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.code != "NETWORK_UNAVAILABLE") {
                    expireSession(UiMessage.SessionExpired)
                }
                refreshed
            }
        }
    }

    private fun submit(block: suspend () -> Unit) {
        _uiState.update {
            it.copy(
                isSubmitting = true,
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
        viewModelScope.launch { block() }
    }

    private fun showFailure(failure: ApiResult.Failure) {
        _uiState.update {
            it.copy(
                isSubmitting = false,
                error = mapFailure(failure),
                fieldErrors = mapViolations(failure.violations),
                correlationId = failure.correlationId,
            )
        }
    }

    private fun mapFailure(
        failure: ApiResult.Failure,
        fallback: UiMessage = UiMessage.ServiceUnavailable,
    ): UiMessage = when (failure.code) {
        "INVALID_CREDENTIALS" -> UiMessage.InvalidCredentials
        "EMAIL_VERIFICATION_REQUIRED" -> UiMessage.EmailVerificationRequired
        "VERIFICATION_CODE_INVALID" -> UiMessage.VerificationCodeInvalid
        "VERIFICATION_CODE_EXPIRED" -> UiMessage.VerificationCodeExpired
        "ACCOUNT_UNAVAILABLE" -> UiMessage.AccountUnavailable
        "SESSION_INVALID" -> UiMessage.SessionExpired
        "IDEMPOTENCY_KEY_REUSED" -> UiMessage.IdempotencyKeyReused
        "HOUSEHOLD_VERSION_CONFLICT" -> UiMessage.HouseholdVersionConflict
        "INVITATION_NOT_FOUND" -> UiMessage.InvitationNotFound
        "INVITATION_EXPIRED" -> UiMessage.InvitationExpired
        "INVITATION_UNAVAILABLE", "INVITATION_HOUSEHOLD_UNAVAILABLE" ->
            UiMessage.InvitationUnavailable
        "INVITATION_EMAIL_MISMATCH" -> UiMessage.InvitationEmailMismatch
        "INVITATION_MANAGE_FORBIDDEN" -> UiMessage.InvitationManageForbidden
        "INVITATION_ROLE_DELEGATION_FORBIDDEN" -> UiMessage.InvitationRoleForbidden
        "VALIDATION_FAILED" -> UiMessage.RequestInvalid
        "RATE_LIMITED" -> UiMessage.RateLimited
        "NETWORK_UNAVAILABLE" -> UiMessage.NetworkUnavailable
        else -> fallback
    }

    private fun mapViolations(violations: List<FieldViolationDto>): Map<FormField, UiMessage> =
        violations.mapNotNull { violation ->
            when (violation.field) {
                "displayName" -> FormField.DisplayName to UiMessage.DisplayNameRequired
                "email" -> FormField.Email to UiMessage.EmailInvalid
                "password" -> FormField.Password to UiMessage.PasswordTooShort
                "code" -> FormField.VerificationCode to UiMessage.VerificationCodeInvalidInput
                "ageConfirmed" -> FormField.AgeConfirmation to UiMessage.AgeConfirmationRequired
                "termsAccepted" -> FormField.TermsConfirmation to UiMessage.TermsConfirmationRequired
                "name" -> FormField.HouseholdName to UiMessage.HouseholdNameRequired
                "countryCode" -> FormField.CountryCode to UiMessage.CountryCodeInvalid
                "timezone" -> FormField.Timezone to UiMessage.TimezoneInvalid
                "currency" -> FormField.Currency to UiMessage.CurrencyInvalid
                "cycleAnchor" -> FormField.CycleAnchor to UiMessage.CycleAnchorInvalid
                else -> null
            }
        }.toMap()

    private suspend fun expireSession(message: UiMessage) {
        sessionStore.clear()
        calendarLoadJob?.cancel()
        moneyLoadJob?.cancel()
        tasksLoadJob?.cancel()
        session = null
        householdCreationIdempotencyKey = null
        calendarCreationIdempotencyKey = null
        calendarCreationDraft = null
        expenseCreationIdempotencyKey = null
        expenseCreationDraft = null
        templateCreationIdempotencyKey = null
        templateCreationDraft = null
        taskCreationIdempotencyKey = null
        taskCreationDraft = null
        _uiState.update {
            it.copy(
                route = AppRoute.SignIn,
                auth = it.auth.copy(password = "", verificationCode = ""),
                account = null,
                households = emptyList(),
                selectedHousehold = null,
                isRestoringSession = false,
                canRetrySessionRestore = false,
                isSubmitting = false,
                error = message,
                notice = null,
                fieldErrors = emptyMap(),
                calendar = CalendarUiState(),
                money = MoneyUiState(),
                tasks = TasksUiState(),
                invitation = InvitationUiState(),
            )
        }
    }

    private fun clearSession(notice: UiMessage) {
        calendarLoadJob?.cancel()
        moneyLoadJob?.cancel()
        tasksLoadJob?.cancel()
        session = null
        householdCreationIdempotencyKey = null
        calendarCreationIdempotencyKey = null
        calendarCreationDraft = null
        expenseCreationIdempotencyKey = null
        expenseCreationDraft = null
        templateCreationIdempotencyKey = null
        templateCreationDraft = null
        taskCreationIdempotencyKey = null
        taskCreationDraft = null
        _uiState.update {
            AppUiState(
                route = AppRoute.Welcome,
                auth = AuthFormState(email = it.auth.email),
                household = it.household.copy(name = ""),
                isRestoringSession = false,
                notice = notice,
            )
        }
    }

    private fun moveTo(route: AppRoute) {
        if (_uiState.value.isSubmitting || _uiState.value.isRestoringSession) return
        _uiState.update {
            it.copy(
                route = route,
                auth = if (route == AppRoute.Welcome) it.auth.copy(password = "") else it.auth,
                error = null,
                notice = null,
                fieldErrors = emptyMap(),
                correlationId = null,
            )
        }
    }

    private fun updateAuth(
        field: FormField,
        transform: AuthFormState.() -> AuthFormState,
    ) {
        _uiState.update { state ->
            state.copy(
                auth = transform(state.auth),
                error = null,
                fieldErrors = state.fieldErrors - field,
                correlationId = null,
            )
        }
    }

    private fun updateHousehold(
        field: FormField? = null,
        transform: HouseholdFormState.() -> HouseholdFormState,
    ) {
        householdCreationIdempotencyKey = null
        _uiState.update { state ->
            state.copy(
                household = transform(state.household),
                error = null,
                fieldErrors = if (field === null) state.fieldErrors else state.fieldErrors - field,
                correlationId = null,
            )
        }
    }

    private fun CalendarUiState.visibleRange() = CalendarPeriodCalculator.rangeFor(
        view = view,
        anchorDate = anchorDate,
        firstDayOfWeek = firstDayOfWeek,
    )

    private fun CalendarUiState.readyEvents(): List<CalendarEventUi> =
        (content as? CalendarContent.Ready)?.events.orEmpty()

    private fun List<CalendarEventUi>.sortedForCalendar(): List<CalendarEventUi> = sortedWith(
        compareBy<CalendarEventUi> { it.date }
            .thenBy { it.startTime ?: LocalTime.MIN }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    )

    private fun CalendarUiState.forHousehold(
        household: com.sharedhouse.network.HouseholdDto,
    ): CalendarUiState {
        val householdZone = runCatching { ZoneId.of(household.timezone) }
            .getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(householdZone)
        return CalendarUiState(
            view = view,
            anchorDate = today,
            selectedDate = today,
            firstDayOfWeek = runCatching { DayOfWeek.of(household.firstDayOfWeek) }
                .getOrDefault(DayOfWeek.MONDAY),
            zoneId = householdZone,
            content = CalendarContent.Loading,
            canCreateEvents = household.role in CALENDAR_WRITER_ROLES,
        )
    }

    private fun MoneyUiState.forHousehold(
        household: com.sharedhouse.network.HouseholdDto,
    ) = MoneyUiState(
        currency = household.currency,
        content = MoneyContent.Loading,
        canCreate = household.role in MONEY_WRITER_ROLES,
        canManageTemplates = household.role in MONEY_MANAGER_ROLES,
    )

    private fun TasksUiState.forHousehold(
        household: com.sharedhouse.network.HouseholdDto,
    ) = TasksUiState(
        content = TasksContent.Loading,
        canCreate = household.role in TASK_MANAGER_ROLES,
    )

    private fun HouseholdTaskDto.toTaskUi(members: List<HouseholdTaskMemberDto>): HouseholdTaskUi {
        val household = requireNotNull(_uiState.value.selectedHousehold)
        require(householdId == household.id)
        val currentMembershipId = members.firstOrNull { it.isCurrentUser }?.membershipId
            ?: _uiState.value.tasks.members.firstOrNull { it.isCurrentUser }?.membershipId
        return HouseholdTaskUi(
            id = id,
            title = title,
            instructions = instructions,
            zone = zone,
            priority = TaskPriority.valueOf(priority.uppercase()),
            dueDate = LocalDate.parse(dueDate),
            dueTime = dueTime,
            estimatedMinutes = estimatedMinutes,
            assigneeMembershipId = assigneeMembershipId,
            assigneeDisplayName = assigneeDisplayName,
            isMine = assigneeMembershipId == currentMembershipId,
            status = TaskStatus.valueOf(status.uppercase()),
            completionNote = completionNote,
            completedAt = completedAt?.let(Instant::parse),
            requests = requests.map { request ->
                TaskRequestUi(
                    id = request.id,
                    type = TaskRequestType.valueOf(request.type.uppercase()),
                    status = TaskRequestStatus.valueOf(request.status.uppercase()),
                    reason = request.reason,
                    requestedAssigneeMembershipId = request.requestedAssigneeMembershipId,
                    requestedDueDate = request.requestedDueDate?.let(LocalDate::parse),
                    requestedDueTime = request.requestedDueTime,
                    createdByDisplayName = request.createdByDisplayName,
                    resolutionNote = request.resolutionNote,
                )
            },
            canManage = canManage,
            canStart = canStart,
            canComplete = canComplete,
            canRequest = canRequest,
            version = version,
        )
    }

    private fun ExpenseDto.toMoneyUi(): ExpenseUi {
        val household = requireNotNull(_uiState.value.selectedHousehold)
        require(householdId == household.id && amount.currency == household.currency)
        val parsedStatus = when (status) {
            "proposed" -> ExpenseStatus.PROPOSED
            "approved" -> ExpenseStatus.APPROVED
            "reversed" -> ExpenseStatus.REVERSED
            else -> error("Unknown expense status")
        }
        require(splitMethod == "equal" && currentUserShare.currency == amount.currency)
        return ExpenseUi(
            id = id,
            title = title,
            category = com.sharedhouse.android.ui.money.ExpenseCategory.fromWire(category),
            customCategoryName = customCategoryName,
            amountMinor = amount.minorUnits,
            currency = amount.currency,
            dueDate = LocalDate.parse(dueDate),
            notes = notes,
            sourceTemplateId = sourceTemplateId,
            occurrenceDate = occurrenceDate?.let(LocalDate::parse),
            status = parsedStatus,
            allocations = allocations.map { allocation ->
                require(allocation.amount.currency == amount.currency)
                ExpenseAllocationUi(
                    membershipId = allocation.membershipId,
                    displayName = allocation.displayName,
                    amountMinor = allocation.amount.minorUnits,
                    roundingAdjustmentMinor = allocation.roundingAdjustmentMinor,
                    status = when (allocation.status) {
                        "outstanding" -> ExpenseAllocationStatus.OUTSTANDING
                        "declared" -> ExpenseAllocationStatus.DECLARED
                        "paid" -> ExpenseAllocationStatus.PAID
                        "disputed" -> ExpenseAllocationStatus.DISPUTED
                        else -> error("Unknown allocation payment status")
                    },
                    paymentDeclarations = allocation.paymentDeclarations.map { payment ->
                        require(
                            payment.expenseId == id &&
                                payment.allocationMembershipId == allocation.membershipId &&
                                payment.amount.currency == amount.currency,
                        )
                        ExpensePaymentUi(
                            id = payment.id,
                            payerDisplayName = payment.payerDisplayName,
                            amountMinor = payment.amount.minorUnits,
                            currency = payment.amount.currency,
                            method = ExpensePaymentMethod.fromWire(payment.method),
                            reference = payment.reference,
                            note = payment.note,
                            paidAt = Instant.parse(payment.paidAt),
                            status = when (payment.status) {
                                "declared" -> ExpensePaymentStatus.DECLARED
                                "confirmed" -> ExpensePaymentStatus.CONFIRMED
                                "disputed" -> ExpensePaymentStatus.DISPUTED
                                "reversed" -> ExpensePaymentStatus.REVERSED
                                else -> error("Unknown payment status")
                            },
                            confirmedAt = payment.confirmedAt?.let(Instant::parse),
                            disputeReason = payment.disputeReason,
                            reversedAt = payment.reversedAt?.let(Instant::parse),
                            reversalReason = payment.reversalReason,
                            canConfirm = payment.canConfirm,
                            canDispute = payment.canDispute,
                            canReverse = payment.canReverse,
                            version = payment.version,
                        )
                    },
                    canDeclarePayment = allocation.canDeclarePayment,
                    isCurrentUser = allocation.isCurrentUser,
                )
            },
            currentUserShareMinor = currentUserShare.minorUnits,
            canApprove = canApprove,
            canReverse = canReverse,
            version = version,
        )
    }

    private enum class PaymentTransition { CONFIRM, DISPUTE, REVERSE }

    private fun ExpenseTemplateDraft.toDto(currency: String) = ExpenseTemplateConfigurationDto(
        title = title,
        category = category.wireValue,
        customCategoryName = customCategoryName,
        amount = MoneyDto(amountMinor, currency),
        cadence = cadence.wireValue,
        nextDueDate = nextDueDate.toString(),
        notes = notes,
    )

    private fun ExpenseTemplateDto.toTemplateUi(): ExpenseTemplateUi {
        val household = requireNotNull(_uiState.value.selectedHousehold)
        require(householdId == household.id && amount.currency == household.currency)
        return ExpenseTemplateUi(
            id = id,
            title = title,
            category = com.sharedhouse.android.ui.money.ExpenseCategory.fromWire(category),
            customCategoryName = customCategoryName,
            amountMinor = amount.minorUnits,
            currency = amount.currency,
            cadence = com.sharedhouse.android.ui.money.ExpenseTemplateCadence.fromWire(cadence),
            nextDueDate = LocalDate.parse(nextDueDate),
            notes = notes,
            active = status == "active",
            canManage = canManage,
            version = version,
        )
    }

    private fun CalendarEventDraft.toDto() = CalendarEventConfigurationDto(
        title = title,
        description = description,
        type = type.wireValue,
        date = date.toString(),
        startTime = startTime?.toString(),
        endTime = endTime?.toString(),
        reminderMinutesBefore = reminderMinutesBefore,
    )

    private fun CalendarEventDto.toCalendarUi(state: AppUiState): CalendarEventUi {
        val household = requireNotNull(state.selectedHousehold) {
            "A selected household is required to map calendar events."
        }
        require(householdId == household.id) { "Calendar event belongs to another household." }
        val roleCanManageAll = household.role == "owner" || household.role == "admin"
        val memberOwnsEvent = household.role == "member" && createdByUserId == state.account?.id
        val canChange = roleCanManageAll || memberOwnsEvent
        return CalendarEventUi(
            id = id,
            title = title,
            description = description,
            type = CalendarEventType.fromWireValue(type),
            date = LocalDate.parse(date),
            startTime = startTime?.let(LocalTime::parse),
            endTime = endTime?.let(LocalTime::parse),
            reminderMinutesBefore = reminderMinutesBefore,
            version = version,
            canEdit = canChange,
            canDelete = canChange,
        )
    }

    private fun HouseholdFormState.toDto() = HouseholdConfigurationDto(
        name = name.trim(),
        countryCode = countryCode.trim().uppercase(),
        timezone = timezone.trim(),
        currency = currency.trim().uppercase(),
        firstDayOfWeek = firstDayOfWeek,
        cycleType = cycleType,
        cycleAnchor = cycleAnchor.trim(),
    )

    private fun com.sharedhouse.network.HouseholdDto.toForm() = HouseholdFormState(
        name = name,
        countryCode = countryCode,
        timezone = timezone,
        currency = currency,
        firstDayOfWeek = firstDayOfWeek,
        cycleType = cycleType,
        cycleAnchor = cycleAnchor,
    )

    private fun HouseholdMemberDto.toMemberUi() = HouseholdMemberUi(
        membershipId = membershipId,
        displayName = displayName,
        role = role,
        status = status,
        isCurrentUser = isCurrentUser,
        canChangeRole = canChangeRole,
        canSuspend = canSuspend,
        canReactivate = canReactivate,
        canRemove = canRemove,
        canTransferOwnership = canTransferOwnership,
        assignableRoles = assignableRoles,
        joinedAt = joinedAt,
        version = version,
    )

    private companion object {
        val CALENDAR_WRITER_ROLES = setOf("owner", "admin", "member")
        val MONEY_WRITER_ROLES = setOf("owner", "admin", "member")
        val MONEY_MANAGER_ROLES = setOf("owner", "admin")
        val TASK_MANAGER_ROLES = setOf("owner", "admin")
        val INVITATION_TOKEN = Regex("^sh_inv_[A-Za-z0-9_-]{43}$")
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
