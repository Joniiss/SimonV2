package com.app.simon.data

import com.google.firebase.Timestamp

data class ChatChannelItem(
    val channelId: String,
    val title: String,
    val lastMessage: String,
    val lastMessageAt: Timestamp?,
    val photoUrl: String? = null
)
