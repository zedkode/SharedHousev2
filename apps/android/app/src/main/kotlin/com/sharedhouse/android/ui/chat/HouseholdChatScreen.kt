package com.sharedhouse.android.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.atmosphere.OutlinedTextField
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Surface
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import com.sharedhouse.android.ui.atmosphere.AlertDialog
import com.sharedhouse.android.ui.atmosphere.TextButton
import com.sharedhouse.android.ui.icons.SharedHouseIcons
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val MessageGroupingWindow: Duration = Duration.ofMinutes(5)

@Composable
fun HouseholdChatScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onPinMessage: (String,Boolean) -> Unit = { _,_ -> },
    onCreateEventFromMessage: (ChatMessageUi) -> Unit = {},
    onPickPhotos: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onShareLocation: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val zoneId = remember { ZoneId.systemDefault() }
    val timeline = remember(state.messages, zoneId) { buildTimeline(state.messages, zoneId) }
    val motionEnabled = AtmosphereTheme.motionEnabled
    var previousTimelineSize by remember { mutableIntStateOf(0) }
    var selectedMessage by remember { mutableStateOf<ChatMessageUi?>(null) }

    LaunchedEffect(timeline.size, state.messages.lastOrNull()?.id) {
        if (timeline.isNotEmpty()) {
            val previousLastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val wasNearLatest = previousTimelineSize == 0 || previousLastVisible >= previousTimelineSize - 2
            val ownMessageConfirmed = state.messages.lastOrNull()?.isCurrentUser == true
            if (wasNearLatest || ownMessageConfirmed) {
                if (motionEnabled && previousTimelineSize > 0) {
                    listState.animateScrollToItem(timeline.lastIndex)
                } else {
                    listState.scrollToItem(timeline.lastIndex)
                }
            }
        }
        previousTimelineSize = timeline.size
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.background(AtmosphereTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(ChatIcons.Back, stringResource(R.string.action_back), Modifier.size(23.dp))
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = AtmosphereTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.primary.copy(alpha = .35f)),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    SharedHouseIcons.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = AtmosphereTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(R.string.chat_title),
                                style = AtmosphereTheme.typography.titleLarge,
                            )
                            ChatStatus(state.connection)
                        }
                    }
                },
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(AtmosphereTheme.colorScheme.background),
        ) {
            if (state.pinnedMessages.isNotEmpty()) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = AtmosphereTheme.shapes.medium,
                    color = AtmosphereTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.chat_pinned_title),style=AtmosphereTheme.typography.labelLarge,color=AtmosphereTheme.colorScheme.primary)
                        state.pinnedMessages.take(5).forEach { Text(it.body.ifBlank { stringResource(R.string.chat_media_message) },maxLines=1,style=AtmosphereTheme.typography.bodySmall) }
                    }
                }
            }
            if (state.problem != null && state.messages.isNotEmpty()) {
                ChatProblemBanner(state.problem, onRetry)
            }

            when {
                state.messages.isEmpty() && state.problem == ChatProblem.LOAD_FAILED -> {
                    ChatUnavailableState(onRetry, Modifier.weight(1f))
                }

                state.messages.isEmpty() && state.connection == ChatConnection.CONNECTING -> {
                    ChatLoadingState(Modifier.weight(1f))
                }

                state.messages.isEmpty() -> {
                    ChatEmptyState(Modifier.weight(1f))
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(timeline, key = ChatTimelineItem::key) { item ->
                            when (item) {
                                is ChatTimelineItem.Day -> DayDivider(item.date)
                                is ChatTimelineItem.Messages -> MessageGroup(item.group, zoneId,onLongPress={selectedMessage=it})
                            }
                        }
                    }
                }
            }

            ChatComposer(
                state = state,
                onDraftChanged = onDraftChanged,
                onSend = onSend,
                onPickPhotos=onPickPhotos,
                onTakePhoto=onTakePhoto,
                onShareLocation=onShareLocation,
            )
        }
    }
    selectedMessage?.let { message ->
        AlertDialog(onDismissRequest={selectedMessage=null},title={Text(stringResource(R.string.chat_message_actions))},text={Column {
            TextButton(onClick={onPinMessage(message.id,!message.isPinned);selectedMessage=null}){Text(stringResource(if(message.isPinned) R.string.chat_unpin else R.string.chat_pin))}
            TextButton(onClick={onCreateEventFromMessage(message);selectedMessage=null}){Text(stringResource(R.string.chat_create_event))}
        }},confirmButton={TextButton(onClick={selectedMessage=null}){Text(stringResource(R.string.chat_close_actions))}})
    }
}

@Composable
private fun ChatStatus(connection: ChatConnection) {
    val label = stringResource(
        when (connection) {
            ChatConnection.CONNECTING -> R.string.chat_connecting
            ChatConnection.LIVE -> R.string.chat_live
            ChatConnection.RECONNECTING -> R.string.chat_reconnecting
            ChatConnection.OFFLINE -> R.string.chat_offline
        },
    )
    val icon = when (connection) {
        ChatConnection.LIVE -> ChatIcons.Live
        ChatConnection.CONNECTING,
        ChatConnection.RECONNECTING,
        -> ChatIcons.Retry
        ChatConnection.OFFLINE -> ChatIcons.Offline
    }
    val accent = when (connection) {
        ChatConnection.LIVE -> AtmosphereTheme.colorScheme.statusPositive
        ChatConnection.CONNECTING,
        ChatConnection.RECONNECTING,
        -> AtmosphereTheme.colorScheme.statusAttention
        ChatConnection.OFFLINE -> AtmosphereTheme.colorScheme.statusNegative
    }

    Row(
        modifier = Modifier
            .wrapContentWidth()
            .background(accent.copy(alpha = .12f), CircleShape)
            .border(1.dp, accent.copy(alpha = .36f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = accent)
        Text(label, style = AtmosphereTheme.typography.labelSmall, color = accent)
    }
}

@Composable
private fun ChatProblemBanner(
    problem: ChatProblem,
    onRetry: () -> Unit,
) {
    val isSendFailure = problem == ChatProblem.SEND_FAILED
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        shape = AtmosphereTheme.shapes.large,
        color = AtmosphereTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.error.copy(alpha = .45f)),
        shadowElevation = 5.dp,
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = AtmosphereTheme.colorScheme.error.copy(alpha = .14f),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        ChatIcons.Offline,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = AtmosphereTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = stringResource(
                    if (isSendFailure) R.string.chat_send_failed else R.string.chat_load_failed,
                ),
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                style = AtmosphereTheme.typography.bodySmall,
                color = AtmosphereTheme.colorScheme.onErrorContainer,
            )
            RetryAction(
                onClick = onRetry,
                label = stringResource(
                    if (isSendFailure) R.string.chat_reconnect else R.string.action_retry,
                ),
            )
        }
    }
}

@Composable
private fun RetryAction(
    onClick: () -> Unit,
    label: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        color = AtmosphereTheme.colorScheme.error.copy(alpha = .14f),
        contentColor = AtmosphereTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ChatIcons.Retry, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = AtmosphereTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChatLoadingState(modifier: Modifier = Modifier) {
    CenteredStateCard(modifier) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = CircleShape,
                color = AtmosphereTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.primary.copy(alpha = .35f)),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        SharedHouseIcons.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = AtmosphereTheme.colorScheme.primary,
                    )
                }
            }
            CircularProgressIndicator(
                modifier = Modifier.size(92.dp),
                color = AtmosphereTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
        Text(
            stringResource(R.string.chat_connecting),
            style = AtmosphereTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.chat_connecting_description),
            style = AtmosphereTheme.typography.bodyMedium,
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatUnavailableState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredStateCard(modifier) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            color = AtmosphereTheme.colorScheme.errorContainer,
            border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.error.copy(alpha = .4f)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    ChatIcons.Offline,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = AtmosphereTheme.colorScheme.error,
                )
            }
        }
        Text(
            stringResource(R.string.chat_load_failed_title),
            style = AtmosphereTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.chat_load_failed),
            style = AtmosphereTheme.typography.bodyMedium,
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        RetryAction(onRetry, stringResource(R.string.action_retry))
    }
}

@Composable
private fun ChatEmptyState(modifier: Modifier = Modifier) {
    CenteredStateCard(modifier) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = RoundedCornerShape(28.dp, 28.dp, 10.dp, 28.dp),
            color = AtmosphereTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.primary.copy(alpha = .4f)),
            shadowElevation = 8.dp,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    SharedHouseIcons.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = AtmosphereTheme.colorScheme.primary,
                )
            }
        }
        Text(
            stringResource(R.string.chat_empty_title),
            style = AtmosphereTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.chat_empty_description),
            style = AtmosphereTheme.typography.bodyMedium,
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CenteredStateCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = AtmosphereTheme.shapes.extraLarge,
            color = AtmosphereTheme.colorScheme.cardLevel1.copy(alpha = .94f),
            border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
            shadowElevation = 10.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun DayDivider(date: LocalDate) {
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val label = when (date) {
        today -> stringResource(R.string.chat_today)
        today.minusDays(1) -> stringResource(R.string.chat_yesterday)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(
            Modifier.weight(1f).height(1.dp)
                .background(AtmosphereTheme.colorScheme.outlineVariant),
        )
        Surface(
            shape = CircleShape,
            color = AtmosphereTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
        ) {
            Text(
                label,
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = AtmosphereTheme.typography.labelSmall,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(
            Modifier.weight(1f).height(1.dp)
                .background(AtmosphereTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun MessageGroup(
    group: ChatMessageGroup,
    zoneId: ZoneId,
    onLongPress: (ChatMessageUi) -> Unit,
) {
    val senderLabel = if (group.isCurrentUser) {
        stringResource(R.string.chat_sender_you, group.senderDisplayName)
    } else {
        group.senderDisplayName
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (group.isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!group.isCurrentUser) {
            SenderAvatar(group.senderDisplayName)
            Spacer(Modifier.width(9.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (group.isCurrentUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                senderLabel,
                modifier = Modifier.padding(start = 7.dp, end = 7.dp, bottom = 2.dp),
                style = AtmosphereTheme.typography.labelSmall,
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            group.messages.forEachIndexed { index, message ->
                MessageBubble(
                    message = message,
                    senderLabel = senderLabel,
                    zoneId = zoneId,
                    position = index,
                    groupSize = group.messages.size,
                    onLongPress = { onLongPress(message) },
                )
            }
        }

        if (group.isCurrentUser) {
            Spacer(Modifier.width(9.dp))
            SenderAvatar(group.senderDisplayName)
        }
    }
}

@Composable
private fun SenderAvatar(displayName: String) {
    val palette = avatarPalette(displayName)
    val initials = remember(displayName) { initialsFor(displayName) }
    Surface(
        modifier = Modifier.size(34.dp).clearAndSetSemantics { },
        shape = CircleShape,
        color = palette.background,
        contentColor = palette.foreground,
        border = BorderStroke(1.dp, palette.foreground.copy(alpha = .24f)),
        shadowElevation = 4.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                initials,
                style = AtmosphereTheme.typography.labelSmall,
                color = palette.foreground,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun avatarPalette(displayName: String): AvatarPalette {
    val palettes = listOf(
        AvatarPalette(
            AtmosphereTheme.colorScheme.primaryContainer,
            AtmosphereTheme.colorScheme.onPrimaryContainer,
        ),
        AvatarPalette(
            AtmosphereTheme.colorScheme.secondaryContainer,
            AtmosphereTheme.colorScheme.onSecondaryContainer,
        ),
        AvatarPalette(
            AtmosphereTheme.colorScheme.tertiaryContainer,
            AtmosphereTheme.colorScheme.onTertiaryContainer,
        ),
        AvatarPalette(
            AtmosphereTheme.colorScheme.surfaceContainerHighest,
            AtmosphereTheme.colorScheme.onSurface,
        ),
    )
    return palettes[Math.floorMod(displayName.hashCode(), palettes.size)]
}

@Composable
private fun MessageBubble(
    message: ChatMessageUi,
    senderLabel: String,
    zoneId: ZoneId,
    position: Int,
    groupSize: Int,
    onLongPress: () -> Unit,
) {
    val time = remember(message.createdAt, zoneId) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(message.createdAt.atZone(zoneId))
    }
    val accessibilityLabel = stringResource(
        R.string.chat_message_accessibility,
        senderLabel,
        message.body,
        time,
    )
    val shape = messageBubbleShape(message.isCurrentUser, position, groupSize)
    val background = if (message.isCurrentUser) {
        Brush.linearGradient(
            listOf(
                AtmosphereTheme.colorScheme.heroStart,
                AtmosphereTheme.colorScheme.heroMiddle,
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                AtmosphereTheme.colorScheme.cardLevel2,
                AtmosphereTheme.colorScheme.surfaceContainerHigh,
            ),
        )
    }
    val bodyColor = if (message.isCurrentUser) Color.White else AtmosphereTheme.colorScheme.onSurface
    val timeColor = if (message.isCurrentUser) {
        Color.White.copy(alpha = .72f)
    } else {
        AtmosphereTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(.86f)
            .widthIn(max = 420.dp)
            .shadow(
                elevation = if (message.isCurrentUser) 7.dp else 4.dp,
                shape = shape,
                clip = false,
                ambientColor = AtmosphereTheme.colorScheme.heroStart.copy(alpha = .18f),
                spotColor = Color.Black.copy(alpha = .28f),
            )
            .background(background, shape)
            .border(
                width = 1.dp,
                color = if (message.isCurrentUser) {
                    Color.White.copy(alpha = .14f)
                } else {
                    AtmosphereTheme.colorScheme.outlineVariant
                },
                shape = shape,
            )
            .combinedClickable(onClick={},onLongClick=onLongPress)
            .clearAndSetSemantics { contentDescription = accessibilityLabel },
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if(message.body.isNotBlank()) Text(message.body, style = AtmosphereTheme.typography.bodyLarge, color = bodyColor)
            if(message.attachments.isNotEmpty()) {
                Surface(shape=RoundedCornerShape(16.dp),color=Color.Black.copy(alpha=.18f),border=BorderStroke(1.dp,Color.White.copy(alpha=.16f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text("🖼️",style=AtmosphereTheme.typography.headlineMedium); Text(pluralStringResource(R.plurals.chat_photo_count,message.attachments.size,message.attachments.size),color=bodyColor) }
                }
            }
            message.location?.let { location -> Text("📍 %.5f, %.5f".format(location.latitude,location.longitude),style=AtmosphereTheme.typography.bodyMedium,color=bodyColor) }
            if(message.isPinned) Text(stringResource(R.string.chat_pinned_by,message.pinnedByDisplayName.orEmpty()),style=AtmosphereTheme.typography.labelSmall,color=timeColor)
            Text(
                time,
                modifier = Modifier.align(Alignment.End),
                style = AtmosphereTheme.typography.labelSmall,
                color = timeColor,
            )
        }
    }
}

private fun messageBubbleShape(
    isCurrentUser: Boolean,
    position: Int,
    groupSize: Int,
): Shape {
    val first = position == 0
    val last = position == groupSize - 1
    val connected = 8.dp
    val rounded = 22.dp
    val tail = 6.dp
    return if (isCurrentUser) {
        RoundedCornerShape(
            topStart = rounded,
            topEnd = if (first) rounded else connected,
            bottomStart = rounded,
            bottomEnd = if (last) tail else connected,
        )
    } else {
        RoundedCornerShape(
            topStart = if (first) rounded else connected,
            topEnd = rounded,
            bottomStart = if (last) tail else connected,
            bottomEnd = rounded,
        )
    }
}

@Composable
private fun ChatComposer(
    state: ChatUiState,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onPickPhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onShareLocation: () -> Unit,
) {
    val canSubmit = state.canSend && !state.isSending && state.draft.isNotBlank()
    val sendFailure = state.problem == ChatProblem.SEND_FAILED
    val supportMessage = when {
        !state.canSend -> stringResource(R.string.chat_read_only_description)
        state.isSending -> stringResource(R.string.chat_sending)
        sendFailure -> stringResource(R.string.chat_draft_preserved)
        else -> null
    }
    val mentionQuery = state.draft.substringAfterLast(' ', "").takeIf { it.startsWith("@") }?.drop(1)?.lowercase()

    Surface(
        modifier = Modifier.fillMaxWidth().imePadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = AtmosphereTheme.colorScheme.surface,
        border = BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
        if (mentionQuery != null) {
            val suggestions=state.members.filter { !it.isCurrentUser && it.displayName.lowercase().contains(mentionQuery) }.take(3)
            if(suggestions.isNotEmpty() || state.canMentionAll) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                if(state.canMentionAll && (mentionQuery.isEmpty() || "toți".startsWith(mentionQuery) || "all".startsWith(mentionQuery))) TextButton(onClick={onDraftChanged(state.draft.substringBeforeLast("@")+"@toți ")}){Text("@toți")}
                suggestions.forEach { member -> TextButton(onClick={onDraftChanged(state.draft.substringBeforeLast("@")+"@${member.displayName} ")}){Text("@${member.displayName}")}}
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onPickPhotos) {
                Icon(Icons.Outlined.AddPhotoAlternate, stringResource(R.string.chat_media_message), Modifier.size(20.dp))
            }
            IconButton(onClick = onTakePhoto) {
                Icon(Icons.Outlined.PhotoCamera, stringResource(R.string.chat_media_message), Modifier.size(20.dp))
            }
            IconButton(onClick = onShareLocation) {
                Icon(Icons.Outlined.LocationOn, stringResource(R.string.chat_media_message), Modifier.size(20.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        stringResource(
                            if (state.canSend) R.string.chat_message_hint else R.string.chat_read_only_hint,
                        ),
                    )
                },
                leadingIcon = if (state.canSend) {
                    null
                } else {
                    {
                        Icon(
                            ChatIcons.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = AtmosphereTheme.colorScheme.statusNeutral,
                        )
                    }
                },
                supportingText = {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        supportMessage?.let {
                            Text(
                                text = it,
                                modifier = Modifier.weight(1f).semantics {
                                    if (state.isSending || sendFailure) liveRegion = LiveRegionMode.Polite
                                },
                                style = AtmosphereTheme.typography.labelSmall,
                                color = if (sendFailure) {
                                    AtmosphereTheme.colorScheme.error
                                } else {
                                    AtmosphereTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        } ?: Spacer(Modifier.weight(1f))
                    }
                },
                enabled = state.canSend && !state.isSending,
                isError = sendFailure,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSubmit) onSend() }),
                maxLines = 8,
                minLines = 1,
            )

            Surface(
                onClick = onSend,
                enabled = canSubmit,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (canSubmit || state.isSending) {
                    AtmosphereTheme.colorScheme.primary
                } else {
                    AtmosphereTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (canSubmit || state.isSending) {
                    AtmosphereTheme.colorScheme.onPrimary
                } else {
                    AtmosphereTheme.colorScheme.statusDisabled
                },
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    if (canSubmit) {
                        AtmosphereTheme.colorScheme.heroMiddle.copy(alpha = .6f)
                    } else {
                        AtmosphereTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(23.dp),
                            color = AtmosphereTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            ChatIcons.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        }
    }
}

private fun buildTimeline(
    messages: List<ChatMessageUi>,
    zoneId: ZoneId,
): List<ChatTimelineItem> {
    if (messages.isEmpty()) return emptyList()

    val groups = mutableListOf<ChatMessageGroup>()
    var currentMessages = mutableListOf<ChatMessageUi>()

    fun flushCurrentGroup() {
        val first = currentMessages.firstOrNull() ?: return
        groups += ChatMessageGroup(
            senderDisplayName = first.senderDisplayName,
            isCurrentUser = first.isCurrentUser,
            date = first.createdAt.atZone(zoneId).toLocalDate(),
            messages = currentMessages.toList(),
        )
        currentMessages = mutableListOf()
    }

    messages.forEach { message ->
        val previous = currentMessages.lastOrNull()
        val sameDate = previous?.createdAt?.atZone(zoneId)?.toLocalDate() ==
            message.createdAt.atZone(zoneId).toLocalDate()
        val gap = previous?.let { Duration.between(it.createdAt, message.createdAt) }
        val closeInTime = gap != null && !gap.isNegative && gap <= MessageGroupingWindow
        val belongsToCurrent = previous != null &&
            previous.senderDisplayName == message.senderDisplayName &&
            previous.isCurrentUser == message.isCurrentUser &&
            sameDate &&
            closeInTime

        if (!belongsToCurrent) flushCurrentGroup()
        currentMessages += message
    }
    flushCurrentGroup()

    return buildList {
        var previousDate: LocalDate? = null
        groups.forEach { group ->
            if (group.date != previousDate) {
                add(ChatTimelineItem.Day(group.date))
                previousDate = group.date
            }
            add(ChatTimelineItem.Messages(group))
        }
    }
}

private fun initialsFor(displayName: String): String {
    val locale = Locale.getDefault()
    return displayName
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .joinToString(separator = "") { it.take(1).uppercase(locale) }
        .ifBlank { "?" }
}

private sealed interface ChatTimelineItem {
    val key: String

    data class Day(val date: LocalDate) : ChatTimelineItem {
        override val key: String = "day-$date"
    }

    data class Messages(val group: ChatMessageGroup) : ChatTimelineItem {
        override val key: String = "messages-${group.messages.first().id}"
    }
}

private data class ChatMessageGroup(
    val senderDisplayName: String,
    val isCurrentUser: Boolean,
    val date: LocalDate,
    val messages: List<ChatMessageUi>,
)

private data class AvatarPalette(
    val background: Color,
    val foreground: Color,
)
