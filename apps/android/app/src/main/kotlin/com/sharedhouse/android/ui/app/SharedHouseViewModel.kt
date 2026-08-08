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
import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.CalendarEventConfigurationDto
import com.sharedhouse.network.CalendarEventDto
import com.sharedhouse.network.CreateHouseholdInvitationPayload
import com.sharedhouse.network.FieldViolationDto
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.RegisterPayload
import com.sharedhouse.network.ResendVerificationPayload
import com.sharedhouse.network.SessionDto
import com.sharedhouse.network.SignInPayload
import com.sharedhouse.network.VerifyEmailPayload
import java.time.DayOfWeek
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
                        )
                    }
                    loadCalendar()
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
                        )
                    }
                    loadCalendar()
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
            )
        }
        loadCalendar()
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
                    )
                }
                if (selected != null) loadCalendar()
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
        session = null
        householdCreationIdempotencyKey = null
        calendarCreationIdempotencyKey = null
        calendarCreationDraft = null
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
                invitation = InvitationUiState(),
            )
        }
    }

    private fun clearSession(notice: UiMessage) {
        calendarLoadJob?.cancel()
        session = null
        householdCreationIdempotencyKey = null
        calendarCreationIdempotencyKey = null
        calendarCreationDraft = null
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

    private companion object {
        val CALENDAR_WRITER_ROLES = setOf("owner", "admin", "member")
        val INVITATION_TOKEN = Regex("^sh_inv_[A-Za-z0-9_-]{43}$")
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
