package com.smu.daiary.feature.write

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.outlined.BrokenImage
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.smu.daiary.R
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.LocalDarkTheme
import java.time.LocalDate

private val weatherIcons: Map<String, ImageVector> = mapOf(
    "맑음" to Icons.Outlined.WbSunny,
    "흐림" to Icons.Outlined.Cloud,
    "비"   to Icons.Outlined.Umbrella,
    "눈"   to Icons.Outlined.AcUnit,
    "바람" to Icons.Outlined.Air
)

private val emotionIcons: Map<String, ImageVector> = mapOf(
    "기쁨" to Icons.Outlined.SentimentVerySatisfied,
    "슬픔" to Icons.Outlined.SentimentDissatisfied,
    "평온" to Icons.Outlined.SentimentNeutral,
    "화남" to Icons.Outlined.SentimentVeryDissatisfied,
    "설렘" to Icons.Outlined.FavoriteBorder
)

@Composable
private fun formatDate(raw: String): String {
    val template = stringResource(R.string.date_format_full)
    return runCatching {
        val d = LocalDate.parse(raw)
        String.format(template, d.year, d.monthValue, d.dayOfMonth)
    }.getOrDefault(raw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    entry: DiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    isDeleting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_diary_title)) },
            text = { Text(stringResource(R.string.dialog_delete_diary_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.btn_delete), color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = modifier) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = wc.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_diary),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = wc.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = wc.TextPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isDeleting
                    ) {
                        Text(
                            text = stringResource(R.string.btn_delete),
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    TextButton(
                        onClick = onEdit,
                        enabled = !isDeleting
                    ) {
                        Text(
                            text = stringResource(R.string.btn_edit_diary),
                            color = wc.Purple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg)
            )
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
                text = formatDate(entry.date),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = wc.Purple
            )

            if (entry.weather.isNotEmpty() || entry.emotion.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    weatherIcons[entry.weather]?.let { DetailMetaChip(icon = it, label = localizedWeatherLabel(entry.weather)) }
                    emotionIcons[entry.emotion]?.let { DetailMetaChip(icon = it, label = localizedEmotionLabel(entry.emotion)) }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = wc.SurfaceBg,
                border = BorderStroke(0.5.dp, wc.Border)
            ) {
                Text(
                    text = entry.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = wc.TextPrimary
                )
            }

            if (entry.photos.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.attached_photos),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = wc.TextMuted
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(entry.photos) { uri ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(wc.PurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onState = { imageState = it }
                            )
                            if (imageState is AsyncImagePainter.State.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = wc.Purple,
                                    strokeWidth = 2.dp
                                )
                            }
                            if (imageState is AsyncImagePainter.State.Error) {
                                Icon(
                                    imageVector = Icons.Outlined.BrokenImage,
                                    contentDescription = stringResource(R.string.photo_load_error),
                                    tint = wc.TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isDeleting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = wc.Purple,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
        }
    }
    } // Box
}

@Composable
private fun DetailMetaChip(icon: ImageVector, label: String) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = wc.Purple,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = wc.TextMuted
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryDetailScreenPreview() {
    val sample = DiaryEntry(
        id = "1",
        title = "2026-05-11 일기",
        content = "오늘은 날씨가 맑았다. 스타벅스에서 아메리카노를 마시며 팀 미팅을 준비했다. " +
                "오후에는 8,342걸음을 걸으며 산책을 즐겼고, 저녁엔 사진 정리를 했다. " +
                "전반적으로 알차고 기분 좋은 하루였다.",
        mood = "happy",
        emotion = "기쁨",
        weather = "맑음",
        photos = listOf("content://media/external/images/1001", "content://media/external/images/1002"),
        date = "2026-05-11"
    )
    DaiaryTheme {
        DiaryDetailScreen(entry = sample, onEdit = {}, onDelete = {}, onBack = {})
    }
}
