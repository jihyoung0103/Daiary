package com.smu.daiary.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// TODO: 실제 Firebase Auth 구현으로 교체 예정 (임시 스텁 — 항상 Authenticated 상태)
class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Authenticated)
    val authState: StateFlow<AuthState> = _authState

    fun logout() {
        _authState.value = AuthState.Unauthenticated
    }
}
