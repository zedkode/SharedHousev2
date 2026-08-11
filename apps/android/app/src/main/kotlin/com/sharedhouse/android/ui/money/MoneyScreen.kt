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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    var templatePrefillId by rememberSaveable { mutableStateOf<String?>(null) }
    var templateAdminOpen by rememberSaveable { mutableStateOf(false) }
    var templateEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
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
    val templatePrefill = templatePrefillId?.let { id -> state.templates.firstOrNull { it.id == id } }
    val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
    val householdTotal = approved.sumOf { it.amountMinor }
    val personalTotal = approved.sumOf { expense ->
        expense.allocations
            .filter { it.isCurrentUser && it.status != ExpenseAllocationStatus.PAID }
            .sumOf(ExpenseAllocationUi::amountMinor)
    }

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
                actions = {
                    if (state.canManageTemplates) {
                        IconButton(onClick = { templateAdminOpen = true }) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.money_manage_costs))
                        }
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
                if (state.templates.any { it.active }) {
                    item {
                        TemplateOverview(
                            templates = state.templates.filter { it.active },
                            canManage = state.canManageTemplates,
                            onUse = { template ->
                                templatePrefillId = template.id
                                createOpen = true
                            },
                            onManage = { templateAdminOpen = true },
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
            initialTemplate = templatePrefill,
            busy = state.isMutationInProgress,
            onDismiss = { createOpen = false; templatePrefillId = null },
            onSubmit = {
                onAction(MoneyAction.Create(it))
                createOpen = false
                templatePrefillId = null
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
            onDeclarePayment = { draft ->
                onAction(MoneyAction.DeclarePayment(selected.id, draft))
            },
            onConfirmPayment = { payment ->
                onAction(MoneyAction.ConfirmPayment(selected.id, payment.id, payment.version))
            },
            onDisputePayment = { payment, reason ->
                onAction(
                    MoneyAction.DisputePayment(
                        selected.id,
                        payment.id,
                        payment.version,
                        reason,
                    ),
                )
            },
            onReversePayment = { payment, reason ->
                onAction(
                    MoneyAction.ReversePayment(
                        selected.id,
                        payment.id,
                        payment.version,
                        reason,
                    ),
                )
            },
        )
    }
    if (templateAdminOpen) {
        ExpenseTemplateAdminSheet(
            templates = state.templates,
            busy = state.isMutationInProgress,
            onDismiss = { templateAdminOpen = false },
            onAdd = { templateAdminOpen = false; templateEditorId = NEW_TEMPLATE_ID },
            onEdit = { templateAdminOpen = false; templateEditorId = it.id },
            onUse = {
                templateAdminOpen = false
                templatePrefillId = it.id
                createOpen = true
            },
            onArchive = { templateAdminOpen = false; archiveTemplateId = it.id },
        )
    }
    if (templateEditorId != null) {
        val editing = state.templates.firstOrNull { it.id == templateEditorId }
        ExpenseTemplateEditor(
            currency = state.currency,
            initial = editing,
            busy = state.isMutationInProgress,
            onDismiss = { templateEditorId = null },
            onSubmit = { draft ->
                if (editing == null) onAction(MoneyAction.CreateTemplate(draft))
                else onAction(MoneyAction.UpdateTemplate(editing.id, editing.version, draft))
                templateEditorId = null
            },
        )
    }
    val archiveTemplate = state.templates.firstOrNull { it.id == archiveTemplateId }
    if (archiveTemplate != null) {
        ArchiveTemplateDialog(
            template = archiveTemplate,
            onDismiss = { archiveTemplateId = null },
            onConfirm = { reason ->
                onAction(MoneyAction.ArchiveTemplate(archiveTemplate.id, archiveTemplate.version, reason))
                archiveTemplateId = null
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
                    Text(expense.category.displayName(expense.customCategoryName), style = MaterialTheme.typography.bodySmall)
                    if (expense.sourceTemplateId != null) {
                        Text(
                            stringResource(R.string.money_generated_cost),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
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
private fun TemplateOverview(
    templates: List<ExpenseTemplateUi>,
    canManage: Boolean,
    onUse: (ExpenseTemplateUi) -> Unit,
    onManage: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.money_planned_costs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.money_planned_costs_description), style = MaterialTheme.typography.bodySmall)
                }
                if (canManage) TextButton(onClick = onManage) { Text(stringResource(R.string.money_manage)) }
            }
            templates.take(3).forEach { template ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(template.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(
                                R.string.money_template_schedule,
                                stringResource(template.cadence.labelResource),
                                template.nextDueDate.toString(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(formatMoney(template.amountMinor, template.currency))
                    TextButton(onClick = { onUse(template) }) { Text(stringResource(R.string.money_use_template)) }
                }
            }
            if (templates.size > 3) Text(stringResource(R.string.money_more_templates, templates.size - 3), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTemplateAdminSheet(
    templates: List<ExpenseTemplateUi>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ExpenseTemplateUi) -> Unit,
    onUse: (ExpenseTemplateUi) -> Unit,
    onArchive: (ExpenseTemplateUi) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.money_admin_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.money_admin_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAdd, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.money_add_household_cost))
                }
            }
            if (templates.isEmpty()) {
                item { Text(stringResource(R.string.money_no_templates), modifier = Modifier.padding(vertical = 24.dp)) }
            } else {
                items(templates, key = ExpenseTemplateUi::id) { template ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(template.category.displayName(template.customCategoryName), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatMoney(template.amountMinor, template.currency), style = MaterialTheme.typography.titleMedium)
                            }
                            Text(stringResource(R.string.money_template_schedule, stringResource(template.cadence.labelResource), template.nextDueDate.toString()))
                            Text(
                                stringResource(if (template.active) R.string.money_template_active else R.string.money_template_archived),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (template.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            )
                            if (template.active) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onUse(template) }, enabled = !busy) { Text(stringResource(R.string.money_use_template)) }
                                TextButton(onClick = { onEdit(template) }, enabled = !busy) { Text(stringResource(R.string.money_edit_template)) }
                                TextButton(onClick = { onArchive(template) }, enabled = !busy) { Text(stringResource(R.string.money_archive_template)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTemplateEditor(
    currency: String,
    initial: ExpenseTemplateUi?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ExpenseTemplateDraft) -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var amount by rememberSaveable(initial?.id) { mutableStateOf(initial?.let { editableMoney(it.amountMinor, currency) }.orEmpty()) }
    var categoryName by rememberSaveable(initial?.id) { mutableStateOf((initial?.category ?: ExpenseCategory.RENT).name) }
    var customName by rememberSaveable(initial?.id) { mutableStateOf(initial?.customCategoryName.orEmpty()) }
    var cadenceName by rememberSaveable(initial?.id) { mutableStateOf((initial?.cadence ?: ExpenseTemplateCadence.MONTHLY).name) }
    var nextDue by rememberSaveable(initial?.id) { mutableStateOf((initial?.nextDueDate ?: LocalDate.now()).toString()) }
    var notes by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var categoriesOpen by remember { mutableStateOf(false) }
    var cadenceOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val category = ExpenseCategory.valueOf(categoryName)
    val cadence = ExpenseTemplateCadence.valueOf(cadenceName)
    val parsedAmount = parseMoney(amount, currency)
    val valid = title.trim().isNotEmpty() && parsedAmount != null && parsedAmount > 0 &&
        (category != ExpenseCategory.CUSTOM || customName.trim().isNotEmpty())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(if (initial == null) R.string.money_add_household_cost else R.string.money_edit_household_cost), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Text(stringResource(R.string.money_template_editor_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.money_expense_title)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                OutlinedTextField(
                    value = category.displayName(customName.ifEmpty { null }),
                    onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.money_category)) },
                    trailingIcon = { IconButton(onClick = { categoriesOpen = true }) { Icon(Icons.Outlined.MoreVert, null) } },
                )
                DropdownMenu(categoriesOpen, { categoriesOpen = false }) {
                    ExpenseCategory.entries.forEach { item ->
                        DropdownMenuItem(text = { Text(stringResource(item.labelResource)) }, onClick = {
                            categoryName = item.name
                            if (item != ExpenseCategory.CUSTOM) customName = ""
                            categoriesOpen = false
                        })
                    }
                }
            }
            if (category == ExpenseCategory.CUSTOM) item {
                OutlinedTextField(customName, { customName = it.take(60) }, label = { Text(stringResource(R.string.money_custom_category_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = attempted && customName.trim().isEmpty())
            }
            item {
                OutlinedTextField(
                    amount, { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(16) },
                    label = { Text(stringResource(R.string.money_amount, currency)) }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    isError = attempted && parsedAmount == null,
                )
            }
            item {
                OutlinedTextField(
                    value = stringResource(cadence.labelResource), onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.money_cadence)) }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { cadenceOpen = true }) { Icon(Icons.Outlined.MoreVert, null) } },
                )
                DropdownMenu(cadenceOpen, { cadenceOpen = false }) {
                    ExpenseTemplateCadence.entries.forEach { item ->
                        DropdownMenuItem(text = { Text(stringResource(item.labelResource)) }, onClick = { cadenceName = item.name; cadenceOpen = false })
                    }
                }
            }
            item {
                OutlinedTextField(nextDue, {}, readOnly = true, label = { Text(stringResource(R.string.money_next_due_date)) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { dateOpen = true }) { Icon(Icons.Outlined.CalendarMonth, null) } })
            }
            item { OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text(stringResource(R.string.money_notes_optional)) }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                Button(
                    onClick = {
                        attempted = true
                        if (valid) onSubmit(ExpenseTemplateDraft(title.trim(), category, customName.trim().ifEmpty { null }, requireNotNull(parsedAmount), cadence, LocalDate.parse(nextDue), notes.trim().ifEmpty { null }))
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.money_save_household_cost)) }
            }
        }
    }
    if (dateOpen) {
        val picker = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(nextDue).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { nextDue = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; dateOpen = false }) { Text(stringResource(R.string.action_confirm)) } },
            dismissButton = { TextButton(onClick = { dateOpen = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(picker) }
    }
}

@Composable
private fun ArchiveTemplateDialog(template: ExpenseTemplateUi, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_archive_title, template.title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.money_archive_explanation))
                OutlinedTextField(reason, { reason = it.take(500) }, label = { Text(stringResource(R.string.money_archive_reason)) }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(reason.trim()) }, enabled = reason.trim().length >= 3) { Text(stringResource(R.string.money_archive_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
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
private fun ExpenseEditor(
    currency: String,
    initialTemplate: ExpenseTemplateUi? = null,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ExpenseDraft) -> Unit,
) {
    var title by rememberSaveable(initialTemplate?.id) { mutableStateOf(initialTemplate?.title.orEmpty()) }
    var amount by rememberSaveable(initialTemplate?.id) { mutableStateOf(initialTemplate?.let { editableMoney(it.amountMinor, currency) }.orEmpty()) }
    var notes by rememberSaveable(initialTemplate?.id) { mutableStateOf(initialTemplate?.notes.orEmpty()) }
    var categoryName by rememberSaveable(initialTemplate?.id) { mutableStateOf((initialTemplate?.category ?: ExpenseCategory.GROCERIES).name) }
    var customCategoryName by rememberSaveable(initialTemplate?.id) { mutableStateOf(initialTemplate?.customCategoryName.orEmpty()) }
    var dueDate by rememberSaveable(initialTemplate?.id) { mutableStateOf((initialTemplate?.nextDueDate ?: LocalDate.now()).toString()) }
    var categoriesOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val category = ExpenseCategory.valueOf(categoryName)
    val parsedAmount = parseMoney(amount, currency)
    val valid = title.trim().isNotEmpty() && parsedAmount != null && parsedAmount > 0 &&
        (category != ExpenseCategory.CUSTOM || customCategoryName.trim().isNotEmpty())

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
                                onClick = { categoryName = item.name; if (item != ExpenseCategory.CUSTOM) customCategoryName = ""; categoriesOpen = false },
                            )
                        }
                    }
                }
                if (category == ExpenseCategory.CUSTOM) {
                    OutlinedTextField(
                        customCategoryName,
                        { customCategoryName = it.take(60) },
                        label = { Text(stringResource(R.string.money_custom_category_name)) },
                        singleLine = true,
                        isError = attempted && customCategoryName.trim().isEmpty(),
                    )
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
                    if (valid) onSubmit(ExpenseDraft(
                        title = title.trim(),
                        category = category,
                        customCategoryName = customCategoryName.trim().ifEmpty { null },
                        amountMinor = requireNotNull(parsedAmount),
                        dueDate = LocalDate.parse(dueDate),
                        notes = notes.trim().ifEmpty { null },
                    ))
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
private fun ExpenseDetails(
    expense: ExpenseUi,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReverse: (String) -> Unit,
    onDeclarePayment: (ExpensePaymentDraft) -> Unit,
    onConfirmPayment: (ExpensePaymentUi) -> Unit,
    onDisputePayment: (ExpensePaymentUi, String) -> Unit,
    onReversePayment: (ExpensePaymentUi, String) -> Unit,
) {
    var reverseOpen by rememberSaveable { mutableStateOf(false) }
    var declarationOpen by rememberSaveable { mutableStateOf(false) }
    var confirmPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var disputePaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var reversePaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    val payments = expense.allocations.flatMap(ExpenseAllocationUi::paymentDeclarations)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    expense.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item { Text(stringResource(expense.status.labelResource), color = expense.status.color()) }
            item {
                Text(
                    formatMoney(expense.amountMinor, expense.currency),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item { Text(stringResource(R.string.money_due_value, expense.dueDate.toString())) }
            if (expense.sourceTemplateId != null) {
                item {
                    Text(
                        stringResource(
                            R.string.money_generated_cost_details,
                            (expense.occurrenceDate ?: expense.dueDate).toString(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            expense.notes?.let { note ->
                item { Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    stringResource(R.string.money_split_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(expense.allocations, key = ExpenseAllocationUi::membershipId) { allocation ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (allocation.isCurrentUser) {
                                stringResource(R.string.money_you_name, allocation.displayName)
                            } else {
                                allocation.displayName
                            },
                            Modifier.weight(1f),
                        )
                        Text(formatMoney(allocation.amountMinor, expense.currency))
                    }
                    Text(
                        stringResource(allocation.status.labelResource),
                        style = MaterialTheme.typography.labelMedium,
                        color = allocation.status.color(),
                    )
                    if (allocation.canDeclarePayment) {
                        Button(
                            onClick = { declarationOpen = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.money_declare_payment))
                        }
                    }
                }
            }
            if (expense.allocations.any { it.roundingAdjustmentMinor != 0L }) {
                item {
                    Text(
                        stringResource(R.string.money_rounding_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (payments.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text(
                        stringResource(R.string.money_payment_history),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(payments, key = ExpensePaymentUi::id) { payment ->
                    PaymentHistoryCard(
                        payment = payment,
                        busy = busy,
                        onConfirm = { confirmPaymentId = payment.id },
                        onDispute = { disputePaymentId = payment.id },
                        onReverse = { reversePaymentId = payment.id },
                    )
                }
            }
            if (expense.canApprove) {
                item {
                    Button(
                        onClick = onApprove,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.money_approve)) }
                }
            }
            if (expense.canReverse) {
                item {
                    TextButton(
                        onClick = { reverseOpen = true },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.money_reverse)) }
                }
            }
        }
    }
    if (declarationOpen) {
        PaymentDeclarationDialog(
            amountMinor = expense.allocations.firstOrNull { it.isCurrentUser }?.amountMinor ?: 0,
            currency = expense.currency,
            onDismiss = { declarationOpen = false },
            onConfirm = {
                onDeclarePayment(it)
                declarationOpen = false
            },
        )
    }
    val confirmation = payments.firstOrNull { it.id == confirmPaymentId }
    if (confirmation != null) {
        AlertDialog(
            onDismissRequest = { confirmPaymentId = null },
            title = { Text(stringResource(R.string.money_confirm_payment_title)) },
            text = { Text(stringResource(R.string.money_confirm_payment_explanation)) },
            confirmButton = {
                Button(onClick = {
                    onConfirmPayment(confirmation)
                    confirmPaymentId = null
                }) { Text(stringResource(R.string.money_confirm_payment)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmPaymentId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    val dispute = payments.firstOrNull { it.id == disputePaymentId }
    if (dispute != null) {
        PaymentReasonDialog(
            title = stringResource(R.string.money_dispute_payment_title),
            explanation = stringResource(R.string.money_dispute_payment_explanation),
            actionLabel = stringResource(R.string.money_dispute_payment),
            onDismiss = { disputePaymentId = null },
            onConfirm = {
                onDisputePayment(dispute, it)
                disputePaymentId = null
            },
        )
    }
    val paymentToReverse = payments.firstOrNull { it.id == reversePaymentId }
    if (paymentToReverse != null) {
        PaymentReasonDialog(
            title = stringResource(R.string.money_reverse_payment_title),
            explanation = stringResource(R.string.money_reverse_payment_explanation),
            actionLabel = stringResource(R.string.money_reverse_payment),
            onDismiss = { reversePaymentId = null },
            onConfirm = {
                onReversePayment(paymentToReverse, it)
                reversePaymentId = null
            },
        )
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

@Composable
private fun PaymentHistoryCard(
    payment: ExpensePaymentUi,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDispute: () -> Unit,
    onReverse: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(payment.payerDisplayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(formatMoney(payment.amountMinor, payment.currency), fontWeight = FontWeight.SemiBold)
            }
            Text(
                stringResource(payment.status.labelResource),
                style = MaterialTheme.typography.labelMedium,
                color = payment.status.color(),
            )
            Text(
                stringResource(
                    R.string.money_payment_method_date,
                    stringResource(payment.method.labelResource),
                    formatPaymentTime(payment.paidAt),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            payment.reference?.let {
                Text(stringResource(R.string.money_payment_reference_value, it))
            }
            payment.note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            payment.disputeReason?.let {
                Text(stringResource(R.string.money_payment_dispute_reason_value, it))
            }
            payment.reversalReason?.let {
                Text(stringResource(R.string.money_payment_reversal_reason_value, it))
            }
            if (payment.canConfirm || payment.canDispute || payment.canReverse) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (payment.canConfirm) {
                        Button(onClick = onConfirm, enabled = !busy) {
                            Text(stringResource(R.string.money_confirm_payment))
                        }
                    }
                    if (payment.canDispute) {
                        TextButton(onClick = onDispute, enabled = !busy) {
                            Text(stringResource(R.string.money_dispute_payment))
                        }
                    }
                    if (payment.canReverse) {
                        TextButton(onClick = onReverse, enabled = !busy) {
                            Text(stringResource(R.string.money_reverse_payment))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentDeclarationDialog(
    amountMinor: Long,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (ExpensePaymentDraft) -> Unit,
) {
    var methodName by rememberSaveable { mutableStateOf(ExpensePaymentMethod.BANK_TRANSFER.name) }
    var reference by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val method = ExpensePaymentMethod.valueOf(methodName)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_declare_payment_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(
                        R.string.money_declare_payment_explanation,
                        formatMoney(amountMinor, currency),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ExpensePaymentMethod.entries.forEach { candidate ->
                        FilterChip(
                            selected = candidate == method,
                            onClick = { methodName = candidate.name },
                            label = { Text(stringResource(candidate.labelResource)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it.take(120) },
                    label = { Text(stringResource(R.string.money_payment_reference_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text(stringResource(R.string.money_payment_note_optional)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.money_payment_declaration_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    ExpensePaymentDraft(
                        method = method,
                        paidAt = Instant.now(),
                        reference = reference.trim().ifEmpty { null },
                        note = note.trim().ifEmpty { null },
                    ),
                )
            }) { Text(stringResource(R.string.money_declare_payment)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PaymentReasonDialog(
    title: String,
    explanation: String,
    actionLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(explanation)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(500) },
                    label = { Text(stringResource(R.string.money_payment_action_reason)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.trim()) },
                enabled = reason.trim().length >= 3,
            ) { Text(actionLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun parseMoney(value: String, currency: String): Long? = runCatching {
    val exponent = Currency.getInstance(currency).defaultFractionDigits.coerceIn(0, 3)
    BigDecimal(value.replace(',', '.')).setScale(exponent, RoundingMode.UNNECESSARY).movePointRight(exponent).longValueExact()
}.getOrNull()

private fun formatMoney(minor: Long, currency: String): String = runCatching {
    val unit = Currency.getInstance(currency)
    NumberFormat.getCurrencyInstance().apply { this.currency = unit }.format(BigDecimal.valueOf(minor, unit.defaultFractionDigits))
}.getOrElse { "$minor $currency" }

private fun editableMoney(minor: Long, currency: String): String = runCatching {
    val exponent = Currency.getInstance(currency).defaultFractionDigits.coerceIn(0, 3)
    BigDecimal.valueOf(minor, exponent).toPlainString()
}.getOrElse { minor.toString() }

private fun formatPaymentTime(value: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(value)

@Composable
private fun ExpenseCategory.displayName(customName: String?): String =
    if (this == ExpenseCategory.CUSTOM && !customName.isNullOrBlank()) customName
    else stringResource(labelResource)

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
    ExpenseCategory.CUSTOM -> R.string.money_category_custom
}

private val ExpenseTemplateCadence.labelResource: Int @StringRes get() = when (this) {
    ExpenseTemplateCadence.WEEKLY -> R.string.money_cadence_weekly
    ExpenseTemplateCadence.MONTHLY -> R.string.money_cadence_monthly
    ExpenseTemplateCadence.QUARTERLY -> R.string.money_cadence_quarterly
    ExpenseTemplateCadence.YEARLY -> R.string.money_cadence_yearly
}

private val ExpenseStatus.labelResource: Int @StringRes get() = when (this) {
    ExpenseStatus.PROPOSED -> R.string.money_status_proposed
    ExpenseStatus.APPROVED -> R.string.money_status_approved
    ExpenseStatus.REVERSED -> R.string.money_status_reversed
}

private val ExpenseAllocationStatus.labelResource: Int @StringRes get() = when (this) {
    ExpenseAllocationStatus.OUTSTANDING -> R.string.money_allocation_outstanding
    ExpenseAllocationStatus.DECLARED -> R.string.money_allocation_declared
    ExpenseAllocationStatus.PAID -> R.string.money_allocation_paid
    ExpenseAllocationStatus.DISPUTED -> R.string.money_allocation_disputed
}

private val ExpensePaymentStatus.labelResource: Int @StringRes get() = when (this) {
    ExpensePaymentStatus.DECLARED -> R.string.money_payment_status_declared
    ExpensePaymentStatus.CONFIRMED -> R.string.money_payment_status_confirmed
    ExpensePaymentStatus.DISPUTED -> R.string.money_payment_status_disputed
    ExpensePaymentStatus.REVERSED -> R.string.money_payment_status_reversed
}

private val ExpensePaymentMethod.labelResource: Int @StringRes get() = when (this) {
    ExpensePaymentMethod.BANK_TRANSFER -> R.string.money_payment_method_bank_transfer
    ExpensePaymentMethod.CASH -> R.string.money_payment_method_cash
    ExpensePaymentMethod.CARD -> R.string.money_payment_method_card
    ExpensePaymentMethod.DIRECT_DEBIT -> R.string.money_payment_method_direct_debit
    ExpensePaymentMethod.OTHER -> R.string.money_payment_method_other
}

@Composable private fun ExpenseStatus.color() = when (this) {
    ExpenseStatus.PROPOSED -> MaterialTheme.colorScheme.tertiary
    ExpenseStatus.APPROVED -> MaterialTheme.colorScheme.primary
    ExpenseStatus.REVERSED -> MaterialTheme.colorScheme.outline
}

@Composable private fun ExpenseAllocationStatus.color() = when (this) {
    ExpenseAllocationStatus.OUTSTANDING -> MaterialTheme.colorScheme.onSurfaceVariant
    ExpenseAllocationStatus.DECLARED -> MaterialTheme.colorScheme.tertiary
    ExpenseAllocationStatus.PAID -> MaterialTheme.colorScheme.primary
    ExpenseAllocationStatus.DISPUTED -> MaterialTheme.colorScheme.error
}

@Composable private fun ExpensePaymentStatus.color() = when (this) {
    ExpensePaymentStatus.DECLARED -> MaterialTheme.colorScheme.tertiary
    ExpensePaymentStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
    ExpensePaymentStatus.DISPUTED -> MaterialTheme.colorScheme.error
    ExpensePaymentStatus.REVERSED -> MaterialTheme.colorScheme.outline
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
    MoneyProblem.TEMPLATE_FAILED -> R.string.money_template_error
    MoneyProblem.PAYMENT_DECLARE_FAILED -> R.string.money_payment_declare_error
    MoneyProblem.PAYMENT_CONFIRM_FAILED -> R.string.money_payment_confirm_error
    MoneyProblem.PAYMENT_DISPUTE_FAILED -> R.string.money_payment_dispute_error
    MoneyProblem.PAYMENT_REVERSE_FAILED -> R.string.money_payment_reverse_error
    MoneyProblem.VERSION_CONFLICT -> R.string.money_version_conflict
}

private const val NEW_TEMPLATE_ID = "new"
