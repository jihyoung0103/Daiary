package com.example.capstone_login.ui.auth

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed class representing all possible UI states for the auth screens.
 * AuthViewModel exposes StateFlow<AuthUiState>.
 * Fragment collects this flow inside repeatOnLifecycle(STARTED).
 */
sealed class AuthUiState {
    /** Initial state — no action in progress */
    object Idle : AuthUiState()

    /** Firebase call is in flight — show loading indicator, disable buttons */
    object Loading : AuthUiState()

    /** Auth succeeded — contains the signed-in Firebase user */
    data class Success(val user: FirebaseUser) : AuthUiState()

    /** Auth failed — contains human-readable error message (mapped from FirebaseAuthException) */
    data class Error(val message: String) : AuthUiState()
}
