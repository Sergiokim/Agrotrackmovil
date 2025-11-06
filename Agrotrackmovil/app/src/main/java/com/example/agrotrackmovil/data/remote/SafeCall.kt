package com.example.agrotrackmovil.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T> safeApiCall(
    call: suspend () -> T
): NetworkResult<T> {
    return try {
        NetworkResult.Success(call())
    } catch (e: HttpException) {
        val code = e.code()
        val msg = when (code) {
            401 -> "No autorizado (401). Revisa tu token/clave."
            403 -> "Prohibido (403). No tienes permisos."
            404 -> "No encontrado (404)."
            408 -> "Tiempo de espera agotado (408)."
            in 500..599 -> "Error del servidor ($code)."
            else -> "Error HTTP ($code)."
        }
        NetworkResult.Error(msg, code, e)
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error("Tiempo de espera agotado. Revisa tu conexión.", null, e)
    } catch (e: UnknownHostException) {
        NetworkResult.Error("Sin conexión o host no accesible.", null, e)
    } catch (e: IOException) {
        NetworkResult.Error("Error de E/S de red. Intenta de nuevo.", null, e)
    } catch (e: Exception) {
        NetworkResult.Error("Error inesperado: ${e.localizedMessage ?: "desconocido"}", null, e)
    }
}
