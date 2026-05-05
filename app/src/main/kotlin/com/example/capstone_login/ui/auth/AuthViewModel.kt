package com.example.capstone_login.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone_login.data.repository.AuthRepository
import com.example.capstone_login.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for auth screens (Login, and Register in Phase 2).
 * Exposes a single StateFlow<AuthUiState> as the source of truth for UI.
 * Fragment observes this state and drives navigation from Success.
 *
 * Uses by viewModels() delegate — no Hilt required for this scope.
 * Repository uses default constructor parameters so no factory needed.
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Read-only StateFlow — Fragment collects this via repeatOnLifecycle(STARTED) */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Stub: trigger email/password login.
     * Phase 2 implements actual auth + error mapping.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signIn(email, password)
            _uiState.value = when (result) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(result.exception.message ?: "로그인 실패")
                is Result.Loading -> AuthUiState.Loading
            }
        }
    }

    /**
     * Stub: trigger email/password registration.
     * Phase 2 implements actual auth + Firestore upsert + error mapping.
     */
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signUp(email, password)
            _uiState.value = when (result) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(result.exception.message ?: "회원가입 실패")
                is Result.Loading -> AuthUiState.Loading
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    /** Reset to Idle — called when user starts correcting input after an error */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
