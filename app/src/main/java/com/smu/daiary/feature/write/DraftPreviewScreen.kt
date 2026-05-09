package com.smu.daiary.feature.write

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftPreviewScreen(
    viewModel: WriteViewModel,
    userId: String,
    onEdit: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val displayText = draft?.editedContent ?: draft?.aiContent ?: ""

    Scaffold(
        containerColor = WriteColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "초안 미리보기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = WriteColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                            tint = WriteColors.TextPrimary
                        )
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 16.dp),
                            color = WriteColors.Purple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.saveDraft(userId) { success ->
                                    if (success) onSaved()
                                }
                            }
                        ) {
                            Text(
                                text = "저장",
                                color = WriteColors.Purple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WriteColors.SurfaceBg)
            )
        },
        bottomBar = {
            Surface(color = WriteColors.SurfaceBg, shadowElevation = 8.dp) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WriteColors.Purple)
                ) {
                    Text(
                        text = "편집하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 날짜 + 편집 여부 뱃지
            draft?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(WriteColors.PurpleLight, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = it.date,
                            fontSize = 12.sp,
                            color = WriteColors.Purple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (it.editedContent != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    WriteColors.MintGreen.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "편집됨",
                                fontSize = 12.sp,
                                color = Color(0xFF2C8C6E),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 본문 카드
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, WriteColors.Border)
            ) {
                Text(
                    text = displayText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = WriteColors.TextPrimary
                )
            }

            // 첨부 사진 목록 (URI 이름으로 표시)
            val photos = draft?.photos.orEmpty()
            if (photos.isNotEmpty()) {
                Text(
                    text = "첨부 사진",
                    fontSize = 12.sp,
                    color = WriteColors.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                photos.forEach { uri ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WriteColors.PurpleLight,
                        border = BorderStroke(0.5.dp, WriteColors.Purple.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = WriteColors.Purple,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = uri.substringAfterLast("/").take(40),
                                fontSize = 12.sp,
                                color = WriteColors.Purple
                            )
                        }
                    }
                }
            }
        }
    }
}
