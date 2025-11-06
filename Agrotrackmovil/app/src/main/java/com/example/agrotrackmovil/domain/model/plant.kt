package com.example.agrotrackmovil.domain.model
import java.io.Serializable

data class Plant(
    var id: String = "",
    val nombre: String = "",
    val nombreCientifico: String? = null,
    val lat: String? = null,
    val lon: String? = null,
    val createdAt: String = "",
    val datosClima: Map<String, Any>? = null,
    val imagen: String? = null,
    val userId: String? = null
) : Serializable {
    fun getLatAsDouble(): Double? = try { lat?.toDouble() } catch (e: NumberFormatException) { null }
    fun getLonAsDouble(): Double? = try { lon?.toDouble() } catch (e: NumberFormatException) { null }
}