package com.sharedhouse.android.ui.chat

import java.time.Instant

data class ChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val draft: String = "",
    val connection: ChatConnection = ChatConnection.CONNECTING,
    val canSend: Boolean = true,
    val isSending: Boolean = false,
    val problem: ChatProblem? = null,
    val pinnedMessages: List<ChatMessageUi> = emptyList(),
    val members: List<ChatMemberUi> = emptyList(),
    val canMentionAll: Boolean = false,
)

data class ChatMessageUi(
    val id: String,
    val senderDisplayName: String,
    val isCurrentUser: Boolean,
    val body: String,
    val createdAt: Instant,
    val kind: String = "member",
    val isPinned: Boolean = false,
    val pinnedByDisplayName: String? = null,
    val mentionedUserIds: List<String> = emptyList(),
    val location: ChatLocationUi? = null,
    val attachments: List<ChatAttachmentUi> = emptyList(),
)

data class ChatMemberUi(val userId:String,val displayName:String,val role:String,val isCurrentUser:Boolean)
data class ChatLocationUi(val latitude:Double,val longitude:Double)
data class ChatAttachmentUi(val id:String,val width:Int,val height:Int,val bytes:ByteArray?=null) {
    override fun equals(other:Any?)=other is ChatAttachmentUi&&id==other.id&&width==other.width&&height==other.height&&bytes.contentEquals(other.bytes)
    override fun hashCode()=31*(31*id.hashCode()+width)+height
}

enum class ChatConnection { CONNECTING, LIVE, RECONNECTING, OFFLINE }
enum class ChatProblem { LOAD_FAILED, SEND_FAILED }
