package com.example.agrotrackmovil

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApiService {
    @GET("search/photos")
    fun searchPhotos(
        @Query("client_id") clientId: String,
        @Query("query") query: String
    ): Call<UnsplashResponse>
}

data class UnsplashResponse(
    val results: List<UnsplashPhoto>
)

data class UnsplashPhoto(
    val urls: UnsplashUrls
)

data class UnsplashUrls(
    val regular: String?
)