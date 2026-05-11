package com.smu.daiary.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.White

private object ProfileColors {
    val Bg = Ivory
    val CardBg = White
    val Accent = SageForest
    val TextPrimary = Ink
    val TextMuted = Stone
    val Border = Linen
    val AvatarBg = Dew
    val Danger = Color(0xFFD32F2F)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    var isDarkMode by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("한국어") }
    var notificationEnabled by remember { mutableStateOf(true) }
    var notificationHour by remember { mutableStateOf(21) }
    var notificationMinute by remember { mutableStateOf(0) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = ProfileColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "프로필",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = ProfileColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                            tint = ProfileColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProfileColors.Bg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 계정 섹션 ──────────────────────────────────────────────────────
            ProfileCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ProfileColors.AvatarBg)
                            .border(1.dp, ProfileColors.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = currentUser?.photoUrl
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "프로필 사진",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val initial = currentUser?.displayName?.firstOrNull()?.toString()
                            if (initial != null) {
                                Text(
                                    text = initial,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ProfileColors.Accent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = ProfileColors.Accent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = currentUser?.displayName ?: "사용자",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfileColors.TextPrimary
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 13.sp,
                            color = ProfileColors.TextMuted
                        )
                    }
                }
            }

            // ── 앱 설정 섹션 ───────────────────────────────────────────────────
            ProfileSectionLabel("앱 설정")
            ProfileCard {
                Column {
                    SwitchRow(
                        label = "다크 모드",
                        checked = isDarkMode,
                        onCheckedChange = { isDarkMode = it }
                    )
                    ProfileDivider()
                    ArrowRow(
                        label = "언어",
                        value = language,
                        onClick = { showLanguageDialog = true }
                    )
                    ProfileDivider()
                    SwitchRow(
                        label = "일기 알림",
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it }
                    )
                    if (notificationEnabled) {
                        ProfileDivider()
                        ArrowRow(
                            label = "알림 시간",
                            value = "%02d:%02d".format(notificationHour, notificationMinute),
                            onClick = { showTimePickerDialog = true }
                        )
                    }
                }
            }

            // ── 개인정보 섹션 ──────────────────────────────────────────────────
            ProfileSectionLabel("개인정보")
            ProfileCard {
                Column {
                    ArrowRow(label = "개인정보 처리방침", onClick = {})
                    ProfileDivider()
                    ArrowRow(label = "서비스 이용약관", onClick = {})
                    ProfileDivider()
                    InfoRow(label = "앱 버전", value = "1.0")
                }
            }

            // ── 계정 관리 섹션 ─────────────────────────────────────────────────
            ProfileSectionLabel("계정 관리")
            ProfileCard {
                Column {
                    SimpleRow(label = "로그아웃", onClick = { showLogoutDialog = true })
                    ProfileDivider()
                    SimpleRow(
                        label = "회원탈퇴",
                        labelColor = ProfileColors.Danger,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── 언어 선택 다이얼로그 ────────────────────────────────────────────────────
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("언어 선택", color = ProfileColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("한국어", "English").forEach { lang ->
                        TextButton(
                            onClick = {
                                language = lang
                                showLanguageDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = lang,
                                color = if (lang == language) ProfileColors.Accent else ProfileColors.TextPrimary,
                                fontWeight = if (lang == language) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("취소", color = ProfileColors.TextMuted)
                }
            },
            containerColor = ProfileColors.CardBg
        )
    }

    // ── 알림 시간 선택 다이얼로그 ──────────────────────────────────────────────
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ProfileColors.CardBg
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "알림 시간 설정",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = ProfileColors.TextPrimary
                    )
                    TimeInput(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTimePickerDialog = false }) {
                            Text("취소", color = ProfileColors.TextMuted)
                        }
                        TextButton(
                            onClick = {
                                notificationHour = timePickerState.hour
                                notificationMinute = timePickerState.minute
                                showTimePickerDialog = false
                            }
                        ) {
                            Text("확인", color = ProfileColors.Accent)
                        }
                    }
                }
            }
        }
    }

    // ── 로그아웃 확인 다이얼로그 ───────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃", color = ProfileColors.TextPrimary) },
            text = { Text("정말 로그아웃 하시겠어요?", color = ProfileColors.TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }
                ) {
                    Text("로그아웃", color = ProfileColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = ProfileColors.TextMuted)
                }
            },
            containerColor = ProfileColors.CardBg
        )
    }

    // ── 회원탈퇴 확인 다이얼로그 ───────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("회원탈퇴", color = ProfileColors.TextPrimary) },
            text = {
                Text(
                    "계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다. 정말 탈퇴하시겠어요?",
                    color = ProfileColors.TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount()
                    }
                ) {
                    Text("탈퇴", color = ProfileColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = ProfileColors.TextMuted)
                }
            },
            containerColor = ProfileColors.CardBg
        )
    }
}

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ProfileColors.CardBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, ProfileColors.Border)
    ) {
        content()
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = ProfileColors.TextMuted,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = ProfileColors.Border,
        thickness = 0.5.dp
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = ProfileColors.TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = ProfileColors.Accent,
                uncheckedThumbColor = White,
                uncheckedTrackColor = ProfileColors.Border
            )
        )
    }
}

@Composable
private fun ArrowRow(
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = ProfileColors.TextPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = ProfileColors.TextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ProfileColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SimpleRow(
    label: String,
    labelColor: Color = ProfileColors.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = labelColor
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = ProfileColors.TextPrimary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = ProfileColors.TextMuted
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenPreview() {
    DaiaryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ProfileColors.Bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ProfileColors.AvatarBg)
                                .border(1.dp, ProfileColors.Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = ProfileColors.Accent)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("홍길동", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ProfileColors.TextPrimary)
                            Text("example@email.com", fontSize = 13.sp, color = ProfileColors.TextMuted)
                        }
                    }
                }

                ProfileSectionLabel("앱 설정")
                ProfileCard {
                    Column {
                        SwitchRow(label = "다크 모드", checked = false, onCheckedChange = {})
                        ProfileDivider()
                        ArrowRow(label = "언어", value = "한국어", onClick = {})
                        ProfileDivider()
                        SwitchRow(label = "일기 알림", checked = true, onCheckedChange = {})
                        ProfileDivider()
                        ArrowRow(label = "알림 시간", value = "21:00", onClick = {})
                    }
                }

                ProfileSectionLabel("개인정보")
                ProfileCard {
                    Column {
                        ArrowRow(label = "개인정보 처리방침", onClick = {})
                        ProfileDivider()
                        ArrowRow(label = "서비스 이용약관", onClick = {})
                        ProfileDivider()
                        InfoRow(label = "앱 버전", value = "1.0")
                    }
                }

                ProfileSectionLabel("계정 관리")
                ProfileCard {
                    Column {
                        SimpleRow(label = "로그아웃", onClick = {})
                        ProfileDivider()
                        SimpleRow(label = "회원탈퇴", labelColor = ProfileColors.Danger, onClick = {})
                    }
                }
            }
        }
    }
}
