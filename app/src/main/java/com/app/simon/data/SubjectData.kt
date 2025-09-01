package com.app.simon.data

import java.io.Serializable

data class SubjectData(
    val name: String,
    val professor: String,
    val term: Int,
    val id: String,
    val school: String,
    val course: String,
    val currentMonitors: Int,
    val monitors: Int
) : Serializable