package com.app.simon.data

import java.io.Serializable

data class User(
    val uid: String,
    val ra: String,
    val email: String,
    val celular: String,
    val horario: String,
    val sala: String,
    val predio: String,
    val nome: String,
    val curso: String,
    val status: String,
    val periodo: Int,
    val foto: String
) : Serializable
