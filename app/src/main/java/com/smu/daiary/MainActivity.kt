package com.smu.daiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smu.daiary.auth.AuthState
import com.smu.daiary.auth.AuthViewModel
import com.smu.daiary.auth.LoginScreen
import com.smu.daiary.ui.theme.DaiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaiaryTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (authState) {
                        // 앱 시작 직후 Firebase 로딩 중
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF1EFE8)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF533AB7))
                            }
                        }
                        // 로그인된 상태 → 캘린더 메인 화면
                        is AuthState.Authenticated -> {
                            MainCalendarScreen(
                                modifier = Modifier.padding(innerPadding),
                                onLogout = { authViewModel.logout() }
                            )
                        }
                        // 미로그인·에러·성공 피드백 중 → 로그인 화면 (피드백 오버레이는 LoginScreen 내부에서 표시)
                        is AuthState.Unauthenticated,
                        is AuthState.Error,
                        is AuthState.LoginSuccess,
                        is AuthState.SignUpSuccess -> {
                            LoginScreen(
                                authViewModel = authViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
