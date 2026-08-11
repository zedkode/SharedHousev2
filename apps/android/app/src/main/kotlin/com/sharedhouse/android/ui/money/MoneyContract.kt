package com.sharedhouse.android.ui.money

import java.time.LocalDate
import java.time.Instant

data class MoneyUiState(
    val currency: String = "GBP",
    val content: MoneyContent = MoneyContent.Loading,
    val canCreate: Boolean = false,
    val canManageTemplates: Boolean = false,
    val templates: List<ExpenseTemplateUi> = emptyList(),
    val billingRoster: BillingRosterUi? = null,
    val isMutationInProgress: Boolean = false,
    val problem: MoneyProblem? = null,
)

sealed interface MoneyContent {
    data object Loading : MoneyContent
    data object Error : MoneyContent
    data class Ready(val expenses: List<ExpenseUi>) : MoneyContent
}

data class ExpenseUi(
    val id: String,
    val title: String,
    val category: ExpenseCategory,
    val customCategoryName: String? = null,
    val amountMinor: Long,
    val currency: String,
    val dueDate: LocalDate,
    val notes: String?,
    val sourceTemplateId: String? = null,
    val occurrenceDate: LocalDate? = null,
    val status: ExpenseStatus,
    val allocations: List<ExpenseAllocationUi>,
    val currentUserShareMinor: Long,
    val canApprove: Boolean,
    val canReverse: Boolean,
    val version: Int,
)

data class ExpenseAllocationUi(
    val membershipId: String,
    val displayName: String,
    val billingUnitType: BillingUnitType = BillingUnitType.INDIVIDUAL,
    val participantCount: Int = 1,
    val amountMinor: Long,
    val roundingAdjustmentMinor: Long,
    val status: ExpenseAllocationStatus,
    val paymentDeclarations: List<ExpensePaymentUi>,
    val canDeclarePayment: Boolean,
    val isCurrentUser: Boolean,
)

enum class BillingUnitType { INDIVIDUAL, COUPLE }

data class BillingRosterMemberUi(
    val membershipId: String,
    val displayName: String,
    val isCurrentUser: Boolean,
)

data class BillingCoupleUi(
    val id: String,
    val primaryMembershipId: String,
    val primaryDisplayName: String,
    val partnerMembershipId: String?,
    val partnerDisplayName: String,
)

data class BillingRosterUi(
    val members: List<BillingRosterMemberUi>,
    val couples: List<BillingCoupleUi>,
    val residentCount: Int,
    val billingUnitCount: Int,
    val canManage: Boolean,
    val version: Int,
)

data class BillingCoupleDraft(
    val primaryMembershipId: String,
    val partnerMembershipId: String? = null,
    val partnerDisplayName: String? = null,
)

enum class ExpenseAllocationStatus { OUTSTANDING, DECLARED, PAID, DISPUTED }

enum class ExpensePaymentMethod(val wireValue: String) {
    BANK_TRANSFER("bank_transfer"),
    CASH("cash"),
    CARD("card"),
    DIRECT_DEBIT("direct_debit"),
    OTHER("other");

    companion object {
        fun fromWire(value: String) = entries.firstOrNull { it.wireValue == value } ?: OTHER
    }
}

enum class ExpensePaymentStatus { DECLARED, CONFIRMED, DISPUTED, REVERSED }

data class ExpensePaymentUi(
    val id: String,
    val payerDisplayName: String,
    val amountMinor: Long,
    val currency: String,
    val method: ExpensePaymentMethod,
    val reference: String?,
    val note: String?,
    val paidAt: Instant,
    val status: ExpensePaymentStatus,
    val confirmedAt: Instant?,
    val disputeReason: String?,
    val reversedAt: Instant?,
    val reversalReason: String?,
    val canConfirm: Boolean,
    val canDispute: Boolean,
    val canReverse: Boolean,
    val version: Int,
)

data class ExpensePaymentDraft(
    val method: ExpensePaymentMethod,
    val paidAt: Instant,
    val reference: String?,
    val note: String?,
)

enum class ExpenseCategory(val wireValue: String) {
    RENT("rent"),
    ELECTRICITY("electricity"),
    GAS("gas"),
    WATER("water"),
    INTERNET("internet"),
    COUNCIL_TAX("council_tax"),
    GROCERIES("groceries"),
    HOUSEHOLD_SUPPLIES("household_supplies"),
    MAINTENANCE("maintenance"),
    OTHER("other"),
    CUSTOM("custom");

    companion object {
        fun fromWire(value: String) = entries.firstOrNull { it.wireValue == value } ?: OTHER
    }
}

enum class ExpenseStatus { PROPOSED, APPROVED, REVERSED }

enum class MoneyFilter { ACTIVE, PROPOSED, REVERSED, ALL }

enum class MoneyProblem {
    LOAD_FAILED,
    CREATE_FAILED,
    APPROVE_FAILED,
    REVERSE_FAILED,
    TEMPLATE_FAILED,
    PAYMENT_DECLARE_FAILED,
    PAYMENT_CONFIRM_FAILED,
    PAYMENT_DISPUTE_FAILED,
    PAYMENT_REVERSE_FAILED,
    BILLING_ROSTER_FAILED,
    VERSION_CONFLICT,
}

data class ExpenseDraft(
    val title: String,
    val category: ExpenseCategory,
    val customCategoryName: String? = null,
    val amountMinor: Long,
    val dueDate: LocalDate,
    val notes: String?,
)

enum class ExpenseTemplateCadence(val wireValue: String) {
    WEEKLY("weekly"), MONTHLY("monthly"), QUARTERLY("quarterly"), YEARLY("yearly");

    companion object {
        fun fromWire(value: String) = entries.firstOrNull { it.wireValue == value } ?: MONTHLY
    }
}

data class ExpenseTemplateUi(
    val id: String,
    val title: String,
    val category: ExpenseCategory,
    val customCategoryName: String?,
    val amountMinor: Long,
    val currency: String,
    val cadence: ExpenseTemplateCadence,
    val nextDueDate: LocalDate,
    val notes: String?,
    val active: Boolean,
    val canManage: Boolean,
    val version: Int,
)

data class ExpenseTemplateDraft(
    val title: String,
    val category: ExpenseCategory,
    val customCategoryName: String?,
    val amountMinor: Long,
    val cadence: ExpenseTemplateCadence,
    val nextDueDate: LocalDate,
    val notes: String?,
)

sealed interface MoneyAction {
    data object Retry : MoneyAction
    data class Create(val draft: ExpenseDraft) : MoneyAction
    data class Approve(val expenseId: String, val expectedVersion: Int) : MoneyAction
    data class Reverse(val expenseId: String, val expectedVersion: Int, val reason: String) : MoneyAction
    data class DeclarePayment(val expenseId: String, val draft: ExpensePaymentDraft) : MoneyAction
    data class ConfirmPayment(val expenseId: String, val paymentId: String, val expectedVersion: Int) : MoneyAction
    data class DisputePayment(
        val expenseId: String,
        val paymentId: String,
        val expectedVersion: Int,
        val reason: String,
    ) : MoneyAction
    data class ReversePayment(
        val expenseId: String,
        val paymentId: String,
        val expectedVersion: Int,
        val reason: String,
    ) : MoneyAction
    data class CreateTemplate(val draft: ExpenseTemplateDraft) : MoneyAction
    data class UpdateTemplate(val templateId: String, val expectedVersion: Int, val draft: ExpenseTemplateDraft) : MoneyAction
    data class ArchiveTemplate(val templateId: String, val expectedVersion: Int, val reason: String) : MoneyAction
    data class UpdateBillingRoster(
        val expectedVersion: Int,
        val couples: List<BillingCoupleDraft>,
    ) : MoneyAction
}
