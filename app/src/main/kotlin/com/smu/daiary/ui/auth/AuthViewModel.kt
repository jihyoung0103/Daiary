package com.smu.daiary.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smu.daiary.data.repository.AuthRepository
import com.smu.daiary.util.Result
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for auth screens (Login + Register).
 * Exposes a single StateFlow<AuthUiState> as the source of truth for UI.
 * Fragment observes this state and drives navigation from Success.
 *
 * Uses by viewModels() delegate — no Hilt required for this scope.
 * Repository uses default constructor parameters so no factory needed.
 *
 * Layer boundary: Firebase imports here are exception types only (no SDK instance calls).
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Read-only StateFlow — Fragment collects this via repeatOnLifecycle(STARTED) */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Trigger email/password login.
     * Sets Loading, calls repository, maps result to Success or Error(Korean message).
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signIn(email, password)
            _uiState.value = when (result) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(mapAuthError(result.exception))
                is Result.Loading -> AuthUiState.Loading
            }
        }
    }

    /**
     * Trigger email/password registration.
     * Sets Loading, calls repository (auth + Firestore upsert), maps result to Success or Error.
     */
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signUp(email, password)
            _uiState.value = when (result) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(mapAuthError(result.exception))
                is Result.Loading -> AuthUiState.Loading
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Reset to Idle — call after navigating away on Success to prevent duplicate navigation
     * when Fragment restarts (e.g., screen rotation). StateFlow retains last value.
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Map FirebaseAuthException subclass and error code to Korean user-facing message.
     *
     * Firebase Email Enumeration Protection (enabled by default since Sep 2023):
     *   ERROR_WRONG_PASSWORD + ERROR_USER_NOT_FOUND are unified into INVALID_LOGIN_CREDENTIALS
     *   under FirebaseAuthInvalidCredentialsException. The `else` branch handles this case.
     */
    private fun mapAuthError(e: Exception): String {
        return when {
            e is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "이메일 형식이 올바르지 않습니다."
                "ERROR_WRONG_PASSWORD" -> "비밀번호가 틀렸습니다."
                else -> "이메일 또는 비밀번호가 올바르지 않습니다."
                // "INVALID_LOGIN_CREDENTIALS" falls here (email enumeration protection active)
            }
            e is FirebaseAuthInvalidUserException -> "등록되지 않은 이메일입니다."
            e is FirebaseAuthUserCollisionException -> "이미 사용 중인 이메일입니다."
            e is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상이어야 합니다."
            e is FirebaseAuthException && e.errorCode == "ERROR_TOO_MANY_REQUESTS" ->
                "요청이 너무 많습니다. 잠시 후 다시 시도하세요."
            e.message?.contains("NETWORK_REQUEST_FAILED", ignoreCase = true) == true ->
                "네트워크 오류가 발생했습니다. 연결을 확인하세요."
            else -> "로그인에 실패했습니다. 다시 시도하세요."
        }
    }
}
