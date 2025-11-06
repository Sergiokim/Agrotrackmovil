package com.example.agrotrackmovil.data.remote

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenMeteoResponse(
    val current: CurrentWeather?
)

data class CurrentWeather(
    val temperature_2m: Double,
    val relative_humidity_2m: Double,
    val precipitation: Double
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,precipitation"
    ): Call<OpenMeteoResponse>
}