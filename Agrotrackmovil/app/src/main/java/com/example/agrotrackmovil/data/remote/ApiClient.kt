package com.example.agrotrackmovil.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.agrotrackmovil.BuildConfig

object ApiClient {
    val UNSPLASH_API_KEY = BuildConfig.UNSPLASH_API_KEY
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