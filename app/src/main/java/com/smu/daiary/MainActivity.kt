package com.smu.daiary

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smu.daiary.feature.auth.AuthState
import com.smu.daiary.feature.auth.AuthViewModel
import com.smu.daiary.feature.auth.LoginScreen
import com.smu.daiary.feature.home.HomeScreen
import com.smu.daiary.feature.home.HomeViewModel
import com.smu.daiary.feature.write.BlockSelectionScreen
import com.smu.daiary.feature.write.DiaryEditScreen
import com.smu.daiary.feature.write.DraftPreviewScreen
import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.ui.theme.DaiaryTheme

// 메인 함수 :ComponentActivity()는 ComponentActivity를 상속받는 의미
// (:) 콜론 = extends 또는 implements로 치환가능
class MainActivity : ComponentActivity() { // = class MainActivity extends ComponentActivity()

    // 시작될 때 딱 한 번 실행되는 함수 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 화면 꽉 채우는 설정

        // Compose UI
        setContent {
            DaiaryTheme {
                // val은 변경 불가 변수(final) / var는 변경 가능 변수
                // 인증 상태 감지
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                // Material3의 기본 레이아웃 틀.
                // innerPadding이란 상단바/하단바 여백 자동 계산 설정
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (authState) { // authState, 즉 로그인 상태
                        // 로딩 중일 때.
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

                        // 로그인 된 상태일 때.
                        is AuthState.Authenticated -> {
                            val userId = (authState as AuthState.Authenticated).user.uid // 유저 id
                            val navController = rememberNavController()                  // 화면 이동 객체
                            val writeViewModel: WriteViewModel = viewModel()

                            // 데이터 수집에 필요한 권한 목록
                            val requiredPermissions = buildList {
                                add(Manifest.permission.READ_CALENDAR)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }.toTypedArray()

                            // 권한 요청 런처 (허용/거부 결과와 무관하게 loadBlocks 실행)
                            val permissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestMultiplePermissions()
                            ) { _ ->
                                // 개별 권한이 거부되어도 수집 가능한 데이터만 부분 수집
                                writeViewModel.loadBlocks(userId)
                                navController.navigate("block_selection")
                            }

                            // 네비게이션 구조
                            NavHost(
                                navController = navController,
                                startDestination = "main" // 시작 화면은 "main"
                            ) {
                                // "main": 메인 캘린더 화면
                                composable("main") {
                                    val homeViewModel: HomeViewModel = viewModel()
                                    val diaries by homeViewModel.diaries.collectAsStateWithLifecycle()
                                    // userId가 설정될 때, 딱 한 번 실행하는 기능
                                    LaunchedEffect(userId) {
                                        homeViewModel.loadDiaries(userId) // userId에 해당하는 일기들 로드함
                                    }
                                    // UI
                                    HomeScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        diaries = diaries,
                                        onLogout = { authViewModel.logout() },
                                        onStartDiary = {
                                            // 권한 요청 → 결과 콜백에서 loadBlocks + navigate 실행
                                            permissionLauncher.launch(requiredPermissions)
                                        }
                                    )
                                }
                                // "block_selection": 블록 선택 화면
                                composable("block_selection") {
                                    BlockSelectionScreen(
                                        viewModel = writeViewModel,
                                        onNext = { navController.navigate("draft_preview") },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                // "draft_preview": 초안 미리보기 화면
                                composable("draft_preview") {
                                    DraftPreviewScreen(
                                        viewModel = writeViewModel,
                                        userId = userId,
                                        onEdit = { navController.navigate("diary_edit") },
                                        onSaved = {
                                            writeViewModel.resetDraft()
                                            navController.popBackStack(route = "main", inclusive = false)
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                // "diary_edit": 일기 편집 화면
                                composable("diary_edit") {
                                    DiaryEditScreen(
                                        viewModel = writeViewModel,
                                        onDone = { navController.popBackStack() },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        // 미인증된 상태
                        is AuthState.Unauthenticated,   // 인증되지 않은 상태, 즉 로그인 안한 상태
                        is AuthState.Error,             // 로그인 실패
                        is AuthState.LoginSuccess,      // 로그인, 회원가입 성공
                        is AuthState.SignUpSuccess -> { // 성공했는데 로그인 화면으로 보내는 이유는, Firebase 인증까지 소요되는 시간이 있기 때문. 인증 후 자동적으로 authenticated 분기로 넘어감
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
