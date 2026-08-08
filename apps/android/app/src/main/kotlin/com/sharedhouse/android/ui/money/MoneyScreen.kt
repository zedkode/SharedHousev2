package com.sharedhouse.android.ui.money

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    state: MoneyUiState,
    onAction: (MoneyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    var filterName by rememberSaveable { mutableStateOf(MoneyFilter.ACTIVE.name) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val problemText = state.problem?.let { stringResource(it.messageResource) }
    LaunchedEffect(problemText) { problemText?.let { snackbar.showSnackbar(it) } }

    val expenses = (state.content as? MoneyContent.Ready)?.expenses.orEmpty()
    val filter = runCatching { MoneyFilter.valueOf(filterName) }.getOrDefault(MoneyFilter.ACTIVE)
    val visible = expenses.filter { expense ->
        when (filter) {
            MoneyFilter.ACTIVE -> expense.status == ExpenseStatus.APPROVED
            MoneyFilter.PROPOSED -> expense.status == ExpenseStatus.PROPOSED
            MoneyFilter.REVERSED -> expense.status == ExpenseStatus.REVERSED
            MoneyFilter.ALL -> true
        }
    }
    val selected = selectedId?.let { id -> expenses.firstOrNull { it.id == id } }
    val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
    val householdTotal = approved.sumOf { it.amountMinor }
    val personalTotal = approved.sumOf { it.currentUserShareMinor }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.money_title))
                        Text(
                            stringResource(R.string.money_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.canCreate && state.content is MoneyContent.Ready) {
                ExtendedFloatingActionButton(
                    onClick = { createOpen = true },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text(stringResource(R.string.money_add_expense)) },
                )
            }
        },
    ) { padding ->
        when (state.content) {
            MoneyContent.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            MoneyContent.Error -> MoneyError(
                onRetry = { onAction(MoneyAction.Retry) },
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is MoneyContent.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = stringResource(R.string.money_your_outstanding),
                            value = formatMoney(personalTotal, state.currency),
                            modifier = Modifier.weight(1f),
                        )
                        SummaryCard(
                            icon = Icons.Outlined.Groups,
                            label = stringResource(R.string.money_household_total),
                            value = formatMoney(householdTotal, state.currency),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoneyFilter.entries.forEach { candidate ->
                            FilterChip(
                                selected = candidate == filter,
                                onClick = { filterName = candidate.name },
                                label = { Text(stringResource(candidate.labelResource)) },
                            )
                        }
                    }
                }
                if (visible.isEmpty()) {
                    item { EmptyMoney(filter, state.canCreate, onAdd = { createOpen = true }) }
                } else {
                    items(visible, key = ExpenseUi::id) { expense ->
                        ExpenseCard(expense = expense, onClick = { selectedId = expense.id })
                    }
                }
            }
        }
    }

    if (createOpen) {
        ExpenseEditor(
            currency = state.currency,
            busy = state.isMutationInProgress,
            onDismiss = { createOpen = false },
            onSubmit = {
                onAction(MoneyAction.Create(it))
                createOpen = false
            },
        )
    }
    if (selected != null) {
        ExpenseDetails(
            expense = selected,
            busy = state.isMutationInProgress,
            onDismiss = { selectedId = null },
            onApprove = { onAction(MoneyAction.Approve(selected.id, selected.version)) },
            onReverse = { reason ->
                onAction(MoneyAction.Reverse(selected.id, selected.version, reason))
                selectedId = null
            },
        )
    }
}

@Composable
private fun SummaryCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExpenseCard(expense: ExpenseUi, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(expense.category.labelResource), style = MaterialTheme.typography.bodySmall)
                }
                Text(formatMoney(expense.amountMinor, expense.currency), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Outlined.MoreVert, null)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.money_due_value, expense.dueDate.toString()), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(stringResource(expense.status.labelResource), style = MaterialTheme.typography.labelLarge, color = expense.status.color())
            }
            HorizontalDivider()
            Text(
                stringResource(R.string.money_your_share_value, formatMoney(expense.currentUserShareMinor, expense.currency)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EmptyMoney(filter: MoneyFilter, canCreate: Boolean, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CheckCircle, null)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(filter.emptyResource), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.money_empty_explanation), style = MaterialTheme.typography.bodyMedium)
            if (canCreate) TextButton(onClick = onAdd) { Text(stringResource(R.string.money_add_first)) }
        }
    }
}

@Composable
private fun MoneyError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.money_load_error), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseEditor(currency: String, busy: Boolean, onDismiss: () -> Unit, onSubmit: (ExpenseDraft) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(ExpenseCategory.GROCERIES.name) }
    var dueDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var categoriesOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val category = ExpenseCategory.valueOf(categoryName)
    val parsedAmount = parseMoney(amount, currency)
    val valid = title.trim().isNotEmpty() && parsedAmount != null && parsedAmount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_new_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.money_equal_split_explanation), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(title, { title = it.take(100) }, label = { Text(stringResource(R.string.money_expense_title)) }, singleLine = true)
                Column {
                    OutlinedTextField(
                        value = stringResource(category.labelResource),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.money_category)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { categoriesOpen = true }) { Icon(Icons.Outlined.MoreVert, null) } },
                    )
                    DropdownMenu(expanded = categoriesOpen, onDismissRequest = { categoriesOpen = false }) {
                        ExpenseCategory.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(stringResource(item.labelResource)) },
                                onClick = { categoryName = item.name; categoriesOpen = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    amount,
                    { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(16) },
                    label = { Text(stringResource(R.string.money_amount, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = attempted && parsedAmount == null,
                    supportingText = if (attempted && parsedAmount == null) ({ Text(stringResource(R.string.money_amount_invalid)) }) else null,
                )
                OutlinedTextField(
                    dueDate,
                    {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.money_due_date)) },
                    trailingIcon = { IconButton(onClick = { dateOpen = true }) { Icon(Icons.Outlined.CalendarMonth, null) } },
                )
                OutlinedTextField(notes, { notes = it.take(500) }, label = { Text(stringResource(R.string.money_notes_optional)) }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    attempted = true
                    if (valid) onSubmit(ExpenseDraft(title.trim(), category, requireNotNull(parsedAmount), LocalDate.parse(dueDate), notes.trim().ifEmpty { null }))
                },
            ) { Text(stringResource(R.string.money_save_expense)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    if (dateOpen) {
        val picker = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(dueDate).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    dateOpen = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { dateOpen = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDetails(expense: ExpenseUi, busy: Boolean, onDismiss: () -> Unit, onApprove: () -> Unit, onReverse: (String) -> Unit) {
    var reverseOpen by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(expense.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(expense.status.labelResource), color = expense.status.color())
            Text(formatMoney(expense.amountMinor, expense.currency), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.money_due_value, expense.dueDate.toString()))
            expense.notes?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HorizontalDivider()
            Text(stringResource(R.string.money_split_breakdown), style = MaterialTheme.typography.titleMedium)
            expense.allocations.forEach { allocation ->
                Row(Modifier.fillMaxWidth()) {
                    Text(if (allocation.isCurrentUser) stringResource(R.string.money_you_name, allocation.displayName) else allocation.displayName, Modifier.weight(1f))
                    Text(formatMoney(allocation.amountMinor, expense.currency))
                }
            }
            if (expense.allocations.any { it.roundingAdjustmentMinor != 0L }) {
                Text(stringResource(R.string.money_rounding_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expense.canApprove) Button(onClick = onApprove, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.money_approve))
            }
            if (expense.canReverse) TextButton(onClick = { reverseOpen = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.money_reverse))
            }
        }
    }
    if (reverseOpen) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reverseOpen = false },
            title = { Text(stringResource(R.string.money_reverse_title)) },
            text = { OutlinedTextField(reason, { reason = it.take(300) }, label = { Text(stringResource(R.string.money_reverse_reason)) }, minLines = 2) },
            confirmButton = { Button(onClick = { onReverse(reason.trim()) }, enabled = reason.trim().length >= 3) { Text(stringResource(R.string.money_confirm_reverse)) } },
            dismissButton = { TextButton(onClick = { reverseOpen = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun parseMoney(value: String, currency: String): Long? = runCatching {
    val exponent = Currency.getInstance(currency).defaultFractionDigits.coerceIn(0, 3)
    BigDecimal(value.replace(',', '.')).setScale(exponent, RoundingMode.UNNECESSARY).movePointRight(exponent).longValueExact()
}.getOrNull()

private fun formatMoney(minor: Long, currency: String): String = runCatching {
    val unit = Currency.getInstance(currency)
    NumberFormat.getCurrencyInstance().apply { this.currency = unit }.format(BigDecimal.valueOf(minor, unit.defaultFractionDigits))
}.getOrElse { "$minor $currency" }

private val ExpenseCategory.labelResource: Int @StringRes get() = when (this) {
    ExpenseCategory.RENT -> R.string.money_category_rent
    ExpenseCategory.ELECTRICITY -> R.string.money_category_electricity
    ExpenseCategory.GAS -> R.string.money_category_gas
    ExpenseCategory.WATER -> R.string.money_category_water
    ExpenseCategory.INTERNET -> R.string.money_category_internet
    ExpenseCategory.COUNCIL_TAX -> R.string.money_category_council_tax
    ExpenseCategory.GROCERIES -> R.string.money_category_groceries
    ExpenseCategory.HOUSEHOLD_SUPPLIES -> R.string.money_category_household_supplies
    ExpenseCategory.MAINTENANCE -> R.string.money_category_maintenance
    ExpenseCategory.OTHER -> R.string.money_category_other
}

private val ExpenseStatus.labelResource: Int @StringRes get() = when (this) {
    ExpenseStatus.PROPOSED -> R.string.money_status_proposed
    ExpenseStatus.APPROVED -> R.string.money_status_approved
    ExpenseStatus.REVERSED -> R.string.money_status_reversed
}

@Composable private fun ExpenseStatus.color() = when (this) {
    ExpenseStatus.PROPOSED -> MaterialTheme.colorScheme.tertiary
    ExpenseStatus.APPROVED -> MaterialTheme.colorScheme.primary
    ExpenseStatus.REVERSED -> MaterialTheme.colorScheme.outline
}

private val MoneyFilter.labelResource: Int @StringRes get() = when (this) {
    MoneyFilter.ACTIVE -> R.string.money_filter_active
    MoneyFilter.PROPOSED -> R.string.money_filter_proposed
    MoneyFilter.REVERSED -> R.string.money_filter_reversed
    MoneyFilter.ALL -> R.string.money_filter_all
}
private val MoneyFilter.emptyResource: Int @StringRes get() = when (this) {
    MoneyFilter.ACTIVE -> R.string.money_empty_active
    MoneyFilter.PROPOSED -> R.string.money_empty_proposed
    MoneyFilter.REVERSED -> R.string.money_empty_reversed
    MoneyFilter.ALL -> R.string.money_empty_all
}
private val MoneyProblem.messageResource: Int @StringRes get() = when (this) {
    MoneyProblem.LOAD_FAILED -> R.string.money_load_error
    MoneyProblem.CREATE_FAILED -> R.string.money_create_error
    MoneyProblem.APPROVE_FAILED -> R.string.money_approve_error
    MoneyProblem.REVERSE_FAILED -> R.string.money_reverse_error
    MoneyProblem.VERSION_CONFLICT -> R.string.money_version_conflict
}
