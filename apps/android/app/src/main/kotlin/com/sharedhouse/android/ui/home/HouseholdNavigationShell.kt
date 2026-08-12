package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.NavigationBar
import com.sharedhouse.android.ui.atmosphere.NavigationBarItem
import com.sharedhouse.android.ui.atmosphere.NavigationBarItemDefaults
import com.sharedhouse.android.ui.atmosphere.NavigationRail
import com.sharedhouse.android.ui.atmosphere.NavigationRailItem
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Text
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
import com.sharedhouse.android.ui.icons.SharedHouseIcons

enum class HouseholdDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, SharedHouseIcons.Home),
    CALENDAR(R.string.nav_calendar, SharedHouseIcons.Calendar),
    MONEY(R.string.nav_money, SharedHouseIcons.Money),
    TASKS(R.string.nav_tasks, SharedHouseIcons.Tasks),
    HOUSE(R.string.nav_house, SharedHouseIcons.House),
}

/**
 * Responsive custom atmospheric navigation shared by authenticated household surfaces. Phone layouts use
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
                containerColor = AtmosphereTheme.colorScheme.background,
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
        containerColor = AtmosphereTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        HouseholdDestination.entries.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(22.dp).padding(bottom = 1.dp)) },
                label = {
                    Text(
                        text = label,
                        style = AtmosphereTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AtmosphereTheme.colorScheme.primaryContainer,
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
            .semantics {
                contentDescription = navigationDescription
                isTraversalGroup = true
            },
        containerColor = AtmosphereTheme.colorScheme.surfaceContainer,
    ) {
        HouseholdDestination.entries.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationRailItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(label, maxLines = 1, style = AtmosphereTheme.typography.labelSmall) },
            )
        }
    }
}
