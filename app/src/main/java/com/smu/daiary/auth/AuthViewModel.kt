package com.smu.daiary.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AuthViewModel"

/** Firebase 인증 상태 */
sealed class AuthState {
    /** 앱 시작 직후 Firebase 확인 중 */
    object Loading : AuthState()
    /** 비로그인 */
    object Unauthenticated : AuthState()
    /** 로그인 성공 직후 (피드백 표시용, 곧 Authenticated로 전환) */
    data class LoginSuccess(val user: FirebaseUser) : AuthState()
    /** 회원가입 성공 직후 (피드백 표시용, 곧 Authenticated로 전환) */
    data class SignUpSuccess(val user: FirebaseUser) : AuthState()
    /** 로그인 완료 → 메인 화면 */
    data class Authenticated(val user: FirebaseUser) : AuthState()
    /** 오류 */
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val currentUser = auth.currentUser
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
        Log.d(TAG, "로그인 시도: $email")
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                Log.d(TAG, "로그인 성공: uid=${result.user?.uid}")
                _authState.value = AuthState.LoginSuccess(result.user!!)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "로그인 실패: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "로그인에 실패했습니다.")
            }
    }

    // ── 이메일 회원가입 ────────────────────────────────────────────────────────

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("이메일과 비밀번호를 입력해주세요.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("비밀번호는 6자리 이상이어야 합니다.")
            return
        }
        Log.d(TAG, "회원가입 시도: $email")
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                Log.d(TAG, "회원가입 성공: uid=${result.user?.uid}")
                _authState.value = AuthState.SignUpSuccess(result.user!!)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "회원가입 실패: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "회원가입에 실패했습니다.")
            }
    }

    // ── Google 로그인 ──────────────────────────────────────────────────────────

    /**
     * Google Sign-In 에서 받은 ID 토큰으로 Firebase 인증을 완료합니다.
     * LoginScreen 에서 GoogleSignIn flow 를 처리한 뒤 idToken 을 넘겨줍니다.
     */
    fun signInWithGoogle(idToken: String) {
        Log.d(TAG, "Google 로그인 시도")
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val isNew = result.additionalUserInfo?.isNewUser == true
                Log.d(TAG, "Google 로그인 성공: uid=${result.user?.uid}, 신규=$isNew")
                if (isNew) {
                    _authState.value = AuthState.SignUpSuccess(result.user!!)
                } else {
                    _authState.value = AuthState.LoginSuccess(result.user!!)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Google 로그인 실패: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Google 로그인에 실패했습니다.")
            }
    }

    // ── 공통 ───────────────────────────────────────────────────────────────────

    /**
     * 성공 피드백 표시가 끝난 뒤 실제 Authenticated 상태로 전환합니다.
     * LoginScreen 의 LaunchedEffect 에서 딜레이 후 호출합니다.
     */
    fun completeAuth(user: FirebaseUser) {
        _authState.value = AuthState.Authenticated(user)
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
