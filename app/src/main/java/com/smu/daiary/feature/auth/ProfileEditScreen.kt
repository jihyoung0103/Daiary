package com.smu.daiary.feature.auth

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.smu.daiary.R
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    val bg = if (isDark) BackgroundDark else Ivory
    val textPrimary = if (isDark) TextPrimaryDark else Ink
    val textMuted = if (isDark) TextSecondaryDark else Stone
    val borderColor = if (isDark) BorderDark else Linen
    val accentColor = if (isDark) SageForestDark else SageForest
    val avatarBg = if (isDark) SurfaceDark else Ivory

    val isGoogleUser = currentUser?.providerData?.any { it.providerId == "google.com" } ?: false

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var customPhotoUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val doc = FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .get().await()
        customPhotoUrl = doc.getString("customPhotoUrl")
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            selectedPhotoUri = uri
        }
    }

    fun validate(): String? {
        if (displayName.isBlank()) return context.getString(R.string.profile_name_empty)
        if (!isGoogleUser && newPassword.isNotEmpty()) {
            if (currentPassword.isEmpty()) return context.getString(R.string.profile_current_password_required)
            if (newPassword.length < 6) return context.getString(R.string.profile_password_short)
            if (newPassword != confirmPassword) return context.getString(R.string.profile_password_mismatch)
        }
        return null
    }

    fun save() {
        val error = validate()
        if (error != null) { errorMessage = error; return }
        errorMessage = null
        isLoading = true
        coroutineScope.launch {
            try {
                // 사진 선택 시 Firebase Storage 업로드 후 다운로드 URL 획득
                val photoDownloadUri: Uri? = if (selectedPhotoUri != null && currentUser != null) {
                    val storageRef = FirebaseStorage.getInstance()
                        .reference.child("users/${currentUser.uid}/profile.jpg")
                    storageRef.putFile(selectedPhotoUri!!).await()
                    storageRef.downloadUrl.await()
                } else null

                // Firestore에 customPhotoUrl 저장 (앱 자체 사진만 추적, Google 사진 무시)
                if (photoDownloadUri != null && currentUser != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(currentUser.uid)
                        .set(mapOf("customPhotoUrl" to photoDownloadUri.toString()), SetOptions.merge())
                        .await()
                }

                // 이름·사진 업데이트
                val builder = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                photoDownloadUri?.let { builder.setPhotoUri(it) }
                currentUser?.updateProfile(builder.build())?.await()

                // 비밀번호 변경 (이메일 로그인 사용자만)
                if (!isGoogleUser && newPassword.isNotEmpty()) {
                    val credential = EmailAuthProvider.getCredential(
                        currentUser?.email ?: "", currentPassword
                    )
                    currentUser?.reauthenticate(credential)?.await()
                    currentUser?.updatePassword(newPassword)?.await()
                }

                isLoading = false
                onBack()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage
                    ?: context.getString(R.string.profile_save_error)
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accentColor,
        unfocusedBorderColor = borderColor,
        focusedLabelColor = accentColor,
        unfocusedLabelColor = textMuted,
        cursorColor = accentColor,
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        disabledTextColor = textMuted,
        disabledBorderColor = borderColor,
        disabledLabelColor = textMuted,
        disabledLeadingIconColor = textMuted,
        disabledTrailingIconColor = textMuted
    )

    Scaffold(
        modifier = modifier,
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_profile_edit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::save, enabled = !isLoading) {
                        Text(
                            text = if (isLoading)
                                stringResource(R.string.saving)
                            else
                                stringResource(R.string.btn_save),
                            color = if (isLoading) textMuted else accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 프로필 사진 영역 ─────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val displayUri: Any? = selectedPhotoUri ?: customPhotoUrl
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(avatarBg)
                            .border(1.5.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (displayUri != null) {
                            AsyncImage(
                                model = displayUri,
                                contentDescription = stringResource(R.string.profile_photo_desc),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = "D",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    TextButton(onClick = { photoLauncher.launch("image/*") }) {
                        Text(
                            text = stringResource(R.string.btn_change_photo),
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── 이름 ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── 이메일 (읽기 전용) ────────────────────────────────────────
                OutlinedTextField(
                    value = currentUser?.email ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.label_email)) },
                    singleLine = true,
                    enabled = false,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── 비밀번호 변경 (이메일 로그인 사용자만) ───────────────────
                if (!isGoogleUser) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.section_password_change),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )

                    PasswordField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = stringResource(R.string.label_current_password),
                        visible = currentPasswordVisible,
                        onToggleVisible = { currentPasswordVisible = !currentPasswordVisible },
                        colors = fieldColors
                    )
                    PasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = stringResource(R.string.label_new_password),
                        visible = newPasswordVisible,
                        onToggleVisible = { newPasswordVisible = !newPasswordVisible },
                        colors = fieldColors
                    )
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = stringResource(R.string.label_confirm_password),
                        visible = confirmPasswordVisible,
                        onToggleVisible = { confirmPasswordVisible = !confirmPasswordVisible },
                        colors = fieldColors
                    )
                }

                // ── 에러 메시지 ───────────────────────────────────────────────
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── 저장 중 로딩 오버레이 ─────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SageForest)
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff
                                  else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = Stone
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = colors,
        modifier = modifier.fillMaxWidth()
    )
}
