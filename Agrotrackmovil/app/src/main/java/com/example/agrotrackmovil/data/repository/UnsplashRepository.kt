package com.example.agrotrackmovil.data.repository

import android.util.Log
import com.example.agrotrackmovil.data.remote.ApiClient
import com.example.agrotrackmovil.data.remote.UnsplashResponse
import okhttp3.internal.http2.StreamResetException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

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
                if (!response.isSuccessful) {
                    val msg = httpErrorMessage(response.code(), response.message())
                    onResult(Result.failure(Exception(msg)))
                    return
                }
                val body = response.body()
                if (body == null) {
                    onResult(Result.failure(Exception("Respuesta vacía del servidor.")))
                    return
                }

                onResult(Result.success(body))
            }

            override fun onFailure(call: Call<UnsplashResponse>, t: Throwable) {
                Log.e("UnsplashRepository", "searchPhotos() failure", t)
                val msg = networkErrorMessage(t)
                onResult(Result.failure(Exception(msg)))
            }
        })
    }

    private fun httpErrorMessage(code: Int, rawMessage: String?): String = when (code) {
        401 -> "No autorizado (401). Revisa tu API key."
        404 -> "No encontrado (404)."
        in 500..599 -> "Error del servidor ($code). Intenta más tarde."
        else -> "Error HTTP $code${rawMessage?.let { ": $it" } ?: ""}."
    }

    private fun networkErrorMessage(t: Throwable): String = when (t) {
        is UnknownHostException -> "Sin conexión a internet o host no accesible."
        is ConnectException -> "No se pudo conectar con el servidor."
        is SocketTimeoutException -> "Tiempo de espera agotado. Intenta de nuevo."
        is SSLException -> "Problema de seguridad en la conexión (SSL)."
        is EOFException -> "Respuesta incompleta del servidor."
        is StreamResetException -> "Conexión reiniciada. Intenta nuevamente."
        is IOException -> "Error de red. Verifica tu conexión."
        else -> "Ocurrió un error inesperado. Intenta más tarde."
    }
}
