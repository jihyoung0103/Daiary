package com.smu.daiary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.auth.AuthState
import com.smu.daiary.feature.auth.AuthViewModel
import com.smu.daiary.feature.auth.LoginScreen
import com.smu.daiary.feature.auth.PrivacyPolicyScreen
import com.smu.daiary.feature.auth.ProfileEditScreen
import com.smu.daiary.feature.auth.ProfileScreen
import com.smu.daiary.feature.auth.TermsOfServiceScreen
import com.smu.daiary.feature.home.HomeScreen
import com.smu.daiary.feature.home.HomeViewModel
import com.smu.daiary.feature.notification.createNotificationChannel
import com.smu.daiary.feature.write.BlockSelectionScreen
import com.smu.daiary.feature.write.DiaryDetailScreen
import com.smu.daiary.feature.write.DiaryEditScreen
import com.smu.daiary.feature.write.DraftPreviewScreen
import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.ui.theme.DaiaryTheme
import java.util.Locale

// 메인 함수 :ComponentActivity()는 ComponentActivity를 상속받는 의미
// (:) 콜론 = extends 또는 implements로 치환가능
class MainActivity : ComponentActivity() {

    // 언어 설정을 Activity 생성 최초 시점에 적용
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("daiary_settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "한국어") ?: "한국어"
        val locale = if (lang == "English") Locale("en") else Locale("ko")
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    // 시작될 때 딱 한 번 실행되는 함수 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("daiary_settings", Context.MODE_PRIVATE)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 화면 꽉 채우는 설정
        createNotificationChannel(this)

        // Compose UI
        setContent {
            val isDarkTheme = remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
            DaiaryTheme(darkTheme = isDarkTheme.value) {
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
                                    .background(Color(0xFFFDFAF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF3D7A5C))
                            }
                        }

                        // 로그인 된 상태일 때.
                        is AuthState.Authenticated -> {
                            val userId = (authState as AuthState.Authenticated).user.uid // 유저 id
                            val navController = rememberNavController()                  // 화면 이동 객체
                            val writeViewModel: WriteViewModel = viewModel()
                            var selectedDiary by remember { mutableStateOf<DiaryEntry?>(null) }

                            // 데이터 수집에 필요한 권한 목록
                            val requiredPermissions = buildList {
                                add(Manifest.permission.READ_CALENDAR)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
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

                            // 앱 시작 시 알림 권한 팝업 (Android 13+, 미허용 상태일 때만)
                            val notifPermissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                            ) { /* 허용/거부 무관하게 계속 진행 */ }

                            LaunchedEffect(Unit) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
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
                                        },
                                        onProfileClick = { navController.navigate("profile") },
                                        onDiaryClick = { entry ->
                                            selectedDiary = entry
                                            navController.navigate("diary_detail")
                                        }
                                    )
                                }
                                // "block_selection": 블록 선택 화면
                                composable("block_selection") {
                                    BlockSelectionScreen(
                                        viewModel = writeViewModel,
                                        onNext = { navController.navigate("draft_preview") },
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier.padding(innerPadding)
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
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                // "diary_edit": 일기 편집 화면
                                composable("diary_edit") {
                                    DiaryEditScreen(
                                        viewModel = writeViewModel,
                                        onDone = { navController.popBackStack() },
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                // "profile": 프로필 화면
                                composable("profile") {
                                    ProfileScreen(
                                        authViewModel = authViewModel,
                                        onBack = { navController.popBackStack() },
                                        isDarkMode = isDarkTheme.value,
                                        onDarkModeChange = { enabled ->
                                            isDarkTheme.value = enabled
                                            prefs.edit().putBoolean("dark_mode", enabled).apply()
                                        },
                                        onPrivacyPolicy = { navController.navigate("privacy_policy") },
                                        onTermsOfService = { navController.navigate("terms_of_service") },
                                        onEditProfile = { navController.navigate("profile_edit") },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                // "privacy_policy": 개인정보 처리방침 화면
                                composable("privacy_policy") {
                                    PrivacyPolicyScreen(
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                // "terms_of_service": 서비스 이용약관 화면
                                composable("terms_of_service") {
                                    TermsOfServiceScreen(
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                // "profile_edit": 프로필 편집 화면
                                composable("profile_edit") {
                                    ProfileEditScreen(
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                // "diary_detail": 일기 상세 화면
                                composable("diary_detail") {
                                    selectedDiary?.let { entry ->
                                        DiaryDetailScreen(
                                            entry = entry,
                                            onEdit = {
                                                writeViewModel.loadExistingEntry(entry)
                                                navController.navigate("diary_edit")
                                            },
                                            onBack = { navController.popBackStack() },
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
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
