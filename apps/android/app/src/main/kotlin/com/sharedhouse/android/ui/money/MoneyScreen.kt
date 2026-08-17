package com.sharedhouse.android.ui.money

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import com.sharedhouse.android.ui.atmosphere.AlertDialog
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.ButtonDefaults
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.atmosphere.DatePicker
import com.sharedhouse.android.ui.atmosphere.DatePickerDialog
import com.sharedhouse.android.ui.atmosphere.DepthIconBadge
import com.sharedhouse.android.ui.atmosphere.DropdownMenu
import com.sharedhouse.android.ui.atmosphere.DropdownMenuItem
import com.sharedhouse.android.ui.atmosphere.FilterChip
import com.sharedhouse.android.ui.atmosphere.HorizontalDivider
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.ModalBottomSheet
import com.sharedhouse.android.ui.atmosphere.OutlinedButton
import com.sharedhouse.android.ui.atmosphere.OutlinedTextField
import com.sharedhouse.android.ui.atmosphere.PremiumHeroCard
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.SnackbarHost
import com.sharedhouse.android.ui.atmosphere.SnackbarHostState
import com.sharedhouse.android.ui.atmosphere.Surface
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TextButton
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.icons.SharedHouseIcons
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
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var templatePrefillId by rememberSaveable { mutableStateOf<String?>(null) }
    var templateAdminOpen by rememberSaveable { mutableStateOf(false) }
    var templateEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var billingRosterOpen by rememberSaveable { mutableStateOf(false) }
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
    val editing = editId?.let { id -> expenses.firstOrNull { it.id == id } }
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
                        Text(
                            stringResource(R.string.money_title),
                            style = AtmosphereTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.money_subtitle),
                            style = AtmosphereTheme.typography.labelMedium,
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (state.canCreate && state.content is MoneyContent.Ready) {
                        IconButton(onClick = { createOpen = true }) {
                            Icon(SharedHouseIcons.Add, stringResource(R.string.money_add_expense))
                        }
                    }
                    if (state.canManageTemplates) {
                        IconButton(onClick = { templateAdminOpen = true }) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.money_manage_costs))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard(
                            icon = SharedHouseIcons.Money,
                            label = stringResource(R.string.money_your_outstanding),
                            value = formatMoney(personalTotal, state.currency),
                            modifier = Modifier.fillMaxWidth(),
                            prominent = true,
                        )
                        SummaryCard(
                            icon = SharedHouseIcons.People,
                            label = stringResource(R.string.money_household_total),
                            value = formatMoney(householdTotal, state.currency),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                val activeTemplates = state.templates.filter { it.active }
                if (state.billingRoster != null || activeTemplates.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            state.billingRoster?.let { roster ->
                                BillingRosterOverview(
                                    roster = roster,
                                    onManage = { billingRosterOpen = true },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (activeTemplates.isNotEmpty()) {
                                TemplateOverview(
                                    templates = activeTemplates,
                                    canManage = state.canManageTemplates,
                                    onUse = { template ->
                                        templatePrefillId = template.id
                                        createOpen = true
                                    },
                                    onManage = { templateAdminOpen = true },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
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
            roster = state.billingRoster,
            initialTemplate = templatePrefill,
            busy = state.isMutationInProgress,
            onDismiss = { createOpen = false; templatePrefillId = null },
            onSubmit = { draft, _ ->
                onAction(MoneyAction.Create(draft))
                createOpen = false
                templatePrefillId = null
            },
        )
    }
    if (editing != null) {
        ExpenseEditor(
            currency = state.currency,
            roster = state.billingRoster,
            initialExpense = editing,
            busy = state.isMutationInProgress,
            onDismiss = { editId = null },
            onSubmit = { draft, reason ->
                onAction(MoneyAction.Revise(editing.id, editing.version, draft, requireNotNull(reason)))
                editId = null
            },
        )
    }
    if (selected != null) {
        ExpenseDetails(
            expense = selected,
            busy = state.isMutationInProgress,
            onDismiss = { selectedId = null },
            onApprove = { onAction(MoneyAction.Approve(selected.id, selected.version)) },
            onEdit = { editId = selected.id; selectedId = null },
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
            roster = state.billingRoster,
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
            onConfigureRoster = {
                templateAdminOpen = false
                billingRosterOpen = true
            },
        )
    }
    if (templateEditorId != null) {
        val editing = state.templates.firstOrNull { it.id == templateEditorId }
        ExpenseTemplateEditor(
            currency = state.currency,
            roster = state.billingRoster,
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
    val roster = state.billingRoster
    if (billingRosterOpen && roster != null) {
        BillingRosterSheet(
            roster = roster,
            busy = state.isMutationInProgress,
            onDismiss = { billingRosterOpen = false },
            onSave = { couples ->
                onAction(MoneyAction.UpdateBillingRoster(roster.version, couples))
                billingRosterOpen = false
            },
        )
    }
}

@Composable
private fun BillingRosterOverview(
    roster: BillingRosterUi,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onManage,
        enabled = roster.canManage,
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = AtmosphereTheme.shapes.small,
                color = AtmosphereTheme.colorScheme.primary.copy(alpha = .16f),
                contentColor = AtmosphereTheme.colorScheme.primary,
            ) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(SharedHouseIcons.People, null, Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(R.string.money_split_household_title),
                    style = AtmosphereTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.money_split_household_summary,
                        pluralStringResource(
                            R.plurals.money_residents_count,
                            roster.residentCount,
                            roster.residentCount,
                        ),
                        pluralStringResource(
                            R.plurals.money_payment_units_count,
                            roster.billingUnitCount,
                            roster.billingUnitCount,
                        ),
                    ),
                    style = AtmosphereTheme.typography.labelSmall,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (roster.canManage) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.money_configure),
                    modifier = Modifier.size(18.dp),
                    tint = AtmosphereTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, prominent: Boolean = false) {
    if (prominent) {
        PremiumHeroCard(modifier = modifier) {
            SummaryCardContent(icon = icon, label = label, value = value, hero = true)
        }
    } else {
        Card(
            modifier,
            shape = AtmosphereTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            SummaryCardContent(icon = icon, label = label, value = value, hero = false)
        }
    }
}

@Composable
private fun SummaryCardContent(icon: ImageVector, label: String, value: String, hero: Boolean) {
    val contentColor = if (hero) Color.White else AtmosphereTheme.colorScheme.onSurface
    val valueStyle = when {
        !hero -> AtmosphereTheme.typography.titleMedium
        value.length <= 8 -> AtmosphereTheme.typography.displayMedium
        else -> AtmosphereTheme.typography.displaySmall
    }.copy(fontFeatureSettings = "tnum")
    Row(
        Modifier.padding(if (hero) 2.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DepthIconBadge(
            icon = icon,
            contentDescription = null,
            hero = hero,
            tint = if (hero) Color.White else AtmosphereTheme.colorScheme.secondary,
            badgeSize = if (hero) 46.dp else 36.dp,
            iconSize = if (hero) 24.dp else 19.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                style = AtmosphereTheme.typography.labelSmall,
                color = if (hero) Color.White.copy(alpha = .75f) else AtmosphereTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ExpenseCard(expense: ExpenseUi, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DepthIconBadge(
                    icon = expense.category.icon(),
                    contentDescription = null,
                    tint = AtmosphereTheme.colorScheme.secondary,
                    badgeSize = 38.dp,
                    iconSize = 21.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(expense.title, style = AtmosphereTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(expense.category.displayName(expense.customCategoryName), style = AtmosphereTheme.typography.bodySmall, color = AtmosphereTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (expense.sourceTemplateId != null) {
                        Text(
                            stringResource(R.string.money_generated_cost),
                            style = AtmosphereTheme.typography.labelSmall,
                            color = AtmosphereTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    formatMoney(expense.amountMinor, expense.currency),
                    modifier = Modifier.widthIn(min = 76.dp, max = 112.dp),
                    style = AtmosphereTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(shape = AtmosphereTheme.shapes.small, color = AtmosphereTheme.colorScheme.surfaceVariant) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Icon(SharedHouseIcons.More, stringResource(R.string.money_section_details), Modifier.size(17.dp)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.money_due_value, expense.dueDate.toString()),
                    modifier = Modifier.weight(1f),
                    style = AtmosphereTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ExpenseStatusBadge(expense.status)
            }
            HorizontalDivider()
            Text(
                stringResource(R.string.money_your_share_value, formatMoney(expense.currentUserShareMinor, expense.currency)),
                style = AtmosphereTheme.typography.bodyMedium,
            )
        }
    }
}

private fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.RENT, ExpenseCategory.COUNCIL_TAX -> SharedHouseIcons.Rent
    ExpenseCategory.ELECTRICITY, ExpenseCategory.GAS, ExpenseCategory.WATER, ExpenseCategory.INTERNET -> SharedHouseIcons.Utilities
    ExpenseCategory.MAINTENANCE -> SharedHouseIcons.Maintenance
    ExpenseCategory.GROCERIES, ExpenseCategory.HOUSEHOLD_SUPPLIES -> SharedHouseIcons.Cleaning
    ExpenseCategory.OTHER, ExpenseCategory.CUSTOM -> SharedHouseIcons.Money
}

@Composable
private fun ExpenseStatusBadge(status: ExpenseStatus) {
    val color = status.color()
    Surface(
        shape = AtmosphereTheme.shapes.extraSmall,
        color = color.copy(alpha = .14f),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(status.icon(), null, Modifier.size(14.dp))
            Text(
                stringResource(status.labelResource),
                style = AtmosphereTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ExpenseStatus.icon(): ImageVector = when (this) {
    ExpenseStatus.PROPOSED -> SharedHouseIcons.Pending
    ExpenseStatus.APPROVED -> SharedHouseIcons.Approved
    ExpenseStatus.REVERSED -> SharedHouseIcons.Reversed
}

@Composable
private fun TemplateOverview(
    templates: List<ExpenseTemplateUi>,
    canManage: Boolean,
    onUse: (ExpenseTemplateUi) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = templates.minByOrNull(ExpenseTemplateUi::nextDueDate) ?: return
    Card(
        onClick = { if (canManage) onManage() else onUse(next) },
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = AtmosphereTheme.shapes.small,
                color = AtmosphereTheme.colorScheme.secondary.copy(alpha = .16f),
                contentColor = AtmosphereTheme.colorScheme.secondary,
            ) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(SharedHouseIcons.Rent, null, Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(R.string.money_planned_costs),
                    style = AtmosphereTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.money_template_schedule,
                        stringResource(next.cadence.labelResource),
                        next.nextDueDate.toString(),
                    ),
                    style = AtmosphereTheme.typography.labelSmall,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(if (canManage) R.string.money_manage else R.string.money_use_template),
                modifier = Modifier.size(18.dp),
                tint = AtmosphereTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ExpenseTemplateAdminSheet(
    templates: List<ExpenseTemplateUi>,
    roster: BillingRosterUi?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ExpenseTemplateUi) -> Unit,
    onUse: (ExpenseTemplateUi) -> Unit,
    onArchive: (ExpenseTemplateUi) -> Unit,
    onConfigureRoster: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.money_admin_title), style = AtmosphereTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.money_admin_description), color = AtmosphereTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAdd, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(SharedHouseIcons.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.money_add_household_cost))
                }
                if (roster?.canManage == true) {
                    TextButton(
                        onClick = onConfigureRoster,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(SharedHouseIcons.People, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.money_configure_people_couples))
                    }
                }
            }
            if (templates.isEmpty()) {
                item { Text(stringResource(R.string.money_no_templates), modifier = Modifier.padding(vertical = 24.dp)) }
            } else {
                items(templates, key = ExpenseTemplateUi::id) { template ->
                    Card(colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(template.title, style = AtmosphereTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(template.category.displayName(template.customCategoryName), style = AtmosphereTheme.typography.bodySmall)
                                }
                                Text(formatMoney(template.amountMinor, template.currency), style = AtmosphereTheme.typography.titleMedium)
                            }
                            Text(stringResource(R.string.money_template_schedule, stringResource(template.cadence.labelResource), template.nextDueDate.toString()))
                            Text(
                                stringResource(if (template.active) R.string.money_template_active else R.string.money_template_archived),
                                style = AtmosphereTheme.typography.labelLarge,
                                color = if (template.active) AtmosphereTheme.colorScheme.primary else AtmosphereTheme.colorScheme.outline,
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

@Composable
private fun BillingRosterSheet(
    roster: BillingRosterUi,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<BillingCoupleDraft>) -> Unit,
) {
    var drafts by remember(roster.version) {
        mutableStateOf(
            roster.couples.map {
                BillingCoupleDraft(
                    primaryMembershipId = it.primaryMembershipId,
                    partnerMembershipId = it.partnerMembershipId,
                    partnerDisplayName = if (it.partnerMembershipId == null) it.partnerDisplayName else null,
                )
            },
        )
    }
    var editorOpen by rememberSaveable(roster.version) { mutableStateOf(false) }
    val memberNames = roster.members.associate { it.membershipId to it.displayName }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.money_billing_roster_title),
                    style = AtmosphereTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.money_billing_roster_description),
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(drafts, key = { "${it.primaryMembershipId}:${it.partnerMembershipId}:${it.partnerDisplayName}" }) { draft ->
                Card(colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainer)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${memberNames[draft.primaryMembershipId].orEmpty()} & " +
                                    (draft.partnerMembershipId?.let(memberNames::get)
                                        ?: draft.partnerDisplayName.orEmpty()),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(
                                    if (draft.partnerMembershipId == null) {
                                        R.string.money_partner_without_account
                                    } else {
                                        R.string.money_partner_with_account
                                    },
                                ),
                                style = AtmosphereTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { drafts = drafts - draft }, enabled = !busy) {
                            Text(stringResource(R.string.money_remove_couple))
                        }
                    }
                }
            }
            item {
                TextButton(
                    onClick = { editorOpen = true },
                    enabled = !busy && availableMemberIds(roster, drafts).isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(SharedHouseIcons.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.money_add_couple))
                }
                Button(
                    onClick = { onSave(drafts) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.money_save_roster)) }
                Text(
                    stringResource(R.string.money_roster_history_notice),
                    style = AtmosphereTheme.typography.bodySmall,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (editorOpen) {
        CoupleEditorDialog(
            roster = roster,
            drafts = drafts,
            onDismiss = { editorOpen = false },
            onAdd = {
                drafts = drafts + it
                editorOpen = false
            },
        )
    }
}

@Composable
private fun CoupleEditorDialog(
    roster: BillingRosterUi,
    drafts: List<BillingCoupleDraft>,
    onDismiss: () -> Unit,
    onAdd: (BillingCoupleDraft) -> Unit,
) {
    val available = availableMemberIds(roster, drafts)
    var primaryId by remember { mutableStateOf(available.firstOrNull().orEmpty()) }
    var useExistingMember by rememberSaveable { mutableStateOf(false) }
    var partnerId by remember { mutableStateOf("") }
    var partnerName by rememberSaveable { mutableStateOf("") }
    val partnerCandidates = available.filterNot { it == primaryId }
    val valid = primaryId.isNotBlank() && if (useExistingMember) {
        partnerId in partnerCandidates
    } else {
        partnerName.trim().isNotEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_add_couple)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.money_choose_responsible_member))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    roster.members.filter { it.membershipId in available }.forEach { member ->
                        FilterChip(
                            selected = member.membershipId == primaryId,
                            onClick = {
                                primaryId = member.membershipId
                                if (partnerId == primaryId) partnerId = ""
                            },
                            label = { Text(member.displayName) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !useExistingMember,
                        onClick = { useExistingMember = false },
                        label = { Text(stringResource(R.string.money_partner_without_account)) },
                    )
                    FilterChip(
                        selected = useExistingMember,
                        onClick = { useExistingMember = true },
                        label = { Text(stringResource(R.string.money_partner_with_account)) },
                    )
                }
                if (useExistingMember) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        roster.members.filter { it.membershipId in partnerCandidates }.forEach { member ->
                            FilterChip(
                                selected = member.membershipId == partnerId,
                                onClick = { partnerId = member.membershipId },
                                label = { Text(member.displayName) },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                    }
                    if (partnerCandidates.isEmpty()) {
                        Text(stringResource(R.string.money_no_partner_member_available))
                    }
                } else {
                    OutlinedTextField(
                        value = partnerName,
                        onValueChange = { partnerName = it.take(80) },
                        label = { Text(stringResource(R.string.money_partner_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        BillingCoupleDraft(
                            primaryMembershipId = primaryId,
                            partnerMembershipId = partnerId.takeIf { useExistingMember },
                            partnerDisplayName = partnerName.trim().takeIf { !useExistingMember },
                        ),
                    )
                },
                enabled = valid,
            ) { Text(stringResource(R.string.money_add_couple)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun availableMemberIds(
    roster: BillingRosterUi,
    drafts: List<BillingCoupleDraft>,
): Set<String> {
    val used = drafts.flatMap { listOfNotNull(it.primaryMembershipId, it.partnerMembershipId) }.toSet()
    return roster.members.map(BillingRosterMemberUi::membershipId).filterNot(used::contains).toSet()
}

@Composable
private fun ExpenseTemplateEditor(
    currency: String,
    roster: BillingRosterUi?,
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
    var endsOn by rememberSaveable(initial?.id) { mutableStateOf(initial?.endsOn?.toString().orEmpty()) }
    var notes by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var categoriesOpen by remember { mutableStateOf(false) }
    var cadenceOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var endDateOpen by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val category = ExpenseCategory.valueOf(categoryName)
    val cadence = ExpenseTemplateCadence.valueOf(cadenceName)
    val parsedAmount = parseMoney(amount, currency)
    val valid = title.trim().isNotEmpty() && parsedAmount != null && parsedAmount > 0 &&
        (category != ExpenseCategory.CUSTOM || customName.trim().isNotEmpty()) &&
        (endsOn.isEmpty() || runCatching { LocalDate.parse(endsOn) >= LocalDate.parse(nextDue) }.getOrDefault(false))

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(if (initial == null) R.string.money_add_household_cost else R.string.money_edit_household_cost), style = AtmosphereTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Text(stringResource(R.string.money_template_editor_explanation), color = AtmosphereTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.money_expense_title)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                OutlinedTextField(
                    value = category.displayName(customName.ifEmpty { null }),
                    onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.money_category)) },
                    trailingIcon = { IconButton(onClick = { categoriesOpen = true }) { Icon(SharedHouseIcons.More, null) } },
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
            if (parsedAmount != null && parsedAmount > 0 && roster != null) {
                item { BillingSplitPreview(parsedAmount, currency, roster) }
            }
            item {
                OutlinedTextField(
                    value = stringResource(cadence.labelResource), onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.money_cadence)) }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { cadenceOpen = true }) { Icon(SharedHouseIcons.More, null) } },
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
            item {
                OutlinedTextField(
                    value = endsOn.ifEmpty { stringResource(R.string.money_no_end_date) },
                    onValueChange = {},
                    readOnly = true,
                    isError = attempted && !valid && endsOn.isNotEmpty(),
                    label = { Text(stringResource(R.string.money_schedule_ends_on)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            if (endsOn.isNotEmpty()) TextButton(onClick = { endsOn = "" }) {
                                Text(stringResource(R.string.action_clear))
                            }
                            IconButton(onClick = { endDateOpen = true }) {
                                Icon(Icons.Outlined.CalendarMonth, null)
                            }
                        }
                    },
                )
            }
            item { OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text(stringResource(R.string.money_notes_optional)) }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                Button(
                    onClick = {
                        attempted = true
                        if (valid) onSubmit(ExpenseTemplateDraft(title.trim(), category, customName.trim().ifEmpty { null }, requireNotNull(parsedAmount), cadence, LocalDate.parse(nextDue), endsOn.ifEmpty { null }?.let(LocalDate::parse), notes.trim().ifEmpty { null }))
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.money_save_household_cost)) }
            }
        }
    }
    if (dateOpen) {
        val picker = com.sharedhouse.android.ui.atmosphere.rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(nextDue).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { nextDue = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; dateOpen = false }) { Text(stringResource(R.string.action_confirm)) } },
            dismissButton = { TextButton(onClick = { dateOpen = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(picker) }
    }
    if (endDateOpen) {
        val initialEnd = endsOn.ifEmpty { nextDue }
        val picker = com.sharedhouse.android.ui.atmosphere.rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(initialEnd).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { endDateOpen = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { endsOn = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; endDateOpen = false }) { Text(stringResource(R.string.action_confirm)) } },
            dismissButton = { TextButton(onClick = { endDateOpen = false }) { Text(stringResource(R.string.action_cancel)) } },
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
    Card(colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DepthIconBadge(
                icon = SharedHouseIcons.Approved,
                contentDescription = null,
                badgeSize = 72.dp,
                iconSize = 34.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(filter.emptyResource), style = AtmosphereTheme.typography.titleMedium)
            Text(stringResource(R.string.money_empty_explanation), style = AtmosphereTheme.typography.bodyMedium)
            if (canCreate) TextButton(onClick = onAdd) { Text(stringResource(R.string.money_add_first)) }
        }
    }
}

@Composable
private fun MoneyError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.money_load_error), style = AtmosphereTheme.typography.titleMedium)
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun ExpenseEditor(
    currency: String,
    roster: BillingRosterUi?,
    initialTemplate: ExpenseTemplateUi? = null,
    initialExpense: ExpenseUi? = null,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ExpenseDraft, String?) -> Unit,
) {
    val identity = initialExpense?.id ?: initialTemplate?.id
    var title by rememberSaveable(identity) { mutableStateOf(initialExpense?.title ?: initialTemplate?.title.orEmpty()) }
    var supplierName by rememberSaveable(identity) { mutableStateOf(initialExpense?.supplierName.orEmpty()) }
    var amount by rememberSaveable(identity) {
        mutableStateOf(
            (initialExpense?.amountMinor ?: initialTemplate?.amountMinor)
                ?.let { editableMoney(it, currency) }.orEmpty(),
        )
    }
    var notes by rememberSaveable(identity) { mutableStateOf(initialExpense?.notes ?: initialTemplate?.notes.orEmpty()) }
    var categoryName by rememberSaveable(identity) {
        mutableStateOf((initialExpense?.category ?: initialTemplate?.category ?: ExpenseCategory.GROCERIES).name)
    }
    var customCategoryName by rememberSaveable(identity) {
        mutableStateOf(initialExpense?.customCategoryName ?: initialTemplate?.customCategoryName.orEmpty())
    }
    var dueDate by rememberSaveable(identity) {
        mutableStateOf((initialExpense?.dueDate ?: initialTemplate?.nextDueDate ?: LocalDate.now()).toString())
    }
    var revisionReason by rememberSaveable(identity) { mutableStateOf("") }
    var categoriesOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val category = ExpenseCategory.valueOf(categoryName)
    val parsedAmount = parseMoney(amount, currency)
    val valid = title.trim().isNotEmpty() && parsedAmount != null && parsedAmount > 0 &&
        (category != ExpenseCategory.CUSTOM || customCategoryName.trim().isNotEmpty()) &&
        (initialExpense == null || revisionReason.trim().length >= 3)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialExpense == null) R.string.money_new_expense else R.string.money_edit_expense)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.money_equal_split_explanation), style = AtmosphereTheme.typography.bodySmall)
                Text(stringResource(R.string.money_section_details), style = AtmosphereTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(title, { title = it.take(100) }, label = { Text(stringResource(R.string.money_expense_title)) }, singleLine = true)
                OutlinedTextField(
                    supplierName,
                    { supplierName = it.take(120) },
                    label = { Text(stringResource(R.string.money_supplier_optional)) },
                    supportingText = { Text(stringResource(R.string.money_supplier_help)) },
                    singleLine = true,
                )
                Column {
                    OutlinedTextField(
                        value = stringResource(category.labelResource),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.money_category)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { categoriesOpen = true }) { Icon(SharedHouseIcons.More, null) } },
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
                if (parsedAmount != null && parsedAmount > 0 && roster != null) {
                    BillingSplitPreview(parsedAmount, currency, roster)
                }
                Text(stringResource(R.string.money_section_schedule), style = AtmosphereTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    dueDate,
                    {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.money_due_date)) },
                    trailingIcon = { IconButton(onClick = { dateOpen = true }) { Icon(Icons.Outlined.CalendarMonth, null) } },
                )
                OutlinedTextField(notes, { notes = it.take(500) }, label = { Text(stringResource(R.string.money_notes_optional)) }, minLines = 2)
                if (initialExpense != null) {
                    Text(stringResource(R.string.money_section_revision), style = AtmosphereTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.money_revision_explanation), style = AtmosphereTheme.typography.bodySmall, color = AtmosphereTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        revisionReason,
                        { revisionReason = it.take(500) },
                        label = { Text(stringResource(R.string.money_revision_reason)) },
                        minLines = 2,
                        isError = attempted && revisionReason.trim().length < 3,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    attempted = true
                    if (valid) onSubmit(ExpenseDraft(
                        title = title.trim(),
                        supplierName = supplierName.trim().ifEmpty { null },
                        category = category,
                        customCategoryName = customCategoryName.trim().ifEmpty { null },
                        amountMinor = requireNotNull(parsedAmount),
                        dueDate = LocalDate.parse(dueDate),
                        notes = notes.trim().ifEmpty { null },
                    ), revisionReason.trim().ifEmpty { null })
                },
            ) { Text(stringResource(R.string.money_save_expense)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    if (dateOpen) {
        val picker = com.sharedhouse.android.ui.atmosphere.rememberDatePickerState(
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

private data class BillingPreviewLine(
    val label: String,
    val participantCount: Int,
    val amountMinor: Long,
)

@Composable
private fun BillingSplitPreview(
    totalMinor: Long,
    currency: String,
    roster: BillingRosterUi,
) {
    val lines = remember(totalMinor, roster) { billingPreviewLines(totalMinor, roster) }
    if (lines.isEmpty()) return
    Card(colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                stringResource(R.string.money_split_preview_title),
                style = AtmosphereTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            lines.forEach { line ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.label, style = AtmosphereTheme.typography.bodyMedium)
                        Text(
                            stringResource(
                                if (line.participantCount == 2) {
                                    R.string.money_split_preview_couple
                                } else {
                                    R.string.money_split_preview_person
                                },
                            ),
                            style = AtmosphereTheme.typography.labelSmall,
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(formatMoney(line.amountMinor, currency), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                stringResource(R.string.money_split_preview_notice),
                style = AtmosphereTheme.typography.labelSmall,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun billingPreviewLines(totalMinor: Long, roster: BillingRosterUi): List<BillingPreviewLine> {
    if (totalMinor <= 0) return emptyList()
    val members = roster.members.sortedBy(BillingRosterMemberUi::membershipId)
    val couplesByPrimary = roster.couples.associateBy(BillingCoupleUi::primaryMembershipId)
    val accountPartners = roster.couples.mapNotNull(BillingCoupleUi::partnerMembershipId).toSet()
    val units = members.mapNotNull { member ->
        if (member.membershipId in accountPartners) return@mapNotNull null
        val couple = couplesByPrimary[member.membershipId]
        BillingPreviewLine(
            label = if (couple == null) member.displayName else "${couple.primaryDisplayName} & ${couple.partnerDisplayName}",
            participantCount = if (couple == null) 1 else 2,
            amountMinor = 0,
        )
    }
    val residentCount = units.sumOf(BillingPreviewLine::participantCount)
    if (residentCount == 0) return emptyList()
    val perResident = totalMinor / residentCount
    var remainder = totalMinor % residentCount
    return units.map { unit ->
        val adjustment = minOf(unit.participantCount.toLong(), remainder)
        remainder -= adjustment
        unit.copy(amountMinor = perResident * unit.participantCount + adjustment)
    }
}

@Composable
private fun ExpenseDetails(
    expense: ExpenseUi,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
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
                    style = AtmosphereTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item { ExpenseStatusBadge(expense.status) }
            item {
                Text(
                    formatMoney(expense.amountMinor, expense.currency),
                    style = AtmosphereTheme.typography.headlineMedium,
                )
            }
            item { Text(stringResource(R.string.money_due_value, expense.dueDate.toString())) }
            expense.supplierName?.let { supplier ->
                item { Text(stringResource(R.string.money_supplier_value, supplier)) }
            }
            if (expense.revisionOfExpenseId != null) {
                item {
                    Text(
                        stringResource(R.string.money_revised_entry),
                        color = AtmosphereTheme.colorScheme.primary,
                        style = AtmosphereTheme.typography.bodySmall,
                    )
                }
            }
            if (expense.sourceTemplateId != null) {
                item {
                    Text(
                        stringResource(
                            R.string.money_generated_cost_details,
                            (expense.occurrenceDate ?: expense.dueDate).toString(),
                        ),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.primary,
                    )
                }
            }
            expense.notes?.let { note ->
                item { Text(note, color = AtmosphereTheme.colorScheme.onSurfaceVariant) }
            }
            if (expense.canApprove || expense.canRevise || expense.canReverse) {
                item {
                    ExpenseActionPanel(
                        canApprove = expense.canApprove,
                        canRevise = expense.canRevise,
                        canReverse = expense.canReverse,
                        activePayments = payments.filter { it.status != ExpensePaymentStatus.REVERSED },
                        busy = busy,
                        onApprove = onApprove,
                        onEdit = onEdit,
                        onReverse = { reverseOpen = true },
                        onCorrectPayment = { reversePaymentId = it.id },
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    stringResource(R.string.money_split_breakdown),
                    style = AtmosphereTheme.typography.titleMedium,
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
                        style = AtmosphereTheme.typography.labelMedium,
                        color = allocation.status.color(),
                    )
                    if (allocation.billingUnitType == BillingUnitType.COUPLE) {
                        Text(
                            pluralStringResource(
                                R.plurals.money_couple_combined_share,
                                allocation.participantCount,
                                allocation.participantCount,
                            ),
                            style = AtmosphereTheme.typography.bodySmall,
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        style = AtmosphereTheme.typography.bodySmall,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (payments.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text(
                        stringResource(R.string.money_payment_history),
                        style = AtmosphereTheme.typography.titleMedium,
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
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.money_reverse_explanation),
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(300) },
                        label = { Text(stringResource(R.string.money_reverse_reason)) },
                        supportingText = {
                            Text(
                                stringResource(R.string.money_reverse_reason_help),
                                style = AtmosphereTheme.typography.labelSmall,
                                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onReverse(reason.trim()) },
                    enabled = reason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AtmosphereTheme.colorScheme.error,
                        contentColor = AtmosphereTheme.colorScheme.onError,
                    ),
                ) { Text(stringResource(R.string.money_confirm_reverse)) }
            },
            dismissButton = { TextButton(onClick = { reverseOpen = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ExpenseActionPanel(
    canApprove: Boolean,
    canRevise: Boolean,
    canReverse: Boolean,
    activePayments: List<ExpensePaymentUi>,
    busy: Boolean,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
    onReverse: () -> Unit,
    onCorrectPayment: (ExpensePaymentUi) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (canReverse && activePayments.isNotEmpty()) {
            Surface(
                shape = AtmosphereTheme.shapes.medium,
                color = AtmosphereTheme.colorScheme.errorContainer,
                contentColor = AtmosphereTheme.colorScheme.onErrorContainer,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.money_remove_blocked_title),
                        style = AtmosphereTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        pluralStringResource(
                            R.plurals.money_remove_blocked_payments,
                            activePayments.size,
                            activePayments.size,
                        ),
                        style = AtmosphereTheme.typography.bodySmall,
                    )
                    activePayments.forEach { payment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(payment.payerDisplayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(payment.status.labelResource),
                                    style = AtmosphereTheme.typography.labelSmall,
                                )
                            }
                            TextButton(
                                onClick = { onCorrectPayment(payment) },
                                enabled = !busy && payment.canReverse,
                            ) { Text(stringResource(R.string.money_reverse_payment)) }
                        }
                    }
                }
            }
        }
        if (canApprove) {
            Button(
                onClick = onApprove,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.money_approve)) }
        }
        if (canRevise || canReverse) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (canRevise) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.money_edit_expense), maxLines = 1) }
                }
                if (canReverse) {
                    Button(
                        onClick = onReverse,
                        enabled = !busy && activePayments.isEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AtmosphereTheme.colorScheme.error,
                            contentColor = AtmosphereTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(R.string.money_remove_expense), maxLines = 1) }
                }
            }
        }
        if (canReverse) {
            Text(
                stringResource(
                    if (activePayments.isEmpty()) {
                        R.string.money_remove_history_notice
                    } else {
                        R.string.money_remove_blocked_notice
                    },
                ),
                style = AtmosphereTheme.typography.labelSmall,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    Card(colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(payment.payerDisplayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(formatMoney(payment.amountMinor, payment.currency), fontWeight = FontWeight.SemiBold)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(payment.status.icon(), null, Modifier.size(15.dp), tint = payment.status.color())
                Text(
                    stringResource(
                        R.string.money_payment_status_by,
                        stringResource(payment.status.labelResource),
                        payment.statusActorDisplayName(),
                    ),
                    style = AtmosphereTheme.typography.labelMedium,
                    color = payment.status.color(),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (payment.status != ExpensePaymentStatus.DECLARED) {
                Text(
                    stringResource(R.string.money_payment_declared_by, payment.declaredByDisplayName),
                    style = AtmosphereTheme.typography.bodySmall,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(
                    R.string.money_payment_method_date,
                    stringResource(payment.method.labelResource),
                    formatPaymentTime(payment.paidAt),
                ),
                style = AtmosphereTheme.typography.bodySmall,
            )
            payment.reference?.let {
                Text(stringResource(R.string.money_payment_reference_value, it))
            }
            payment.note?.let { Text(it, color = AtmosphereTheme.colorScheme.onSurfaceVariant) }
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

private fun ExpensePaymentUi.statusActorDisplayName(): String = when (status) {
    ExpensePaymentStatus.DECLARED -> declaredByDisplayName
    ExpensePaymentStatus.CONFIRMED -> confirmedByDisplayName ?: declaredByDisplayName
    ExpensePaymentStatus.DISPUTED -> disputedByDisplayName ?: declaredByDisplayName
    ExpensePaymentStatus.REVERSED -> reversedByDisplayName ?: declaredByDisplayName
}

private fun ExpensePaymentStatus.icon(): ImageVector = when (this) {
    ExpensePaymentStatus.DECLARED -> SharedHouseIcons.Pending
    ExpensePaymentStatus.CONFIRMED -> SharedHouseIcons.Approved
    ExpensePaymentStatus.DISPUTED, ExpensePaymentStatus.REVERSED -> SharedHouseIcons.Reversed
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
                    style = AtmosphereTheme.typography.bodySmall,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
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
    ExpenseTemplateCadence.FORTNIGHTLY -> R.string.money_cadence_fortnightly
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
    ExpenseStatus.PROPOSED -> AtmosphereTheme.colorScheme.statusAttention
    ExpenseStatus.APPROVED -> AtmosphereTheme.colorScheme.statusPositive
    ExpenseStatus.REVERSED -> AtmosphereTheme.colorScheme.statusNeutral
}

@Composable private fun ExpenseAllocationStatus.color() = when (this) {
    ExpenseAllocationStatus.OUTSTANDING -> AtmosphereTheme.colorScheme.statusAttention
    ExpenseAllocationStatus.DECLARED -> AtmosphereTheme.colorScheme.statusAttention
    ExpenseAllocationStatus.PAID -> AtmosphereTheme.colorScheme.statusPositive
    ExpenseAllocationStatus.DISPUTED -> AtmosphereTheme.colorScheme.statusNegative
}

@Composable private fun ExpensePaymentStatus.color() = when (this) {
    ExpensePaymentStatus.DECLARED -> AtmosphereTheme.colorScheme.statusAttention
    ExpensePaymentStatus.CONFIRMED -> AtmosphereTheme.colorScheme.statusPositive
    ExpensePaymentStatus.DISPUTED -> AtmosphereTheme.colorScheme.statusNegative
    ExpensePaymentStatus.REVERSED -> AtmosphereTheme.colorScheme.statusNeutral
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
    MoneyProblem.REVISE_FAILED -> R.string.money_revise_error
    MoneyProblem.TEMPLATE_FAILED -> R.string.money_template_error
    MoneyProblem.PAYMENT_DECLARE_FAILED -> R.string.money_payment_declare_error
    MoneyProblem.PAYMENT_CONFIRM_FAILED -> R.string.money_payment_confirm_error
    MoneyProblem.PAYMENT_DISPUTE_FAILED -> R.string.money_payment_dispute_error
    MoneyProblem.PAYMENT_REVERSE_FAILED -> R.string.money_payment_reverse_error
    MoneyProblem.BILLING_ROSTER_FAILED -> R.string.money_billing_roster_error
    MoneyProblem.VERSION_CONFLICT -> R.string.money_version_conflict
}

private const val NEW_TEMPLATE_ID = "new"
