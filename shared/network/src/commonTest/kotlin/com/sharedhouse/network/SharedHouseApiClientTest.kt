package com.sharedhouse.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class SharedHouseApiClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun registerSendsJsonPayloadAndAcceptHeader() = runBlocking {
        val payload = RegisterPayload(
            email = "owner@example.test",
            password = "synthetic-password",
            displayName = "House Owner",
            preferredLocale = "ro",
            ageConfirmed = true,
            termsAccepted = true,
            marketingConsent = false,
        )
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v1/auth/register", request.url.encodedPath)
            assertEquals(ContentType.Application.Json.toString(), request.headers[HttpHeaders.Accept])
            assertEquals(ContentType.Application.Json, request.body.contentType)
            assertEquals(
                payload,
                json.decodeFromString<RegisterPayload>(request.body.toByteArray().decodeToString()),
            )

            respond(
                content = """{"verificationRequired":true}""",
                status = HttpStatusCode.Created,
                headers = JsonResponseHeaders,
            )
        }
        val api = apiClient(engine, "https://api.sharedhouse.test/")

        try {
            assertEquals(
                ApiResult.Success(RegistrationAcceptedDto(verificationRequired = true)),
                api.register(payload),
            )
            assertEquals(1, requestCount)
        } finally {
            api.close()
        }
    }

    @Test
    fun resendVerificationUsesGenericEmailOnlyRequest() = runBlocking {
        val payload = ResendVerificationPayload(email = "owner@example.test")
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v1/auth/resend-verification", request.url.encodedPath)
            assertEquals(
                payload,
                json.decodeFromString<ResendVerificationPayload>(
                    request.body.toByteArray().decodeToString(),
                ),
            )
            respond(
                content = """{"verificationRequired":true}""",
                status = HttpStatusCode.Accepted,
                headers = JsonResponseHeaders,
            )
        }
        val api = apiClient(engine)

        try {
            assertEquals(
                ApiResult.Success(RegistrationAcceptedDto(verificationRequired = true)),
                api.resendVerification(payload),
            )
        } finally {
            api.close()
        }
    }

    @Test
    fun householdWritesSendAuthenticationConcurrencyAndIdempotencyHeaders() = runBlocking {
        val configuration = HouseholdConfigurationDto(
            name = "Casa Verde",
            countryCode = "GB",
            timezone = "Europe/London",
            currency = "GBP",
            firstDayOfWeek = 1,
            cycleType = "fourteen_day",
            cycleAnchor = "2026-08-03",
        )
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
            assertEquals(ContentType.Application.Json, request.body.contentType)
            assertEquals(
                configuration,
                json.decodeFromString<HouseholdConfigurationDto>(
                    request.body.toByteArray().decodeToString(),
                ),
            )

            when (requestCount) {
                1 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/v1/households", request.url.encodedPath)
                    assertEquals("operation-123456", request.headers["Idempotency-Key"])
                }

                2 -> {
                    assertEquals(HttpMethod.Patch, request.method)
                    assertEquals("/v1/households/household-1", request.url.encodedPath)
                    assertEquals("\"7\"", request.headers[HttpHeaders.IfMatch])
                }

                else -> error("Unexpected request $requestCount")
            }

            respond(
                content = householdResponse(version = requestCount),
                status = if (requestCount == 1) HttpStatusCode.Created else HttpStatusCode.OK,
                headers = JsonResponseHeaders,
            )
        }
        val api = apiClient(engine)

        try {
            assertIs<ApiResult.Success<HouseholdDto>>(
                api.createHousehold(
                    accessToken = "access-token",
                    idempotencyKey = "operation-123456",
                    configuration = configuration,
                ),
            )
            assertIs<ApiResult.Success<HouseholdDto>>(
                api.updateHousehold(
                    accessToken = "access-token",
                    householdId = "household-1",
                    expectedVersion = 7,
                    configuration = configuration,
                ),
            )
            assertEquals(2, requestCount)
        } finally {
            api.close()
        }
    }

    @Test
    fun calendarCrudSendsRangeAuthenticationIdempotencyAndConcurrencyMetadata() = runBlocking {
        val configuration = CalendarEventConfigurationDto(
            title = "Boiler service",
            description = "Annual safety visit",
            type = "maintenance",
            date = "2026-08-14",
            startTime = "09:30",
            endTime = "10:30",
            reminderMinutesBefore = 60,
        )
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])

            when (requestCount) {
                1 -> {
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals(
                        "/v1/households/household-1/calendar-events",
                        request.url.encodedPath,
                    )
                    assertEquals("2026-08-01", request.url.parameters["from"])
                    assertEquals("2027-07-31", request.url.parameters["to"])
                    respond("[$CalendarEventResponse]", HttpStatusCode.OK, JsonResponseHeaders)
                }

                2 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals(
                        "/v1/households/household-1/calendar-events",
                        request.url.encodedPath,
                    )
                    assertEquals("calendar-create-0001", request.headers["Idempotency-Key"])
                    assertEquals(
                        configuration,
                        json.decodeFromString<CalendarEventConfigurationDto>(
                            request.body.toByteArray().decodeToString(),
                        ),
                    )
                    respond(CalendarEventResponse, HttpStatusCode.Created, JsonResponseHeaders)
                }

                3 -> {
                    assertEquals(HttpMethod.Patch, request.method)
                    assertEquals(
                        "/v1/households/household-1/calendar-events/event-1",
                        request.url.encodedPath,
                    )
                    assertEquals("\"1\"", request.headers[HttpHeaders.IfMatch])
                    respond(
                        CalendarEventResponse.replace("\"version\":1", "\"version\":2"),
                        HttpStatusCode.OK,
                        JsonResponseHeaders,
                    )
                }

                4 -> {
                    assertEquals(HttpMethod.Delete, request.method)
                    assertEquals(
                        "/v1/households/household-1/calendar-events/event-1",
                        request.url.encodedPath,
                    )
                    assertEquals("\"2\"", request.headers[HttpHeaders.IfMatch])
                    respond("", HttpStatusCode.NoContent)
                }

                else -> error("Unexpected request $requestCount")
            }
        }
        val api = apiClient(engine)

        try {
            val listed = assertIs<ApiResult.Success<List<CalendarEventDto>>>(
                api.listCalendarEvents(
                    accessToken = "access-token",
                    householdId = "household-1",
                    from = "2026-08-01",
                    to = "2027-07-31",
                ),
            )
            assertEquals(1, listed.value.size)
            assertEquals("maintenance", listed.value.single().type)
            assertIs<ApiResult.Success<CalendarEventDto>>(
                api.createCalendarEvent(
                    accessToken = "access-token",
                    householdId = "household-1",
                    idempotencyKey = "calendar-create-0001",
                    configuration = configuration,
                ),
            )
            assertIs<ApiResult.Success<CalendarEventDto>>(
                api.updateCalendarEvent(
                    accessToken = "access-token",
                    householdId = "household-1",
                    eventId = "event-1",
                    expectedVersion = 1,
                    configuration = configuration,
                ),
            )
            assertEquals(
                ApiResult.Success(Unit),
                api.deleteCalendarEvent(
                    accessToken = "access-token",
                    householdId = "household-1",
                    eventId = "event-1",
                    expectedVersion = 2,
                ),
            )
            assertEquals(4, requestCount)
        } finally {
            api.close()
        }
    }

    @Test
    fun expenseLedgerSendsAuthenticationIdempotencyAndConcurrencyMetadata() = runBlocking {
        val configuration = ExpenseConfigurationDto(
            title = "Weekly groceries",
            category = "groceries",
            amount = MoneyDto(1001, "GBP"),
            dueDate = "2026-08-14",
            notes = "Shared shop",
        )
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
            assertEquals("/v1/households/household-1/expenses" + when (requestCount) {
                3 -> "/expense-1/approve"
                4 -> "/expense-1/reverse"
                else -> ""
            }, request.url.encodedPath)
            when (requestCount) {
                1 -> respond("[$ExpenseResponse]", HttpStatusCode.OK, JsonResponseHeaders)
                2 -> {
                    assertEquals("expense-create-0001", request.headers["Idempotency-Key"])
                    assertEquals(configuration, json.decodeFromString<ExpenseConfigurationDto>(request.body.toByteArray().decodeToString()))
                    respond(ExpenseResponse, HttpStatusCode.Created, JsonResponseHeaders)
                }
                3 -> {
                    assertEquals("\"1\"", request.headers[HttpHeaders.IfMatch])
                    respond(ExpenseResponse.replace("\"version\":1", "\"version\":2"), HttpStatusCode.Created, JsonResponseHeaders)
                }
                4 -> {
                    assertEquals("\"2\"", request.headers[HttpHeaders.IfMatch])
                    assertEquals(ReverseExpensePayload("Duplicate receipt"), json.decodeFromString<ReverseExpensePayload>(request.body.toByteArray().decodeToString()))
                    respond(ExpenseResponse.replace("\"version\":1", "\"version\":3").replace("\"status\":\"approved\"", "\"status\":\"reversed\""), HttpStatusCode.Created, JsonResponseHeaders)
                }
                else -> error("Unexpected request")
            }
        }
        val api = apiClient(engine)
        try {
            assertIs<ApiResult.Success<List<ExpenseDto>>>(api.listExpenses("access-token", "household-1"))
            assertIs<ApiResult.Success<ExpenseDto>>(api.createExpense("access-token", "household-1", "expense-create-0001", configuration))
            assertIs<ApiResult.Success<ExpenseDto>>(api.approveExpense("access-token", "household-1", "expense-1", 1))
            assertIs<ApiResult.Success<ExpenseDto>>(api.reverseExpense("access-token", "household-1", "expense-1", 2, "Duplicate receipt"))
            assertEquals(4, requestCount)
        } finally {
            api.close()
        }
    }

    @Test
    fun invitationFlowKeepsSecretOnlyInCreateAndPathRequests() = runBlocking {
        val invitationToken = "sh_inv_1234567890123456789012345678901234567890123"
        val invitationResponse = """
            {
              "id":"invitation-1",
              "householdId":"household-1",
              "householdName":"Casa Verde",
              "role":"member",
              "email":"member@example.test",
              "status":"pending",
              "expiresAt":"2026-08-15T10:00:00.000Z",
              "createdAt":"2026-08-08T10:00:00.000Z",
              "token":"$invitationToken"
            }
        """.trimIndent()
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            when (requestCount) {
                1 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals(
                        "/v1/households/household-1/invitations",
                        request.url.encodedPath,
                    )
                    assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
                    assertEquals(
                        CreateHouseholdInvitationPayload(
                            role = "member",
                            email = "member@example.test",
                        ),
                        json.decodeFromString<CreateHouseholdInvitationPayload>(
                            request.body.toByteArray().decodeToString(),
                        ),
                    )
                    respond(invitationResponse, HttpStatusCode.Created, JsonResponseHeaders)
                }

                2 -> {
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/v1/invitations/$invitationToken", request.url.encodedPath)
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    respond(
                        """{"householdName":"Casa Verde","role":"member","emailRestricted":true,"status":"pending","expiresAt":"2026-08-15T10:00:00.000Z"}""",
                        HttpStatusCode.OK,
                        JsonResponseHeaders,
                    )
                }

                3 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals(
                        "/v1/invitations/$invitationToken/accept",
                        request.url.encodedPath,
                    )
                    assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
                    respond(
                        """{"household":${householdResponse(version = 1)}}""",
                        HttpStatusCode.OK,
                        JsonResponseHeaders,
                    )
                }

                4 -> {
                    assertEquals(HttpMethod.Delete, request.method)
                    assertEquals(
                        "/v1/households/household-1/invitations/invitation-1",
                        request.url.encodedPath,
                    )
                    assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
                    respond("", HttpStatusCode.NoContent)
                }

                else -> error("Unexpected request $requestCount")
            }
        }
        val api = apiClient(engine)

        try {
            val created = assertIs<ApiResult.Success<HouseholdInvitationDto>>(
                api.createHouseholdInvitation(
                    accessToken = "access-token",
                    householdId = "household-1",
                    payload = CreateHouseholdInvitationPayload(
                        role = "member",
                        email = "member@example.test",
                    ),
                ),
            )
            assertEquals(invitationToken, created.value.token)
            val preview = assertIs<ApiResult.Success<HouseholdInvitationPreviewDto>>(
                api.previewHouseholdInvitation(invitationToken),
            )
            assertTrue(preview.value.emailRestricted)
            assertIs<ApiResult.Success<AcceptHouseholdInvitationDto>>(
                api.acceptHouseholdInvitation("access-token", invitationToken),
            )
            assertEquals(
                ApiResult.Success(Unit),
                api.revokeHouseholdInvitation("access-token", "household-1", "invitation-1"),
            )
            assertEquals(4, requestCount)
        } finally {
            api.close()
        }
    }

    @Test
    fun mapsValidProblemDetailsWithoutExposingDetailOrInvalidViolations() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "type":"https://sharedhouse.example/problems/validation_failed",
                      "title":"The request contains invalid fields.",
                      "status":422,
                      "code":"VALIDATION_FAILED",
                      "correlationId":"018f8c28-5e4b-7cc0-9f8a-bfbf81e423b1",
                      "detail":"Internal detail must not be surfaced.",
                      "violations":[
                        {"field":"timezone","message":"Use an IANA timezone."},
                        {"field":"","message":"Unsafe empty field."}
                      ],
                      "unknownServerField":true
                    }
                """.trimIndent(),
                status = UnprocessableContent,
                headers = ProblemResponseHeaders,
            )
        }
        val api = apiClient(engine)

        try {
            val failure = assertIs<ApiResult.Failure>(api.account("access-token"))
            assertEquals("VALIDATION_FAILED", failure.code)
            assertEquals("The request contains invalid fields.", failure.title)
            assertEquals(422, failure.status)
            assertEquals("018f8c28-5e4b-7cc0-9f8a-bfbf81e423b1", failure.correlationId)
            assertEquals(
                listOf(FieldViolationDto("timezone", "Use an IANA timezone.")),
                failure.violations,
            )
        } finally {
            api.close()
        }
    }

    @Test
    fun malformedAndInconsistentErrorsUseSafeHttpStatusFallback() = runBlocking {
        var requestCount = 0
        val engine = MockEngine {
            requestCount += 1
            if (requestCount == 1) {
                respond(
                    content = "<html>upstream failure</html>",
                    status = HttpStatusCode.BadGateway,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()),
                )
            } else {
                respond(
                    content = """
                        {
                          "type":"about:blank",
                          "title":"Misleading success",
                          "status":200,
                          "code":"NOT_AN_ERROR",
                          "correlationId":"018f8c28-5e4b-7cc0-9f8a-bfbf81e423b1"
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = ProblemResponseHeaders,
                )
            }
        }
        val api = apiClient(engine)

        try {
            val malformed = assertIs<ApiResult.Failure>(api.account("access-token"))
            assertEquals("REQUEST_FAILED", malformed.code)
            assertEquals(502, malformed.status)
            assertEquals(null, malformed.correlationId)

            val inconsistent = assertIs<ApiResult.Failure>(api.account("access-token"))
            assertEquals("REQUEST_FAILED", inconsistent.code)
            assertEquals(503, inconsistent.status)
            assertEquals(null, inconsistent.correlationId)
        } finally {
            api.close()
        }
    }

    @Test
    fun invalidSuccessBodyIsNotReportedAsNetworkFailure() = runBlocking {
        val engine = MockEngine {
            respond(
                content = "not-json",
                status = HttpStatusCode.OK,
                headers = JsonResponseHeaders,
            )
        }
        val api = apiClient(engine)

        try {
            val failure = assertIs<ApiResult.Failure>(api.account("access-token"))
            assertEquals("INVALID_RESPONSE", failure.code)
            assertEquals(200, failure.status)
        } finally {
            api.close()
        }
    }

    @Test
    fun requestCancellationIsPropagated() = runBlocking {
        val engine = MockEngine {
            throw CancellationException("cancelled by caller")
        }
        val api = apiClient(engine)
        var cancellation: CancellationException? = null

        try {
            api.account("access-token")
        } catch (error: CancellationException) {
            cancellation = error
        } finally {
            api.close()
        }

        assertNotNull(cancellation)
        assertTrue(cancellation.message.orEmpty().contains("cancelled"))
    }

    private fun apiClient(
        engine: MockEngine,
        baseUrl: String = "https://api.sharedhouse.test",
    ): SharedHouseApiClient = SharedHouseApiClient(
        client = createSharedHouseHttpClient(engine),
        baseUrl = baseUrl,
    )

    private fun householdResponse(version: Int): String = """
        {
          "id":"household-1",
          "name":"Casa Verde",
          "countryCode":"GB",
          "timezone":"Europe/London",
          "currency":"GBP",
          "firstDayOfWeek":1,
          "cycleType":"fourteen_day",
          "cycleAnchor":"2026-08-03",
          "role":"owner",
          "status":"active",
          "version":$version,
          "createdAt":"2026-08-01T10:00:00.000Z",
          "updatedAt":"2026-08-01T10:00:00.000Z"
        }
    """.trimIndent()

    private companion object {
        val CalendarEventResponse = """
            {
              "id":"event-1",
              "householdId":"household-1",
              "title":"Boiler service",
              "description":"Annual safety visit",
              "type":"maintenance",
              "date":"2026-08-14",
              "startTime":"09:30",
              "endTime":"10:30",
              "reminderMinutesBefore":60,
              "createdByUserId":"user-1",
              "version":1,
              "createdAt":"2026-08-01T10:00:00.000Z",
              "updatedAt":"2026-08-01T10:00:00.000Z"
            }
        """.trimIndent()
        val ExpenseResponse = """
            {
              "id":"expense-1","householdId":"household-1","title":"Weekly groceries",
              "category":"groceries","amount":{"minorUnits":1001,"currency":"GBP"},
              "dueDate":"2026-08-14","notes":"Shared shop","splitMethod":"equal","status":"approved",
              "allocations":[{"membershipId":"membership-1","displayName":"Alex","amount":{"minorUnits":1001,"currency":"GBP"},"roundingAdjustmentMinor":0,"status":"outstanding","isCurrentUser":true}],
              "currentUserShare":{"minorUnits":1001,"currency":"GBP"},"createdByUserId":"user-1",
              "canApprove":false,"canReverse":true,"version":1,
              "createdAt":"2026-08-01T10:00:00.000Z","updatedAt":"2026-08-01T10:00:00.000Z"
            }
        """.trimIndent()
        val JsonResponseHeaders = headersOf(
            HttpHeaders.ContentType,
            ContentType.Application.Json.toString(),
        )
        val ProblemResponseHeaders = headersOf(
            HttpHeaders.ContentType,
            "application/problem+json",
        )
        val UnprocessableContent = HttpStatusCode(422, "Unprocessable Content")
    }
}
