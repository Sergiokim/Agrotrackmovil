package com.example.agrotrackmovil.data.remote

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T): NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val cause: Throwable? = null
    ): NetworkResult<Nothing>()
}
