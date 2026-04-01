package com.smu.daiary

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
import com.smu.daiary.ui.theme.DaiaryTheme
import java.time.LocalDate
import java.time.YearMonth

/** HTML 목업과 동일한 메인 캘린더·일기 목록 화면 색상. */
private object MainCalendarColors {
    val BackgroundOuter = Color(0xFFF1EFE8)
    val SurfacePhone = Color(0xFFFAFAF8)
    val TextPrimary = Color(0xFF2C2C2A)
    val TextMuted = Color(0xFF888780)
    val CalCard = Color(0xFFEEEDFE)
    val CalHeader = Color(0xFF3C3489)
    val AccentPurple = Color(0xFF533AB7)
    val DayNames = Color(0xFF7F77DD)
    val Border = Color(0xFFD3D1C7)
    val NavInactive = Color(0xFFB4B2A9)
    val DotDiary = Color(0xFF9FE1CB)
    val MoodMint = Color(0xFF9FE1CB)
    val MoodYellow = Color(0xFFFAC775)
    val AvatarBg = Color(0xFFEEEDFE)
}

/**
 * 메인 탭: 상단 인사·캘린더 카드·최근 일기·하단 네비(캘린더 / 작성 FAB / 프로필).
 *
 * @param modifier [Scaffold] 안전 영역 등 바깥에서 넘기는 [Modifier].
 */
@Composable
fun MainCalendarScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    var visibleMonth by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }

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
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp)
            ) {
                StatusBarPill()
                TopBarSection(
                    yearMonth = visibleMonth,
                    onAvatarClick = onLogout   // 아바타 클릭 → 로그아웃
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    CalendarCard(
                        yearMonth = visibleMonth,
                        onPrevMonth = {
                            visibleMonth = visibleMonth.minusMonths(1)
                        },
                        onNextMonth = {
                            visibleMonth = visibleMonth.plusMonths(1)
                        }
                    )
                    RecentDiaryList()
                    Spacer(modifier = Modifier.height(24.dp))
                }
                BottomNavBar(
                    onCalendarClick = { /* 현재 탭 */ },
                    onFabClick = { /* TODO: 일기 작성 */ },
                    onProfileClick = { /* TODO: 프로필 */ }
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
private fun TopBarSection(
    yearMonth: YearMonth,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.month_year_label,
                    yearMonth.year,
                    yearMonth.monthValue
                ),
                fontSize = 12.sp,
                color = MainCalendarColors.TextMuted,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = stringResource(R.string.today_record_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MainCalendarColors.TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MainCalendarColors.AvatarBg)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = stringResource(R.string.nav_profile),
                tint = MainCalendarColors.AccentPurple,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CalendarCard(
    yearMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()
    val first = yearMonth.atDay(1)
    /** 일요일=0 … 토요일=6. ISO 요일(월=1…일=7)을 일요일 시작 열에 맞춤. */
    val leadingEmpty = first.dayOfWeek.value % 7
    val hasDiaryDays = remember(yearMonth) {
        // 목업: 월이 2025-03일 때만 일기 있는 날 고정
        if (yearMonth.year == 2025 && yearMonth.monthValue == 3) {
            setOf(5, 7, 10, 14, 17, 19)
        } else {
            emptySet()
        }
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
                                hasDiary = day != null && day in hasDiaryDays,
                                onClick = { /* TODO: 날짜 선택 */ }
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
    hasDiary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isToday) MainCalendarColors.AccentPurple else Color.Transparent
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
                fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
                color = if (isToday) MainCalendarColors.CalCard else MainCalendarColors.TextPrimary
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
    val moodColor: Color
)

@Composable
private fun RecentDiaryList() {
    val samples = listOf(
        DiaryListItemUi(
            19, "수",
            stringResource(R.string.diary_sample_1_title),
            stringResource(R.string.diary_sample_1_preview),
            MainCalendarColors.MoodMint
        ),
        DiaryListItemUi(
            17, "월",
            stringResource(R.string.diary_sample_2_title),
            stringResource(R.string.diary_sample_2_preview),
            MainCalendarColors.MoodYellow
        ),
        DiaryListItemUi(
            14, "금",
            stringResource(R.string.diary_sample_3_title),
            stringResource(R.string.diary_sample_3_preview),
            MainCalendarColors.MoodMint
        )
    )
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(
            text = stringResource(R.string.recent_diaries),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MainCalendarColors.TextMuted,
            letterSpacing = 0.05.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            samples.forEach { item ->
                DiaryRow(item = item, onClick = { /* TODO */ })
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
private fun MainCalendarScreenPreview() {
    DaiaryTheme {
        MainCalendarScreen()
    }
}
