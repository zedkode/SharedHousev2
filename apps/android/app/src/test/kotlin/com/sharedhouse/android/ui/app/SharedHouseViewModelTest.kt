package com.sharedhouse.android.ui.app

import com.sharedhouse.network.AccountDto
import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.HouseholdDto
import com.sharedhouse.network.RegisterPayload
import com.sharedhouse.network.RegistrationAcceptedDto
import com.sharedhouse.network.SessionDto
import com.sharedhouse.network.SignInPayload
import com.sharedhouse.network.VerifyEmailPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            signIn(viewModel)
            advanceUntilIdle()
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
            val viewModel = viewModel(fake)

            signIn(viewModel)
            advanceUntilIdle()

            assertEquals(1, fake.refreshCalls)
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

    private suspend fun TestScope.withMainDispatcher(block: suspend TestScope.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(fake: FakeGateway) = SharedHouseViewModel(
        gateway = fake,
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

private class FakeGateway : SharedHouseGateway {
    var registerCalls = 0
    var refreshCalls = 0
    val createKeys = mutableListOf<String>()

    var registerHandler: suspend (RegisterPayload) -> ApiResult<RegistrationAcceptedDto> = {
        error("Unexpected register")
    }
    var verifyHandler: suspend (VerifyEmailPayload) -> ApiResult<SessionDto> = {
        error("Unexpected verify")
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

    override suspend fun register(payload: RegisterPayload) = registerHandler(payload)
    override suspend fun verifyEmail(payload: VerifyEmailPayload) = verifyHandler(payload)
    override suspend fun signIn(payload: SignInPayload) = signInHandler(payload)
    override suspend fun refresh(refreshToken: String) = refreshHandler(refreshToken)
    override suspend fun signOut(accessToken: String) = signOutHandler(accessToken)
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
) = HouseholdDto(
    id = "018f0000-0000-7000-8000-000000000002",
    name = name,
    countryCode = "GB",
    timezone = "Europe/London",
    currency = "GBP",
    firstDayOfWeek = 1,
    cycleType = "fourteen_day",
    cycleAnchor = "2026-08-01",
    role = "owner",
    status = "active",
    version = version,
    createdAt = "2026-08-01T12:00:00Z",
    updatedAt = "2026-08-01T12:00:00Z",
)

