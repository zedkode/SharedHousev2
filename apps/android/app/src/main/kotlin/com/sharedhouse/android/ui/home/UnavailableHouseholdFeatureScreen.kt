package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.FilledTonalButton
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.Surface
import com.sharedhouse.android.ui.atmosphere.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R

/** Honest information surface for feature areas that do not yet have an authoritative data source. */
@Composable
fun UnavailableHouseholdFeatureScreen(
    feature: UnavailableHouseholdFeature,
    onOpenCalendar: () -> Unit,
    onOpenGuides: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = unavailableFeatureContent(feature)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AtmosphereTheme.colorScheme.surfaceContainer,
                    shape = AtmosphereTheme.shapes.extraLarge,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            color = AtmosphereTheme.colorScheme.primaryContainer,
                            contentColor = AtmosphereTheme.colorScheme.onPrimaryContainer,
                            shape = AtmosphereTheme.shapes.large,
                        ) {
                            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                Icon(content.icon, contentDescription = null)
                            }
                        }
                        Text(
                            text = stringResource(content.title),
                            style = AtmosphereTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(content.description),
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                            style = AtmosphereTheme.typography.bodyLarge,
                        )
                        Surface(
                            color = AtmosphereTheme.colorScheme.surface,
                            shape = AtmosphereTheme.shapes.extraLarge,
                            border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.feature_not_connected_badge),
                                    style = AtmosphereTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.feature_what_you_can_do_title),
                            style = AtmosphereTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        GuidanceRow(stringResource(content.guidanceOne))
                        GuidanceRow(stringResource(content.guidanceTwo))
                        GuidanceRow(stringResource(content.guidanceThree))
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AtmosphereTheme.colorScheme.tertiaryContainer,
                    contentColor = AtmosphereTheme.colorScheme.onTertiaryContainer,
                    shape = AtmosphereTheme.shapes.extraLarge,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.feature_honest_state_title),
                                style = AtmosphereTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(content.honestState),
                                style = AtmosphereTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                        Text(
                            text = stringResource(R.string.feature_open_calendar),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    }
                    FilledTonalButton(onClick = onOpenGuides, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                        Text(
                            text = stringResource(R.string.feature_open_guides),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Outlined.CheckCircleOutline,
            contentDescription = null,
            tint = AtmosphereTheme.colorScheme.primary,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            style = AtmosphereTheme.typography.bodyMedium,
        )
    }
}

private data class UnavailableFeatureContent(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val guidanceOne: Int,
    @StringRes val guidanceTwo: Int,
    @StringRes val guidanceThree: Int,
    @StringRes val honestState: Int,
    val icon: ImageVector,
)

private fun unavailableFeatureContent(feature: UnavailableHouseholdFeature) = when (feature) {
    UnavailableHouseholdFeature.MONEY -> UnavailableFeatureContent(
        title = R.string.feature_money_title,
        description = R.string.feature_money_description,
        guidanceOne = R.string.feature_money_guidance_one,
        guidanceTwo = R.string.feature_money_guidance_two,
        guidanceThree = R.string.feature_money_guidance_three,
        honestState = R.string.feature_money_honest_state,
        icon = Icons.Outlined.AccountBalanceWallet,
    )

    UnavailableHouseholdFeature.TASKS -> UnavailableFeatureContent(
        title = R.string.feature_tasks_title,
        description = R.string.feature_tasks_description,
        guidanceOne = R.string.feature_tasks_guidance_one,
        guidanceTwo = R.string.feature_tasks_guidance_two,
        guidanceThree = R.string.feature_tasks_guidance_three,
        honestState = R.string.feature_tasks_honest_state,
        icon = Icons.Outlined.Checklist,
    )
}
