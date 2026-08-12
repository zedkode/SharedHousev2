package com.sharedhouse.android.ui.tutorial

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.LinearProgressIndicator
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.OutlinedButton
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TextButton
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.preferences.AppPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class TutorialPage(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val detail: Int,
)

private val tutorialPages = listOf(
    TutorialPage(
        icon = Icons.Outlined.HomeWork,
        title = R.string.tutorial_home_title,
        description = R.string.tutorial_home_description,
        detail = R.string.tutorial_home_detail,
    ),
    TutorialPage(
        icon = Icons.Outlined.CalendarMonth,
        title = R.string.tutorial_calendar_title,
        description = R.string.tutorial_calendar_description,
        detail = R.string.tutorial_calendar_detail,
    ),
    TutorialPage(
        icon = Icons.Outlined.NotificationsActive,
        title = R.string.tutorial_notifications_title,
        description = R.string.tutorial_notifications_description,
        detail = R.string.tutorial_notifications_detail,
    ),
    TutorialPage(
        icon = Icons.Outlined.Shield,
        title = R.string.tutorial_control_title,
        description = R.string.tutorial_control_description,
        detail = R.string.tutorial_control_detail,
    ),
)

@Composable
fun TutorialRoute(
    repository: AppPreferencesRepository,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }
    var persistenceFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun finish(skipped: Boolean) {
        if (isSaving) return
        isSaving = true
        persistenceFailed = false
        scope.launch {
            try {
                repository.completeTutorial(skipped = skipped)
                onFinished()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                persistenceFailed = true
                isSaving = false
            }
        }
    }

    TutorialScreen(
        currentPage = currentPage,
        isSaving = isSaving,
        persistenceFailed = persistenceFailed,
        onBack = { currentPage = (currentPage - 1).coerceAtLeast(0) },
        onNext = { currentPage = (currentPage + 1).coerceAtMost(tutorialPages.lastIndex) },
        onSkip = { finish(true) },
        onGetStarted = { finish(false) },
        modifier = modifier,
    )
}

@Composable
fun TutorialScreen(
    currentPage: Int,
    isSaving: Boolean,
    persistenceFailed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safePage = currentPage.coerceIn(tutorialPages.indices)
    val page = tutorialPages[safePage]
    val isLastPage = safePage == tutorialPages.lastIndex

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tutorial_app_name)) },
                actions = {
                    TextButton(onClick = onSkip, enabled = !isSaving) {
                        Text(stringResource(R.string.tutorial_skip))
                    }
                },
            )
        },
        containerColor = AtmosphereTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LinearProgressIndicator(
                    progress = { (safePage + 1f) / tutorialPages.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(
                        R.string.tutorial_step_count,
                        safePage + 1,
                        tutorialPages.size,
                    ),
                    style = AtmosphereTheme.typography.labelLarge,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(AtmosphereTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = AtmosphereTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(page.title),
                        style = AtmosphereTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(page.description),
                        style = AtmosphereTheme.typography.bodyLarge,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AtmosphereTheme.colorScheme.surfaceContainer,
                    ),
                    border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = stringResource(page.detail),
                        modifier = Modifier.padding(18.dp),
                        style = AtmosphereTheme.typography.bodyMedium,
                    )
                }
                if (persistenceFailed) {
                    Text(
                        text = stringResource(R.string.tutorial_save_error),
                        color = AtmosphereTheme.colorScheme.error,
                        style = AtmosphereTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = safePage > 0 && !isSaving,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text(stringResource(R.string.tutorial_back))
                    }
                    Button(
                        onClick = if (isLastPage) onGetStarted else onNext,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (isLastPage) {
                                        R.string.tutorial_get_started
                                    } else {
                                        R.string.tutorial_next
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
