package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R

enum class HouseholdDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Outlined.Home),
    CALENDAR(R.string.nav_calendar, Icons.Outlined.CalendarMonth),
    MONEY(R.string.nav_money, Icons.Outlined.AccountBalanceWallet),
    TASKS(R.string.nav_tasks, Icons.Outlined.Checklist),
    HOUSE(R.string.nav_house, Icons.Outlined.Groups),
}

/**
 * Responsive Material 3 navigation shared by authenticated household surfaces. Phone layouts use
 * the five-destination navigation bar; larger layouts keep content visible beside a rail.
 */
@Composable
fun HouseholdNavigationShell(
    selectedDestination: HouseholdDestination,
    onDestinationSelected: (HouseholdDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                HouseholdNavigationRail(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                )
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    HouseholdNavigationBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = onDestinationSelected,
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HouseholdNavigationBar(
    selectedDestination: HouseholdDestination,
    onDestinationSelected: (HouseholdDestination) -> Unit,
) {
    val navigationDescription = stringResource(R.string.main_navigation)
    NavigationBar(
        modifier = Modifier.semantics {
            contentDescription = navigationDescription
            isTraversalGroup = true
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        HouseholdDestination.entries.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
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

@Composable
private fun HouseholdNavigationRail(
    selectedDestination: HouseholdDestination,
    onDestinationSelected: (HouseholdDestination) -> Unit,
) {
    val navigationDescription = stringResource(R.string.main_navigation)
    NavigationRail(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .semantics {
                contentDescription = navigationDescription
                isTraversalGroup = true
            },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        HouseholdDestination.entries.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationRailItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}
