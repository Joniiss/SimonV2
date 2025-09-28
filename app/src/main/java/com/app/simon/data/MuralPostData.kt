package com.app.simon.data

import java.io.Serializable

data class MuralPostData(
    val title: String,
    val content: String,
    val disciplinaId: String,
    val userName: String,
    val createdAt: String,
    val files: List<String>,
    val images: List<String>,
    val videos: List<String>
) : Serializable
