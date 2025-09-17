package com.app.simon.data

import java.io.Serializable


data class ForumPostData(
    val title: String,
    val userName: String,
    val userId: String,
    val likes: Int,
    val createdAt: String,
    val courseId: String,
    val content: String,
    val comments: Array<String>
) : Serializable
