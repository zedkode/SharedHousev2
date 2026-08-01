package com.sharedhouse.android.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.sharedhouse.network.FieldViolationDto
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.RegisterPayload
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
 * The session deliberately lives only in memory. Process death returns the user to Welcome until
 * a Keystore-backed session store is implemented; the UI states this limitation explicitly.
 */
class SharedHouseViewModel(
    private val gateway: SharedHouseGateway,
    private val deviceName: String,
    private val preferredLocale: String,
    initialHousehold: HouseholdFormState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(household = initialHousehold))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var session: SessionDto? = null
    private var householdCreationIdempotencyKey: String? = null
    private var calendarLoadJob: Job? = null
    private var calendarCreationIdempotencyKey: String? = null
    private var calendarCreationDraft: CalendarEventDraft? = null

    fun openWelcome() = moveTo(AppRoute.Welcome)

    fun openRegister() = moveTo(AppRoute.Register)

    fun openSignIn() = moveTo(AppRoute.SignIn)

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
                .plus(mapped.takeIf { it.date in range }.let(::listOfNotNull))
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
        if (snapshot.isSubmitting) return
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
        if (snapshot.isSubmitting) return
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

    fun signIn() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) return
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

    fun openHouseholdEditor() {
        val selected = _uiState.value.selectedHousehold ?: return
        if (_uiState.value.isSubmitting) return
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
        if (_uiState.value.isSubmitting || _uiState.value.selectedHousehold === null) return
        _uiState.update {
            it.copy(
                route = AppRoute.Home,
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
                        )
                    }
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

    fun signOut() {
        if (_uiState.value.isSubmitting) return
        val active = session
        if (active === null) {
            clearSession(UiMessage.SignedOut)
            return
        }
        submit {
            val result = gateway.signOut(active.accessToken)
            clearSession(
                if (result is ApiResult.Success) {
                    UiMessage.SignedOut
                } else {
                    UiMessage.SessionRevocationUnconfirmed
                },
            )
        }
    }

    private suspend fun establishSession(newSession: SessionDto) {
        session = newSession
        _uiState.update {
            it.copy(
                route = AppRoute.HouseholdGate,
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
                _uiState.update {
                    it.copy(
                        route = if (active.isEmpty()) AppRoute.HouseholdSetup else AppRoute.Home,
                        households = active,
                        selectedHousehold = active.firstOrNull(),
                        household = active.firstOrNull()?.toForm() ?: it.household,
                        householdEditorMode = if (active.isEmpty()) {
                            HouseholdEditorMode.Create
                        } else {
                            HouseholdEditorMode.Edit
                        },
                        isSubmitting = false,
                        error = null,
                        fieldErrors = emptyMap(),
                        correlationId = null,
                    )
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
                session = refreshed.value
                _uiState.update { it.copy(account = refreshed.value.account) }
                request(refreshed.value.accessToken)
            }

            is ApiResult.Failure -> {
                if (refreshed.code != "NETWORK_UNAVAILABLE") {
                    expireSession()
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

    private fun expireSession() {
        session = null
        householdCreationIdempotencyKey = null
        _uiState.update {
            it.copy(
                route = AppRoute.SignIn,
                auth = it.auth.copy(password = "", verificationCode = ""),
                account = null,
                households = emptyList(),
                selectedHousehold = null,
                isSubmitting = false,
                error = UiMessage.SessionExpired,
                notice = null,
                fieldErrors = emptyMap(),
            )
        }
    }

    private fun clearSession(notice: UiMessage) {
        session = null
        householdCreationIdempotencyKey = null
        _uiState.update {
            AppUiState(
                route = AppRoute.Welcome,
                auth = AuthFormState(email = it.auth.email),
                household = it.household.copy(name = ""),
                notice = notice,
            )
        }
    }

    private fun moveTo(route: AppRoute) {
        if (_uiState.value.isSubmitting) return
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
}
