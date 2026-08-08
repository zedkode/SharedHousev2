package com.sharedhouse.android.ui.app

import com.sharedhouse.android.ui.calendar.CalendarAction
import com.sharedhouse.android.ui.calendar.CalendarContent
import com.sharedhouse.android.ui.calendar.CalendarEventDraft
import com.sharedhouse.android.ui.calendar.CalendarEventType
import com.sharedhouse.android.ui.calendar.CalendarMutationProblem
import com.sharedhouse.android.platform.security.SessionLoadResult
import com.sharedhouse.android.platform.security.SessionSaveResult
import com.sharedhouse.android.platform.security.SessionStore
import com.sharedhouse.network.AccountDto
import com.sharedhouse.network.AccountDeletionResultDto
import com.sharedhouse.network.AccountExportDto
import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.CalendarEventConfigurationDto
import com.sharedhouse.network.CalendarEventDto
import com.sharedhouse.network.AcceptHouseholdInvitationDto
import com.sharedhouse.network.CreateHouseholdInvitationPayload
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.HouseholdDto
import com.sharedhouse.network.HouseholdInvitationDto
import com.sharedhouse.network.HouseholdInvitationPreviewDto
import com.sharedhouse.network.RegisterPayload
import com.sharedhouse.network.RegistrationAcceptedDto
import com.sharedhouse.network.ResendVerificationPayload
import com.sharedhouse.network.SessionDto
import com.sharedhouse.network.SignInPayload
import com.sharedhouse.network.VerifyEmailPayload
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@OptIn(ExperimentalCoroutinesApi::class)
class SharedHouseViewModelTest {
    @Test
    fun `register ignores double submit and does not navigate before server confirmation`() = runTest {
        withMainDispatcher {
            val releaseResponse = CompletableDeferred<Unit>()
            val fake = FakeGateway().apply {
                registerHandler = {
                    registerCalls += 1
                    releaseResponse.await()
                    ApiResult.Success(RegistrationAcceptedDto(verificationRequired = true))
                }
            }
            val viewModel = viewModel(fake)
            runCurrent()
            validRegistration(viewModel)

            viewModel.register()
            viewModel.register()
            runCurrent()

            assertEquals(1, fake.registerCalls)
            assertEquals(AppRoute.Register, viewModel.uiState.value.route)
            assertNull(viewModel.uiState.value.selectedHousehold)

            releaseResponse.complete(Unit)
            advanceUntilIdle()
            assertEquals(AppRoute.VerifyEmail, viewModel.uiState.value.route)
        }
    }

    @Test
    fun `verification screen can request a replacement code without exposing account existence`() =
        runTest {
            withMainDispatcher {
                val fake = FakeGateway().apply {
                    registerHandler = {
                        ApiResult.Success(RegistrationAcceptedDto(verificationRequired = true))
                    }
                    resendHandler = { payload ->
                        resendCalls += 1
                        assertEquals("alex@example.test", payload.email)
                        ApiResult.Success(RegistrationAcceptedDto(verificationRequired = true))
                    }
                }
                val viewModel = viewModel(fake)
                runCurrent()
                validRegistration(viewModel)
                viewModel.register()
                advanceUntilIdle()

                viewModel.resendVerification()
                advanceUntilIdle()

                assertEquals(1, fake.resendCalls)
                assertEquals(UiMessage.VerificationCodeSent, viewModel.uiState.value.notice)
                assertEquals(AppRoute.VerifyEmail, viewModel.uiState.value.route)
            }
        }

    @Test
    fun `household retry reuses idempotency key and succeeds only on server success`() = runTest {
        withMainDispatcher {
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                listHandler = { ApiResult.Success(emptyList()) }
                createHandler = { _, key, configuration ->
                    createKeys += key
                    if (createKeys.size == 1) {
                        ApiResult.Failure(code = "NETWORK_UNAVAILABLE", title = "offline")
                    } else {
                        ApiResult.Success(household(name = configuration.name))
                    }
                }
            }
            val viewModel = viewModel(fake)
            runCurrent()
            signIn(viewModel)
            advanceUntilIdle()
            assertEquals(AppRoute.HouseholdChoice, viewModel.uiState.value.route)
            viewModel.openCreateHousehold()
            assertEquals(AppRoute.HouseholdSetup, viewModel.uiState.value.route)

            viewModel.updateHouseholdName("Oak House")
            viewModel.createHousehold()
            advanceUntilIdle()
            assertEquals(AppRoute.HouseholdSetup, viewModel.uiState.value.route)
            assertEquals(UiMessage.NetworkUnavailable, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.selectedHousehold)

            viewModel.createHousehold()
            advanceUntilIdle()
            assertEquals(2, fake.createKeys.size)
            assertEquals(fake.createKeys[0], fake.createKeys[1])
            assertEquals(AppRoute.Home, viewModel.uiState.value.route)
            assertEquals("Oak House", viewModel.uiState.value.selectedHousehold?.name)
        }
    }

    @Test
    fun `verified account can preview and accept an invitation before creating a household`() =
        runTest {
            withMainDispatcher {
                val invitationToken =
                    "sh_inv_1234567890123456789012345678901234567890123"
                val acceptedHousehold = household(role = "member")
                val fake = FakeGateway().apply {
                    signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                    listHandler = { ApiResult.Success(emptyList()) }
                    previewInvitationHandler = { token ->
                        assertEquals(invitationToken, token)
                        ApiResult.Success(
                            HouseholdInvitationPreviewDto(
                                householdName = "Oak House",
                                role = "member",
                                emailRestricted = true,
                                status = "pending",
                                expiresAt = "2026-08-15T12:00:00Z",
                            ),
                        )
                    }
                    acceptInvitationHandler = { accessToken, token ->
                        assertEquals("access-1", accessToken)
                        assertEquals(invitationToken, token)
                        ApiResult.Success(AcceptHouseholdInvitationDto(acceptedHousehold))
                    }
                }
                val viewModel = viewModel(fake)
                runCurrent()
                signIn(viewModel)
                advanceUntilIdle()

                assertEquals(AppRoute.HouseholdChoice, viewModel.uiState.value.route)
                viewModel.openInvitationJoin()
                viewModel.updateInvitationToken(invitationToken)
                viewModel.previewInvitation()
                advanceUntilIdle()
                assertEquals("Oak House", viewModel.uiState.value.invitation.preview?.householdName)

                viewModel.acceptInvitation()
                advanceUntilIdle()

                assertEquals(AppRoute.Home, viewModel.uiState.value.route)
                assertEquals(acceptedHousehold, viewModel.uiState.value.selectedHousehold)
                assertEquals(UiMessage.InvitationAccepted, viewModel.uiState.value.notice)
            }
        }

    @Test
    fun `expired access token refreshes once before household access is accepted`() = runTest {
        withMainDispatcher {
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("expired-access", "refresh-1")) }
                listHandler = { token ->
                    if (token == "expired-access") {
                        ApiResult.Failure(code = "SESSION_INVALID", title = "expired", status = 401)
                    } else {
                        ApiResult.Success(listOf(household()))
                    }
                }
                refreshHandler = {
                    refreshCalls += 1
                    ApiResult.Success(session("fresh-access", "refresh-2"))
                }
            }
            val store = FakeSessionStore()
            val viewModel = viewModel(fake, store)
            runCurrent()

            signIn(viewModel)
            advanceUntilIdle()

            assertEquals(1, fake.refreshCalls)
            assertEquals("refresh-2", store.savedSessions.last().refreshToken)
            assertEquals(AppRoute.Home, viewModel.uiState.value.route)
            assertEquals("Oak House", viewModel.uiState.value.selectedHousehold?.name)
        }
    }

    @Test
    fun `version conflict stays in editor until authoritative settings are reloaded`() = runTest {
        withMainDispatcher {
            var listed = household(version = 1)
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                listHandler = { ApiResult.Success(listOf(listed)) }
                updateHandler = { _, _, expectedVersion, _ ->
                    assertEquals(1, expectedVersion)
                    ApiResult.Failure(
                        code = "HOUSEHOLD_VERSION_CONFLICT",
                        title = "conflict",
                        status = 412,
                    )
                }
            }
            val viewModel = viewModel(fake)
            runCurrent()
            signIn(viewModel)
            advanceUntilIdle()
            viewModel.openHouseholdEditor()
            viewModel.updateHouseholdName("Local edit")

            viewModel.createHousehold()
            advanceUntilIdle()

            assertEquals(AppRoute.HouseholdSetup, viewModel.uiState.value.route)
            assertEquals(UiMessage.HouseholdVersionConflict, viewModel.uiState.value.error)
            assertEquals(1, viewModel.uiState.value.selectedHousehold?.version)

            listed = household(name = "Server edit", version = 2)
            viewModel.reloadHouseholdEditor()
            advanceUntilIdle()

            assertEquals("Server edit", viewModel.uiState.value.household.name)
            assertEquals(2, viewModel.uiState.value.selectedHousehold?.version)
            assertEquals(UiMessage.HouseholdReloaded, viewModel.uiState.value.notice)
        }
    }

    @Test
    fun `calendar loads real events and derives member permissions from authorship`() = runTest {
        withMainDispatcher {
            var requestedRange: Pair<String, String>? = null
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                listHandler = { ApiResult.Success(listOf(household(role = "member"))) }
                listCalendarHandler = { _, householdId, from, to ->
                    requestedRange = from to to
                    ApiResult.Success(
                        listOf(
                            calendarEvent(
                                id = "018f0000-0000-7000-8000-000000000010",
                                householdId = householdId,
                                date = from,
                                createdByUserId = "018f0000-0000-7000-8000-000000000001",
                            ),
                            calendarEvent(
                                id = "018f0000-0000-7000-8000-000000000011",
                                householdId = householdId,
                                date = to,
                                createdByUserId = "018f0000-0000-7000-8000-000000000099",
                            ),
                        ),
                    )
                }
            }
            val viewModel = viewModel(fake)
            runCurrent()

            signIn(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            val events = (state.calendar.content as CalendarContent.Ready).events
            assertTrue(state.calendar.canCreateEvents)
            assertTrue(events.first().canEdit)
            assertTrue(events.first().canDelete)
            assertFalse(events.last().canEdit)
            assertFalse(events.last().canDelete)
            assertTrue(requestedRange?.let { (from, to) -> from <= to } == true)
        }
    }

    @Test
    fun `calendar create retry reuses idempotency key and updates only after server success`() = runTest {
        withMainDispatcher {
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                listHandler = { ApiResult.Success(listOf(household(role = "owner"))) }
                createCalendarHandler = { _, householdId, key, configuration ->
                    calendarCreateKeys += key
                    if (calendarCreateKeys.size == 1) {
                        ApiResult.Failure(code = "NETWORK_UNAVAILABLE", title = "offline")
                    } else {
                        ApiResult.Success(
                            calendarEvent(
                                householdId = householdId,
                                date = configuration.date,
                                title = configuration.title,
                            ),
                        )
                    }
                }
            }
            val viewModel = viewModel(fake)
            runCurrent()
            signIn(viewModel)
            advanceUntilIdle()
            val draft = CalendarEventDraft(
                title = "Boiler service",
                description = "Annual visit",
                type = CalendarEventType.MAINTENANCE,
                date = viewModel.uiState.value.calendar.selectedDate,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                reminderMinutesBefore = 60,
            )

            viewModel.handleCalendarAction(CalendarAction.CreateEvent(draft))
            advanceUntilIdle()
            assertEquals(CalendarMutationProblem.CREATE_FAILED, viewModel.uiState.value.calendar.mutationProblem)
            assertTrue((viewModel.uiState.value.calendar.content as CalendarContent.Ready).events.isEmpty())

            viewModel.handleCalendarAction(CalendarAction.CreateEvent(draft))
            advanceUntilIdle()

            assertEquals(2, fake.calendarCreateKeys.size)
            assertEquals(fake.calendarCreateKeys[0], fake.calendarCreateKeys[1])
            assertEquals(
                "Boiler service",
                (viewModel.uiState.value.calendar.content as CalendarContent.Ready).events.single().title,
            )
        }
    }

    @Test
    fun `saved session is rotated before household content is restored`() = runTest {
        withMainDispatcher {
            val stored = session("old-access", "old-refresh")
            val rotated = session("rotated-access", "rotated-refresh")
            val store = FakeSessionStore(loadResult = SessionLoadResult.Restored(stored))
            val fake = FakeGateway().apply {
                refreshHandler = { token ->
                    assertEquals("old-refresh", token)
                    ApiResult.Success(rotated)
                }
                listHandler = { token ->
                    assertEquals("rotated-access", token)
                    ApiResult.Success(listOf(household()))
                }
            }

            val viewModel = viewModel(fake, store)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRestoringSession)
            assertEquals(AppRoute.Home, viewModel.uiState.value.route)
            assertEquals("rotated-refresh", store.savedSessions.single().refreshToken)
            assertEquals("Alex", viewModel.uiState.value.account?.displayName)
        }
    }

    @Test
    fun `network failure keeps saved session available for explicit retry`() = runTest {
        withMainDispatcher {
            var attempts = 0
            val stored = session("old-access", "old-refresh")
            val store = FakeSessionStore(loadResult = SessionLoadResult.Restored(stored))
            val fake = FakeGateway().apply {
                refreshHandler = {
                    attempts += 1
                    if (attempts == 1) {
                        ApiResult.Failure(code = "NETWORK_UNAVAILABLE", title = "offline")
                    } else {
                        ApiResult.Success(session("fresh-access", "fresh-refresh"))
                    }
                }
                listHandler = { ApiResult.Success(listOf(household())) }
            }
            val viewModel = viewModel(fake, store)

            advanceUntilIdle()
            assertEquals(AppRoute.Welcome, viewModel.uiState.value.route)
            assertTrue(viewModel.uiState.value.canRetrySessionRestore)
            assertEquals(UiMessage.SessionRestoreNetworkUnavailable, viewModel.uiState.value.notice)
            assertEquals(0, store.clearCalls)

            viewModel.retrySessionRestore()
            advanceUntilIdle()

            assertEquals(2, attempts)
            assertEquals(AppRoute.Home, viewModel.uiState.value.route)
            assertEquals("fresh-refresh", store.savedSessions.single().refreshToken)
        }
    }

    @Test
    fun `sign in is rejected locally when rotated credentials cannot be secured`() = runTest {
        withMainDispatcher {
            val store = FakeSessionStore(saveResult = SessionSaveResult.UNAVAILABLE)
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                signOutHandler = {
                    signOutCalls += 1
                    ApiResult.Success(Unit)
                }
            }
            val viewModel = viewModel(fake, store)
            runCurrent()

            signIn(viewModel)
            advanceUntilIdle()

            assertEquals(AppRoute.SignIn, viewModel.uiState.value.route)
            assertEquals(UiMessage.SecureStorageUnavailable, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.account)
            assertEquals(1, fake.signOutCalls)
            assertEquals(1, store.clearCalls)
        }
    }

    @Test
    fun `sign out removes local credentials even when server revocation is unavailable`() = runTest {
        withMainDispatcher {
            val store = FakeSessionStore()
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                listHandler = { ApiResult.Success(listOf(household())) }
                signOutHandler = {
                    ApiResult.Failure(code = "NETWORK_UNAVAILABLE", title = "offline")
                }
            }
            val viewModel = viewModel(fake, store)
            runCurrent()
            signIn(viewModel)
            advanceUntilIdle()

            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(AppRoute.Welcome, viewModel.uiState.value.route)
            assertEquals(UiMessage.SessionRevocationUnconfirmed, viewModel.uiState.value.notice)
            assertEquals(1, store.clearCalls)
            assertNull(viewModel.uiState.value.account)
        }
    }

    @Test
    fun `confirmed account deletion clears the encrypted local session`() = runTest {
        withMainDispatcher {
            val store = FakeSessionStore()
            val fake = FakeGateway().apply {
                signInHandler = { ApiResult.Success(session("access-1", "refresh-1")) }
                listHandler = { ApiResult.Success(listOf(household())) }
                deleteAccountHandler = { _, _ ->
                    ApiResult.Success(AccountDeletionResultDto("completed", emptyList(), emptyList()))
                }
            }
            val viewModel = viewModel(fake, store)
            runCurrent()
            signIn(viewModel)
            advanceUntilIdle()

            viewModel.deleteAccount("current password")
            advanceUntilIdle()

            assertEquals(AppRoute.Welcome, viewModel.uiState.value.route)
            assertEquals(UiMessage.AccountDeleted, viewModel.uiState.value.notice)
            assertEquals(1, store.clearCalls)
            assertNull(viewModel.uiState.value.account)
        }
    }

    private suspend fun TestScope.withMainDispatcher(block: suspend TestScope.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(
        fake: FakeGateway,
        sessionStore: FakeSessionStore = FakeSessionStore(),
    ) = SharedHouseViewModel(
        gateway = fake,
        sessionStore = sessionStore,
        deviceName = "Test phone",
        preferredLocale = "en",
        initialHousehold = HouseholdFormState(
            countryCode = "GB",
            timezone = "Europe/London",
            currency = "GBP",
            firstDayOfWeek = 1,
            cycleAnchor = "2026-08-01",
        ),
    )

    private fun validRegistration(viewModel: SharedHouseViewModel) {
        viewModel.openRegister()
        viewModel.updateDisplayName("Alex")
        viewModel.updateEmail("alex@example.test")
        viewModel.updatePassword("a-secure-password-for-tests")
        viewModel.updateAgeConfirmed(true)
        viewModel.updateTermsAccepted(true)
    }

    private fun signIn(viewModel: SharedHouseViewModel) {
        viewModel.openSignIn()
        viewModel.updateEmail("alex@example.test")
        viewModel.updatePassword("password")
        viewModel.signIn()
    }
}

private class FakeSessionStore(
    var loadResult: SessionLoadResult = SessionLoadResult.Missing,
    var saveResult: SessionSaveResult = SessionSaveResult.SAVED,
) : SessionStore {
    val savedSessions = mutableListOf<SessionDto>()
    var clearCalls = 0

    override suspend fun load(): SessionLoadResult = loadResult

    override suspend fun save(session: SessionDto): SessionSaveResult {
        if (saveResult == SessionSaveResult.SAVED) savedSessions += session
        return saveResult
    }

    override suspend fun clear(): Boolean {
        clearCalls += 1
        return true
    }
}

private class FakeGateway : SharedHouseGateway {
    var registerCalls = 0
    var resendCalls = 0
    var refreshCalls = 0
    var signOutCalls = 0
    val createKeys = mutableListOf<String>()
    val calendarCreateKeys = mutableListOf<String>()

    var registerHandler: suspend (RegisterPayload) -> ApiResult<RegistrationAcceptedDto> = {
        error("Unexpected register")
    }
    var verifyHandler: suspend (VerifyEmailPayload) -> ApiResult<SessionDto> = {
        error("Unexpected verify")
    }
    var resendHandler:
        suspend (ResendVerificationPayload) -> ApiResult<RegistrationAcceptedDto> = {
            error("Unexpected resend")
        }
    var signInHandler: suspend (SignInPayload) -> ApiResult<SessionDto> = {
        error("Unexpected sign in")
    }
    var refreshHandler: suspend (String) -> ApiResult<SessionDto> = {
        error("Unexpected refresh")
    }
    var signOutHandler: suspend (String) -> ApiResult<Unit> = {
        error("Unexpected sign out")
    }
    var deleteAccountHandler: suspend (String, String) -> ApiResult<AccountDeletionResultDto> = { _, _ ->
        error("Unexpected account deletion")
    }
    var exportAccountHandler: suspend (String, String) -> ApiResult<AccountExportDto> = { _, _ ->
        error("Unexpected account export")
    }
    var listHandler: suspend (String) -> ApiResult<List<HouseholdDto>> = {
        error("Unexpected list")
    }
    var createHandler:
        suspend (String, String, HouseholdConfigurationDto) -> ApiResult<HouseholdDto> = { _, _, _ ->
            error("Unexpected create")
        }
    var updateHandler:
        suspend (String, String, Int, HouseholdConfigurationDto) -> ApiResult<HouseholdDto> = { _, _, _, _ ->
            error("Unexpected update")
        }
    var listInvitationsHandler:
        suspend (String, String) -> ApiResult<List<HouseholdInvitationDto>> = { _, _ ->
            ApiResult.Success(emptyList())
        }
    var createInvitationHandler:
        suspend (String, String, CreateHouseholdInvitationPayload) -> ApiResult<HouseholdInvitationDto> =
        { _, _, _ -> error("Unexpected invitation create") }
    var previewInvitationHandler:
        suspend (String) -> ApiResult<HouseholdInvitationPreviewDto> = {
            error("Unexpected invitation preview")
        }
    var acceptInvitationHandler:
        suspend (String, String) -> ApiResult<AcceptHouseholdInvitationDto> = { _, _ ->
            error("Unexpected invitation accept")
        }
    var revokeInvitationHandler: suspend (String, String, String) -> ApiResult<Unit> = { _, _, _ ->
        error("Unexpected invitation revoke")
    }
    var listCalendarHandler:
        suspend (String, String, String, String) -> ApiResult<List<CalendarEventDto>> = { _, _, _, _ ->
            ApiResult.Success(emptyList())
        }
    var createCalendarHandler:
        suspend (String, String, String, CalendarEventConfigurationDto) -> ApiResult<CalendarEventDto> =
        { _, _, _, _ -> error("Unexpected calendar create") }
    var updateCalendarHandler:
        suspend (String, String, String, Int, CalendarEventConfigurationDto) -> ApiResult<CalendarEventDto> =
        { _, _, _, _, _ -> error("Unexpected calendar update") }
    var deleteCalendarHandler:
        suspend (String, String, String, Int) -> ApiResult<Unit> = { _, _, _, _ ->
            error("Unexpected calendar delete")
        }

    override suspend fun register(payload: RegisterPayload) = registerHandler(payload)
    override suspend fun verifyEmail(payload: VerifyEmailPayload) = verifyHandler(payload)
    override suspend fun resendVerification(payload: ResendVerificationPayload) =
        resendHandler(payload)
    override suspend fun signIn(payload: SignInPayload) = signInHandler(payload)
    override suspend fun refresh(refreshToken: String) = refreshHandler(refreshToken)
    override suspend fun signOut(accessToken: String) = signOutHandler(accessToken)
    override suspend fun deleteAccount(accessToken: String, password: String) =
        deleteAccountHandler(accessToken, password)
    override suspend fun exportAccount(accessToken: String, password: String) =
        exportAccountHandler(accessToken, password)
    override suspend fun listHouseholds(accessToken: String) = listHandler(accessToken)
    override suspend fun createHousehold(
        accessToken: String,
        idempotencyKey: String,
        configuration: HouseholdConfigurationDto,
    ) = createHandler(accessToken, idempotencyKey, configuration)

    override suspend fun updateHousehold(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        configuration: HouseholdConfigurationDto,
    ) = updateHandler(accessToken, householdId, expectedVersion, configuration)

    override suspend fun listHouseholdInvitations(accessToken: String, householdId: String) =
        listInvitationsHandler(accessToken, householdId)

    override suspend fun createHouseholdInvitation(
        accessToken: String,
        householdId: String,
        payload: CreateHouseholdInvitationPayload,
    ) = createInvitationHandler(accessToken, householdId, payload)

    override suspend fun previewHouseholdInvitation(token: String) =
        previewInvitationHandler(token)

    override suspend fun acceptHouseholdInvitation(accessToken: String, token: String) =
        acceptInvitationHandler(accessToken, token)

    override suspend fun revokeHouseholdInvitation(
        accessToken: String,
        householdId: String,
        invitationId: String,
    ) = revokeInvitationHandler(accessToken, householdId, invitationId)

    override suspend fun listCalendarEvents(
        accessToken: String,
        householdId: String,
        from: String,
        to: String,
    ) = listCalendarHandler(accessToken, householdId, from, to)

    override suspend fun createCalendarEvent(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: CalendarEventConfigurationDto,
    ) = createCalendarHandler(accessToken, householdId, idempotencyKey, configuration)

    override suspend fun updateCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
        configuration: CalendarEventConfigurationDto,
    ) = updateCalendarHandler(accessToken, householdId, eventId, expectedVersion, configuration)

    override suspend fun deleteCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
    ) = deleteCalendarHandler(accessToken, householdId, eventId, expectedVersion)
}

private fun session(accessToken: String, refreshToken: String) = SessionDto(
    accessToken = accessToken,
    refreshToken = refreshToken,
    accessTokenExpiresAt = "2026-08-01T12:15:00Z",
    refreshTokenExpiresAt = "2026-08-08T12:00:00Z",
    account = AccountDto(
        id = "018f0000-0000-7000-8000-000000000001",
        email = "alex@example.test",
        emailVerified = true,
        displayName = "Alex",
        preferredLocale = "en",
    ),
)

private fun household(
    name: String = "Oak House",
    version: Int = 1,
    role: String = "owner",
) = HouseholdDto(
    id = "018f0000-0000-7000-8000-000000000002",
    name = name,
    countryCode = "GB",
    timezone = "Europe/London",
    currency = "GBP",
    firstDayOfWeek = 1,
    cycleType = "fourteen_day",
    cycleAnchor = "2026-08-01",
    role = role,
    status = "active",
    version = version,
    createdAt = "2026-08-01T12:00:00Z",
    updatedAt = "2026-08-01T12:00:00Z",
)

private fun calendarEvent(
    id: String = "018f0000-0000-7000-8000-000000000010",
    householdId: String = "018f0000-0000-7000-8000-000000000002",
    date: String,
    title: String = "Household event",
    createdByUserId: String = "018f0000-0000-7000-8000-000000000001",
) = CalendarEventDto(
    id = id,
    householdId = householdId,
    title = title,
    description = null,
    type = "household",
    date = date,
    startTime = "10:00",
    endTime = "11:00",
    reminderMinutesBefore = 15,
    createdByUserId = createdByUserId,
    version = 1,
    createdAt = "2026-08-08T09:00:00Z",
    updatedAt = "2026-08-08T09:00:00Z",
)
