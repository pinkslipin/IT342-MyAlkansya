package com.example.myalkansyamobile.utils

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String?, val code: Int? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}