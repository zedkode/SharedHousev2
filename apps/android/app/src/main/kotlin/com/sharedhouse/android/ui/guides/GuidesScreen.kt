package com.sharedhouse.android.ui.guides

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R

enum class GuideTopic {
    GETTING_STARTED,
    CALENDAR,
    NOTIFICATIONS,
    FEATURE_AVAILABILITY,
    PRIVACY,
    SECURITY,
    LEGAL,
}

private enum class GuideAvailability {
    AVAILABLE,
    HOUSEHOLD_REQUIRED,
    PARTIAL,
    INFORMATION,
}

private data class GuideEntry(
    val topic: GuideTopic,
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val availability: GuideAvailability,
)

private val guides = listOf(
    GuideEntry(
        GuideTopic.GETTING_STARTED,
        Icons.Outlined.HomeWork,
        R.string.guide_getting_started_title,
        R.string.guide_getting_started_description,
        GuideAvailability.AVAILABLE,
    ),
    GuideEntry(
        GuideTopic.CALENDAR,
        Icons.Outlined.CalendarMonth,
        R.string.guide_calendar_title,
        R.string.guide_calendar_description,
        GuideAvailability.HOUSEHOLD_REQUIRED,
    ),
    GuideEntry(
        GuideTopic.NOTIFICATIONS,
        Icons.Outlined.NotificationsActive,
        R.string.guide_notifications_title,
        R.string.guide_notifications_description,
        GuideAvailability.PARTIAL,
    ),
    GuideEntry(
        GuideTopic.FEATURE_AVAILABILITY,
        Icons.Outlined.Info,
        R.string.guide_availability_title,
        R.string.guide_availability_description,
        GuideAvailability.INFORMATION,
    ),
    GuideEntry(
        GuideTopic.PRIVACY,
        Icons.Outlined.PrivacyTip,
        R.string.guide_privacy_title,
        R.string.guide_privacy_description,
        GuideAvailability.INFORMATION,
    ),
    GuideEntry(
        GuideTopic.SECURITY,
        Icons.Outlined.Lock,
        R.string.guide_security_title,
        R.string.guide_security_description,
        GuideAvailability.INFORMATION,
    ),
    GuideEntry(
        GuideTopic.LEGAL,
        Icons.AutoMirrored.Outlined.Rule,
        R.string.guide_legal_title,
        R.string.guide_legal_description,
        GuideAvailability.INFORMATION,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidesScreen(
    onBack: () -> Unit,
    onOpenTopic: (GuideTopic) -> Unit,
    modifier: Modifier = Modifier,
    sponsoredContent: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.guide_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.guide_heading),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.guide_heading_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            guides.forEach { guide ->
                item(key = guide.topic.name) {
                    GuideCard(guide = guide, onClick = { onOpenTopic(guide.topic) })
                }
            }
            item(key = "sponsored_content") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sponsoredContent()
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    guide: GuideEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = guide.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = stringResource(guide.title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(guide.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            stringResource(
                                when (guide.availability) {
                                    GuideAvailability.AVAILABLE -> R.string.guide_status_available
                                    GuideAvailability.HOUSEHOLD_REQUIRED -> R.string.guide_status_household_required
                                    GuideAvailability.PARTIAL -> R.string.guide_status_partial
                                    GuideAvailability.INFORMATION -> R.string.guide_status_information
                                },
                            ),
                        )
                    },
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
