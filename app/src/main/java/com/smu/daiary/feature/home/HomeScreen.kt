package com.smu.daiary.feature.home

import com.smu.daiary.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.retrospect.BannerStatus
import com.smu.daiary.feature.retrospect.RetrospectBanner
import com.smu.daiary.ui.components.SkeletonBox
import com.smu.daiary.ui.components.WaveHeader
import com.smu.daiary.ui.components.pullUp
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.DewDark
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.emotionColor
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.Silver
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.util.DiaryDateUtil
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.smu.daiary.feature.dashboard.EMOTIONS
import com.smu.daiary.feature.write.screen.localizedEmotionLabel
import com.smu.daiary.ui.theme.White
import com.smu.daiary.util.KoreanHolidays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// Fix 5: 프로퍼티명 소문자 시작으로 수정
private data class MainCalendarColorScheme(
    val backgroundOuter: Color,
    val surfacePhone: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val calCard: Color,
    val calHeader: Color,
    val accentPurple: Color,
    val dayNames: Color,
    val border: Color,
    val navInactive: Color,
    val dotDiary: Color,
    val sunday: Color,
    val saturday: Color
)

private val MainCalendarColors = MainCalendarColorScheme(
    backgroundOuter = Ivory,
    surfacePhone    = Ivory,
    textPrimary     = Ink,
    textMuted       = Stone,
    calCard         = Dew,
    calHeader       = SageForest,
    accentPurple    = SageForest,
    dayNames        = Stone,
    border          = Linen,
    navInactive     = Silver,
    dotDiary        = SageForest,
    sunday          = Color(0xFFB14A40),
    saturday        = Color(0xFF3F6FAE)
)

private val MainCalendarColorsDark = MainCalendarColorScheme(
    backgroundOuter = BackgroundDark,
    surfacePhone    = BackgroundDark,
    textPrimary     = TextPrimaryDark,
    textMuted       = TextSecondaryDark,
    calCard         = DewDark,
    calHeader       = SageForestDark,
    accentPurple    = SageForestDark,
    dayNames        = TextSecondaryDark,
    border          = BorderDark,
    navInactive     = TextSecondaryDark,
    dotDiary        = SageForestDark,
    sunday          = Color(0xFFE3897E),
    saturday        = Color(0xFF86B0D4)
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    diaries: List<DiaryEntry> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {},
    onWriteDiary: (String) -> Unit = {},
    weeklyBannerStatus: BannerStatus = BannerStatus.INSUFFICIENT,
    monthlyBannerStatus: BannerStatus = BannerStatus.INSUFFICIENT,
    weeklyBannerSubLabel: String = "",
    monthlyBannerSubLabel: String = "",
    onWeeklyBannerClick: () -> Unit = {},
    onMonthlyBannerClick: () -> Unit = {}
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    var visibleMonth by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(mc.backgroundOuter)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            color = mc.surfacePhone,
            shape = RectangleShape,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    MonthHeader(
                        yearMonth = visibleMonth,
                        diaryCount = if (isLoading || error != null) null
                        else diaries.count { it.date.startsWith(visibleMonth.toString()) },
                        onPrevMonth = {
                            visibleMonth = visibleMonth.minusMonths(1)
                            selectedDate = null
                        },
                        onNextMonth = {
                            visibleMonth = visibleMonth.plusMonths(1)
                            selectedDate = null
                        }
                    )
                    // 달력 카드는 헤더 물결 위로 겹쳐 올린다
                    Box(modifier = Modifier.pullUp(64.dp)) {
                    when {
                        error != null -> CalendarErrorPlaceholder(onRetry = onRetry)
                        isLoading -> CalendarCardSkeleton()
                        else -> CalendarCard(
                            yearMonth = visibleMonth,
                            diaries = diaries,
                            selectedDate = selectedDate,
                            onDateSelect = { date ->
                                selectedDate = if (selectedDate == date) null else date
                            }
                        )
                    }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPaddingHorizontal),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isLoading || error != null) {
                            BannerSkeleton()
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BannerSkeleton(Modifier.weight(1f), height = 96.dp)
                                BannerSkeleton(Modifier.weight(1f), height = 96.dp)
                            }
                        } else {
                            // 배너의 "오늘"은 달력의 오늘이 아니라 일기 기준 날짜(04시 이전이면 전날).
                            // FAB(+ 버튼)와 같은 기준이어야 한다. LocalDate.now()를 쓰면 오전 4시
                            // 이전에 두 진입점이 서로 다른 날짜를 가리키고, 일기 유무 판정·이동
                            // 날짜도 작성 플로우와 어긋난다.
                            val today = DiaryDateUtil.diaryDate()
                            val diaryBannerDate = selectedDate ?: today
                            val weekDays = stringArrayResource(R.array.week_days_mon_first)
                            // 제목에 붙일 꼬리표. 달력이 가리키는 실제 날짜(LocalDate.now())를
                            // 기준으로 계산해야 사용자가 보는 달력과 어긋나지 않는다.
                            val dateSuffix = if (diaryBannerDate == LocalDate.now().minusDays(1)) "어제"
                            else weekDays[diaryBannerDate.dayOfWeek.value - 1]
                            val existingDiary = diaries.firstOrNull { it.date == diaryBannerDate.toString() }
                            val diaryBannerState = when {
                                existingDiary != null -> DiaryBannerState.HAS_DIARY
                                diaryBannerDate.isAfter(today) -> DiaryBannerState.FUTURE
                                else -> DiaryBannerState.WRITABLE
                            }
                            DiaryBanner(
                                // 새벽 0~4시엔 달력이 가리키는 날(29일)과 일기 기준일(28일)이
                                // 달라 "오늘의 일기"가 어느 날인지 모호해진다. 이 구간에서는
                                // 날짜와 꼬리표를 함께 보여 어느 날 일기인지 드러낸다.
                                title = if (diaryBannerDate == today && !DiaryDateUtil.isLateNight()) "오늘의 일기"
                                else "${diaryBannerDate.monthValue}월 ${diaryBannerDate.dayOfMonth}일 ($dateSuffix) 일기",
                                subLabel = when (diaryBannerState) {
                                    DiaryBannerState.HAS_DIARY ->
                                        existingDiary?.content?.replace("\n", " ")?.trim()
                                            ?.take(24)?.ifBlank { "작성 완료" } ?: "작성 완료"
                                    DiaryBannerState.WRITABLE -> "아직 작성하지 않았어요"
                                    DiaryBannerState.FUTURE -> ""
                                },
                                state = diaryBannerState,
                                onClick = {
                                    when (diaryBannerState) {
                                        DiaryBannerState.HAS_DIARY -> existingDiary?.let { onDiaryClick(it) }
                                        DiaryBannerState.WRITABLE -> onWriteDiary(diaryBannerDate.toString())
                                        DiaryBannerState.FUTURE -> {}
                                    }
                                }
                            )
                            // 주간·월간 회고는 반씩 나란히 둬서 첫 화면에 스크롤 없이 모든 배너가 보이게 한다
                            Row(
                                modifier = Modifier.height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RetrospectBanner(
                                    title = "이번 주 회고",
                                    subLabel = weeklyBannerSubLabel,
                                    status = weeklyBannerStatus,
                                    onClick = onWeeklyBannerClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                RetrospectBanner(
                                    title = "이번 달 회고",
                                    subLabel = monthlyBannerSubLabel,
                                    status = monthlyBannerStatus,
                                    onClick = onMonthlyBannerClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/** 연도·"N월의 기록"·이번 달 일기 수를 담은 물결 헤더. 달 이동 버튼도 여기 있다. 달력 카드가 이 위로 겹쳐 올라온다 */
@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    diaryCount: Int?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    WaveHeader(
        overline = stringResource(R.string.year_label, yearMonth.year),
        title = stringResource(R.string.month_record_title, yearMonth.monthValue),
        subtitle = diaryCount?.let { stringResource(R.string.month_record_count, it) },
        height = 196.dp,
        topPadding = 28.dp,
        titleTrailing = {
            MonthNavButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.prev_month), onPrevMonth)
            MonthNavButton(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.next_month), onNextMonth)
        }
    )
}

/** 헤더 위 반투명 원 버튼. 터치 영역은 44dp */
@Composable
private fun MonthNavButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = description, tint = White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CalendarCard(
    yearMonth: YearMonth,
    diaries: List<DiaryEntry>,
    selectedDate: LocalDate?,
    onDateSelect: (LocalDate) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    val today = LocalDate.now()
    val leadingEmpty = yearMonth.atDay(1).dayOfWeek.value % 7
    val emotionByDay = remember(yearMonth, diaries) {
        val prefix = yearMonth.toString()
        diaries.filter { it.date.startsWith(prefix) }
            .mapNotNull { e -> e.date.substringAfterLast("-").toIntOrNull()?.let { it to e.emotion } }
            .toMap()
    }

    Column(modifier = Modifier.padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp)) {
        Surface(
            color = mc.calCard,
            shape = RoundedCornerShape(CardCornerRadius),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp)) {
                val weekDays = stringArrayResource(R.array.week_days).toList()
                CalendarWeekRow {
                    weekDays.forEachIndexed { i, d ->
                        Text(
                            text = d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (i) {
                                0 -> mc.sunday
                                6 -> mc.saturday
                                else -> mc.dayNames
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val cells = buildList {
                    repeat(leadingEmpty) { add(null) }
                    for (d in 1..yearMonth.lengthOfMonth()) add(d)
                    while (size % 7 != 0) add(null)
                }
                cells.chunked(7).forEach { week ->
                    CalendarWeekRow {
                        week.forEach { day ->
                            val date = day?.let { yearMonth.atDay(it) }
                            CalendarDayCell(
                                date = date,
                                modifier = Modifier.weight(1f),
                                isToday = date == today,
                                isSelected = date != null && date == selectedDate,
                                isFuture = date != null && date.isAfter(today),
                                emotion = day?.let { emotionByDay[it] },
                                onClick = { date?.let(onDateSelect) }
                            )
                        }
                    }
                }
                // 기분은 원 색으로만 전하지 않도록 범례를 붙인다
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(1.dp)
                        .background(mc.accentPurple.copy(alpha = 0.15f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EMOTIONS.forEach { key ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(8.dp).background(emotionColor(key, isDark), CircleShape))
                            Text(text = localizedEmotionLabel(key), fontSize = 11.sp, color = mc.textMuted)
                        }
                    }
                }
            }
        }
    }
}

/** 캘린더 카드 자리의 로딩 스켈레톤 — shimmer 교체 예정. */
@Composable
private fun CalendarCardSkeleton() {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    Column(modifier = Modifier.padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp)) {
        Surface(
            color = mc.calCard,
            shape = RoundedCornerShape(CardCornerRadius),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                repeat(5) {
                    CalendarWeekRow {
                        repeat(7) {
                            SkeletonBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .height(40.dp),
                                color = mc.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/** 일기/회고 배너 자리의 로딩 스켈레톤 — shimmer 교체 예정. */
@Composable
private fun BannerSkeleton(modifier: Modifier = Modifier.fillMaxWidth(), height: Dp = 60.dp) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    SkeletonBox(
        modifier = modifier.height(height),
        color = mc.border
    )
}

/** 캘린더 카드 자리에 표시하는 에러 안내 — 기존 RecentDiaryList 에러 UI 패턴 재사용. */
@Composable
private fun CalendarErrorPlaceholder(onRetry: () -> Unit) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    Column(modifier = Modifier.padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp)) {
        Surface(
            color = mc.calCard,
            shape = RoundedCornerShape(CardCornerRadius),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.error_load_failed),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = mc.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.btn_retry),
                        color = mc.accentPurple,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * 날짜 칸: 일기 있는 날은 그날 기분 색의 연한 원, 오늘은 테두리 + "오늘", 선택한 날은 채운 원.
 * 일요일·공휴일은 빨강, 토요일은 파랑. 앞으로 올 날은 흐리게.
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    modifier: Modifier = Modifier,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    emotion: String?,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    val holiday = date?.let { KoreanHolidays.nameOf(it) }

    Box(
        modifier = modifier
            .height(44.dp)
            .clickable(enabled = date != null, onClick = onClick)
            .then(
                // 스크린리더에 공휴일 이름도 읽어 준다
                if (date != null && holiday != null) Modifier.semantics { contentDescription = "${date.dayOfMonth}일 $holiday" }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (date == null) return@Box
        val dayColor = when {
            holiday != null || date.dayOfWeek == DayOfWeek.SUNDAY -> mc.sunday
            date.dayOfWeek == DayOfWeek.SATURDAY -> mc.saturday
            else -> mc.textPrimary
        }
        val circle = Modifier.size(34.dp).clip(CircleShape)
        Box(
            modifier = when {
                isSelected -> circle.background(mc.accentPurple)
                isToday -> circle.border(2.dp, mc.accentPurple, CircleShape)
                emotion != null && !isFuture -> circle.background(emotionColor(emotion, isDark).copy(alpha = 0.45f))
                else -> circle
            },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = when {
                    isSelected || isToday -> FontWeight.Bold
                    emotion != null -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                },
                color = when {
                    isSelected -> mc.calCard
                    isToday -> mc.accentPurple
                    isFuture -> dayColor.copy(alpha = 0.4f)
                    else -> dayColor
                }
            )
        }
        if (isToday && !isSelected) {
            Text(
                text = stringResource(R.string.calendar_today),
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold,
                color = mc.accentPurple,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 5.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun HomeScreenPreview() {
    DaiaryTheme {
        HomeScreen()
    }
}
