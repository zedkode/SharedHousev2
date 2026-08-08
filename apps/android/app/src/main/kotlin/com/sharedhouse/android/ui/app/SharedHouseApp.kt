package com.sharedhouse.android.ui.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sharedhouse.android.preferences.AppLanguage
import com.sharedhouse.android.preferences.AppPreferences
import com.sharedhouse.android.preferences.AppPreferencesRepository
import com.sharedhouse.android.platform.google.GoogleServicesStatus
import com.sharedhouse.android.platform.google.CompliantAdBanner
import com.sharedhouse.android.ui.auth.HouseholdGateScreen
import com.sharedhouse.android.ui.auth.HouseholdChoiceScreen
import com.sharedhouse.android.ui.auth.RegisterScreen
import com.sharedhouse.android.ui.auth.SignInScreen
import com.sharedhouse.android.ui.auth.VerifyEmailScreen
import com.sharedhouse.android.ui.auth.WelcomeScreen
import com.sharedhouse.android.ui.calendar.CalendarContent
import com.sharedhouse.android.ui.calendar.CalendarScreen
import com.sharedhouse.android.ui.guides.GuideArticleScreen
import com.sharedhouse.android.ui.guides.GuideTopic
import com.sharedhouse.android.ui.guides.GuidesScreen
import com.sharedhouse.android.ui.home.DashboardCalendarContent
import com.sharedhouse.android.ui.home.HouseholdDashboardScreen
import com.sharedhouse.android.ui.home.HouseholdDashboardUiModel
import com.sharedhouse.android.ui.home.HouseholdDestination
import com.sharedhouse.android.ui.home.HouseholdHubScreen
import com.sharedhouse.android.ui.home.HouseholdHubUiModel
import com.sharedhouse.android.ui.home.HouseholdOptionUi
import com.sharedhouse.android.ui.home.HouseholdNavigationShell
import com.sharedhouse.android.ui.home.UnavailableHouseholdFeature
import com.sharedhouse.android.ui.home.UnavailableHouseholdFeatureScreen
import com.sharedhouse.android.ui.invitations.InvitationJoinScreen
import com.sharedhouse.android.ui.invitations.InvitationManagerScreen
import com.sharedhouse.android.ui.onboarding.HouseholdSetupScreen
import com.sharedhouse.android.ui.settings.SettingsRoute
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun SharedHouseApp(
    viewModel: SharedHouseViewModel,
    preferencesRepository: AppPreferencesRepository,
    appPreferences: AppPreferences,
    googleServicesStatus: GoogleServicesStatus,
    onShowAdPrivacyOptions: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    if (state.isRestoringSession) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.session_restoring),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        return
    }

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
                onRetrySession = viewModel::retrySessionRestore,
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
                onResend = viewModel::resendVerification,
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
        composable(AppRoute.HouseholdChoice.path) {
            HouseholdChoiceScreen(
                state = state,
                onCreateHousehold = viewModel::openCreateHousehold,
                onJoinHousehold = viewModel::openInvitationJoin,
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
        composable(AppRoute.InvitationJoin.path) {
            InvitationJoinScreen(
                state = state,
                onBack = viewModel::closeInvitationFlow,
                onTokenChange = viewModel::updateInvitationToken,
                onPreview = viewModel::previewInvitation,
                onAccept = viewModel::acceptInvitation,
            )
        }
        composable(AppRoute.InvitationManage.path) {
            val context = LocalContext.current
            InvitationManagerScreen(
                state = state,
                canInviteAdmins = state.selectedHousehold?.role == "owner",
                onBack = viewModel::closeInvitationFlow,
                onEmailChange = viewModel::updateInvitationEmail,
                onRoleChange = viewModel::updateInvitationRole,
                onCreate = viewModel::createInvitation,
                onShare = { token -> shareInvitation(context, token) },
                onRevoke = viewModel::revokeInvitation,
            )
        }
        composable(AppRoute.Home.path) {
            AuthenticatedHouseholdExperience(
                state = state,
                viewModel = viewModel,
                preferencesRepository = preferencesRepository,
                appPreferences = appPreferences,
                googleServicesStatus = googleServicesStatus,
                onShowAdPrivacyOptions = onShowAdPrivacyOptions,
                onLanguageChanged = onLanguageChanged,
            )
        }
    }
}

private enum class SecondarySurface {
    NONE,
    SETTINGS,
    GUIDES,
    ARTICLE,
}

@Composable
private fun AuthenticatedHouseholdExperience(
    state: AppUiState,
    viewModel: SharedHouseViewModel,
    preferencesRepository: AppPreferencesRepository,
    appPreferences: AppPreferences,
    googleServicesStatus: GoogleServicesStatus,
    onShowAdPrivacyOptions: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val household = state.selectedHousehold
    if (household == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedDestinationName by rememberSaveable {
        mutableStateOf(HouseholdDestination.HOME.name)
    }
    var secondarySurfaceName by rememberSaveable { mutableStateOf(SecondarySurface.NONE.name) }
    var guideTopicName by rememberSaveable { mutableStateOf(GuideTopic.GETTING_STARTED.name) }
    var articleParentName by rememberSaveable { mutableStateOf(SecondarySurface.GUIDES.name) }

    val selectedDestination = runCatching {
        HouseholdDestination.valueOf(selectedDestinationName)
    }.getOrDefault(HouseholdDestination.HOME)
    val secondarySurface = runCatching {
        SecondarySurface.valueOf(secondarySurfaceName)
    }.getOrDefault(SecondarySurface.NONE)
    val guideTopic = runCatching { GuideTopic.valueOf(guideTopicName) }
        .getOrDefault(GuideTopic.GETTING_STARTED)

    fun openSecondary(surface: SecondarySurface) {
        secondarySurfaceName = surface.name
    }

    fun openArticle(topic: GuideTopic, parent: SecondarySurface) {
        guideTopicName = topic.name
        articleParentName = parent.name
        secondarySurfaceName = SecondarySurface.ARTICLE.name
    }

    HouseholdNavigationShell(
        selectedDestination = selectedDestination,
        onDestinationSelected = { destination ->
            selectedDestinationName = destination.name
            secondarySurfaceName = SecondarySurface.NONE.name
        },
    ) {
        when (secondarySurface) {
            SecondarySurface.SETTINGS -> SettingsRoute(
                repository = preferencesRepository,
                onBack = { openSecondary(SecondarySurface.NONE) },
                onOpenGuides = { openSecondary(SecondarySurface.GUIDES) },
                onOpenPrivacy = {
                    openArticle(GuideTopic.PRIVACY, SecondarySurface.SETTINGS)
                },
                onOpenSecurity = {
                    openArticle(GuideTopic.SECURITY, SecondarySurface.SETTINGS)
                },
                onOpenLegal = {
                    openArticle(GuideTopic.LEGAL, SecondarySurface.SETTINGS)
                },
                onTutorialRequested = {
                    scope.launch { preferencesRepository.showTutorialAgain() }
                },
                accountError = state.error,
                accountOperationInProgress = state.isSubmitting,
                onDeleteAccount = viewModel::deleteAccount,
                googleServicesStatus = googleServicesStatus,
                onShowAdPrivacyOptions = onShowAdPrivacyOptions,
                onLanguageChanged = onLanguageChanged,
            )

            SecondarySurface.GUIDES -> GuidesScreen(
                onBack = { openSecondary(SecondarySurface.NONE) },
                onOpenTopic = { topic -> openArticle(topic, SecondarySurface.GUIDES) },
                sponsoredContent = {
                    if (appPreferences.privacy.adsEnabled && googleServicesStatus.adsReady) {
                        Text(
                            text = stringResource(R.string.sponsored_content_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CompliantAdBanner(enabled = true)
                    }
                },
            )

            SecondarySurface.ARTICLE -> GuideArticleScreen(
                topic = guideTopic,
                onBack = {
                    secondarySurfaceName = runCatching {
                        SecondarySurface.valueOf(articleParentName).name
                    }.getOrDefault(SecondarySurface.GUIDES.name)
                },
            )

            SecondarySurface.NONE -> when (selectedDestination) {
                HouseholdDestination.HOME -> HouseholdDashboardScreen(
                    model = HouseholdDashboardUiModel(
                        householdName = household.name,
                        accountDisplayName = state.account?.displayName.orEmpty(),
                        calendar = state.calendar.toDashboardCalendar(),
                    ),
                    onOpenCalendar = {
                        selectedDestinationName = HouseholdDestination.CALENDAR.name
                    },
                    onRetryCalendar = viewModel::refreshCalendar,
                    onEditHousehold = viewModel::openHouseholdEditor,
                    onOpenGuides = { openSecondary(SecondarySurface.GUIDES) },
                    onOpenSettings = { openSecondary(SecondarySurface.SETTINGS) },
                    onOpenMoney = {
                        selectedDestinationName = HouseholdDestination.MONEY.name
                    },
                    onOpenTasks = {
                        selectedDestinationName = HouseholdDestination.TASKS.name
                    },
                )

                HouseholdDestination.CALENDAR -> CalendarScreen(
                    state = state.calendar,
                    onAction = viewModel::handleCalendarAction,
                )

                HouseholdDestination.MONEY -> UnavailableHouseholdFeatureScreen(
                    feature = UnavailableHouseholdFeature.MONEY,
                    onOpenCalendar = {
                        selectedDestinationName = HouseholdDestination.CALENDAR.name
                    },
                    onOpenGuides = { openSecondary(SecondarySurface.GUIDES) },
                )

                HouseholdDestination.TASKS -> UnavailableHouseholdFeatureScreen(
                    feature = UnavailableHouseholdFeature.TASKS,
                    onOpenCalendar = {
                        selectedDestinationName = HouseholdDestination.CALENDAR.name
                    },
                    onOpenGuides = { openSecondary(SecondarySurface.GUIDES) },
                )

                HouseholdDestination.HOUSE -> HouseholdHubScreen(
                    model = HouseholdHubUiModel(
                        householdName = household.name,
                        accountDisplayName = state.account?.displayName.orEmpty(),
                        countryCode = household.countryCode,
                        timezone = household.timezone,
                        currencyCode = household.currency,
                        firstDayOfWeek = household.firstDayOfWeek,
                        cycleType = household.cycleType,
                        cycleAnchor = household.cycleAnchor,
                        householdRole = household.role,
                        membershipStatus = household.status,
                        households = state.households.map { option ->
                            HouseholdOptionUi(
                                id = option.id,
                                name = option.name,
                                role = option.role,
                                selected = option.id == household.id,
                            )
                        },
                    ),
                    onEditHousehold = viewModel::openHouseholdEditor,
                    onManageInvitations = viewModel::openInvitationManager,
                    onJoinHousehold = viewModel::openInvitationJoin,
                    onSelectHousehold = viewModel::selectHousehold,
                    onOpenSettings = { openSecondary(SecondarySurface.SETTINGS) },
                    onOpenGuides = { openSecondary(SecondarySurface.GUIDES) },
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}

private fun shareInvitation(context: Context, token: String) {
    val text = context.getString(R.string.invitation_share_message, token)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.invitation_share_action)),
    )
}

private fun com.sharedhouse.android.ui.calendar.CalendarUiState.toDashboardCalendar():
    DashboardCalendarContent = when (val current = content) {
        CalendarContent.Loading -> DashboardCalendarContent.Loading
        is CalendarContent.Error -> DashboardCalendarContent.Error
        is CalendarContent.Ready -> DashboardCalendarContent.Ready(
            events = current.events.filter { event ->
                !event.date.isBefore(LocalDate.now(zoneId))
            },
        )
    }
