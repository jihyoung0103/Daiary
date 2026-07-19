package com.smu.daiary.feature.write.screen

import com.smu.daiary.feature.write.model.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import com.smu.daiary.R
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.Error
import com.smu.daiary.ui.theme.ErrorDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.White
import com.smu.daiary.ui.theme.emotionColor
import java.time.LocalDate
import androidx.compose.ui.window.Dialog

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
    initialDate: LocalDate,
    diaries: List<DiaryEntry>,
    onWrite: (LocalDate) -> Unit,
    onEdit: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit,
    onBack: () -> Unit,
    isDeleting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val dialogBg = if (isDark) SurfaceDark else White
    val dialogText = if (isDark) TextPrimaryDark else Ink
    val errorColor = if (isDark) ErrorDark else Error

    // 인접 날짜를 좌우 페이지로 넘기는 무한 페이저. 중앙(startIndex)을 initialDate로 매핑.
    val startIndex = 50_000
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { 100_001 })
    fun dateOf(page: Int): LocalDate = initialDate.plusDays((page - startIndex).toLong())
    val currentDate = dateOf(pagerState.currentPage)
    val currentEntry = diaries.firstOrNull { it.date == currentDate.toString() }

    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_diary_title), color = dialogText) },
            text = { Text(stringResource(R.string.dialog_delete_diary_message), color = dialogText) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    currentEntry?.let(onDelete)
                }) {
                    Text(stringResource(R.string.btn_delete), color = errorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = dialogBg
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
                        text = formatDate(currentDate.toString()),
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
                    if (currentEntry != null) {
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isDeleting
                        ) {
                            Text(
                                text = stringResource(R.string.btn_delete),
                                color = errorColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { onEdit(currentEntry) },
                            enabled = !isDeleting
                        ) {
                            Text(
                                text = stringResource(R.string.btn_edit_diary),
                                color = wc.Accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            pageSpacing = 8.dp
        ) { page ->
            val pageDate = dateOf(page)
            val pageEntry = diaries.firstOrNull { it.date == pageDate.toString() }
            DiaryDayContent(
                date = pageDate,
                entry = pageEntry,
                onWrite = { onWrite(pageDate) },
                onImageClick = { selectedImageUri = it }
            )
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
                color = wc.Accent,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
        }
    }

        if (selectedImageUri != null) {
            Dialog(
                onDismissRequest = { selectedImageUri = null }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(dialogBg)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PhotoThumbnail(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        contentScale = ContentScale.Fit,
                        errorIconSize = 32.dp
                    )

                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    } // Box
}

/** 페이저 한 페이지: 특정 날짜의 일기 내용, 없으면 빈 상태(생성하기). */
@Composable
private fun DiaryDayContent(
    date: LocalDate,
    entry: DiaryEntry?,
    onWrite: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    if (entry == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "아직 일기가 없어요...", fontSize = 15.sp, color = wc.TextMuted)
            if (!date.isAfter(LocalDate.now())) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onWrite,
                    colors = ButtonDefaults.buttonColors(containerColor = wc.Accent)
                ) {
                    Text(text = "생성하기", color = White, fontWeight = FontWeight.Medium)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (entry.weather.isNotEmpty() || entry.emotion.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                weatherIcons[entry.weather]?.let { DetailMetaChip(icon = it, label = localizedWeatherLabel(entry.weather)) }
                emotionIcons[entry.emotion]?.let {
                    DetailMetaChip(icon = it, label = localizedEmotionLabel(entry.emotion), tint = emotionColor(entry.emotion, isDark))
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(CardCornerRadius),
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
                    PhotoThumbnail(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { onImageClick(uri) },
                        shape = RoundedCornerShape(12.dp),
                        errorIconSize = 32.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetaChip(icon: ImageVector, label: String, tint: Color? = null) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint ?: wc.Accent,
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
        emotion = "기쁨",
        weather = "맑음",
        photos = listOf("content://media/external/images/1001", "content://media/external/images/1002"),
        date = "2026-05-11"
    )
    DaiaryTheme {
        DiaryDetailScreen(
            initialDate = LocalDate.parse("2026-05-11"),
            diaries = listOf(sample),
            onWrite = {},
            onEdit = {},
            onDelete = {},
            onBack = {}
        )
    }
}
