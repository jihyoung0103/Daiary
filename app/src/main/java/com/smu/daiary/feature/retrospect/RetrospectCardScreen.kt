package com.smu.daiary.feature.retrospect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.R
import com.smu.daiary.data.model.RetrospectReport
import com.smu.daiary.data.model.RetrospectType
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.White
import com.smu.daiary.ui.theme.emotionColor
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class RetroCardType { OPENING, EMOTION, NARRATIVE, ACTIVITY, SPENDING, SCHEDULE, KEYWORDS, MEMORABLE, SUMMARY }

private fun cardsFor(report: RetrospectReport): List<RetroCardType> = buildList {
    add(RetroCardType.OPENING)
    add(RetroCardType.EMOTION)
    add(RetroCardType.NARRATIVE)
    if (report.totalSteps > 0 || report.avgSleepHours > 0f) add(RetroCardType.ACTIVITY)
    if (report.totalSpending > 0) add(RetroCardType.SPENDING)
    if (report.scheduleCount > 0) add(RetroCardType.SCHEDULE)
    add(RetroCardType.KEYWORDS)
    if (report.memorableDate.isNotBlank()) add(RetroCardType.MEMORABLE)
    add(RetroCardType.SUMMARY)
}

private fun periodWord(report: RetrospectReport): String =
    if (report.type == RetrospectType.WEEKLY.name) "이번 주" else "이번 달"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RetrospectCardScreen(
    report: RetrospectReport,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onViewDiary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest

    val pages = remember(report) { cardsFor(report) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val barColor = textColor

    Column(modifier = modifier.fillMaxSize()) {
        // 상단바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        if (pagerState.currentPage == 0) onBack()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = barColor)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (index == pagerState.currentPage) barColor
                                else barColor.copy(alpha = 0.3f)
                            )
                    )
                }
            }
            TextButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pages.lastIndex) }
            }) {
                Text(text = "건너뛰기", color = barColor, fontSize = 13.sp)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (pages[page]) {
                    RetroCardType.OPENING -> OpeningCard(report)
                    RetroCardType.EMOTION -> EmotionCard(report)
                    RetroCardType.NARRATIVE -> NarrativeCard(report)
                    RetroCardType.ACTIVITY -> ActivityCard(report)
                    RetroCardType.SPENDING -> SpendingCard(report)
                    RetroCardType.SCHEDULE -> ScheduleCard(report)
                    RetroCardType.KEYWORDS -> KeywordsCard(report)
                    RetroCardType.MEMORABLE -> MemorableCard(report, onViewDiary)
                    RetroCardType.SUMMARY -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardBg)
                    ) {
                        SummaryContent(
                            report,
                            modifier = Modifier.align(Alignment.Center),
                            textColor = textColor,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }

        // 하단 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pagerState.currentPage == pages.lastIndex) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("저장하기", fontWeight = FontWeight.Medium) }
            } else {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("다음 →", fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun CardContainer(background: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun OpeningCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    CardContainer(cardBg) {
        Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, tint = textColor, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(20.dp))
        Text(report.periodLabel, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
        Text("기록을 돌아봤어요", fontSize = 18.sp, color = textColor, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(periodRangeLabel(report), fontSize = 13.sp, color = textColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(4.dp))
        Text("일기 ${report.diaryCount}편", fontSize = 13.sp, color = textColor.copy(alpha = 0.6f))
    }
}

private fun periodRangeLabel(report: RetrospectReport): String {
    val start = runCatching { LocalDate.parse(report.periodStart) }.getOrNull()
    val end = runCatching { LocalDate.parse(report.periodEnd) }.getOrNull()
    if (start == null || end == null) return ""
    return if (report.type == RetrospectType.WEEKLY.name)
        "${start.monthValue}월 ${start.dayOfMonth}일 ~ ${end.monthValue}월 ${end.dayOfMonth}일"
    else "${start.year}년 ${start.monthValue}월"
}

/** DiaryEditScreen과 동일한 감정 아이콘 세트 사용 */
private fun emotionIcon(emotion: String): ImageVector = when (emotion) {
    "기쁨" -> Icons.Outlined.SentimentVerySatisfied
    "평온" -> Icons.Outlined.SentimentNeutral
    "슬픔" -> Icons.Outlined.SentimentDissatisfied
    "화남" -> Icons.Outlined.SentimentVeryDissatisfied
    "설렘" -> Icons.Outlined.FavoriteBorder
    else -> Icons.Outlined.SentimentNeutral
}

@Composable
private fun EmotionCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest
    CardContainer(cardBg) {
        Text("${periodWord(report)} 감정", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(20.dp))
        val maxCount = (report.emotionDistribution.values.maxOrNull() ?: 1).coerceAtLeast(1)
        val sorted = report.emotionDistribution.entries.sortedByDescending { it.value }
        if (sorted.isEmpty()) {
            Text("이번 기간엔 감정 기록이 없어요", fontSize = 14.sp, color = textColor.copy(alpha = 0.6f))
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sorted.forEach { (emotion, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.width(90.dp)
                        ) {
                            Icon(imageVector = emotionIcon(emotion), contentDescription = null, tint = emotionColor(emotion, isDark), modifier = Modifier.size(16.dp))
                            Text(emotion, fontSize = 14.sp, color = textColor)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(textColor.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(count.toFloat() / maxCount)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(accentColor)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${count}일", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            val top = sorted.first()
            Text("${periodWord(report)} 가장 많은 감정", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = emotionIcon(top.key), contentDescription = null, tint = emotionColor(top.key, isDark), modifier = Modifier.size(22.dp))
                Text(top.key, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
    }
}

@Composable
private fun NarrativeCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    CardContainer(cardBg) {
        Text(
            text = "“${report.aiNarrative}”",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.retrospect_narrative_footer, periodWord(report)),
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ActivityCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    CardContainer(cardBg) {
        Text("${periodWord(report)} 몸은 어땠나요", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
        Spacer(Modifier.height(28.dp))
        if (report.totalSteps > 0) {
            Text("👟 총 걸음수", fontSize = 13.sp, color = textColor.copy(alpha = 0.7f))
            Text("${"%,d".format(report.totalSteps)} 보", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(Modifier.height(20.dp))
        }
        if (report.avgSleepHours > 0f) {
            Text("😴 평균 수면", fontSize = 13.sp, color = textColor.copy(alpha = 0.7f))
            Text("${"%.1f".format(report.avgSleepHours)} 시간", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(Modifier.height(20.dp))
        }
        if (report.bestActivityDay.isNotBlank()) {
            Text("가장 활발했던 날: ${report.bestActivityDay}", fontSize = 13.sp, color = textColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SpendingCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest
    CardContainer(cardBg) {
        Text("${periodWord(report)} 지출", fontSize = 15.sp, color = textColor.copy(alpha = 0.6f))
        Text("${"%,d".format(report.totalSpending)} 원", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(24.dp))
        val sorted = report.spendingByCategory.entries.sortedByDescending { it.value }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sorted.forEach { (category, amount) ->
                val percent = if (report.totalSpending > 0) (amount * 100 / report.totalSpending) else 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category, fontSize = 13.sp, color = textColor, modifier = Modifier.width(60.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(textColor.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("$percent%", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                }
            }
        }
        if (report.topMerchant.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text("가장 자주 간 곳", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
            Text(report.topMerchant, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun ScheduleCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest
    CardContainer(cardBg) {
        Text(
            stringResource(R.string.retrospect_schedule_title, periodWord(report)),
            fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor
        )
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.retrospect_schedule_count_label), fontSize = 13.sp, color = textColor.copy(alpha = 0.7f))
        Text(
            stringResource(R.string.retrospect_schedule_count_value, report.scheduleCount),
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor
        )
        if (report.busiestScheduleDay.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.retrospect_schedule_busiest_day, report.busiestScheduleDay),
                fontSize = 13.sp, color = textColor.copy(alpha = 0.7f)
            )
        }
        if (report.topScheduleTitles.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                report.topScheduleTitles.forEach { title ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Text(title, fontSize = 14.sp, color = textColor.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordsCard(report: RetrospectReport) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest
    CardContainer(cardBg) {
        Text("${periodWord(report)}를 담은 키워드", fontSize = 16.sp, color = textColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(28.dp))
        report.keywords.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowItems.forEachIndexed { i, keyword ->
                    val overallIndex = rowIndex * 2 + i
                    val size = (24 - overallIndex * 2).coerceAtLeast(14)
                    Text("#$keyword", fontSize = size.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MemorableCard(report: RetrospectReport, onViewDiary: (String) -> Unit) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    CardContainer(cardBg) {
        Text("${periodWord(report)} 가장 기억에 남는 날", fontSize = 15.sp, color = textColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))
        Text(memorableDateLabel(report.memorableDate), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(20.dp))
        Text(
            report.memorableReason,
            fontSize = 15.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { onViewDiary(report.memorableDate) }) {
            Text("그 날 일기 보기 →", color = textColor)
        }
    }
}

private fun memorableDateLabel(date: String): String {
    val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    val weekday = listOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")[d.dayOfWeek.value - 1]
    return "${d.monthValue}월 ${d.dayOfMonth}일 $weekday"
}

@Composable
fun SummaryContent(
    report: RetrospectReport,
    modifier: Modifier = Modifier,
    textColor: Color,
    accentColor: Color
) {
    val isDark = LocalDarkTheme.current
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("${report.periodLabel} 요약", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(8.dp))
        val topEmotion = report.emotionDistribution.entries.maxByOrNull { it.value }
        if (topEmotion != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = emotionIcon(topEmotion.key), contentDescription = null, tint = emotionColor(topEmotion.key, isDark), modifier = Modifier.size(18.dp))
                Text("${topEmotion.key} ${topEmotion.value}일", fontSize = 15.sp, color = textColor)
            }
        }
        if (report.totalSteps > 0) {
            Text("👟 ${"%,d".format(report.totalSteps)} 보", fontSize = 15.sp, color = textColor)
        }
        if (report.totalSpending > 0) {
            Text("💳 ${"%,d".format(report.totalSpending)} 원", fontSize = 15.sp, color = textColor)
        }
        if (report.memorableDate.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Outlined.StarOutline, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                Text(memorableDateLabel(report.memorableDate), fontSize = 15.sp, color = textColor)
            }
        }
        if (report.keywords.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(report.keywords.joinToString(" ") { "#$it" }, fontSize = 13.sp, color = accentColor, textAlign = TextAlign.Center)
        }
    }
}

/** 재진입(저장 후 배너 [보기→]) 화면 — 요약 카드 1장 + 전체 다시 보기 */
@Composable
fun RetrospectSummaryScreen(
    report: RetrospectReport,
    onBack: () -> Unit,
    onViewFull: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cardBg)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = textColor)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            SummaryContent(report, textColor = textColor, accentColor = accentColor)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Button(
                onClick = onViewFull,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) { Text("전체 다시 보기", fontWeight = FontWeight.Medium) }
        }
    }
}
