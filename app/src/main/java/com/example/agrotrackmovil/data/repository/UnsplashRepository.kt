package com.example.agrotrackmovil.data.repository

import com.example.agrotrackmovil.data.remote.ApiClient
import com.example.agrotrackmovil.data.remote.UnsplashResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UnsplashRepository {

    fun searchPhotos(query: String, onResult: (Result<UnsplashResponse>) -> Unit) {
        val call = ApiClient.unsplashApiService.searchPhotos(
            ApiClient.getUnsplashApiKey(),
            query
        )

        call.enqueue(object : Callback<UnsplashResponse> {
            override fun onResponse(
                call: Call<UnsplashResponse>,
                response: Response<UnsplashResponse>
            ) {
                when (response.code()) {
                    200 -> {
                        response.body()?.let {
                            onResult(Result.success(it))
                        } ?: onResult(Result.failure(Exception("Respuesta vacía del servidor.")))
                    }
                    401 -> {
                        onResult(Result.failure(Exception("Error 401: No autorizado. Verifica tu API key.")))
                    }
                    404 -> {
                        onResult(Result.failure(Exception("Error 404: No se encontró el recurso.")))
                    }
                    else -> {
                        onResult(Result.failure(Exception("Error ${response.code()}: ${response.message()}")))
                    }
                }
            }

            override fun onFailure(call: Call<UnsplashResponse>, t: Throwable) {
                onResult(Result.failure(t))
            }
        })
    }
}
