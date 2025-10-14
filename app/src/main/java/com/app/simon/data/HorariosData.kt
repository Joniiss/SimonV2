package com.app.simon.data

import java.io.Serializable

data class HorariosData(
    val day: String,
    val time: Array<Int>
) : Serializable
