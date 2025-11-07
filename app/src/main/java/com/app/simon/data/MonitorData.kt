package com.app.simon.data

import java.io.Serializable


data class GeoData (
    val latitude: Double,
    val longitude: Double
) : Serializable

data class MonitorData (
    val nome: String,
    val ra: String,
    val uid: String,
    val horarioDisponivel: ArrayList<HorariosData>,
    val disciplina: String,
    val disciplinaId: String,
    val status: Boolean,
    val sala: String,
    val local: String,
    val remuneracao: Any,
    val mensagem: String,
    val foto: String,
    val cargaHoraria: Integer,
    val aprovacao: Integer,
    val geoLoc: GeoData? = GeoData(latitude=-22.9797, longitude=-43.2333)
) : Serializable