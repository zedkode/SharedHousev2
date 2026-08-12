package com.sharedhouse.android.ui.guides

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R

private data class GuideArticle(
    @StringRes val title: Int,
    @StringRes val introduction: Int,
    val points: List<Int>,
    @StringRes val note: Int,
)

private fun articleFor(topic: GuideTopic): GuideArticle = when (topic) {
    GuideTopic.GETTING_STARTED -> GuideArticle(
        R.string.guide_getting_started_title,
        R.string.guide_article_getting_started_intro,
        listOf(
            R.string.guide_article_getting_started_point_1,
            R.string.guide_article_getting_started_point_2,
            R.string.guide_article_getting_started_point_3,
        ),
        R.string.guide_article_getting_started_note,
    )
    GuideTopic.CALENDAR -> GuideArticle(
        R.string.guide_calendar_title,
        R.string.guide_article_calendar_intro,
        listOf(
            R.string.guide_article_calendar_point_1,
            R.string.guide_article_calendar_point_2,
            R.string.guide_article_calendar_point_3,
        ),
        R.string.guide_article_calendar_note,
    )
    GuideTopic.MONEY -> GuideArticle(
        R.string.guide_money_title,
        R.string.guide_article_money_intro,
        listOf(
            R.string.guide_article_money_point_1,
            R.string.guide_article_money_point_2,
            R.string.guide_article_money_point_3,
            R.string.guide_article_money_point_4,
            R.string.guide_article_money_point_5,
        ),
        R.string.guide_article_money_note,
    )
    GuideTopic.TASKS -> GuideArticle(
        R.string.guide_tasks_title,
        R.string.guide_article_tasks_intro,
        listOf(
            R.string.guide_article_tasks_point_1,
            R.string.guide_article_tasks_point_2,
            R.string.guide_article_tasks_point_3,
            R.string.guide_article_tasks_point_4,
            R.string.guide_article_tasks_point_5,
        ),
        R.string.guide_article_tasks_note,
    )
    GuideTopic.HOUSEHOLD_ADMIN -> GuideArticle(
        R.string.guide_admin_title,
        R.string.guide_article_admin_intro,
        listOf(
            R.string.guide_article_admin_point_1,
            R.string.guide_article_admin_point_2,
            R.string.guide_article_admin_point_3,
            R.string.guide_article_admin_point_4,
            R.string.guide_article_admin_point_5,
        ),
        R.string.guide_article_admin_note,
    )
    GuideTopic.CHAT -> GuideArticle(
        R.string.guide_chat_title,
        R.string.guide_article_chat_intro,
        listOf(
            R.string.guide_article_chat_point_1,
            R.string.guide_article_chat_point_2,
            R.string.guide_article_chat_point_3,
            R.string.guide_article_chat_point_4,
            R.string.guide_article_chat_point_5,
        ),
        R.string.guide_article_chat_note,
    )
    GuideTopic.NOTIFICATIONS -> GuideArticle(
        R.string.guide_notifications_title,
        R.string.guide_article_notifications_intro,
        listOf(
            R.string.guide_article_notifications_point_1,
            R.string.guide_article_notifications_point_2,
            R.string.guide_article_notifications_point_3,
        ),
        R.string.guide_article_notifications_note,
    )
    GuideTopic.FEATURE_AVAILABILITY -> GuideArticle(
        R.string.guide_availability_title,
        R.string.guide_article_availability_intro,
        listOf(
            R.string.guide_article_availability_point_1,
            R.string.guide_article_availability_point_2,
            R.string.guide_article_availability_point_3,
        ),
        R.string.guide_article_availability_note,
    )
    GuideTopic.PRIVACY -> GuideArticle(
        R.string.guide_privacy_title,
        R.string.guide_article_privacy_intro,
        listOf(
            R.string.guide_article_privacy_point_1,
            R.string.guide_article_privacy_point_2,
            R.string.guide_article_privacy_point_3,
        ),
        R.string.guide_article_privacy_note,
    )
    GuideTopic.SECURITY -> GuideArticle(
        R.string.guide_security_title,
        R.string.guide_article_security_intro,
        listOf(
            R.string.guide_article_security_point_1,
            R.string.guide_article_security_point_2,
            R.string.guide_article_security_point_3,
        ),
        R.string.guide_article_security_note,
    )
    GuideTopic.LEGAL -> GuideArticle(
        R.string.guide_legal_title,
        R.string.guide_article_legal_intro,
        listOf(
            R.string.guide_article_legal_point_1,
            R.string.guide_article_legal_point_2,
            R.string.guide_article_legal_point_3,
        ),
        R.string.guide_article_legal_note,
    )
}

@Composable
fun GuideArticleScreen(
    topic: GuideTopic,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val article = articleFor(topic)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AtmosphereTheme.colorScheme.background,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(article.title),
                        style = AtmosphereTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(article.introduction),
                        style = AtmosphereTheme.typography.bodyLarge,
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        article.points.forEach { point ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = AtmosphereTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(point),
                                    modifier = Modifier.weight(1f),
                                    style = AtmosphereTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AtmosphereTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                        Text(
                            text = stringResource(article.note),
                            modifier = Modifier.weight(1f),
                            style = AtmosphereTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
