package com.smu.daiary.feature.auth

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
import androidx.compose.ui.res.stringResource
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.DewDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White
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
import androidx.compose.ui.tooling.preview.Preview
import com.smu.daiary.ui.theme.DaiaryTheme
import kotlinx.coroutines.delay

private data class LoginColorScheme(
    val Background: Color,
    val Surface: Color,
    val InputBg: Color,
    val TextPrimary: Color,
    val TextMuted: Color,
    val AccentPurple: Color,
    val Border: Color,
    val ErrorRed: Color,
    val SuccessGreen: Color,
    val Overlay: Color
)

private val LoginColors = LoginColorScheme(
    Background   = Ivory,
    Surface      = White,
    InputBg      = Dew,
    TextPrimary  = Ink,
    TextMuted    = Stone,
    AccentPurple = SageForest,
    Border       = Linen,
    ErrorRed     = Color(0xFFD32F2F),
    SuccessGreen = Color(0xFF2E7D32),
    Overlay      = Color.Black.copy(alpha = 0.6f)
)

private val LoginColorsDark = LoginColorScheme(
    Background   = BackgroundDark,
    Surface      = SurfaceDark,
    InputBg      = DewDark,
    TextPrimary  = TextPrimaryDark,
    TextMuted    = TextSecondaryDark,
    AccentPurple = SageForestDark,
    Border       = BorderDark,
    ErrorRed     = Color(0xFFEF9A9A),
    SuccessGreen = Color(0xFF81C784),
    Overlay      = Color.Black.copy(alpha = 0.7f)
)

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
    val isDark = LocalDarkTheme.current
    val lc = if (isDark) LoginColorsDark else LoginColors

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab   by remember { mutableIntStateOf(0) }  // 0=로그인, 1=회원가입
    var email         by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var emailError    by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

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
        email         = ""
        password      = ""
        emailError    = null
        passwordError = null
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
            .background(lc.Background),
        contentAlignment = Alignment.Center
    ) {
        // ── 로그인·회원가입 카드 ───────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(24.dp),
            color = lc.Surface,
            border = BorderStroke(1.5.dp, lc.AccentPurple),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 앱 타이틀
                Text(
                    text       = "D.log",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = lc.AccentPurple,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.padding(bottom = 24.dp)
                )

                // ── 탭 ──────────────────────────────────────────────────────────
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = Color.Transparent,
                    contentColor     = lc.AccentPurple,
                    indicator        = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color    = lc.AccentPurple
                        )
                    },
                    divider = {}
                ) {
                    listOf(stringResource(R.string.tab_login), stringResource(R.string.tab_signup)).forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text     = {
                                Text(
                                    text       = title,
                                    fontSize   = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal,
                                    color      = if (selectedTab == index) lc.AccentPurple else lc.TextMuted
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 이메일 입력 ────────────────────────────────────────────────
                OutlinedTextField(
                    value           = email,
                    onValueChange   = { email = it; if (emailError != null) emailError = null },
                    label           = { Text(stringResource(R.string.label_email)) },
                    singleLine      = true,
                    isError         = emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = lc.AccentPurple,
                        focusedLabelColor       = lc.AccentPurple,
                        cursorColor             = lc.AccentPurple,
                        focusedContainerColor   = lc.InputBg,
                        unfocusedContainerColor = lc.InputBg,
                        errorBorderColor        = lc.ErrorRed,
                        errorLabelColor         = lc.ErrorRed,
                        errorContainerColor     = lc.InputBg
                    )
                )
                if (emailError != null) {
                    Text(
                        text     = emailError!!,
                        color    = lc.ErrorRed,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 비밀번호 입력 ──────────────────────────────────────────────
                OutlinedTextField(
                    value                = password,
                    onValueChange        = { password = it; if (passwordError != null) passwordError = null },
                    label                = { Text(stringResource(R.string.label_password)) },
                    singleLine           = true,
                    isError              = passwordError != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier             = Modifier.fillMaxWidth(),
                    shape                = RoundedCornerShape(12.dp),
                    colors               = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = lc.AccentPurple,
                        focusedLabelColor       = lc.AccentPurple,
                        cursorColor             = lc.AccentPurple,
                        focusedContainerColor   = lc.InputBg,
                        unfocusedContainerColor = lc.InputBg,
                        errorBorderColor        = lc.ErrorRed,
                        errorLabelColor         = lc.ErrorRed,
                        errorContainerColor     = lc.InputBg
                    )
                )
                if (passwordError != null) {
                    Text(
                        text     = passwordError!!,
                        color    = lc.ErrorRed,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                // ── 에러 메시지 ────────────────────────────────────────────────
                AnimatedVisibility(visible = authState is AuthState.Error) {
                    Text(
                        text     = (authState as? AuthState.Error)?.message ?: "",
                        color    = lc.ErrorRed,
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
                        emailError = when {
                            email.isBlank()      -> context.getString(R.string.error_email_empty)
                            !email.contains('@') -> context.getString(R.string.error_email_invalid)
                            else                 -> null
                        }
                        passwordError = when {
                            password.isBlank()   -> context.getString(R.string.error_password_empty)
                            password.length < 6  -> context.getString(R.string.error_password_short)
                            else                 -> null
                        }
                        if (emailError == null && passwordError == null) {
                            if (selectedTab == 0) authViewModel.login(email, password)
                            else authViewModel.signUp(email, password)
                        }
                    },
                    enabled  = authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = lc.AccentPurple
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
                            Text(stringResource(R.string.loading_text), color = Color.White, fontSize = 15.sp)
                        }
                    } else {
                        Text(
                            text       = if (selectedTab == 0) stringResource(R.string.tab_login) else stringResource(R.string.tab_signup),
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
                    HorizontalDivider(modifier = Modifier.weight(1f), color = lc.Border)
                    Text(stringResource(R.string.or_divider), fontSize = 12.sp, color = lc.TextMuted)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = lc.Border)
                }

                // ── Google 로그인 버튼 ─────────────────────────────────────────
                OutlinedButton(
                    onClick  = { googleLauncher.launch(googleSignInClient.signInIntent) },
                    enabled  = authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, lc.Border),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = lc.TextPrimary
                    )
                ) {
                    Box(
                        modifier         = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF4285F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.google_signin), fontSize = 15.sp, color = lc.TextPrimary)
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
                modifier         = Modifier.fillMaxSize().background(lc.Overlay),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape           = RoundedCornerShape(20.dp),
                    color           = lc.Surface,
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
                            tint               = lc.SuccessGreen,
                            modifier           = Modifier.size(56.dp)
                        )
                        Text(
                            text       = if (authState is AuthState.SignUpSuccess) stringResource(R.string.signup_success) else stringResource(R.string.login_success),
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = lc.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenPreview() {
    DaiaryTheme {
        val lc = LoginColors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(lc.Background),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(24.dp),
                color = lc.Surface,
                border = BorderStroke(1.5.dp, lc.AccentPurple),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "D.log",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = lc.AccentPurple,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    TabRow(
                        selectedTabIndex = 0,
                        containerColor = Color.Transparent,
                        contentColor = lc.AccentPurple,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[0]),
                                color = lc.AccentPurple
                            )
                        },
                        divider = {}
                    ) {
                        listOf("로그인", "회원가입").forEachIndexed { index, title ->
                            Tab(
                                selected = index == 0,
                                onClick = {},
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                                        color = if (index == 0) lc.AccentPurple else lc.TextMuted
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("이메일") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = lc.AccentPurple,
                            focusedLabelColor = lc.AccentPurple,
                            cursorColor = lc.AccentPurple,
                            focusedContainerColor = lc.InputBg,
                            unfocusedContainerColor = lc.InputBg
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("비밀번호") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = lc.AccentPurple,
                            focusedLabelColor = lc.AccentPurple,
                            cursorColor = lc.AccentPurple,
                            focusedContainerColor = lc.InputBg,
                            unfocusedContainerColor = lc.InputBg
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = lc.AccentPurple)
                    ) {
                        Text("로그인", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = lc.Border)
                        Text("  또는  ", fontSize = 12.sp, color = lc.TextMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = lc.Border)
                    }
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, lc.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = lc.TextPrimary)
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF4285F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Google로 계속하기", fontSize = 15.sp, color = lc.TextPrimary)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SignUpSuccessOverlayPreview() {
    DaiaryTheme {
        val lc = LoginColors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(lc.Overlay),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = lc.Surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = lc.SuccessGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "회원가입 성공",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = lc.TextPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginSuccessOverlayPreview() {
    DaiaryTheme {
        val lc = LoginColors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(lc.Overlay),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = lc.Surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = lc.SuccessGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "로그인 성공",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = lc.TextPrimary
                    )
                }
            }
        }
    }
}
