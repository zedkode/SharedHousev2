package com.sharedhouse.android.ui.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.auth.HouseholdGateScreen
import com.sharedhouse.android.ui.auth.RegisterScreen
import com.sharedhouse.android.ui.auth.SignInScreen
import com.sharedhouse.android.ui.auth.VerifyEmailScreen
import com.sharedhouse.android.ui.auth.WelcomeScreen
import com.sharedhouse.android.ui.home.HomeFoundationState
import com.sharedhouse.android.ui.home.SharedHouseHome
import com.sharedhouse.android.ui.onboarding.HouseholdSetupScreen
import kotlinx.coroutines.launch

@Composable
fun SharedHouseApp(
    viewModel: SharedHouseViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(state.route, currentRoute) {
        if (currentRoute != state.route.path) {
            navController.navigate(state.route.path) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Welcome.path,
    ) {
        composable(AppRoute.Welcome.path) {
            WelcomeScreen(
                state = state,
                onRegister = viewModel::openRegister,
                onSignIn = viewModel::openSignIn,
                onDismissNotice = viewModel::clearNotice,
            )
        }
        composable(AppRoute.Register.path) {
            RegisterScreen(
                state = state,
                onBack = viewModel::openWelcome,
                onDisplayNameChange = viewModel::updateDisplayName,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onAgeChange = viewModel::updateAgeConfirmed,
                onTermsChange = viewModel::updateTermsAccepted,
                onMarketingChange = viewModel::updateMarketingConsent,
                onSubmit = viewModel::register,
            )
        }
        composable(AppRoute.VerifyEmail.path) {
            VerifyEmailScreen(
                state = state,
                onBack = viewModel::openWelcome,
                onCodeChange = viewModel::updateVerificationCode,
                onSubmit = viewModel::verifyEmail,
            )
        }
        composable(AppRoute.SignIn.path) {
            SignInScreen(
                state = state,
                onBack = viewModel::openWelcome,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onSubmit = viewModel::signIn,
            )
        }
        composable(AppRoute.HouseholdGate.path) {
            HouseholdGateScreen(
                state = state,
                onRetry = viewModel::retryHouseholds,
                onSignOut = viewModel::signOut,
            )
        }
        composable(AppRoute.HouseholdSetup.path) {
            HouseholdSetupScreen(
                state = state,
                onNameChange = viewModel::updateHouseholdName,
                onCountryChange = viewModel::updateCountryCode,
                onTimezoneChange = viewModel::updateTimezone,
                onCurrencyChange = viewModel::updateCurrency,
                onFirstDayChange = viewModel::updateFirstDayOfWeek,
                onCycleTypeChange = viewModel::updateCycleType,
                onCycleAnchorChange = viewModel::updateCycleAnchor,
                onSave = viewModel::createHousehold,
                onBack = viewModel::closeHouseholdEditor,
                onReload = viewModel::reloadHouseholdEditor,
                onSignOut = viewModel::signOut,
            )
        }
        composable(AppRoute.Home.path) {
            AuthenticatedHome(
                state = state,
                onEditHousehold = viewModel::openHouseholdEditor,
                onSignOut = viewModel::signOut,
            )
        }
    }
}

@Composable
private fun AuthenticatedHome(
    state: AppUiState,
    onEditHousehold: () -> Unit,
    onSignOut: () -> Unit,
) {
    val household = state.selectedHousehold
    val account = state.account
    var showAccountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SharedHouseHome(
        snackbarHostState = snackbarHostState,
        onMessage = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        },
        onProfileClick = { showAccountDialog = true },
        onHouseholdClick = onEditHousehold,
        state = HomeFoundationState.authenticated(
            householdName = household?.name.orEmpty(),
            accountDisplayName = account?.displayName.orEmpty(),
        ),
    )

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text(stringResource(R.string.account_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.account_session_details,
                        account?.displayName.orEmpty(),
                        account?.email.orEmpty(),
                    ) + "\n\n" + stringResource(R.string.session_memory_notice),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        onSignOut()
                    },
                ) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}
