package com.sharedhouse.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readLine
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

private val SharedHouseJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun createSharedHouseHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(SharedHouseJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
    }
    defaultRequest {
        accept(ContentType.Application.Json)
    }
}

class SharedHouseApiClient(
    private val client: HttpClient,
    baseUrl: String,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun register(payload: RegisterPayload): ApiResult<RegistrationAcceptedDto> =
        execute {
            client.post("$baseUrl/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }

    suspend fun verifyEmail(payload: VerifyEmailPayload): ApiResult<SessionDto> =
        execute {
            client.post("$baseUrl/v1/auth/verify-email") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }

    suspend fun resendVerification(
        payload: ResendVerificationPayload,
    ): ApiResult<RegistrationAcceptedDto> = execute {
        client.post("$baseUrl/v1/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend fun signIn(payload: SignInPayload): ApiResult<SessionDto> =
        execute {
            client.post("$baseUrl/v1/auth/sign-in") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }

    suspend fun refresh(refreshToken: String): ApiResult<SessionDto> =
        execute {
            client.post("$baseUrl/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshSessionPayload(refreshToken))
            }
        }

    suspend fun signOut(accessToken: String): ApiResult<Unit> = executeWithoutBody {
        client.post("$baseUrl/v1/auth/sign-out") {
            bearerAuth(accessToken)
        }
    }

    suspend fun account(accessToken: String): ApiResult<AccountDto> = execute {
        client.get("$baseUrl/v1/account") {
            bearerAuth(accessToken)
        }
    }

    suspend fun deleteAccount(
        accessToken: String,
        password: String,
    ): ApiResult<AccountDeletionResultDto> = execute {
        client.delete("$baseUrl/v1/account") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(DeleteAccountPayload(password))
        }
    }

    suspend fun exportAccount(
        accessToken: String,
        password: String,
    ): ApiResult<AccountExportDto> = execute {
        client.post("$baseUrl/v1/account/export") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(ExportAccountPayload(password))
        }
    }

    suspend fun listHouseholds(accessToken: String): ApiResult<List<HouseholdDto>> = execute {
        client.get("$baseUrl/v1/households") {
            bearerAuth(accessToken)
        }
    }

    suspend fun createHousehold(
        accessToken: String,
        idempotencyKey: String,
        configuration: HouseholdConfigurationDto,
    ): ApiResult<HouseholdDto> = execute {
        client.post("$baseUrl/v1/households") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun updateHousehold(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        configuration: HouseholdConfigurationDto,
    ): ApiResult<HouseholdDto> = execute {
        client.patch("$baseUrl/v1/households/$householdId") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun listHouseholdMembers(
        accessToken: String,
        householdId: String,
    ): ApiResult<HouseholdMemberBoardDto> = execute {
        client.get("$baseUrl/v1/households/$householdId/members") {
            bearerAuth(accessToken)
        }
    }

    suspend fun actOnHouseholdMember(
        accessToken: String,
        householdId: String,
        membershipId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        action: HouseholdMemberActionDto,
    ): ApiResult<HouseholdMemberDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/members/$membershipId/actions") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(action)
        }
    }

    suspend fun listHouseholdInvitations(
        accessToken: String,
        householdId: String,
    ): ApiResult<List<HouseholdInvitationDto>> = execute {
        client.get("$baseUrl/v1/households/$householdId/invitations") {
            bearerAuth(accessToken)
        }
    }

    suspend fun createHouseholdInvitation(
        accessToken: String,
        householdId: String,
        payload: CreateHouseholdInvitationPayload,
    ): ApiResult<HouseholdInvitationDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/invitations") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend fun previewHouseholdInvitation(
        token: String,
    ): ApiResult<HouseholdInvitationPreviewDto> = execute {
        client.get("$baseUrl/v1/invitations/$token")
    }

    suspend fun acceptHouseholdInvitation(
        accessToken: String,
        token: String,
    ): ApiResult<AcceptHouseholdInvitationDto> = execute {
        client.post("$baseUrl/v1/invitations/$token/accept") {
            bearerAuth(accessToken)
        }
    }

    suspend fun revokeHouseholdInvitation(
        accessToken: String,
        householdId: String,
        invitationId: String,
    ): ApiResult<Unit> = executeWithoutBody {
        client.delete("$baseUrl/v1/households/$householdId/invitations/$invitationId") {
            bearerAuth(accessToken)
        }
    }

    suspend fun listCalendarEvents(
        accessToken: String,
        householdId: String,
        from: String,
        to: String,
    ): ApiResult<List<CalendarEventDto>> = execute {
        client.get("$baseUrl/v1/households/$householdId/calendar-events") {
            bearerAuth(accessToken)
            parameter("from", from)
            parameter("to", to)
        }
    }

    suspend fun createCalendarEvent(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: CalendarEventConfigurationDto,
    ): ApiResult<CalendarEventDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/calendar-events") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun updateCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
        configuration: CalendarEventConfigurationDto,
    ): ApiResult<CalendarEventDto> = execute {
        client.patch("$baseUrl/v1/households/$householdId/calendar-events/$eventId") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun deleteCalendarEvent(
        accessToken: String,
        householdId: String,
        eventId: String,
        expectedVersion: Int,
    ): ApiResult<Unit> = executeWithoutBody {
        client.delete("$baseUrl/v1/households/$householdId/calendar-events/$eventId") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
        }
    }

    suspend fun listExpenses(
        accessToken: String,
        householdId: String,
    ): ApiResult<List<ExpenseDto>> = execute {
        client.get("$baseUrl/v1/households/$householdId/expenses") {
            bearerAuth(accessToken)
        }
    }

    suspend fun getBillingRoster(
        accessToken: String,
        householdId: String,
    ): ApiResult<BillingRosterDto> = execute {
        client.get("$baseUrl/v1/households/$householdId/billing-roster") {
            bearerAuth(accessToken)
        }
    }

    suspend fun updateBillingRoster(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        couples: List<BillingCoupleConfigurationDto>,
    ): ApiResult<BillingRosterDto> = execute {
        client.put("$baseUrl/v1/households/$householdId/billing-roster") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(UpdateBillingRosterDto(couples))
        }
    }

    suspend fun listHouseholdTasks(
        accessToken: String,
        householdId: String,
    ): ApiResult<HouseholdTaskBoardDto> = execute {
        client.get("$baseUrl/v1/households/$householdId/tasks") { bearerAuth(accessToken) }
    }

    suspend fun listHouseholdChatMessages(
        accessToken: String,
        householdId: String,
        after: String? = null,
        limit: Int = 100,
    ): ApiResult<HouseholdChatPageDto> = execute {
        client.get("$baseUrl/v1/households/$householdId/chat/messages") {
            bearerAuth(accessToken)
            after?.let { parameter("after", it) }
            parameter("limit", limit)
        }
    }

    suspend fun createHouseholdChatMessage(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        body: String,
    ): ApiResult<HouseholdChatMessageDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/chat/messages") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(CreateHouseholdChatMessageDto(body))
        }
    }

    fun streamHouseholdChatMessages(
        accessToken: String,
        householdId: String,
        after: String?,
    ): Flow<ApiResult<HouseholdChatMessageDto>> = flow {
        try {
            client.prepareGet("$baseUrl/v1/households/$householdId/chat/messages/stream") {
                bearerAuth(accessToken)
                after?.let { parameter("after", it) }
                accept(ContentType.Text.EventStream)
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    emit(response.toFailure())
                    return@execute
                }
                val channel = response.bodyAsChannel()
                var eventType: String? = null
                val data = StringBuilder()
                while (!channel.isClosedForRead) {
                    val line = channel.readLine() ?: break
                    when {
                        line.startsWith("event:") -> eventType = line.substringAfter(':').trim()
                        line.startsWith("data:") -> data.append(line.substringAfter(':').trim())
                        line.isEmpty() -> {
                            if (eventType == "message" && data.isNotEmpty()) {
                                emit(
                                    ApiResult.Success(
                                        SharedHouseJson.decodeFromString<HouseholdChatMessageDto>(
                                            data.toString(),
                                        ),
                                    ),
                                )
                            }
                            eventType = null
                            data.clear()
                        }
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emit(networkFailure())
        }
    }

    suspend fun createHouseholdTask(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: HouseholdTaskConfigurationDto,
    ): ApiResult<HouseholdTaskDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/tasks") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun actOnHouseholdTask(
        accessToken: String,
        householdId: String,
        taskId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        action: HouseholdTaskActionDto,
    ): ApiResult<HouseholdTaskDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/tasks/$taskId/actions") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(action)
        }
    }

    suspend fun createExpense(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: ExpenseConfigurationDto,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun approveExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/approve") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
        }
    }

    suspend fun reviseExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        payload: ReviseExpensePayload,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/revise") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend fun reverseExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/reverse") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(ReverseExpensePayload(reason))
        }
    }

    suspend fun declareExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        idempotencyKey: String,
        configuration: ExpensePaymentDeclarationDto,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/payments") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun confirmExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/payments/$paymentId/confirm") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
        }
    }

    suspend fun disputeExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/payments/$paymentId/dispute") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(ExpensePaymentActionPayload(reason))
        }
    }

    suspend fun reverseExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expenses/$expenseId/payments/$paymentId/reverse") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(ExpensePaymentActionPayload(reason))
        }
    }

    suspend fun listExpenseTemplates(
        accessToken: String,
        householdId: String,
    ): ApiResult<List<ExpenseTemplateDto>> = execute {
        client.get("$baseUrl/v1/households/$householdId/expense-templates") {
            bearerAuth(accessToken)
        }
    }

    suspend fun createExpenseTemplate(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: ExpenseTemplateConfigurationDto,
    ): ApiResult<ExpenseTemplateDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expense-templates") {
            bearerAuth(accessToken)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun updateExpenseTemplate(
        accessToken: String,
        householdId: String,
        templateId: String,
        expectedVersion: Int,
        configuration: ExpenseTemplateConfigurationDto,
    ): ApiResult<ExpenseTemplateDto> = execute {
        client.patch("$baseUrl/v1/households/$householdId/expense-templates/$templateId") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(configuration)
        }
    }

    suspend fun archiveExpenseTemplate(
        accessToken: String,
        householdId: String,
        templateId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseTemplateDto> = execute {
        client.post("$baseUrl/v1/households/$householdId/expense-templates/$templateId/archive") {
            bearerAuth(accessToken)
            header(HttpHeaders.IfMatch, "\"$expectedVersion\"")
            contentType(ContentType.Application.Json)
            setBody(ArchiveExpenseTemplatePayload(reason))
        }
    }

    suspend fun close() {
        client.close()
    }

    private suspend inline fun <reified T> execute(
        request: suspend () -> HttpResponse,
    ): ApiResult<T> {
        val response = try {
            request()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return networkFailure()
        }

        return if (response.status.isSuccess()) {
            try {
                ApiResult.Success(response.body<T>())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                invalidResponse(response.status.value)
            }
        } else {
            response.toFailure()
        }
    }

    private suspend fun executeWithoutBody(
        request: suspend () -> HttpResponse,
    ): ApiResult<Unit> {
        val response = try {
            request()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return networkFailure()
        }

        return if (response.status.isSuccess()) {
            ApiResult.Success(Unit)
        } else {
            response.toFailure()
        }
    }
}

private suspend fun HttpResponse.toFailure(): ApiResult.Failure {
    val problem = try {
        SharedHouseJson.decodeFromString<ProblemDetailsDto>(bodyAsText())
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    return problem?.toSafeFailure(status.value) ?: requestFailure(status.value)
}

private fun ProblemDetailsDto.toSafeFailure(responseStatus: Int): ApiResult.Failure? {
    if (
        status != responseStatus ||
        status !in 400..599 ||
        !ProblemCode.matches(code) ||
        title.isBlank() ||
        title.length > MaximumProblemTitleLength
    ) {
        return null
    }

    val safeCorrelationId = correlationId.takeIf(CorrelationId::matches)
    val safeViolations = violations
        .asSequence()
        .filter { violation ->
            violation.field.isNotBlank() &&
                violation.field.length <= MaximumViolationFieldLength &&
                violation.message.isNotBlank() &&
                violation.message.length <= MaximumViolationMessageLength
        }
        .take(MaximumViolationCount)
        .toList()

    return ApiResult.Failure(
        code = code,
        title = title,
        status = responseStatus,
        correlationId = safeCorrelationId,
        violations = safeViolations,
    )
}

private fun requestFailure(status: Int): ApiResult.Failure = ApiResult.Failure(
    code = "REQUEST_FAILED",
    title = "The service could not complete the request.",
    status = status,
)

private fun invalidResponse(status: Int): ApiResult.Failure = ApiResult.Failure(
    code = "INVALID_RESPONSE",
    title = "The service returned an invalid response.",
    status = status,
)

private fun networkFailure(): ApiResult.Failure = ApiResult.Failure(
    code = "NETWORK_UNAVAILABLE",
    title = "The service could not be reached.",
)

private val ProblemCode = Regex("^[A-Z][A-Z0-9_]{1,63}$")
private val CorrelationId = Regex(
    "^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE,
)
private const val MaximumProblemTitleLength = 200
private const val MaximumViolationCount = 50
private const val MaximumViolationFieldLength = 120
private const val MaximumViolationMessageLength = 500
