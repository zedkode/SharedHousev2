package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.theme.SharedHouseTheme

private enum class TopLevelDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.nav_home, Icons.Outlined.Home),
    Calendar(R.string.nav_calendar, Icons.Outlined.CalendarMonth),
    Money(R.string.nav_money, Icons.Outlined.AccountBalanceWallet),
    Tasks(R.string.nav_tasks, Icons.Outlined.Checklist),
    House(R.string.nav_house, Icons.Outlined.Groups),
}

@Composable
fun SharedHouseHome(
    snackbarHostState: SnackbarHostState,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: HomeFoundationState = HomeFoundationState.empty(),
    onProfileClick: (() -> Unit)? = null,
    onHouseholdClick: (() -> Unit)? = null,
) {
    val setupUnavailableMessage = stringResource(R.string.setup_not_available)
    val switchingUnavailableMessage = stringResource(R.string.household_switching_not_available)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                householdName = state.householdName,
                accountDisplayName = state.accountDisplayName,
                onHouseholdClick = {
                    if (state.isHouseholdConfigured && onHouseholdClick != null) {
                        onHouseholdClick()
                    } else {
                        onMessage(
                            if (state.isHouseholdConfigured) {
                                switchingUnavailableMessage
                            } else {
                                setupUnavailableMessage
                            },
                        )
                    }
                },
                onProfileClick = onProfileClick ?: { onMessage(setupUnavailableMessage) },
            )
        },
        bottomBar = { HomeBottomNavigation(onMessage = onMessage) },
    ) { innerPadding ->
        HomeContent(
            state = state,
            onStartSetup = { onMessage(setupUnavailableMessage) },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun HomeTopBar(
    householdName: String?,
    accountDisplayName: String?,
    onHouseholdClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val householdSelectorDescription = if (householdName.isNullOrBlank()) {
        stringResource(R.string.household_selector_description)
    } else {
        stringResource(R.string.household_selector_current_description, householdName)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TextButton(
                        onClick = onHouseholdClick,
                        modifier = Modifier.semantics {
                            contentDescription = householdSelectorDescription
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = householdName?.takeIf(String::isNotBlank)
                                ?: stringResource(R.string.household_not_configured),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = accountDisplayName
                            ?.takeIf(String::isNotBlank)
                            ?.let { stringResource(R.string.profile_current_description, it) }
                            ?: stringResource(R.string.profile_description),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeFoundationState,
    onStartSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            SetupCard(
                state = state,
                onStartSetup = onStartSetup,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(text = stringResource(R.string.next_up_title))
                FoundationStatusCard(
                    title = stringResource(R.string.bills_and_payments),
                    status = stringResource(R.string.nothing_scheduled),
                    icon = Icons.Outlined.CreditCard,
                )
                FoundationStatusCard(
                    title = stringResource(R.string.household_tasks),
                    status = stringResource(R.string.no_tasks_assigned),
                    icon = Icons.Outlined.TaskAlt,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(text = stringResource(R.string.requests_title))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = stringResource(
                                if (state.isHouseholdConfigured) {
                                    R.string.requests_empty_connected
                                } else {
                                    R.string.requests_empty
                                },
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(
    state: HomeFoundationState,
    onStartSetup: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isHouseholdConfigured) {
                            Icons.Outlined.CheckCircleOutline
                        } else {
                            Icons.Outlined.Home
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = stringResource(
                    if (state.isHouseholdConfigured) {
                        R.string.household_connected_title
                    } else {
                        R.string.setup_title
                    },
                ),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = if (state.isHouseholdConfigured) {
                    stringResource(
                        R.string.household_connected_description,
                        state.householdName.orEmpty(),
                    )
                } else {
                    stringResource(R.string.setup_description)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HonestEmptyRow(text = stringResource(R.string.setup_no_bills))
                HonestEmptyRow(text = stringResource(R.string.setup_no_tasks))
                HonestEmptyRow(
                    text = stringResource(
                        if (state.isHouseholdConfigured) {
                            R.string.household_activity_not_connected
                        } else {
                            R.string.setup_no_members
                        },
                    ),
                )
            }

            if (!state.isHouseholdConfigured) {
                Button(
                    onClick = onStartSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(text = stringResource(R.string.start_setup))
                }
            }
        }
    }
}

@Composable
private fun HonestEmptyRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun FoundationStatusCard(
    title: String,
    status: String,
    icon: ImageVector,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = title.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = status,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun HomeBottomNavigation(onMessage: (String) -> Unit) {
    val destinations = TopLevelDestination.entries
    val navigationDescription = stringResource(R.string.main_navigation)
    val unavailableTemplate = stringResource(R.string.foundation_not_available)

    NavigationBar(
        modifier = Modifier.semantics {
            contentDescription = navigationDescription
            isTraversalGroup = true
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        destinations.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = destination == TopLevelDestination.Home,
                onClick = {
                    if (destination != TopLevelDestination.Home) {
                        onMessage(unavailableTemplate.format(label))
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SharedHouseHomePreview() {
    SharedHouseTheme {
        SharedHouseHome(
            snackbarHostState = SnackbarHostState(),
            onMessage = {},
        )
    }
}
