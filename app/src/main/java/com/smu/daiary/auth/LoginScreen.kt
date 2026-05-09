package com.smu.daiary.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.smu.daiary.R
import kotlinx.coroutines.delay

private object LoginColors {
    val Background   = Color(0xFFF1EFE8)
    val Surface      = Color(0xFFFAFAF8)
    val TextPrimary  = Color(0xFF2C2C2A)
    val TextMuted    = Color(0xFF888780)
    val AccentPurple = Color(0xFF533AB7)
    val Border       = Color(0xFFD3D1C7)
    val ErrorRed     = Color(0xFFD32F2F)
    val SuccessGreen = Color(0xFF2E7D32)
    val Overlay      = Color(0x99000000)
}

/**
 * 로그인 / 회원가입 화면.
 * - 이메일·비밀번호 탭 전환
 * - Google 소셜 로그인
 * - 로그인·회원가입 성공 시 오버레이 피드백 후 자동 전환
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }  // 0=로그인, 1=회원가입
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }

    // ── Google Sign-In 런처 ──────────────────────────────────────────────────
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { authViewModel.signInWithGoogle(it) }
                    ?: authViewModel.setError("Google 계정 토큰을 가져올 수 없습니다.")
            } catch (e: ApiException) {
                authViewModel.setError("Google 로그인 실패 (code ${e.statusCode})")
            }
        }
    }

    // ── 탭 전환 시 입력값·에러 초기화 ───────────────────────────────────────
    LaunchedEffect(selectedTab) {
        email    = ""
        password = ""
        authViewModel.clearError()
    }

    // ── 핵심: 성공 상태 감지 → 딜레이 후 메인 화면으로 전환 ─────────────────
    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.LoginSuccess  -> {
                delay(800)   // 0.8초 피드백 표시
                authViewModel.completeAuth(s.user)
            }
            is AuthState.SignUpSuccess -> {
                delay(1800)  // 1.8초 피드백 표시
                authViewModel.completeAuth(s.user)
            }
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LoginColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // ── 로그인·회원가입 카드 ───────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(24.dp),
            color = LoginColors.Surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 앱 타이틀
                Text(
                    text       = "Daiary",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = LoginColors.AccentPurple
                )
                Text(
                    text     = "나만의 일기장",
                    fontSize = 13.sp,
                    color    = LoginColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // ── 탭 ──────────────────────────────────────────────────────────
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = Color.Transparent,
                    contentColor     = LoginColors.AccentPurple,
                    indicator        = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color    = LoginColors.AccentPurple
                        )
                    },
                    divider = {}
                ) {
                    listOf("로그인", "회원가입").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text     = {
                                Text(
                                    text       = title,
                                    fontSize   = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal,
                                    color      = if (selectedTab == index) LoginColors.AccentPurple else LoginColors.TextMuted
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 이메일 입력 ────────────────────────────────────────────────
                OutlinedTextField(
                    value           = email,
                    onValueChange   = { email = it },
                    label           = { Text("이메일") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginColors.AccentPurple,
                        focusedLabelColor  = LoginColors.AccentPurple,
                        cursorColor        = LoginColors.AccentPurple
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 비밀번호 입력 ──────────────────────────────────────────────
                OutlinedTextField(
                    value                = password,
                    onValueChange        = { password = it },
                    label                = { Text("비밀번호") },
                    singleLine           = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier             = Modifier.fillMaxWidth(),
                    shape                = RoundedCornerShape(12.dp),
                    colors               = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginColors.AccentPurple,
                        focusedLabelColor  = LoginColors.AccentPurple,
                        cursorColor        = LoginColors.AccentPurple
                    )
                )

                // ── 에러 메시지 ────────────────────────────────────────────────
                AnimatedVisibility(visible = authState is AuthState.Error) {
                    Text(
                        text     = (authState as? AuthState.Error)?.message ?: "",
                        color    = LoginColors.ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 이메일 확인 버튼 ───────────────────────────────────────────
                Button(
                    onClick  = {
                        if (selectedTab == 0) authViewModel.login(email, password)
                        else authViewModel.signUp(email, password)
                    },
                    enabled  = authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = LoginColors.AccentPurple
                    )
                ) {
                    if (authState is AuthState.Loading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text("처리 중...", color = Color.White, fontSize = 15.sp)
                        }
                    } else {
                        Text(
                            text       = if (selectedTab == 0) "로그인" else "회원가입",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── 구분선 ─────────────────────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = LoginColors.Border)
                    Text("  또는  ", fontSize = 12.sp, color = LoginColors.TextMuted)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = LoginColors.Border)
                }

                // ── Google 로그인 버튼 ─────────────────────────────────────────
                OutlinedButton(
                    onClick  = { googleLauncher.launch(googleSignInClient.signInIntent) },
                    enabled  = authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, LoginColors.Border),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = LoginColors.TextPrimary
                    )
                ) {
                    Box(
                        modifier         = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF4285F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Google로 계속하기", fontSize = 15.sp, color = LoginColors.TextPrimary)
                }
            }
        }

        // ── 성공 오버레이 ────────────────────────────────────────────────────────
        val showOverlay = authState is AuthState.LoginSuccess || authState is AuthState.SignUpSuccess

        AnimatedVisibility(
            visible  = showOverlay,
            enter    = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f),
            exit     = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            Box(
                modifier         = Modifier.fillMaxSize().background(LoginColors.Overlay),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape           = RoundedCornerShape(20.dp),
                    color           = LoginColors.Surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 48.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint               = LoginColors.SuccessGreen,
                            modifier           = Modifier.size(56.dp)
                        )
                        Text(
                            text       = if (authState is AuthState.SignUpSuccess) "회원가입 성공!" else "로그인 성공!",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = LoginColors.TextPrimary
                        )
                        Text(
                            text      = if (authState is AuthState.SignUpSuccess) "환영합니다 🎉\n잠시 후 이동합니다." else "잠시 후 이동합니다.",
                            fontSize  = 13.sp,
                            color     = LoginColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
