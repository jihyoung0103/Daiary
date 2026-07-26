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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.retrospect.BannerStatus
import com.smu.daiary.feature.retrospect.RetrospectBanner
import com.smu.daiary.ui.components.SkeletonBox
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
    val dotDiary: Color
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
    dotDiary        = SageForest
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
    dotDiary        = SageForestDark
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    diaries: List<DiaryEntry> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onStartDiary: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {},
    onWriteDiary: (String) -> Unit = {},
    onViewAllDiaries: () -> Unit = {},
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
                StatusBarPill()
                TopBarSection(yearMonth = visibleMonth)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when {
                        error != null -> CalendarErrorPlaceholder(onRetry = onRetry)
                        isLoading -> CalendarCardSkeleton()
                        else -> CalendarCard(
                            yearMonth = visibleMonth,
                            diaries = diaries,
                            selectedDate = selectedDate,
                            onDateSelect = { date ->
                                selectedDate = if (selectedDate == date) null else date
                            },
                            onPrevMonth = {
                                visibleMonth = visibleMonth.minusMonths(1)
                                selectedDate = null
                            },
                            onNextMonth = {
                                visibleMonth = visibleMonth.plusMonths(1)
                                selectedDate = null
                            }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPaddingHorizontal),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isLoading || error != null) {
                            BannerSkeleton()
                            BannerSkeleton()
                            BannerSkeleton()
                        } else {
                            val today = LocalDate.now()
                            val diaryBannerDate = selectedDate ?: today
                            val existingDiary = diaries.firstOrNull { it.date == diaryBannerDate.toString() }
                            val diaryBannerState = when {
                                existingDiary != null -> DiaryBannerState.HAS_DIARY
                                diaryBannerDate.isAfter(today) -> DiaryBannerState.FUTURE
                                else -> DiaryBannerState.WRITABLE
                            }
                            DiaryBanner(
                                title = if (diaryBannerDate == today) "오늘의 일기"
                                else "${diaryBannerDate.monthValue}월 ${diaryBannerDate.dayOfMonth}일 일기",
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
                            RetrospectBanner(
                                title = "이번 주 회고",
                                subLabel = weeklyBannerSubLabel,
                                status = weeklyBannerStatus,
                                onClick = onWeeklyBannerClick
                            )
                            RetrospectBanner(
                                title = "이번 달 회고",
                                subLabel = monthlyBannerSubLabel,
                                status = monthlyBannerStatus,
                                onClick = onMonthlyBannerClick
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 작성한 모든 일기를 일기 날짜순으로 보는 리스트 페이지 진입
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPaddingHorizontal)
                            .clickable(onClick = onViewAllDiaries),
                        color = if (isDark) DewDark else Dew,
                        shape = RoundedCornerShape(CardCornerRadius)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "모든 일기 보기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) TextPrimaryDark else Ink
                            )
                            Text(
                                text = "→",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) TextPrimaryDark else Ink
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                BottomNavBar(
                    onCalendarClick = { /* 현재 탭 */ },
                    onFabClick = onStartDiary,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun StatusBarPill() {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(top = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(mc.textPrimary.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun TopBarSection(yearMonth: YearMonth) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = ScreenPaddingHorizontal),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = stringResource(R.string.year_label, yearMonth.year),
                fontSize = 12.sp,
                color = mc.textMuted,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = stringResource(R.string.month_record_title, yearMonth.monthValue),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = mc.textPrimary
            )
        }
    }
}

@Composable
private fun CalendarCard(
    yearMonth: YearMonth,
    diaries: List<DiaryEntry>,
    selectedDate: LocalDate?,
    onDateSelect: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()
    val first = yearMonth.atDay(1)
    val leadingEmpty = first.dayOfWeek.value % 7
    val emotionColorByDay = remember(yearMonth, diaries) {
        val prefix = "${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}"
        diaries
            .filter { it.date.startsWith(prefix) }
            .mapNotNull { entry ->
                entry.date.substringAfterLast("-").toIntOrNull()?.let { day ->
                    // entry.emotion이 빈 문자열(감정 미기록)이면 emotionColor()의 else 분기(회색)가 적용됨
                    day to emotionColor(entry.emotion, isDark)
                }
            }.toMap()
    }

    Column(modifier = Modifier.padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp)) {
        Surface(
            color = mc.calCard,
            shape = RoundedCornerShape(CardCornerRadius)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onPrevMonth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "‹", fontSize = 22.sp, color = mc.accentPurple)
                    }
                    Text(
                        text = stringResource(R.string.month_year_label, yearMonth.year, yearMonth.monthValue),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.calHeader
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNextMonth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "›", fontSize = 22.sp, color = mc.accentPurple)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val weekDays = stringArrayResource(R.array.week_days).toList()
                CalendarWeekRow {
                    weekDays.forEach { d ->
                        Text(
                            text = d,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = mc.dayNames,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val cells = buildList {
                    repeat(leadingEmpty) { add(null) }
                    for (d in 1..daysInMonth) add(d)
                    while (size % 7 != 0) add(null)
                }
                cells.chunked(7).forEach { week ->
                    CalendarWeekRow {
                        week.forEach { day ->
                            CalendarDayCell(
                                day = day,
                                modifier = Modifier.weight(1f),
                                isToday = day != null &&
                                    yearMonth.year == today.year &&
                                    yearMonth.monthValue == today.monthValue &&
                                    day == today.dayOfMonth,
                                // Fix 4: selectedDate?.year 는 남기고, smart-cast 이후 나머지는 .으로
                                isSelected = day != null &&
                                    selectedDate?.year == yearMonth.year &&
                                    selectedDate.monthValue == yearMonth.monthValue &&
                                    selectedDate.dayOfMonth == day,
                                diaryEmotionColor = if (day != null) emotionColorByDay[day] else null,
                                onClick = {
                                    if (day != null) onDateSelect(yearMonth.atDay(day))
                                }
                            )
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
            shape = RoundedCornerShape(CardCornerRadius)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SkeletonBox(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(96.dp)
                        .height(16.dp),
                    color = mc.border,
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
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
private fun BannerSkeleton() {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    SkeletonBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
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
            shape = RoundedCornerShape(CardCornerRadius)
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

@Composable
private fun CalendarDayCell(
    day: Int?,
    modifier: Modifier = Modifier,
    isToday: Boolean,
    isSelected: Boolean,
    diaryEmotionColor: Color?,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> mc.accentPurple
                    else       -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected)
                    Modifier.border(1.dp, mc.accentPurple, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(enabled = day != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day == null) return@Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    isSelected -> mc.calCard
                    isToday    -> mc.accentPurple
                    else       -> mc.textPrimary
                }
            )
            if (diaryEmotionColor != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) mc.calCard else diaryEmotionColor)
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    onCalendarClick: () -> Unit,
    onFabClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Surface(
        color = mc.surfacePhone,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(mc.border)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPaddingHorizontal, vertical = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(onClick = onCalendarClick)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.nav_calendar),
                        tint = mc.accentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.nav_calendar),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.accentPurple
                    )
                }
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.size(50.dp),
                    containerColor = mc.accentPurple,
                    contentColor = mc.calCard,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(onClick = onProfileClick)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.nav_profile),
                        tint = mc.navInactive,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.nav_profile),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.navInactive
                    )
                }
            }
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
