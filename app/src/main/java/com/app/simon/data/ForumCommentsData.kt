package com.app.simon.data

import java.io.Serializable

data class ForumCommentsData(
    val postId: String,
    val userId: String,
    val userName: String,
    val userRole: String,
    val createdAt: String,
    val content: String
) : Serializable
