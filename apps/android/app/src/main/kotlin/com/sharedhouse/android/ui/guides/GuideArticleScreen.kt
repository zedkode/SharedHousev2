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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideArticleScreen(
    topic: GuideTopic,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val article = articleFor(topic)
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
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(article.introduction),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(point),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
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
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
