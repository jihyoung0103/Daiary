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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Thunderstorm
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
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
import com.smu.daiary.ui.components.waveHeaderColor
import com.smu.daiary.ui.theme.Black
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.Error
import com.smu.daiary.ui.theme.ErrorTextDark
import com.smu.daiary.ui.theme.HeaderBottomCorner
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.ScreenPaddingVertical
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.White
import com.smu.daiary.ui.theme.emotionColor
import com.smu.daiary.ui.theme.weatherColor
import com.smu.daiary.util.DiaryDateUtil
import java.time.LocalDate
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog

/** 상세 앱바 줄(뒤로가기·날짜·편집·삭제)을 기본 위치보다 내리는 양. 앱바 높이도 그만큼 늘어난다 */
private val AppBarTopOffset = 2.dp

private val weatherIcons: Map<String, ImageVector> = mapOf(
    "맑음" to Icons.Outlined.WbSunny,
    "흐림" to Icons.Outlined.Cloud,
    "비"   to Icons.Outlined.Umbrella,
    "뇌우" to Icons.Outlined.Thunderstorm,
    "눈"   to Icons.Outlined.AcUnit
)

private val emotionIcons: Map<String, ImageVector> = mapOf(
    "기쁨" to Icons.Outlined.SentimentVerySatisfied,
    "슬픔" to Icons.Outlined.SentimentDissatisfied,
    "평온" to Icons.Outlined.SentimentNeutral,
    "화남" to Icons.Outlined.SentimentVeryDissatisfied,
    "설렘" to Icons.Outlined.FavoriteBorder
)

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
    // 삭제 버튼 + 삭제 확인 다이얼로그 버튼. 다크는 ErrorDark(4.42:1) 대신 글자용 ErrorTextDark
    val errorColor = if (isDark) ErrorTextDark else Error

    // 인접 날짜를 좌우 페이지로 넘기는 무한 페이저. 중앙(startIndex)을 initialDate로 매핑.
    val startIndex = 50_000
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { 100_001 })
    fun dateOf(page: Int): LocalDate = initialDate.plusDays((page - startIndex).toLong())
    val currentDate = dateOf(pagerState.currentPage)
    val currentEntry = diaries.firstOrNull { it.date == currentDate.toString() }
    val locale = LocalConfiguration.current.locales[0]
    val datePattern = stringResource(R.string.timeline_header_date_pattern)

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

    // isDeleting 오버레이와 사진 확대 Dialog가 TopAppBar까지 덮도록 Scaffold의 형제로 겹쳐 쌓는다.
    Box(modifier = Modifier.fillMaxSize()) {
    // 시스템 바 여백은 MainActivity의 바깥 Scaffold가 이미 준다. 여기서 또 주면 하단이 한 번 더 비어 보인다
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = modifier,
        containerColor = wc.Bg,
        topBar = {
            // 날짜는 뒤로가기 바로 옆. 다른 화면 앱바 제목과 같은 18sp Medium
            TopAppBar(
                // 홈 헤더와 같은 초록. 모서리는 아래 페이지의 초록 확장 영역에서 둥글게 끝난다(이음새 없이 같은 색)
                title = {
                    Text(
                        text = currentDate.format(DateTimeFormatter.ofPattern(datePattern, locale)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = White
                        )
                    }
                },
                actions = {
                    if (currentEntry != null) {
                        TextButton(
                            onClick = { onEdit(currentEntry) },
                            enabled = !isDeleting
                        ) {
                            Text(
                                text = stringResource(R.string.btn_edit_diary),
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isDeleting
                        ) {
                            // 초록 위 빨강은 대비가 안 나와(1.0~1.8:1) 흰색. 삭제는 휴지통 아이콘으로 구분
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_delete),
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                // scrollBehavior가 없어 그림자는 0
                colors = TopAppBarDefaults.topAppBarColors(containerColor = waveHeaderColor()),
                // 초록 면 안에서 줄이 위로 붙어 보이지 않도록 AppBarTopOffset만큼 내린다
                windowInsets = WindowInsets(top = AppBarTopOffset)
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
                .background(Black.copy(alpha = 0.35f)),
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
                        shape = RoundedCornerShape(CardCornerRadius),
                        contentScale = ContentScale.Fit,
                        errorIconSize = 32.dp
                    )

                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_desc),
                            tint = White
                        )
                    }
                }
            }
        }
    } // Box
}

/** 페이저 한 페이지: 하루 요약 + 타임라인(날짜는 앱바 제목). 일기가 없으면 빈 상태(생성하기). */
@Composable
private fun DiaryDayContent(
    date: LocalDate,
    entry: DiaryEntry?,
    onWrite: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val headerGreen = waveHeaderColor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = ScreenPaddingVertical)
    ) {
        if (entry == null) {
            // 카드가 없는 날은 앱바 아래를 모서리 반경만큼만 이어 둥글게 마무리한다
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(HeaderBottomCorner)
                    .background(headerGreen, RoundedCornerShape(bottomStart = HeaderBottomCorner, bottomEnd = HeaderBottomCorner))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.empty_diary_placeholder), fontSize = 15.sp, color = wc.TextMuted)
                if (!date.isAfter(DiaryDateUtil.diaryDate())) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onWrite,
                        colors = ButtonDefaults.buttonColors(containerColor = wc.Accent)
                    ) {
                        Text(text = stringResource(R.string.btn_create_diary), color = White, fontWeight = FontWeight.Medium)
                    }
                }
            }
            return@Column
        }

        // 앱바 초록을 요약 카드 위쪽 1/3까지 이어 그려, 카드가 초록에 살짝 걸치게 한다.
        // (절반까지 내리면 초록 덩어리가 커져 앱바 글자가 위로 쏠려 보였다)
        // 카드 높이는 글자 크기에 따라 달라지므로 실제 높이로 계산한다
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // = 위 여백 + 카드 높이 / 3
                    val topPad = ScreenPaddingVertical.toPx()
                    val greenBottom = topPad + (size.height - topPad) / 3f
                    val r = CornerRadius(HeaderBottomCorner.toPx())
                    drawPath(
                        Path().apply {
                            addRoundRect(
                                RoundRect(0f, 0f, size.width, greenBottom, bottomLeftCornerRadius = r, bottomRightCornerRadius = r)
                            )
                        },
                        headerGreen
                    )
                }
                .padding(top = ScreenPaddingVertical)
                .padding(horizontal = ScreenPaddingHorizontal)
        ) {
            DaySummaryCard(entry = entry)
        }

        Column(
            modifier = Modifier.padding(start = ScreenPaddingHorizontal, end = ScreenPaddingHorizontal, top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DiaryTimeline(
                blocks = entry.blocks,
                fallbackText = entry.content,
                onPhotoClick = onImageClick
            )

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
                            shape = RoundedCornerShape(CardCornerRadius),
                            errorIconSize = 32.dp
                        )
                    }
                }
            }
        }
    }
}

/** 헤더 아래 하루 요약: 날씨 · 기분 · 블록 수 */
@Composable
private fun DaySummaryCard(entry: DiaryEntry, modifier: Modifier = Modifier) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    val weatherIcon = weatherIcons[entry.weather]
    val weatherLabel = when {
        weatherIcon != null -> localizedWeatherLabel(entry.weather)
        entry.customWeatherText.isNotBlank() -> entry.customWeatherText
        else -> "-"
    }
    val emotionIcon = emotionIcons[entry.emotion]
    val emotionLabel = when {
        emotionIcon != null -> localizedEmotionLabel(entry.emotion)
        entry.customEmotionText.isNotBlank() -> entry.customEmotionText
        else -> "-"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) wc.SurfaceBg else White,
        border = BorderStroke(0.5.dp, wc.Border)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryCell(
                icon = weatherIcon ?: Icons.Outlined.WbSunny,
                tint = if (weatherIcon != null) weatherColor(entry.weather, isDark) else wc.TextMuted,
                value = weatherLabel,
                caption = stringResource(R.string.timeline_summary_weather)
            )
            VerticalDivider(color = wc.Border)
            SummaryCell(
                icon = emotionIcon ?: Icons.Outlined.SentimentNeutral,
                tint = if (emotionIcon != null) emotionColor(entry.emotion, isDark) else wc.TextMuted,
                value = emotionLabel,
                caption = stringResource(R.string.timeline_summary_mood)
            )
            VerticalDivider(color = wc.Border)
            SummaryCell(
                icon = Icons.Outlined.Timeline,
                tint = wc.Accent,
                value = stringResource(R.string.timeline_summary_blocks_value, entry.blocks.size.coerceAtLeast(1)),
                caption = stringResource(R.string.timeline_summary_blocks)
            )
        }
    }
}

@Composable
private fun RowScope.SummaryCell(icon: ImageVector, tint: Color, value: String, caption: String) {
    val wc = if (LocalDarkTheme.current) WriteColorsDark else WriteColors
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = wc.TextPrimary, maxLines = 1)
        Text(text = caption, fontSize = 11.sp, color = wc.TextMuted)
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
