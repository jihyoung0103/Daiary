package com.smu.daiary.feature.write

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditScreen(
    viewModel: WriteViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var text by remember(draft?.date) {
        mutableStateOf(draft?.editedContent ?: draft?.aiContent ?: "")
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        uris.forEach { viewModel.addPhoto(it.toString()) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = WriteColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "일기 편집",
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
                    TextButton(
                        onClick = {
                            viewModel.updateEditedContent(text)
                            onDone()
                        }
                    ) {
                        Text(
                            text = "완료",
                            color = WriteColors.Purple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WriteColors.SurfaceBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── 텍스트 편집 영역 ──────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, WriteColors.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "일기를 작성해보세요...",
                            color = WriteColors.TextMuted,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = WriteColors.TextPrimary
                        )
                    )
                }
            }

            HorizontalDivider(color = WriteColors.Border, thickness = 0.5.dp)

            // ── 사진 섹션 ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WriteColors.SurfaceBg)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "사진",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = WriteColors.TextPrimary
                    )
                    TextButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "사진 추가",
                            tint = WriteColors.Purple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "추가",
                            color = WriteColors.Purple,
                            fontSize = 13.sp
                        )
                    }
                }

                val photos = draft?.photos.orEmpty()
                if (photos.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(photos) { uri ->
                            PhotoChip(
                                uri = uri,
                                onRemove = { viewModel.removePhoto(uri) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "사진을 추가하면 여기에 표시됩니다",
                        fontSize = 12.sp,
                        color = WriteColors.TextMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoChip(uri: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = WriteColors.PurpleLight,
        border = BorderStroke(0.5.dp, WriteColors.Purple.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = WriteColors.Purple,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = uri.substringAfterLast("/").take(24),
                fontSize = 12.sp,
                color = WriteColors.Purple
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(WriteColors.Purple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "삭제",
                        tint = WriteColors.Purple,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
