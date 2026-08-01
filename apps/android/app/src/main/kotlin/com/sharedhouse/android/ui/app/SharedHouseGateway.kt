package com.sharedhouse.android.ui.app

import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.CalendarEventConfigurationDto
import com.sharedhouse.network.CalendarEventDto
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.HouseholdDto
import com.sharedhouse.network.RegisterPayload
import com.sharedhouse.network.RegistrationAcceptedDto
import com.sharedhouse.network.SessionDto
import com.sharedhouse.network.SharedHouseApiClient
import com.sharedhouse.network.SignInPayload
import com.sharedhouse.network.VerifyEmailPayload

interface SharedHouseGateway {
    suspend fun register(payload: RegisterPayload): ApiResult<RegistrationAcceptedDto>

    suspend fun verifyEmail(payload: VerifyEmailPayload): ApiResult<SessionDto>

    suspend fun signIn(payload: SignInPayload): ApiResult<SessionDto>

    suspend fun refresh(refreshToken: String): ApiResult<SessionDto>

    suspend fun signOut(accessToken: String): ApiResult<Unit>

    suspend fun listHouseholds(accessToken: String): ApiResult<List<HouseholdDto>>

    suspend fun createHousehold(
        accessToken: String,
        idempotencyKey: String,
        configuration: HouseholdConfigurationDto,
    ): ApiResult<HouseholdDto>

    suspend fun updateHousehold(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        configuration: HouseholdConfigurationDto,
    ): ApiResult<HouseholdDto>

    suspend fun listCalendarEvents(
        accessToken: String,
        householdId: String,
        from: String,
        to: String,
    ): ApiResult<List<CalendarEventDto>>

    suspend fun createCalendarEvent(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: CalendarEventConfigurationDto,
    ): ApiResult<CalendarEventDto>

    suspend fun updateCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
        configuration: CalendarEventConfigurationDto,
    ): ApiResult<CalendarEventDto>

    suspend fun deleteCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
    ): ApiResult<Unit>
}

class ApiSharedHouseGateway(
    private val api: SharedHouseApiClient,
) : SharedHouseGateway {
    override suspend fun register(payload: RegisterPayload) = api.register(payload)

    override suspend fun verifyEmail(payload: VerifyEmailPayload) = api.verifyEmail(payload)

    override suspend fun signIn(payload: SignInPayload) = api.signIn(payload)

    override suspend fun refresh(refreshToken: String) = api.refresh(refreshToken)

    override suspend fun signOut(accessToken: String) = api.signOut(accessToken)

    override suspend fun listHouseholds(accessToken: String) = api.listHouseholds(accessToken)

    override suspend fun createHousehold(
        accessToken: String,
        idempotencyKey: String,
        configuration: HouseholdConfigurationDto,
    ) = api.createHousehold(accessToken, idempotencyKey, configuration)

    override suspend fun updateHousehold(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        configuration: HouseholdConfigurationDto,
    ) = api.updateHousehold(accessToken, householdId, expectedVersion, configuration)

    override suspend fun listCalendarEvents(
        accessToken: String,
        householdId: String,
        from: String,
        to: String,
    ) = api.listCalendarEvents(accessToken, householdId, from, to)

    override suspend fun createCalendarEvent(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: CalendarEventConfigurationDto,
    ) = api.createCalendarEvent(accessToken, householdId, idempotencyKey, configuration)

    override suspend fun updateCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
        configuration: CalendarEventConfigurationDto,
    ) = api.updateCalendarEvent(
        accessToken,
        householdId,
        eventId,
        expectedVersion,
        configuration,
    )

    override suspend fun deleteCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
    ) = api.deleteCalendarEvent(accessToken, householdId, eventId, expectedVersion)
}
