package com.app.simon.data

import java.io.Serializable

data class MuralPostData(
    val title: String,
    val content: String,
    val disciplinaId: String,
    val userName: String,
    val createdAt: Map<String, Int>,
    val files: Array<String>,
    val images: Array<String>,
    val videos: Array<String>
) : Serializable
