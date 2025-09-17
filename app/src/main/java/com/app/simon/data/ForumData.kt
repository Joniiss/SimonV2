package com.app.simon.data

import java.io.Serializable

data class ForumData(
    val docId: String,
    val data: ForumPostData
) : Serializable
