package com.app.simon.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ChatAttachment(
    val url: String = "",
    val storagePath: String = "",
    val mime: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null
)

data class MessageStatus(
    val deliveredTo: List<String> = emptyList(),
    val readBy: List<String> = emptyList()
)

/**
 * Documento em channels/{channelId}/messages/{messageId}
 */
data class ChatMessage(
    @DocumentId val id: String? = null,
    val text: String? = null,
    val type: String = "text",          // "text" | "image" | ...
    val senderId: String = "",
    val createdAt: Timestamp? = null,
    val attachments: List<ChatAttachment> = emptyList(),
    val replyTo: String? = null,
    val editedAt: Timestamp? = null,
    val status: MessageStatus = MessageStatus()
)
