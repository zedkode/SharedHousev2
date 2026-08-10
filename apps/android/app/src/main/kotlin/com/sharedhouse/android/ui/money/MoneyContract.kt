package com.sharedhouse.android.ui.money

import java.time.LocalDate

data class MoneyUiState(
    val currency: String = "GBP",
    val content: MoneyContent = MoneyContent.Loading,
    val canCreate: Boolean = false,
    val canManageTemplates: Boolean = false,
    val templates: List<ExpenseTemplateUi> = emptyList(),
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
    val status: ExpenseStatus,
    val allocations: List<ExpenseAllocationUi>,
    val currentUserShareMinor: Long,
    val canApprove: Boolean,
    val canReverse: Boolean,
    val version: Int,
)

data class ExpenseAllocationUi(
    val displayName: String,
    val amountMinor: Long,
    val roundingAdjustmentMinor: Long,
    val isCurrentUser: Boolean,
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

enum class MoneyProblem { LOAD_FAILED, CREATE_FAILED, APPROVE_FAILED, REVERSE_FAILED, TEMPLATE_FAILED, VERSION_CONFLICT }

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
    data class CreateTemplate(val draft: ExpenseTemplateDraft) : MoneyAction
    data class UpdateTemplate(val templateId: String, val expectedVersion: Int, val draft: ExpenseTemplateDraft) : MoneyAction
    data class ArchiveTemplate(val templateId: String, val expectedVersion: Int, val reason: String) : MoneyAction
}
