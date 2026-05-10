package com.smu.daiary.feature.write

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smu.daiary.ui.theme.DaiaryTheme
import java.time.LocalDate

private val weatherIconMap: Map<String, ImageVector> = mapOf(
    "맑음" to Icons.Outlined.WbSunny,
    "흐림" to Icons.Outlined.Cloud,
    "비"   to Icons.Outlined.Umbrella,
    "눈"   to Icons.Outlined.AcUnit,
    "바람" to Icons.Outlined.Air
)

private val emotionIconMap: Map<String, ImageVector> = mapOf(
    "기쁨" to Icons.Outlined.SentimentVerySatisfied,
    "슬픔" to Icons.Outlined.SentimentDissatisfied,
    "평온" to Icons.Outlined.SentimentNeutral,
    "화남" to Icons.Outlined.SentimentVeryDissatisfied,
    "설렘" to Icons.Outlined.Favorite
)

private fun formatDate(raw: String): String {
    return runCatching {
        val d = LocalDate.parse(raw)
        "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일"
    }.getOrDefault(raw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftPreviewScreen(
    viewModel: WriteViewModel,
    userId: String,
    onEdit: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val selectedWeather by viewModel.selectedWeather.collectAsStateWithLifecycle()
    val selectedEmotion by viewModel.selectedEmotion.collectAsStateWithLifecycle()
    val displayText = draft?.editedContent ?: draft?.aiContent ?: ""

    Scaffold(
        modifier = modifier,
        containerColor = WriteColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "미리보기",
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
                    Text(text = "편집하기", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 날짜
            draft?.let {
                Text(
                    text = formatDate(it.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3D7A5C)
                )
            }

            // 날씨 + 감정
            if (selectedWeather != null || selectedEmotion != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    selectedWeather?.let { label ->
                        weatherIconMap[label]?.let { icon ->
                            MetaChip(icon = icon, label = label)
                        }
                    }
                    selectedEmotion?.let { label ->
                        emotionIconMap[label]?.let { icon ->
                            MetaChip(icon = icon, label = label)
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

            // 첨부 사진 목록
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

@Composable
private fun MetaChip(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = WriteColors.Purple,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = WriteColors.TextMuted
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftPreviewScreenPreview() {
    val sampleDraft = DiaryDraft(
        date = "2026-05-11",
        aiContent = "오늘은 날씨가 맑았다. 스타벅스에서 아메리카노를 마시며 팀 미팅을 준비했다. " +
                "오후에는 8,342걸음을 걸으며 산책을 즐겼고, 저녁엔 사진 정리를 했다. 전반적으로 알차고 기분 좋은 하루였다.",
        editedContent = null,
        photos = emptyList()
    )
    DaiaryTheme {
        Scaffold(
            containerColor = WriteColors.Bg,
            topBar = {
                TopAppBar(
                    title = {
                        Text("미리보기", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = WriteColors.TextPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = WriteColors.TextPrimary)
                        }
                    },
                    actions = {
                        TextButton(onClick = {}) {
                            Text("저장", color = WriteColors.Purple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WriteColors.SurfaceBg)
                )
            },
            bottomBar = {
                Surface(color = WriteColors.SurfaceBg, shadowElevation = 8.dp) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .padding(bottom = 8.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WriteColors.Purple)
                    ) {
                        Text("편집하기", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatDate(sampleDraft.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3D7A5C)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaChip(icon = Icons.Outlined.WbSunny, label = "맑음")
                    MetaChip(icon = Icons.Outlined.SentimentVerySatisfied, label = "기쁨")
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(0.5.dp, WriteColors.Border)
                ) {
                    Text(
                        text = sampleDraft.aiContent,
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = WriteColors.TextPrimary
                    )
                }
            }
        }
    }
}
