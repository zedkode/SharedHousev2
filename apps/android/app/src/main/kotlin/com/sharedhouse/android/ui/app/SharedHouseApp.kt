package com.sharedhouse.android.ui.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sharedhouse.android.preferences.AppLanguage
import com.sharedhouse.android.preferences.AppPreferences
import com.sharedhouse.android.preferences.AppPreferencesRepository
import com.sharedhouse.android.platform.notifications.HouseholdReminderScheduler
import com.sharedhouse.android.platform.notifications.SharedHouseNotifications
import com.sharedhouse.android.platform.google.GoogleServicesStatus
import com.sharedhouse.android.platform.google.CompliantAdBanner
import com.sharedhouse.android.ui.auth.HouseholdGateScreen
import com.sharedhouse.android.ui.auth.HouseholdChoiceScreen
import com.sharedhouse.android.ui.auth.RegisterScreen
import com.sharedhouse.android.ui.auth.SignInScreen
import com.sharedhouse.android.ui.auth.VerifyEmailScreen
import com.sharedhouse.android.ui.auth.WelcomeScreen
import com.sharedhouse.android.ui.calendar.CalendarContent
import com.sharedhouse.android.ui.calendar.CalendarEventType
import com.sharedhouse.android.ui.calendar.CalendarEventUi
import com.sharedhouse.android.ui.calendar.CalendarScreen
import com.sharedhouse.android.ui.chat.HouseholdChatScreen
import com.sharedhouse.android.ui.guides.GuideArticleScreen
import com.sharedhouse.android.ui.guides.GuideTopic
import com.sharedhouse.android.ui.guides.GuidesScreen
import com.sharedhouse.android.ui.home.DashboardCalendarContent
import com.sharedhouse.android.ui.home.DashboardTasksContent
import com.sharedhouse.android.ui.home.DashboardMoneyContent
import com.sharedhouse.android.ui.home.HouseholdDashboardScreen
import com.sharedhouse.android.ui.home.HouseholdDashboardUiModel
import com.sharedhouse.android.ui.home.HouseholdDestination
import com.sharedhouse.android.ui.home.HouseholdHubScreen
import com.sharedhouse.android.ui.home.HouseholdHubUiModel
import com.sharedhouse.android.ui.home.HouseholdOptionUi
import com.sharedhouse.android.ui.home.HouseholdNavigationShell
import com.sharedhouse.android.ui.home.UnavailableHouseholdFeature
import com.sharedhouse.android.ui.home.UnavailableHouseholdFeatureScreen
import com.sharedhouse.android.ui.money.MoneyScreen
import com.sharedhouse.android.ui.tasks.TasksScreen
import com.sharedhouse.android.ui.tasks.TaskFilter
import com.sharedhouse.android.ui.invitations.InvitationJoinScreen
import com.sharedhouse.android.ui.invitations.InvitationManagerScreen
import com.sharedhouse.android.ui.onboarding.HouseholdSetupScreen
import com.sharedhouse.android.ui.settings.SettingsRoute
import com.sharedhouse.android.ui.settings.HouseholdCreatorSettingsScreen
import java.time.LocalDate
import java.time.LocalTime
import java.text.NumberFormat
import java.util.Currency
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
                    style = AtmosphereTheme.typography.bodyLarge,
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
    CHAT,
    HOUSEHOLD_SETTINGS,
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
    LifecycleStartEffect(viewModel, state.selectedHousehold?.id) {
        viewModel.startLiveSync()
        onStopOrDispose { viewModel.stopLiveSync() }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val household = state.selectedHousehold
    if (household == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val reminderScheduler = remember(context) { HouseholdReminderScheduler(context) }
    LaunchedEffect(
        household.id,
        household.timezone,
        appPreferences.notifications,
        state.money.content,
        state.tasks.content,
    ) {
        reminderScheduler.replaceScheduledReminders(
            householdId = household.id,
            zoneId = runCatching { java.time.ZoneId.of(household.timezone) }
                .getOrDefault(java.time.ZoneId.systemDefault()),
            preferences = appPreferences.notifications,
            expenses = (state.money.content as? com.sharedhouse.android.ui.money.MoneyContent.Ready)
                ?.expenses.orEmpty(),
            tasks = (state.tasks.content as? com.sharedhouse.android.ui.tasks.TasksContent.Ready)
                ?.tasks.orEmpty(),
        )
    }

    var selectedDestinationName by rememberSaveable {
        mutableStateOf(HouseholdDestination.HOME.name)
    }
    var secondarySurfaceName by rememberSaveable { mutableStateOf(SecondarySurface.NONE.name) }
    var guideTopicName by rememberSaveable { mutableStateOf(GuideTopic.GETTING_STARTED.name) }
    var articleParentName by rememberSaveable { mutableStateOf(SecondarySurface.GUIDES.name) }
    var tasksInitialFilterName by rememberSaveable { mutableStateOf(TaskFilter.MY_TASKS.name) }

    val selectedDestination = runCatching {
        HouseholdDestination.valueOf(selectedDestinationName)
    }.getOrDefault(HouseholdDestination.HOME)
    val secondarySurface = runCatching {
        SecondarySurface.valueOf(secondarySurfaceName)
    }.getOrDefault(SecondarySurface.NONE)
    val guideTopic = runCatching { GuideTopic.valueOf(guideTopicName) }
        .getOrDefault(GuideTopic.GETTING_STARTED)
    var lastObservedChatMessageId by rememberSaveable(household.id) { mutableStateOf<String?>(null) }
    val newestChatMessage = state.chat.messages.lastOrNull()
    LaunchedEffect(newestChatMessage?.id, secondarySurface) {
        val message = newestChatMessage ?: return@LaunchedEffect
        val previous = lastObservedChatMessageId
        if (previous != null && previous != message.id && !message.isCurrentUser && secondarySurface != SecondarySurface.CHAT) {
            SharedHouseNotifications.postChatMessage(context, appPreferences.notifications)
        }
        lastObservedChatMessageId = message.id
    }

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
            SecondarySurface.HOUSEHOLD_SETTINGS -> HouseholdCreatorSettingsScreen(
                householdName = household.name,
                role = household.role,
                countryCode = household.countryCode,
                timezone = household.timezone,
                currency = household.currency,
                firstDayOfWeek = household.firstDayOfWeek,
                cycleType = household.cycleType,
                cycleAnchor = household.cycleAnchor,
                onBack = { openSecondary(SecondarySurface.NONE) },
                onEditHousehold = viewModel::openHouseholdEditor,
                onManageMembers = { openSecondary(SecondarySurface.NONE); selectedDestinationName = HouseholdDestination.HOUSE.name },
                onManageInvitations = viewModel::openInvitationManager,
                onManageFinance = { openSecondary(SecondarySurface.NONE); selectedDestinationName = HouseholdDestination.MONEY.name },
                onManageChores = { openSecondary(SecondarySurface.NONE); selectedDestinationName = HouseholdDestination.TASKS.name },
                onOpenCalendar = { openSecondary(SecondarySurface.NONE); selectedDestinationName = HouseholdDestination.CALENDAR.name },
                onOpenUserNotifications = { openSecondary(SecondarySurface.SETTINGS) },
            )

            SecondarySurface.CHAT -> {
                LifecycleStartEffect(viewModel, household.id) {
                    viewModel.startChatLive()
                    onStopOrDispose { viewModel.stopChatLive() }
                }
                HouseholdChatScreen(
                    state = state.chat,
                    onBack = { openSecondary(SecondarySurface.NONE) },
                    onDraftChanged = viewModel::updateChatDraft,
                    onSend = viewModel::sendChatMessage,
                    onRetry = viewModel::retryChat,
                )
            }

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
                accountExport = state.accountExport,
                onExportAccount = viewModel::exportAccount,
                onAccountExportHandled = viewModel::accountExportHandled,
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
                            style = AtmosphereTheme.typography.labelMedium,
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
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
                        tasks = state.tasks.toDashboardTasks(),
                        money = state.money.toDashboardMoney(),
                    ),
                    chat = state.chat,
                    onOpenChat = { openSecondary(SecondarySurface.CHAT) },
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
                        tasksInitialFilterName = TaskFilter.ACTIVE.name
                        selectedDestinationName = HouseholdDestination.TASKS.name
                    },
                    onOpenRequests = {
                        tasksInitialFilterName = TaskFilter.REQUESTS.name
                        selectedDestinationName = HouseholdDestination.TASKS.name
                    },
                )

                HouseholdDestination.CALENDAR -> CalendarScreen(
                    state = state.calendar.withHouseholdItems(state),
                    onAction = viewModel::handleCalendarAction,
                )

                HouseholdDestination.MONEY -> MoneyScreen(
                    state = state.money,
                    onAction = viewModel::handleMoneyAction,
                )

                HouseholdDestination.TASKS -> TasksScreen(
                    state = state.tasks,
                    onAction = viewModel::handleTasksAction,
                    initialFilter = runCatching { TaskFilter.valueOf(tasksInitialFilterName) }
                        .getOrDefault(TaskFilter.MY_TASKS),
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
                        memberState = state.householdMembers,
                    ),
                    onEditHousehold = viewModel::openHouseholdEditor,
                    onManageInvitations = viewModel::openInvitationManager,
                    onManageCosts = {
                        selectedDestinationName = HouseholdDestination.MONEY.name
                    },
                    onScheduleTasks = {
                        selectedDestinationName = HouseholdDestination.TASKS.name
                    },
                    onJoinHousehold = viewModel::openInvitationJoin,
                    onSelectHousehold = viewModel::selectHousehold,
                    onOpenSettings = { openSecondary(SecondarySurface.SETTINGS) },
                    onOpenHouseholdSettings = { openSecondary(SecondarySurface.HOUSEHOLD_SETTINGS) },
                    onOpenGuides = { openSecondary(SecondarySurface.GUIDES) },
                    onSignOut = viewModel::signOut,
                    onRetryMembers = viewModel::retryHouseholdMembers,
                    onMemberAction = viewModel::actOnHouseholdMember,
                )
            }
        }
    }
}

private fun com.sharedhouse.android.ui.calendar.CalendarUiState.withHouseholdItems(
    appState: AppUiState,
): com.sharedhouse.android.ui.calendar.CalendarUiState {
    val ready = content as? CalendarContent.Ready ?: return this
    val moneyEvents = (appState.money.content as? com.sharedhouse.android.ui.money.MoneyContent.Ready)
        ?.expenses.orEmpty()
        .filter { it.status != com.sharedhouse.android.ui.money.ExpenseStatus.REVERSED }
        .map { expense ->
            val amount = runCatching {
                NumberFormat.getCurrencyInstance().apply {
                    currency = Currency.getInstance(expense.currency)
                }.format(expense.amountMinor / 100.0)
            }.getOrDefault("${expense.currency} ${expense.amountMinor}")
            CalendarEventUi(
                id = "expense:${expense.id}",
                title = expense.title,
                description = amount,
                type = CalendarEventType.MONEY,
                date = expense.dueDate,
                version = expense.version,
            )
        }
    val taskEvents = (appState.tasks.content as? com.sharedhouse.android.ui.tasks.TasksContent.Ready)
        ?.tasks.orEmpty()
        .filter { it.status != com.sharedhouse.android.ui.tasks.TaskStatus.CANCELLED }
        .map { task ->
            CalendarEventUi(
                id = "task:${task.id}",
                title = task.title,
                description = task.assigneeDisplayName,
                type = CalendarEventType.TASK,
                date = task.dueDate,
                startTime = task.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
                version = task.version,
            )
        }
    return copy(
        content = CalendarContent.Ready(
            (ready.events + moneyEvents + taskEvents)
                .distinctBy(CalendarEventUi::id)
                .sortedWith(compareBy(CalendarEventUi::date).thenBy { it.startTime }),
        ),
    )
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

private fun com.sharedhouse.android.ui.tasks.TasksUiState.toDashboardTasks(): DashboardTasksContent =
    when (val current = content) {
        com.sharedhouse.android.ui.tasks.TasksContent.Loading -> DashboardTasksContent.Loading
        com.sharedhouse.android.ui.tasks.TasksContent.Error -> DashboardTasksContent.Error
        is com.sharedhouse.android.ui.tasks.TasksContent.Ready -> {
            val active = current.tasks.filter { it.status == com.sharedhouse.android.ui.tasks.TaskStatus.OPEN || it.status == com.sharedhouse.android.ui.tasks.TaskStatus.IN_PROGRESS }
            DashboardTasksContent.Ready(
                nextMineTitle = active.filter { it.isMine }.minByOrNull { it.dueDate }?.title,
                activeCount = active.size,
                pendingRequests = current.tasks.sumOf { task -> task.requests.count { it.status == com.sharedhouse.android.ui.tasks.TaskRequestStatus.PENDING } },
            )
        }
    }

private fun com.sharedhouse.android.ui.money.MoneyUiState.toDashboardMoney(): DashboardMoneyContent =
    when (val current = content) {
        com.sharedhouse.android.ui.money.MoneyContent.Loading -> DashboardMoneyContent.Loading
        com.sharedhouse.android.ui.money.MoneyContent.Error -> DashboardMoneyContent.Error
        is com.sharedhouse.android.ui.money.MoneyContent.Ready -> {
            val active = current.expenses.filter {
                it.status == com.sharedhouse.android.ui.money.ExpenseStatus.APPROVED
            }
            val outstanding = active.flatMap { it.allocations }.filter {
                it.isCurrentUser && it.status != com.sharedhouse.android.ui.money.ExpenseAllocationStatus.PAID
            }
            DashboardMoneyContent.Ready(
                amountDueMinor = outstanding.sumOf { it.amountMinor },
                currency = currency,
                outstandingCount = outstanding.size,
            )
        }
    }
