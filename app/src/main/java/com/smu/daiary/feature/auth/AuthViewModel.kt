package com.smu.daiary.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.smu.daiary.data.repository.AuthRepository
import com.smu.daiary.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val TAG = "AuthViewModel"

/** Firebase 인증 상태 */
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class LoginSuccess(val user: FirebaseUser) : AuthState()
    data class SignUpSuccess(val user: FirebaseUser) : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val currentUser = repository.currentUser()
        _authState.value = if (currentUser != null) {
            AuthState.Authenticated(currentUser)
        } else {
            AuthState.Unauthenticated
        }
    }

    // ── 이메일 로그인 ──────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("이메일과 비밀번호를 입력해주세요.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = repository.signIn(email.trim(), password)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "로그인 성공: uid=${result.data.uid}")
                    _authState.value = AuthState.LoginSuccess(result.data)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "로그인 실패: ${result.exception.message}")
                    _authState.value = AuthState.Error(mapAuthError(result.exception))
                }
            }
        }
    }

    // ── 이메일 회원가입 ────────────────────────────────────────────────────────

    fun signUp(email: String, password: String, displayName: String = "") {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("이메일과 비밀번호를 입력해주세요.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("비밀번호는 6자리 이상이어야 합니다.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = repository.signUp(email.trim(), password, displayName.trim())) {
                is AuthResult.Success -> {
                    Log.d(TAG, "회원가입 성공: uid=${result.data.uid}")
                    _authState.value = AuthState.SignUpSuccess(result.data)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "회원가입 실패: ${result.exception.message}")
                    _authState.value = AuthState.Error(mapAuthError(result.exception))
                }
            }
        }
    }

    // ── Google 로그인 ──────────────────────────────────────────────────────────

    fun signInWithGoogle(idToken: String) {
        Log.d(TAG, "Google 로그인 시도")
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user!!
                val isNew = result.additionalUserInfo?.isNewUser == true
                Log.d(TAG, "Google 로그인 성공: uid=${user.uid}, 신규=$isNew")
                viewModelScope.launch {
                    repository.upsertUser(user.uid, user.email ?: "", user.displayName ?: "")
                    _authState.value = if (isNew) AuthState.SignUpSuccess(user) else AuthState.LoginSuccess(user)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Google 로그인 실패: ${e.message}")
                _authState.value = AuthState.Error("Google 로그인에 실패했습니다.")
            }
    }

    // ── 공통 ───────────────────────────────────────────────────────────────────

    fun completeAuth(user: FirebaseUser) {
        _authState.value = AuthState.Authenticated(user)
    }

    fun logout() {
        repository.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun deleteAccount() {
        val user = repository.currentUser() ?: return
        _authState.value = AuthState.Loading
        user.delete()
            .addOnSuccessListener {
                Log.d(TAG, "회원탈퇴 완료")
                _authState.value = AuthState.Unauthenticated
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "회원탈퇴 실패: ${e.message}")
                _authState.value = AuthState.Error(mapAuthError(e))
            }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    // ── 에러 메시지 매핑 ───────────────────────────────────────────────────────

    private fun mapAuthError(e: Exception): String = when {
        e is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "이메일 형식이 올바르지 않습니다."
            else -> "이메일 또는 비밀번호가 올바르지 않습니다."
        }
        e is FirebaseAuthInvalidUserException -> "등록되지 않은 이메일입니다."
        e is FirebaseAuthUserCollisionException -> "이미 사용 중인 이메일입니다."
        e is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상이어야 합니다."
        e.message?.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) == true ->
            "요청이 너무 많습니다. 잠시 후 다시 시도하세요."
        e.message?.contains("NETWORK_REQUEST_FAILED", ignoreCase = true) == true ->
            "네트워크 오류가 발생했습니다. 연결을 확인하세요."
        else -> "오류가 발생했습니다. 다시 시도하세요."
    }
}
