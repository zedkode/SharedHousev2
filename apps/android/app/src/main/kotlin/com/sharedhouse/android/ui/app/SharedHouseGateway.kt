package com.sharedhouse.android.ui.app

import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.CalendarEventConfigurationDto
import com.sharedhouse.network.CalendarEventDto
import com.sharedhouse.network.BillingCoupleConfigurationDto
import com.sharedhouse.network.BillingRosterDto
import com.sharedhouse.network.ExpenseConfigurationDto
import com.sharedhouse.network.ReviseExpensePayload
import com.sharedhouse.network.ExpenseDto
import com.sharedhouse.network.ExpensePaymentDeclarationDto
import com.sharedhouse.network.ExpenseTemplateConfigurationDto
import com.sharedhouse.network.ExpenseTemplateDto
import com.sharedhouse.network.AcceptHouseholdInvitationDto
import com.sharedhouse.network.AccountDeletionResultDto
import com.sharedhouse.network.AccountExportDto
import com.sharedhouse.network.CreateHouseholdInvitationPayload
import com.sharedhouse.network.HouseholdConfigurationDto
import com.sharedhouse.network.HouseholdDto
import com.sharedhouse.network.HouseholdInvitationDto
import com.sharedhouse.network.HouseholdInvitationPreviewDto
import com.sharedhouse.network.HouseholdMemberActionDto
import com.sharedhouse.network.HouseholdMemberBoardDto
import com.sharedhouse.network.HouseholdMemberDto
import com.sharedhouse.network.HouseholdTaskActionDto
import com.sharedhouse.network.HouseholdTaskBoardDto
import com.sharedhouse.network.HouseholdTaskConfigurationDto
import com.sharedhouse.network.HouseholdTaskDto
import com.sharedhouse.network.HouseholdChatMessageDto
import com.sharedhouse.network.HouseholdChatPageDto
import com.sharedhouse.network.HouseholdChatAttachmentDto
import com.sharedhouse.network.UploadChatAttachmentDto
import com.sharedhouse.network.RegisterPayload
import com.sharedhouse.network.RegistrationAcceptedDto
import com.sharedhouse.network.ResendVerificationPayload
import com.sharedhouse.network.SessionDto
import com.sharedhouse.network.SharedHouseApiClient
import com.sharedhouse.network.SignInPayload
import com.sharedhouse.network.VerifyEmailPayload
import com.sharedhouse.network.ChangePasswordDto
import com.sharedhouse.network.RequestEmailChangeDto
import com.sharedhouse.network.AccountSecurityResultDto
import com.sharedhouse.network.AccountDto
import kotlinx.coroutines.flow.Flow

interface SharedHouseGateway {
    suspend fun register(payload: RegisterPayload): ApiResult<RegistrationAcceptedDto>

    suspend fun verifyEmail(payload: VerifyEmailPayload): ApiResult<SessionDto>

    suspend fun resendVerification(payload: ResendVerificationPayload): ApiResult<RegistrationAcceptedDto>

    suspend fun signIn(payload: SignInPayload): ApiResult<SessionDto>

    suspend fun refresh(refreshToken: String): ApiResult<SessionDto>

    suspend fun signOut(accessToken: String): ApiResult<Unit>

    suspend fun deleteAccount(accessToken: String, password: String): ApiResult<AccountDeletionResultDto>

    suspend fun exportAccount(accessToken: String, password: String): ApiResult<AccountExportDto>

    suspend fun updateAccountProfile(accessToken: String, displayName: String): ApiResult<AccountDto>
    suspend fun changePassword(accessToken: String, payload: ChangePasswordDto): ApiResult<AccountDto>
    suspend fun requestEmailChange(accessToken: String, payload: RequestEmailChangeDto): ApiResult<AccountSecurityResultDto>
    suspend fun confirmEmailChange(accessToken: String, code: String): ApiResult<AccountDto>

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

    suspend fun listHouseholdMembers(
        accessToken: String,
        householdId: String,
    ): ApiResult<HouseholdMemberBoardDto>

    suspend fun actOnHouseholdMember(
        accessToken: String,
        householdId: String,
        membershipId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        action: HouseholdMemberActionDto,
    ): ApiResult<HouseholdMemberDto>

    suspend fun listHouseholdInvitations(
        accessToken: String,
        householdId: String,
    ): ApiResult<List<HouseholdInvitationDto>>

    suspend fun createHouseholdInvitation(
        accessToken: String,
        householdId: String,
        payload: CreateHouseholdInvitationPayload,
    ): ApiResult<HouseholdInvitationDto>

    suspend fun previewHouseholdInvitation(
        token: String,
    ): ApiResult<HouseholdInvitationPreviewDto>

    suspend fun acceptHouseholdInvitation(
        accessToken: String,
        token: String,
    ): ApiResult<AcceptHouseholdInvitationDto>

    suspend fun revokeHouseholdInvitation(
        accessToken: String,
        householdId: String,
        invitationId: String,
    ): ApiResult<Unit>

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

    suspend fun listHouseholdTasks(accessToken: String, householdId: String): ApiResult<HouseholdTaskBoardDto>

    suspend fun createHouseholdTask(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: HouseholdTaskConfigurationDto,
    ): ApiResult<HouseholdTaskDto>

    suspend fun actOnHouseholdTask(
        accessToken: String,
        householdId: String,
        taskId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        action: HouseholdTaskActionDto,
    ): ApiResult<HouseholdTaskDto>

    suspend fun listHouseholdChatMessages(
        accessToken: String,
        householdId: String,
        after: String?,
    ): ApiResult<HouseholdChatPageDto>

    suspend fun createHouseholdChatMessage(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        body: String,
    ): ApiResult<HouseholdChatMessageDto>

    fun streamHouseholdChatMessages(
        accessToken: String,
        householdId: String,
        after: String?,
    ): Flow<ApiResult<HouseholdChatMessageDto>>

    suspend fun uploadHouseholdChatAttachment(accessToken:String,householdId:String,payload:UploadChatAttachmentDto): ApiResult<HouseholdChatAttachmentDto>
    suspend fun setHouseholdChatMessagePinned(accessToken:String,householdId:String,messageId:String,pinned:Boolean): ApiResult<HouseholdChatMessageDto>
    suspend fun createRichHouseholdChatMessage(accessToken:String,householdId:String,idempotencyKey:String,body:String,attachmentIds:List<String>,mentionedUserIds:List<String>,mentionAll:Boolean,location:com.sharedhouse.network.HouseholdChatLocationDto?): ApiResult<HouseholdChatMessageDto>

    suspend fun listExpenses(accessToken: String, householdId: String): ApiResult<List<ExpenseDto>>

    suspend fun getBillingRoster(accessToken: String, householdId: String): ApiResult<BillingRosterDto>

    suspend fun updateBillingRoster(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        couples: List<BillingCoupleConfigurationDto>,
    ): ApiResult<BillingRosterDto>

    suspend fun createExpense(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: ExpenseConfigurationDto,
    ): ApiResult<ExpenseDto>

    suspend fun approveExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
    ): ApiResult<ExpenseDto>

    suspend fun reviseExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        payload: ReviseExpensePayload,
    ): ApiResult<ExpenseDto>

    suspend fun reverseExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseDto>

    suspend fun declareExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        idempotencyKey: String,
        configuration: ExpensePaymentDeclarationDto,
    ): ApiResult<ExpenseDto>

    suspend fun confirmExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
    ): ApiResult<ExpenseDto>

    suspend fun disputeExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseDto>

    suspend fun reverseExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseDto>

    suspend fun listExpenseTemplates(accessToken: String, householdId: String): ApiResult<List<ExpenseTemplateDto>>

    suspend fun createExpenseTemplate(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: ExpenseTemplateConfigurationDto,
    ): ApiResult<ExpenseTemplateDto>

    suspend fun updateExpenseTemplate(
        accessToken: String,
        householdId: String,
        templateId: String,
        expectedVersion: Int,
        configuration: ExpenseTemplateConfigurationDto,
    ): ApiResult<ExpenseTemplateDto>

    suspend fun archiveExpenseTemplate(
        accessToken: String,
        householdId: String,
        templateId: String,
        expectedVersion: Int,
        reason: String,
    ): ApiResult<ExpenseTemplateDto>
}

class ApiSharedHouseGateway(
    private val api: SharedHouseApiClient,
) : SharedHouseGateway {
    override suspend fun register(payload: RegisterPayload) = api.register(payload)

    override suspend fun verifyEmail(payload: VerifyEmailPayload) = api.verifyEmail(payload)

    override suspend fun resendVerification(payload: ResendVerificationPayload) =
        api.resendVerification(payload)

    override suspend fun signIn(payload: SignInPayload) = api.signIn(payload)

    override suspend fun refresh(refreshToken: String) = api.refresh(refreshToken)

    override suspend fun signOut(accessToken: String) = api.signOut(accessToken)

    override suspend fun deleteAccount(accessToken: String, password: String) =
        api.deleteAccount(accessToken, password)

    override suspend fun exportAccount(accessToken: String, password: String) =
        api.exportAccount(accessToken, password)

    override suspend fun updateAccountProfile(accessToken: String,displayName: String)=api.updateAccountProfile(accessToken,displayName)
    override suspend fun changePassword(accessToken: String,payload: ChangePasswordDto)=api.changePassword(accessToken,payload)
    override suspend fun requestEmailChange(accessToken: String,payload: RequestEmailChangeDto)=api.requestEmailChange(accessToken,payload)
    override suspend fun confirmEmailChange(accessToken: String,code: String)=api.confirmEmailChange(accessToken,code)

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

    override suspend fun listHouseholdMembers(accessToken: String, householdId: String) =
        api.listHouseholdMembers(accessToken, householdId)

    override suspend fun actOnHouseholdMember(
        accessToken: String,
        householdId: String,
        membershipId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        action: HouseholdMemberActionDto,
    ) = api.actOnHouseholdMember(
        accessToken,
        householdId,
        membershipId,
        expectedVersion,
        idempotencyKey,
        action,
    )

    override suspend fun listHouseholdInvitations(
        accessToken: String,
        householdId: String,
    ) = api.listHouseholdInvitations(accessToken, householdId)

    override suspend fun createHouseholdInvitation(
        accessToken: String,
        householdId: String,
        payload: CreateHouseholdInvitationPayload,
    ) = api.createHouseholdInvitation(accessToken, householdId, payload)

    override suspend fun previewHouseholdInvitation(token: String) =
        api.previewHouseholdInvitation(token)

    override suspend fun acceptHouseholdInvitation(accessToken: String, token: String) =
        api.acceptHouseholdInvitation(accessToken, token)

    override suspend fun revokeHouseholdInvitation(
        accessToken: String,
        householdId: String,
        invitationId: String,
    ) = api.revokeHouseholdInvitation(accessToken, householdId, invitationId)

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

    override suspend fun listHouseholdTasks(accessToken: String, householdId: String) =
        api.listHouseholdTasks(accessToken, householdId)

    override suspend fun createHouseholdTask(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: HouseholdTaskConfigurationDto,
    ) = api.createHouseholdTask(accessToken, householdId, idempotencyKey, configuration)

    override suspend fun actOnHouseholdTask(
        accessToken: String,
        householdId: String,
        taskId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        action: HouseholdTaskActionDto,
    ) = api.actOnHouseholdTask(
        accessToken,
        householdId,
        taskId,
        expectedVersion,
        idempotencyKey,
        action,
    )

    override suspend fun listHouseholdChatMessages(
        accessToken: String,
        householdId: String,
        after: String?,
    ) = api.listHouseholdChatMessages(accessToken, householdId, after)

    override suspend fun createHouseholdChatMessage(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        body: String,
    ) = api.createHouseholdChatMessage(accessToken, householdId, idempotencyKey, body)

    override fun streamHouseholdChatMessages(
        accessToken: String,
        householdId: String,
        after: String?,
    ) = api.streamHouseholdChatMessages(accessToken, householdId, after)

    override suspend fun uploadHouseholdChatAttachment(accessToken:String,householdId:String,payload:UploadChatAttachmentDto)=api.uploadHouseholdChatAttachment(accessToken,householdId,payload)
    override suspend fun setHouseholdChatMessagePinned(accessToken:String,householdId:String,messageId:String,pinned:Boolean)=api.setHouseholdChatMessagePinned(accessToken,householdId,messageId,pinned)
    override suspend fun createRichHouseholdChatMessage(accessToken:String,householdId:String,idempotencyKey:String,body:String,attachmentIds:List<String>,mentionedUserIds:List<String>,mentionAll:Boolean,location:com.sharedhouse.network.HouseholdChatLocationDto?)=api.createHouseholdChatMessage(accessToken,householdId,idempotencyKey,body,attachmentIds=attachmentIds,mentionedUserIds=mentionedUserIds,mentionAll=mentionAll,location=location)

    override suspend fun listExpenses(accessToken: String, householdId: String) =
        api.listExpenses(accessToken, householdId)

    override suspend fun getBillingRoster(accessToken: String, householdId: String) =
        api.getBillingRoster(accessToken, householdId)

    override suspend fun updateBillingRoster(
        accessToken: String,
        householdId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        couples: List<BillingCoupleConfigurationDto>,
    ) = api.updateBillingRoster(
        accessToken,
        householdId,
        expectedVersion,
        idempotencyKey,
        couples,
    )

    override suspend fun createExpense(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: ExpenseConfigurationDto,
    ) = api.createExpense(accessToken, householdId, idempotencyKey, configuration)

    override suspend fun approveExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
    ) = api.approveExpense(accessToken, householdId, expenseId, expectedVersion)

    override suspend fun reviseExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
        idempotencyKey: String,
        payload: ReviseExpensePayload,
    ) = api.reviseExpense(
        accessToken,
        householdId,
        expenseId,
        expectedVersion,
        idempotencyKey,
        payload,
    )

    override suspend fun reverseExpense(
        accessToken: String,
        householdId: String,
        expenseId: String,
        expectedVersion: Int,
        reason: String,
    ) = api.reverseExpense(accessToken, householdId, expenseId, expectedVersion, reason)

    override suspend fun declareExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        idempotencyKey: String,
        configuration: ExpensePaymentDeclarationDto,
    ) = api.declareExpensePayment(accessToken, householdId, expenseId, idempotencyKey, configuration)

    override suspend fun confirmExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
    ) = api.confirmExpensePayment(accessToken, householdId, expenseId, paymentId, expectedVersion)

    override suspend fun disputeExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
        reason: String,
    ) = api.disputeExpensePayment(
        accessToken,
        householdId,
        expenseId,
        paymentId,
        expectedVersion,
        reason,
    )

    override suspend fun reverseExpensePayment(
        accessToken: String,
        householdId: String,
        expenseId: String,
        paymentId: String,
        expectedVersion: Int,
        reason: String,
    ) = api.reverseExpensePayment(
        accessToken,
        householdId,
        expenseId,
        paymentId,
        expectedVersion,
        reason,
    )

    override suspend fun listExpenseTemplates(accessToken: String, householdId: String) =
        api.listExpenseTemplates(accessToken, householdId)

    override suspend fun createExpenseTemplate(
        accessToken: String,
        householdId: String,
        idempotencyKey: String,
        configuration: ExpenseTemplateConfigurationDto,
    ) = api.createExpenseTemplate(accessToken, householdId, idempotencyKey, configuration)

    override suspend fun updateExpenseTemplate(
        accessToken: String,
        householdId: String,
        templateId: String,
        expectedVersion: Int,
        configuration: ExpenseTemplateConfigurationDto,
    ) = api.updateExpenseTemplate(accessToken, householdId, templateId, expectedVersion, configuration)

    override suspend fun archiveExpenseTemplate(
        accessToken: String,
        householdId: String,
        templateId: String,
        expectedVersion: Int,
        reason: String,
    ) = api.archiveExpenseTemplate(accessToken, householdId, templateId, expectedVersion, reason)
}
