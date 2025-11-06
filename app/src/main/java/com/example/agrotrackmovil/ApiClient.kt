package com.example.agrotrackmovil

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val UNSPLASH_API_KEY = "JQAo04XN-5Sl8aMK9yVbGZSzRNtLzHdkYrgl8X7uUKE" // Reemplaza con tu clave de Unsplash
    private const val BASE_URL_UNSPLASH = "https://api.unsplash.com/"

    val unsplashApiService: UnsplashApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_UNSPLASH)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UnsplashApiService::class.java)
    }

    fun getUnsplashApiKey(): String = UNSPLASH_API_KEY
}
