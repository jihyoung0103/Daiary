package com.smu.daiary.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.R
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.write.screen.PhotoThumbnail
import com.smu.daiary.feature.write.screen.RailWidth
import com.smu.daiary.feature.write.screen.localizedEmotionLabel
import com.smu.daiary.feature.write.screen.rail
import com.smu.daiary.ui.components.SkeletonBox
import com.smu.daiary.ui.components.WaveHeader
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White
import com.smu.daiary.ui.theme.emotionColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 카드 첫 줄(날짜) 높이의 가운데. 점과 마지막 선 끝이 여기에 맞는다 */
private val DotCenterY = 27.dp

private data class ListColors(
    val bg: Color, val surface: Color, val border: Color,
    val text: Color, val muted: Color, val accent: Color
)

/**
 * 작성한 모든 일기를 일기 날짜(작성 날짜 아님) 내림차순의 타임라인으로 보여준다.
 * 달이 바뀌는 곳에 월 라벨, 날마다 그날 기분 색의 점을 찍는다.
 */
@Composable
fun DiaryListScreen(
    diaries: List<DiaryEntry>,
    isLoading: Boolean = false,
    onDiaryClick: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val c = if (isDark) ListColors(BackgroundDark, SurfaceDark, BorderDark, TextPrimaryDark, TextSecondaryDark, SageForestDark)
    else ListColors(Ivory, White, Linen, Ink, Stone, SageForest)

    // 일기 날짜(entry.date, "YYYY-MM-DD") 기준 내림차순. 문자열 사전식 정렬 = 날짜 정렬.
    val sorted = remember(diaries) { diaries.sortedByDescending { it.date } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        item(key = "header") {
            WaveHeader(
                title = stringResource(R.string.screen_diary_list),
                subtitle = if (isLoading) null else stringResource(R.string.dashboard_total, sorted.size),
                height = 168.dp,
                topPadding = 28.dp
            )
            Spacer(Modifier.height(20.dp))
        }

        when {
            isLoading -> items(5) {
                DiaryRowSkeleton(c, Modifier.padding(horizontal = ScreenPaddingHorizontal, vertical = 5.dp))
            }
            sorted.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_diaries_written), fontSize = 14.sp, color = c.muted)
                }
            }
            else -> itemsIndexed(sorted, key = { _, e -> e.id.ifEmpty { e.date } }) { index, entry ->
                // "YYYY-MM"이 바뀌는 첫 일기 위에 월 라벨을 붙인다
                val month = entry.date.take(7)
                val newMonth = index == 0 || sorted[index - 1].date.take(7) != month
                Column(Modifier.padding(horizontal = ScreenPaddingHorizontal)) {
                    if (newMonth) MonthLabel(entry.date, isFirst = index == 0, c = c)
                    DiaryRow(
                        entry = entry,
                        isLast = index == sorted.lastIndex,
                        c = c,
                        onClick = { onDiaryClick(entry) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MonthLabel(date: String, isFirst: Boolean, c: ListColors) {
    // Compose는 try/catch 안에서 composable을 부를 수 없어 파싱만 감싼다
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val label = parsed?.let { stringResource(R.string.month_year_label, it.year, it.monthValue) } ?: date.take(7)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFirst) Modifier else Modifier.rail(c.border))
            .padding(top = if (isFirst) 0.dp else 8.dp, bottom = 10.dp)
    ) {
        Spacer(Modifier.width(RailWidth + 12.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.accent)
    }
}

@Composable
private fun DiaryRow(entry: DiaryEntry, isLast: Boolean, c: ListColors, onClick: () -> Unit) {
    val isDark = LocalDarkTheme.current
    val locale = LocalConfiguration.current.locales[0]
    val pattern = stringResource(R.string.list_entry_date_pattern)
    val dateLabel = remember(entry.date, pattern, locale) {
        runCatching { LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern(pattern, locale)) }
            .getOrDefault(entry.date)
    }
    // 본문 블록 사진이 먼저, 옛 일기면 첨부 사진
    val thumbnail = entry.blocks.firstNotNullOfOrNull { it.imageUri } ?: entry.photos.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rail(c.border, to = if (isLast) DotCenterY else null)
            .padding(bottom = if (isLast) 0.dp else 12.dp)
    ) {
        Box(
            modifier = Modifier.width(RailWidth).padding(top = DotCenterY - 7.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 바깥 링을 배경색으로 칠해 점 뒤로 선이 비치지 않게 한다. 점 색 = 그날 기분(달력 점과 같은 규칙)
            Box(
                Modifier
                    .size(14.dp)
                    .background(c.bg, CircleShape)
                    .padding(2.dp)
                    .background(emotionColor(entry.emotion, isDark), CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(CardCornerRadius))
                .clickable(onClick = onClick),
            color = c.surface,
            shape = RoundedCornerShape(CardCornerRadius),
            border = BorderStroke(0.5.dp, c.border)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = dateLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.text)
                        // 기분은 점 색만으로 전하지 않고 이름도 적는다
                        if (entry.emotion.isNotBlank() || entry.customEmotionText.isNotBlank()) {
                            Text(
                                text = if (entry.emotion.isNotBlank()) localizedEmotionLabel(entry.emotion) else entry.customEmotionText,
                                fontSize = 12.sp,
                                color = c.muted,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = entry.content.replace("\n", " ").ifBlank { stringResource(R.string.no_content_placeholder) },
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = c.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (thumbnail != null) {
                    PhotoThumbnail(
                        model = thumbnail,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

/** 일기 행 자리의 로딩 스켈레톤 */
@Composable
private fun DiaryRowSkeleton(c: ListColors, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = c.surface,
        shape = RoundedCornerShape(CardCornerRadius),
        border = BorderStroke(0.5.dp, c.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBox(modifier = Modifier.width(96.dp).height(13.dp), color = c.border, shape = RoundedCornerShape(4.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(14.dp), color = c.border, shape = RoundedCornerShape(4.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp), color = c.border, shape = RoundedCornerShape(4.dp))
        }
    }
}
