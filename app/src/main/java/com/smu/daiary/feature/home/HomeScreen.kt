package com.smu.daiary.feature.home

import com.smu.daiary.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.MoodSad
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.Fern
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.Silver
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.White
import java.time.LocalDate
import java.time.YearMonth

private object MainCalendarColors {
    val BackgroundOuter = White
    val SurfacePhone    = Ivory
    val TextPrimary     = Ink
    val TextMuted       = Stone
    val CalCard         = Dew
    val CalHeader       = SageForest
    val AccentPurple    = SageForest
    val DayNames        = Stone
    val Border          = Linen
    val NavInactive     = Silver
    val DotDiary        = SageForest
    val MoodMint        = Fern
    val MoodYellow      = Dew
}

/**
 * 메인 탭: 상단 인사·캘린더 카드·최근 일기·하단 네비(캘린더 / 작성 FAB / 프로필).
 *
 * @param modifier [Scaffold] 안전 영역 등 바깥에서 넘기는 [Modifier].
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    diaries: List<DiaryEntry> = emptyList(),
    onLogout: () -> Unit = {},
    onStartDiary: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {}
) {
    var visibleMonth by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MainCalendarColors.BackgroundOuter)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            color = MainCalendarColors.SurfacePhone,
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
                    CalendarCard(
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
                    RecentDiaryList(diaries = diaries, selectedDate = selectedDate, onDiaryClick = onDiaryClick)
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
                .background(MainCalendarColors.TextPrimary.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun TopBarSection(yearMonth: YearMonth) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "${yearMonth.year}년",
            fontSize = 12.sp,
            color = MainCalendarColors.TextMuted,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "${yearMonth.monthValue}월의 기록",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MainCalendarColors.TextPrimary
        )
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
    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()
    val first = yearMonth.atDay(1)
    /** 일요일=0 … 토요일=6. ISO 요일(월=1…일=7)을 일요일 시작 열에 맞춤. */
    val leadingEmpty = first.dayOfWeek.value % 7
    val hasDiaryDays = remember(yearMonth, diaries) {
        val prefix = "${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}"
        diaries
            .filter { it.date.startsWith(prefix) }
            .mapNotNull { it.date.substringAfterLast("-").toIntOrNull() }
            .toSet()
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Surface(
            color = MainCalendarColors.CalCard,
            shape = RoundedCornerShape(20.dp)
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
                        Text(
                            text = "‹",
                            fontSize = 22.sp,
                            color = MainCalendarColors.AccentPurple
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.month_year_label,
                            yearMonth.year,
                            yearMonth.monthValue
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MainCalendarColors.CalHeader
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNextMonth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            fontSize = 22.sp,
                            color = MainCalendarColors.AccentPurple
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
                CalendarWeekRow {
                    weekDays.forEach { d ->
                        Text(
                            text = d,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MainCalendarColors.DayNames,
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
                                isSelected = day != null &&
                                    selectedDate?.year == yearMonth.year &&
                                    selectedDate?.monthValue == yearMonth.monthValue &&
                                    selectedDate?.dayOfMonth == day,
                                hasDiary = day != null && day in hasDiaryDays,
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

/** 요일/날짜 열 너비를 동일하게 맞추기 위해 `spacedBy` 없이 7칸 [Row]만 사용합니다. */
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
    hasDiary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday    -> MainCalendarColors.AccentPurple
                    isSelected -> MainCalendarColors.CalCard
                    else       -> Color.Transparent
                }
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
                    isToday    -> MainCalendarColors.CalCard
                    isSelected -> MainCalendarColors.AccentPurple
                    else       -> MainCalendarColors.TextPrimary
                }
            )
            if (hasDiary) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday) MainCalendarColors.CalCard else MainCalendarColors.DotDiary
                        )
                )
            }
        }
    }
}

/** 최근 일기 한 줄에 표시할 데이터(날짜 뱃지·제목·미리보기·무드 색). */
data class DiaryListItemUi(
    val day: Int,
    val weekdayLabel: String,
    val title: String,
    val preview: String,
    val moodColor: Color,
    val entry: DiaryEntry
)

@Composable
private fun RecentDiaryList(
    diaries: List<DiaryEntry>,
    selectedDate: LocalDate? = null,
    onDiaryClick: (DiaryEntry) -> Unit = {}
) {
    val items = remember(diaries, selectedDate) {
        val source = if (selectedDate != null) {
            diaries.filter { it.date == selectedDate.toString() }
        } else {
            diaries.take(5)
        }
        val weekLabels = listOf("월", "화", "수", "목", "금", "토", "일")
        source.mapNotNull { entry ->
            runCatching {
                val localDate = LocalDate.parse(entry.date)
                DiaryListItemUi(
                    day = localDate.dayOfMonth,
                    weekdayLabel = weekLabels[localDate.dayOfWeek.value - 1],
                    title = entry.title,
                    preview = entry.content.replace("\n", " "),
                    moodColor = when (entry.mood) {
                        "happy" -> MainCalendarColors.MoodMint
                        "sad" -> MoodSad
                        else -> MainCalendarColors.MoodYellow
                    },
                    entry = entry
                )
            }.getOrNull()
        }
    }
    val sectionTitle = if (selectedDate != null)
        "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 기록"
    else "최근 기록"
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(
            text = sectionTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MainCalendarColors.TextMuted,
            letterSpacing = 0.05.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (items.isEmpty()) {
            Text(
                text = if (selectedDate != null) "이 날의 일기가 없어요" else "아직 작성된 일기가 없어요",
                fontSize = 14.sp,
                color = MainCalendarColors.TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    DiaryRow(item = item, onClick = { onDiaryClick(item.entry) })
                }
            }
        }
    }
}

@Composable
private fun DiaryRow(item: DiaryListItemUi, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(0.5.dp, MainCalendarColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp, 14.dp, 16.dp, 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MainCalendarColors.BackgroundOuter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.day.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MainCalendarColors.TextPrimary,
                    lineHeight = 16.sp
                )
                Text(
                    text = item.weekdayLabel,
                    fontSize = 9.sp,
                    color = MainCalendarColors.TextMuted
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MainCalendarColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.preview,
                    fontSize = 12.sp,
                    color = MainCalendarColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(item.moodColor)
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    onCalendarClick: () -> Unit,
    onFabClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = MainCalendarColors.SurfacePhone,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MainCalendarColors.Border)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
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
                        tint = MainCalendarColors.AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.nav_calendar),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MainCalendarColors.AccentPurple
                    )
                }
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.size(50.dp),
                    containerColor = MainCalendarColors.AccentPurple,
                    contentColor = MainCalendarColors.CalCard,
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
                        tint = MainCalendarColors.NavInactive,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.nav_profile),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MainCalendarColors.NavInactive
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
