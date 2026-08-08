package com.sharedhouse.android.ui.money

import java.time.LocalDate

data class MoneyUiState(
    val currency: String = "GBP",
    val content: MoneyContent = MoneyContent.Loading,
    val canCreate: Boolean = false,
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
    OTHER("other");

    companion object {
        fun fromWire(value: String) = entries.firstOrNull { it.wireValue == value } ?: OTHER
    }
}

enum class ExpenseStatus { PROPOSED, APPROVED, REVERSED }

enum class MoneyFilter { ACTIVE, PROPOSED, REVERSED, ALL }

enum class MoneyProblem { LOAD_FAILED, CREATE_FAILED, APPROVE_FAILED, REVERSE_FAILED, VERSION_CONFLICT }

data class ExpenseDraft(
    val title: String,
    val category: ExpenseCategory,
    val amountMinor: Long,
    val dueDate: LocalDate,
    val notes: String?,
)

sealed interface MoneyAction {
    data object Retry : MoneyAction
    data class Create(val draft: ExpenseDraft) : MoneyAction
    data class Approve(val expenseId: String, val expectedVersion: Int) : MoneyAction
    data class Reverse(val expenseId: String, val expectedVersion: Int, val reason: String) : MoneyAction
}
