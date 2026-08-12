package com.sharedhouse.android.ui.chat

import java.time.Instant

data class ChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val draft: String = "",
    val connection: ChatConnection = ChatConnection.CONNECTING,
    val canSend: Boolean = true,
    val isSending: Boolean = false,
    val problem: ChatProblem? = null,
)

data class ChatMessageUi(
    val id: String,
    val senderDisplayName: String,
    val isCurrentUser: Boolean,
    val body: String,
    val createdAt: Instant,
)

enum class ChatConnection { CONNECTING, LIVE, RECONNECTING, OFFLINE }
enum class ChatProblem { LOAD_FAILED, SEND_FAILED }
