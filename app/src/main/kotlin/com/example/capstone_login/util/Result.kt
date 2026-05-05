package com.example.capstone_login.util

/**
 * Generic result wrapper for repository operations.
 * Repository functions return Result<T> instead of throwing exceptions.
 * ViewModel maps Result<T> to AuthUiState.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
