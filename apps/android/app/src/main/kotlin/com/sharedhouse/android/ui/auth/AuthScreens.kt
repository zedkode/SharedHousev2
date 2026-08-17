package com.sharedhouse.android.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.Checkbox
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.atmosphere.FilledTonalButton
import com.sharedhouse.android.ui.atmosphere.HorizontalDivider
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.atmosphere.LinearProgressIndicator
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.OutlinedButton
import com.sharedhouse.android.ui.atmosphere.OutlinedTextField
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Surface
import com.sharedhouse.android.ui.atmosphere.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.app.AppUiState
import com.sharedhouse.android.ui.app.FormField
import com.sharedhouse.android.ui.components.FormScreenScaffold
import com.sharedhouse.android.ui.components.StatusMessage
import com.sharedhouse.android.ui.components.localized

@Composable
fun WelcomeScreen(
    state: AppUiState,
    onRegister: () -> Unit,
    onSignIn: () -> Unit,
    onRetrySession: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AtmosphereTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Surface(
                        shape = AtmosphereTheme.shapes.large,
                        color = AtmosphereTheme.colorScheme.surface,
                        contentColor = AtmosphereTheme.colorScheme.primary,
                        border = BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant.copy(alpha = .70f)),
                    ) {
                        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground_art),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.welcome_title),
                            style = AtmosphereTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.welcome_description),
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                            style = AtmosphereTheme.typography.bodyLarge,
                        )
                    }
                }
                state.notice?.let { notice ->
                    item {
                        StatusMessage(
                            message = notice,
                            isError = notice in setOf(
                                com.sharedhouse.android.ui.app.UiMessage.SessionRevocationUnconfirmed,
                                com.sharedhouse.android.ui.app.UiMessage.SecureStorageUnavailable,
                                com.sharedhouse.android.ui.app.UiMessage.SessionRestoreNetworkUnavailable,
                            ),
                            correlationId = null,
                            modifier = Modifier.clickable(onClick = onDismissNotice),
                        )
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
                        border = BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant.copy(alpha = .70f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = AtmosphereTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.session_secure_notice),
                                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                                style = AtmosphereTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.canRetrySessionRestore) {
                            FilledTonalButton(
                                onClick = onRetrySession,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) {
                                Text(text = stringResource(R.string.retry_secure_session))
                            }
                        }
                        Button(
                            onClick = onRegister,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) {
                            Text(text = stringResource(R.string.create_account))
                        }
                        OutlinedButton(
                            onClick = onSignIn,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) {
                            Text(text = stringResource(R.string.sign_in))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgeChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onMarketingChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    FormScreenScaffold(
        title = R.string.register_title,
        description = R.string.register_description,
        onBack = onBack,
    ) {
        ScreenMessages(state)
        OutlinedTextField(
            value = state.auth.displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.display_name_label)) },
            supportingText = { FieldError(state, FormField.DisplayName) },
            isError = FormField.DisplayName in state.fieldErrors,
            enabled = !state.isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        EmailField(
            value = state.auth.email,
            onValueChange = onEmailChange,
            error = state.fieldErrors[FormField.Email],
            enabled = !state.isSubmitting,
        )
        PasswordField(
            value = state.auth.password,
            onValueChange = onPasswordChange,
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            error = state.fieldErrors[FormField.Password],
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onDone = { focusManager.clearFocus() },
            supportingFallback = stringResource(R.string.password_requirements),
        )
        HorizontalDivider()
        ConsentRow(
            checked = state.auth.ageConfirmed,
            onCheckedChange = onAgeChange,
            text = stringResource(R.string.age_confirmation),
            error = state.fieldErrors[FormField.AgeConfirmation],
            enabled = !state.isSubmitting,
        )
        ConsentRow(
            checked = state.auth.termsAccepted,
            onCheckedChange = onTermsChange,
            text = stringResource(R.string.terms_confirmation),
            error = state.fieldErrors[FormField.TermsConfirmation],
            enabled = !state.isSubmitting,
        )
        ConsentRow(
            checked = state.auth.marketingConsent,
            onCheckedChange = onMarketingChange,
            text = stringResource(R.string.marketing_consent),
            error = null,
            enabled = !state.isSubmitting,
        )
        SubmitButton(
            text = R.string.create_account,
            loadingText = R.string.creating_account,
            loading = state.isSubmitting,
            onClick = onSubmit,
        )
    }
}

@Composable
fun VerifyEmailScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val focusManager = LocalFocusManager.current
    FormScreenScaffold(
        title = R.string.verify_email_title,
        description = R.string.verify_email_description,
        onBack = onBack,
        header = {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.verification_sent_to, state.auth.email),
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                style = AtmosphereTheme.typography.bodyMedium,
            )
        },
    ) {
        ScreenMessages(state)
        state.developmentVerificationCode?.let { code ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.secondaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.development_code_label),
                        style = AtmosphereTheme.typography.labelLarge,
                    )
                    Text(text = code, style = AtmosphereTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.development_code_notice),
                        color = AtmosphereTheme.colorScheme.onSecondaryContainer,
                        style = AtmosphereTheme.typography.bodySmall,
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.auth.verificationCode,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.verification_code_label)) },
            supportingText = { FieldError(state, FormField.VerificationCode) },
            isError = FormField.VerificationCode in state.fieldErrors,
            enabled = !state.isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onSubmit() }),
        )
        SubmitButton(
            text = R.string.verify_and_continue,
            loadingText = R.string.verifying_email,
            loading = state.isSubmitting,
            onClick = onSubmit,
        )
        OutlinedButton(
            onClick = onResend,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(text = stringResource(R.string.resend_verification_code))
        }
    }
}

@Composable
fun SignInScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    FormScreenScaffold(
        title = R.string.sign_in_title,
        description = R.string.sign_in_description,
        onBack = onBack,
    ) {
        ScreenMessages(state)
        EmailField(
            value = state.auth.email,
            onValueChange = onEmailChange,
            error = state.fieldErrors[FormField.Email],
            enabled = !state.isSubmitting,
        )
        PasswordField(
            value = state.auth.password,
            onValueChange = onPasswordChange,
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            error = state.fieldErrors[FormField.Password],
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onDone = { focusManager.clearFocus(); onSubmit() },
        )
        SubmitButton(
            text = R.string.sign_in,
            loadingText = R.string.signing_in,
            loading = state.isSubmitting,
            onClick = onSubmit,
        )
    }
}

@Composable
fun HouseholdGateScreen(
    state: AppUiState,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    FormScreenScaffold(
        title = R.string.loading_households_title,
        description = R.string.loading_households_description,
        onBack = null,
    ) {
        if (state.isSubmitting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.contacting_service),
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
        ScreenMessages(state)
        if (!state.isSubmitting) {
            FilledTonalButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(text = stringResource(R.string.action_retry))
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(text = stringResource(R.string.sign_out))
            }
        }
    }
}

@Composable
fun HouseholdChoiceScreen(
    state: AppUiState,
    onCreateHousehold: () -> Unit,
    onJoinHousehold: () -> Unit,
    onSignOut: () -> Unit,
) {
    FormScreenScaffold(
        title = R.string.household_choice_title,
        description = R.string.household_choice_description,
        onBack = null,
    ) {
        ScreenMessages(state)
        Button(
            onClick = onCreateHousehold,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.household_choice_create))
        }
        FilledTonalButton(
            onClick = onJoinHousehold,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.household_choice_join))
        }
        Text(
            text = stringResource(R.string.household_choice_guidance),
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            style = AtmosphereTheme.typography.bodyMedium,
        )
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
private fun ScreenMessages(state: AppUiState) {
    state.error?.let {
        StatusMessage(message = it, isError = true, correlationId = state.correlationId)
    }
    state.notice?.let {
        StatusMessage(message = it, isError = false, correlationId = null)
    }
}

@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    error: com.sharedhouse.android.ui.app.UiMessage?,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.EmailAddress },
        label = { Text(stringResource(R.string.email_label)) },
        supportingText = { error?.let { Text(it.localized()) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
    error: com.sharedhouse.android.ui.app.UiMessage?,
    enabled: Boolean,
    imeAction: ImeAction,
    onDone: () -> Unit,
    supportingFallback: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Password },
        label = { Text(stringResource(R.string.password_label)) },
        supportingText = {
            Text(error?.localized() ?: supportingFallback.orEmpty())
        },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange, enabled = enabled) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.hide_password else R.string.show_password,
                    ),
                )
            }
        },
    )
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    error: com.sharedhouse.android.ui.app.UiMessage?,
    enabled: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
            Text(text = text, modifier = Modifier.padding(start = 8.dp), style = AtmosphereTheme.typography.bodyMedium)
        }
        error?.let {
            Text(
                text = it.localized(),
                color = AtmosphereTheme.colorScheme.error,
                style = AtmosphereTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 48.dp),
            )
        }
    }
}

@Composable
private fun FieldError(state: AppUiState, field: FormField) {
    state.fieldErrors[field]?.let { Text(it.localized()) }
}

@Composable
private fun SubmitButton(
    @androidx.annotation.StringRes text: Int,
    @androidx.annotation.StringRes loadingText: Int,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AtmosphereTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.size(10.dp))
        }
        Text(text = stringResource(if (loading) loadingText else text))
    }
}
