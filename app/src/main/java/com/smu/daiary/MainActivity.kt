package com.smu.daiary

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smu.daiary.feature.auth.AuthState
import com.smu.daiary.feature.auth.AuthViewModel
import com.smu.daiary.feature.auth.LoginScreen
import com.smu.daiary.feature.auth.PrivacyPolicyScreen
import com.smu.daiary.feature.auth.ProfileEditScreen
import com.smu.daiary.feature.auth.ProfileScreen
import com.smu.daiary.feature.auth.TermsOfServiceScreen
import com.smu.daiary.feature.home.HomeScreen
import com.smu.daiary.feature.home.HomeViewModel
import com.smu.daiary.feature.write.BlockSelectionScreen
import com.smu.daiary.feature.write.DiaryEditScreen
import com.smu.daiary.feature.write.DraftPreviewScreen
import com.smu.daiary.feature.write.DiaryDetailScreen
import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.data.model.DiaryEntry
import java.util.Calendar
import java.util.Locale

// ── 알림 채널 ID / 알림 ID ─────────────────────────────────────────────────────
const val NOTIFICATION_CHANNEL_ID = "diary_reminder_v2"   // v2: IMPORTANCE_HIGH 보장
const val NOTIFICATION_ID = 1001

// ── 알림 예약 (지정한 시각에 1회, 수신 시 다음 날로 자동 재예약) ────────────────
fun scheduleNotification(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DiaryNotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, NOTIFICATION_ID, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        // 오늘 해당 시각이 이미 지났으면 내일로 설정
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        else ->
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

// ── 즉시 테스트 알림 발송 ──────────────────────────────────────────────────────
fun sendTestNotification(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val tapPendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.notification_title))
        .setContentText(context.getString(R.string.notification_body))
        .setContentIntent(tapPendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .notify(NOTIFICATION_ID, notification)
}

// ── 알림 취소 ─────────────────────────────────────────────────────────────────
fun cancelNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DiaryNotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, NOTIFICATION_ID, intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) ?: return
    alarmManager.cancel(pendingIntent)
}

// ── 알림 수신기: 알림 표시 + 부팅 후 재예약 ──────────────────────────────────
class DiaryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("daiary_settings", Context.MODE_PRIVATE)

        // 부팅 완료 시 → 다음 알림 재예약만 수행
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (prefs.getBoolean("notification_enabled", true)) {
                scheduleNotification(
                    context,
                    prefs.getInt("notification_hour", 21),
                    prefs.getInt("notification_minute", 0)
                )
            }
            return
        }

        // API 33+ 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        // 탭 시 메인 화면으로 이동하는 PendingIntent
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 생성 및 표시
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)

        // 다음 날 동일 시각으로 재예약
        if (prefs.getBoolean("notification_enabled", true)) {
            scheduleNotification(
                context,
                prefs.getInt("notification_hour", 21),
                prefs.getInt("notification_minute", 0)
            )
        }
    }
}

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
        createNotificationChannel()

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
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = 80.dp)
                        ) { data ->
                            Snackbar(
                                snackbarData    = data,
                                containerColor  = Color(0xFF3D7A5C),
                                contentColor    = Color.White
                            )
                        }
                    }
                ) { innerPadding ->
                    when (authState) { // authState, 즉 로그인 상태
                        // 로딩 중일 때.
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // 로그인 된 상태일 때.
                        is AuthState.Authenticated -> {
                            val userId = (authState as AuthState.Authenticated).user.uid // 유저 id
                            val navController = rememberNavController()                  // 화면 이동 객체
                            val writeViewModel: WriteViewModel = viewModel()
                            val homeViewModel: HomeViewModel = viewModel()
                            val diaries by homeViewModel.diaries.collectAsStateWithLifecycle()
                            val isLoading by homeViewModel.isLoading.collectAsStateWithLifecycle()
                            val homeError by homeViewModel.error.collectAsStateWithLifecycle()
                            val isDeletingDiary by homeViewModel.isDeletingDiary.collectAsStateWithLifecycle()
                            var selectedDiary by remember { mutableStateOf<DiaryEntry?>(null) }
                            var editFromDetail by remember { mutableStateOf(false) }
                            val scope = rememberCoroutineScope()
                            val saveFailedMessage = stringResource(R.string.profile_save_error)

                            LaunchedEffect(userId) {
                                homeViewModel.loadDiaries(userId)
                            }

                            val saveDoneMessage = stringResource(R.string.save_done)
                            LaunchedEffect(writeViewModel) {
                                writeViewModel.saveEvent.collect {
                                    snackbarHostState.showSnackbar(saveDoneMessage)
                                }
                            }

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
                                    // UI
                                    HomeScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        diaries = diaries,
                                        isLoading = isLoading,
                                        error = homeError,
                                        onRetry = { homeViewModel.loadDiaries(userId) },
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
                                        onDone = {
                                            if (editFromDetail) {
                                                writeViewModel.saveDraft(userId) { success ->
                                                    if (success) {
                                                        editFromDetail = false
                                                        writeViewModel.resetDraft()
                                                        navController.popBackStack(route = "main", inclusive = false)
                                                    } else {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(saveFailedMessage)
                                                        }
                                                    }
                                                }
                                            } else {
                                                navController.popBackStack()
                                            }
                                        },
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
                                            isDeleting = isDeletingDiary,
                                            onEdit = {
                                                editFromDetail = true
                                                writeViewModel.loadExistingEntry(entry)
                                                navController.navigate("diary_edit")
                                            },
                                            onDelete = {
                                                homeViewModel.deleteDiary(userId, entry.id) { success ->
                                                    if (success) navController.popBackStack()
                                                }
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

    // 알림 채널 생성 (API 26+에서 필수)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // 이전 채널("diary_notification") 제거 — 낮은 importance로 등록됐을 수 있음
            nm.deleteNotificationChannel("diary_notification")
            // 새 채널이 이미 IMPORTANCE_HIGH로 존재하면 스킵
            val existing = nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (existing != null && existing.importance >= NotificationManager.IMPORTANCE_HIGH) return
            nm.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }
}
