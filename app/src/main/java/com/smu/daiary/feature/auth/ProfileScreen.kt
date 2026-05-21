package com.smu.daiary.feature.auth

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.smu.daiary.R
import com.smu.daiary.feature.notification.cancelNotification
import com.smu.daiary.feature.notification.scheduleNotification
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.DaiaryTheme
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

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(context.packageName) == true
}

private object ProfileColors {
    val Bg = Ivory
    val CardBg = White
    val Accent = SageForest
    val TextPrimary = Ink
    val TextMuted = Stone
    val Border = Linen
    val AvatarBg = Ivory
    val Danger = Color(0xFFD32F2F)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyPolicy: () -> Unit = {},
    onTermsOfService: () -> Unit = {},
    onEditProfile: () -> Unit = {},
) {
    val isDark = LocalDarkTheme.current
    val activity = LocalActivity.current
    val bg = if (isDark) BackgroundDark else ProfileColors.Bg
    val cardBg = if (isDark) SurfaceDark else ProfileColors.CardBg
    val textPrimary = if (isDark) TextPrimaryDark else ProfileColors.TextPrimary
    val textMuted = if (isDark) TextSecondaryDark else ProfileColors.TextMuted
    val borderColor = if (isDark) BorderDark else ProfileColors.Border
    val accentColor = if (isDark) SageForestDark else ProfileColors.Accent
    val avatarBg = if (isDark) DewDark else ProfileColors.AvatarBg

    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("daiary_settings", Context.MODE_PRIVATE) }

    var language by remember { mutableStateOf(prefs.getString("language", "한국어") ?: "한국어") }
    var notificationEnabled by remember { mutableStateOf(prefs.getBoolean("notification_enabled", true)) }
    var notificationHour by remember { mutableStateOf(prefs.getInt("notification_hour", 21)) }
    var notificationMinute by remember { mutableStateOf(prefs.getInt("notification_minute", 0)) }

    var paymentListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                paymentListenerEnabled = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNotificationListenerDialog by remember { mutableStateOf(false) }

    var customPhotoUrl by remember { mutableStateOf<String?>(null) }
    var firestoreDisplayName by remember { mutableStateOf("") }
    var isLoadingPhoto by remember { mutableStateOf(currentUser?.uid != null) }
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: run {
            isLoadingPhoto = false
            return@LaunchedEffect
        }
        isLoadingPhoto = true
        val doc = FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .get().await()
        customPhotoUrl = doc.getString("customPhotoUrl")
        firestoreDisplayName = doc.getString("displayName") ?: ""
        isLoadingPhoto = false
    }

    Scaffold(
        modifier = modifier,
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_profile),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
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
                            .background(avatarBg)
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoadingPhoto -> {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                            customPhotoUrl != null -> {
                                AsyncImage(
                                    model = customPhotoUrl,
                                    contentDescription = stringResource(R.string.profile_photo_desc),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            else -> {
                                Text(
                                    text = firestoreDisplayName.firstOrNull()?.uppercase() ?: "D",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = firestoreDisplayName.ifEmpty { currentUser?.displayName ?: stringResource(R.string.default_user) },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                fontSize = 13.sp,
                                color = textMuted
                            )
                        }
                        TextButton(onClick = onEditProfile) {
                            Text(
                                text = stringResource(R.string.btn_profile_edit),
                                color = accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── 앱 설정 섹션 ───────────────────────────────────────────────────
            ProfileSectionLabel(stringResource(R.string.section_app_settings))
            ProfileCard {
                Column {
                    SwitchRow(
                        label = stringResource(R.string.setting_dark_mode),
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            onDarkModeChange(enabled)
                            activity?.recreate()
                        }
                    )
                    ProfileDivider()
                    ArrowRow(
                        label = stringResource(R.string.setting_language),
                        value = language,
                        onClick = { showLanguageDialog = true }
                    )
                    ProfileDivider()
                    SwitchRow(
                        label = stringResource(R.string.setting_diary_notification),
                        checked = notificationEnabled,
                        onCheckedChange = { enabled ->
                            notificationEnabled = enabled
                            prefs.edit().putBoolean("notification_enabled", enabled).apply()
                            if (enabled) {
                                scheduleNotification(context, notificationHour, notificationMinute)
                            } else {
                                cancelNotification(context)
                            }
                        }
                    )
                    if (notificationEnabled) {
                        ProfileDivider()
                        ArrowRow(
                            label = stringResource(R.string.setting_notification_time),
                            value = "%02d:%02d".format(notificationHour, notificationMinute),
                            onClick = { showTimePickerDialog = true }
                        )
                    }
                    ProfileDivider()
                    SwitchRow(
                        label = stringResource(R.string.setting_payment_notification),
                        checked = paymentListenerEnabled,
                        onCheckedChange = {
                            if (!paymentListenerEnabled) {
                                showNotificationListenerDialog = true
                            } else {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        }
                    )
                }
            }

            // ── 개인정보 섹션 ──────────────────────────────────────────────────
            ProfileSectionLabel(stringResource(R.string.section_privacy))
            ProfileCard {
                Column {
                    ArrowRow(label = stringResource(R.string.privacy_policy), onClick = onPrivacyPolicy)
                    ProfileDivider()
                    ArrowRow(label = stringResource(R.string.terms_of_service), onClick = onTermsOfService)
                    ProfileDivider()
                    InfoRow(label = stringResource(R.string.app_version), value = "1.0")
                }
            }

            // ── 계정 관리 섹션 ─────────────────────────────────────────────────
            ProfileSectionLabel(stringResource(R.string.section_account))
            ProfileCard {
                Column {
                    SimpleRow(label = stringResource(R.string.logout), onClick = { showLogoutDialog = true })
                    ProfileDivider()
                    SimpleRow(
                        label = stringResource(R.string.delete_account),
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
            title = { Text(stringResource(R.string.dialog_language_title), color = textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("한국어", "English").forEach { lang ->
                        TextButton(
                            onClick = {
                                language = lang
                                prefs.edit().putString("language", lang).apply()
                                AppCompatDelegate.setApplicationLocales(
                                    if (lang == "English") LocaleListCompat.forLanguageTags("en")
                                    else LocaleListCompat.forLanguageTags("ko")
                                )
                                showLanguageDialog = false
                                // 언어 변경 즉시 적용을 위해 Activity 재시작
                                activity?.recreate()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = lang,
                                color = if (lang == language) accentColor else textPrimary,
                                fontWeight = if (lang == language) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel), color = textMuted)
                }
            },
            containerColor = cardBg
        )
    }

    // ── 알림 시간 선택 다이얼로그 ──────────────────────────────────────────────
    if (showTimePickerDialog) {
        var hourText by remember { mutableStateOf("%02d".format(notificationHour)) }
        var minuteText by remember { mutableStateOf("%02d".format(notificationMinute)) }
        var hourFocused by remember { mutableStateOf(false) }
        var minuteFocused by remember { mutableStateOf(false) }
        val cardSelected = Color(0xFF3D7A5C)
        val cardUnselected = Color(0xFFC8E6C9)

        Dialog(onDismissRequest = { showTimePickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_notification_time_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ── 시 카드 ──
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp, 72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hourFocused) cardSelected else cardUnselected),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = hourText,
                                    onValueChange = { v ->
                                        hourText = v.filter { it.isDigit() }.take(2)
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (hourFocused) Color.White else Color(0xFF3D7A5C),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .onFocusChanged { hourFocused = it.isFocused }
                                )
                            }
                            Text(
                                text = stringResource(R.string.hour_label),
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }

                        // ── 구분자 ──
                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // ── 분 카드 ──
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp, 72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (minuteFocused) cardSelected else cardUnselected),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = minuteText,
                                    onValueChange = { v ->
                                        minuteText = v.filter { it.isDigit() }.take(2)
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (minuteFocused) Color.White else Color(0xFF3D7A5C),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .onFocusChanged { minuteFocused = it.isFocused }
                                )
                            }
                            Text(
                                text = stringResource(R.string.minute_label),
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTimePickerDialog = false }) {
                            Text(stringResource(R.string.cancel), color = textMuted)
                        }
                        TextButton(
                            onClick = {
                                notificationHour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: notificationHour
                                notificationMinute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: notificationMinute
                                prefs.edit()
                                    .putInt("notification_hour", notificationHour)
                                    .putInt("notification_minute", notificationMinute)
                                    .apply()
                                // 알림이 켜져 있으면 변경된 시각으로 재예약
                                if (notificationEnabled) {
                                    scheduleNotification(context, notificationHour, notificationMinute)
                                }
                                showTimePickerDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.confirm), color = accentColor)
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
            title = { Text(stringResource(R.string.dialog_logout_title), color = textPrimary) },
            text = { Text(stringResource(R.string.dialog_logout_message), color = textMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }
                ) {
                    Text(stringResource(R.string.logout), color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel), color = textMuted)
                }
            },
            containerColor = cardBg
        )
    }

    // ── 회원탈퇴 확인 다이얼로그 ───────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_title), color = textPrimary) },
            text = {
                Text(
                    stringResource(R.string.dialog_delete_message),
                    color = textMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount()
                    }
                ) {
                    Text(stringResource(R.string.btn_delete_confirm), color = ProfileColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = textMuted)
                }
            },
            containerColor = cardBg
        )
    }
    if (showNotificationListenerDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationListenerDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_notification_listener_title),
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_notification_listener_message),
                    color = textMuted,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationListenerDialog = false
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_notification_listener_confirm),
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationListenerDialog = false }) {
                    Text(stringResource(R.string.cancel), color = textMuted)
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    val isDark = LocalDarkTheme.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) SurfaceDark else ProfileColors.CardBg,
        border = BorderStroke(0.5.dp, if (isDark) BorderDark else ProfileColors.Border)
    ) {
        content()
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    val isDark = LocalDarkTheme.current
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = if (isDark) TextSecondaryDark else ProfileColors.TextMuted,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ProfileDivider() {
    val isDark = LocalDarkTheme.current
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = if (isDark) BorderDark else ProfileColors.Border,
        thickness = 0.5.dp
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = LocalDarkTheme.current
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
            color = if (isDark) TextPrimaryDark else ProfileColors.TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = if (isDark) SageForestDark else ProfileColors.Accent,
                uncheckedThumbColor = White,
                uncheckedTrackColor = if (isDark) BorderDark else ProfileColors.Border
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
    val isDark = LocalDarkTheme.current
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
            color = if (isDark) TextPrimaryDark else ProfileColors.TextPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = if (isDark) TextSecondaryDark else ProfileColors.TextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isDark) TextSecondaryDark else ProfileColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SimpleRow(
    label: String,
    labelColor: Color? = null,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val color = labelColor ?: if (isDark) TextPrimaryDark else ProfileColors.TextPrimary
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
            color = color
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val isDark = LocalDarkTheme.current
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
            color = if (isDark) TextPrimaryDark else ProfileColors.TextPrimary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = if (isDark) TextSecondaryDark else ProfileColors.TextMuted
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

                ProfileSectionLabel(stringResource(R.string.section_app_settings))
                ProfileCard {
                    Column {
                        SwitchRow(label = stringResource(R.string.setting_dark_mode), checked = false, onCheckedChange = {})
                        ProfileDivider()
                        ArrowRow(label = stringResource(R.string.setting_language), value = "한국어", onClick = {})
                        ProfileDivider()
                        SwitchRow(label = stringResource(R.string.setting_diary_notification), checked = true, onCheckedChange = {})
                        ProfileDivider()
                        ArrowRow(label = stringResource(R.string.setting_notification_time), value = "21:00", onClick = {})
                    }
                }

                ProfileSectionLabel(stringResource(R.string.section_privacy))
                ProfileCard {
                    Column {
                        ArrowRow(label = stringResource(R.string.privacy_policy), onClick = {})
                        ProfileDivider()
                        ArrowRow(label = stringResource(R.string.terms_of_service), onClick = {})
                        ProfileDivider()
                        InfoRow(label = stringResource(R.string.app_version), value = "1.0")
                    }
                }

                ProfileSectionLabel(stringResource(R.string.section_account))
                ProfileCard {
                    Column {
                        SimpleRow(label = stringResource(R.string.logout), onClick = {})
                        ProfileDivider()
                        SimpleRow(label = stringResource(R.string.delete_account), labelColor = ProfileColors.Danger, onClick = {})
                    }
                }
            }
        }
    }
}
