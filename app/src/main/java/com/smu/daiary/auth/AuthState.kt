package com.smu.daiary.auth

// TODO: 실제 Firebase Auth 구현으로 교체 예정 (임시 스텁)
sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
    object LoginSuccess : AuthState()
    object SignUpSuccess : AuthState()
}
