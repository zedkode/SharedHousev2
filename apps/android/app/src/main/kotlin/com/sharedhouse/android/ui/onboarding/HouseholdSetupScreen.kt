package com.sharedhouse.android.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.app.AppUiState
import com.sharedhouse.android.ui.app.FormField
import com.sharedhouse.android.ui.app.HouseholdEditorMode
import com.sharedhouse.android.ui.app.UiMessage
import com.sharedhouse.android.ui.components.FormScreenScaffold
import com.sharedhouse.android.ui.components.StatusMessage
import com.sharedhouse.android.ui.components.localized

@Composable
fun HouseholdSetupScreen(
    state: AppUiState,
    onNameChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onFirstDayChange: (Int) -> Unit,
    onCycleTypeChange: (String) -> Unit,
    onCycleAnchorChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onSignOut: () -> Unit,
) {
    val editing = state.householdEditorMode == HouseholdEditorMode.Edit
    BackHandler(enabled = editing, onBack = onBack)
    FormScreenScaffold(
        title = if (editing) R.string.household_edit_title else R.string.household_setup_title,
        description = if (editing) {
            R.string.household_edit_description
        } else {
            R.string.household_setup_description
        },
        onBack = if (editing) onBack else null,
        header = {
            Text(
                text = stringResource(
                    R.string.signed_in_as,
                    state.account?.displayName.orEmpty(),
                    state.account?.email.orEmpty(),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.session_memory_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    ) {
        state.error?.let {
            StatusMessage(message = it, isError = true, correlationId = state.correlationId)
        }
        state.notice?.let {
            StatusMessage(message = it, isError = false, correlationId = null)
        }
        if (state.error == UiMessage.HouseholdVersionConflict) {
            OutlinedButton(
                onClick = onReload,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = stringResource(R.string.reload_household_settings))
            }
        }
        OutlinedTextField(
            value = state.household.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.household_name_label)) },
            supportingText = { FieldError(state, FormField.HouseholdName) },
            isError = FormField.HouseholdName in state.fieldErrors,
            enabled = !state.isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.household.countryCode,
                onValueChange = onCountryChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.country_code_label)) },
                supportingText = { FieldError(state, FormField.CountryCode) },
                isError = FormField.CountryCode in state.fieldErrors,
                enabled = !state.isSubmitting,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.household.currency,
                onValueChange = onCurrencyChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.currency_label)) },
                supportingText = { FieldError(state, FormField.Currency) },
                isError = FormField.Currency in state.fieldErrors,
                enabled = !state.isSubmitting,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            )
        }
        OutlinedTextField(
            value = state.household.timezone,
            onValueChange = onTimezoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.timezone_label)) },
            supportingText = {
                FieldError(state, FormField.Timezone)
                if (FormField.Timezone !in state.fieldErrors) {
                    Text(stringResource(R.string.timezone_support))
                }
            },
            isError = FormField.Timezone in state.fieldErrors,
            enabled = !state.isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        HorizontalDivider()
        ChoiceGroupTitle(R.string.first_day_title)
        ChoiceRow(
            label = stringResource(R.string.monday),
            selected = state.household.firstDayOfWeek == 1,
            enabled = !state.isSubmitting,
            onClick = { onFirstDayChange(1) },
        )
        ChoiceRow(
            label = stringResource(R.string.saturday),
            selected = state.household.firstDayOfWeek == 6,
            enabled = !state.isSubmitting,
            onClick = { onFirstDayChange(6) },
        )
        ChoiceRow(
            label = stringResource(R.string.sunday),
            selected = state.household.firstDayOfWeek == 7,
            enabled = !state.isSubmitting,
            onClick = { onFirstDayChange(7) },
        )
        HorizontalDivider()
        ChoiceGroupTitle(R.string.billing_cycle_title)
        ChoiceRow(
            label = stringResource(R.string.cycle_weekly),
            selected = state.household.cycleType == "weekly",
            enabled = !state.isSubmitting,
            onClick = { onCycleTypeChange("weekly") },
        )
        ChoiceRow(
            label = stringResource(R.string.cycle_fourteen_day),
            selected = state.household.cycleType == "fourteen_day",
            enabled = !state.isSubmitting,
            onClick = { onCycleTypeChange("fourteen_day") },
        )
        ChoiceRow(
            label = stringResource(R.string.cycle_calendar_month),
            selected = state.household.cycleType == "calendar_month",
            enabled = !state.isSubmitting,
            onClick = { onCycleTypeChange("calendar_month") },
        )
        OutlinedTextField(
            value = state.household.cycleAnchor,
            onValueChange = onCycleAnchorChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.cycle_anchor_label)) },
            supportingText = {
                FieldError(state, FormField.CycleAnchor)
                if (FormField.CycleAnchor !in state.fieldErrors) {
                    Text(stringResource(R.string.cycle_anchor_support))
                }
            },
            isError = FormField.CycleAnchor in state.fieldErrors,
            enabled = !state.isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        )
        Button(
            onClick = onSave,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(
                text = stringResource(
                    if (state.isSubmitting) {
                        if (editing) R.string.saving_household else R.string.creating_household
                    } else {
                        if (editing) R.string.save_household else R.string.create_household
                    },
                ),
            )
        }
        OutlinedButton(
            onClick = onSignOut,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(text = stringResource(R.string.sign_out))
        }
    }
}

@Composable
private fun ChoiceGroupTitle(resource: Int) {
    Text(text = stringResource(resource), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun FieldError(state: AppUiState, field: FormField) {
    state.fieldErrors[field]?.let { Text(it.localized()) }
}
