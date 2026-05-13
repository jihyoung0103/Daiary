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
import androidx.compose.material3.Scaffold
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
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.DewDark
import com.smu.daiary.ui.theme.FernDark
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.MoodSad
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.Fern
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.Silver
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White
import java.time.LocalDate
import java.time.YearMonth

private data class MainCalendarColorScheme(
    val BackgroundOuter: Color,
    val SurfacePhone: Color,
    val TextPrimary: Color,
    val TextMuted: Color,
    val CalCard: Color,
    val CalHeader: Color,
    val AccentPurple: Color,
    val DayNames: Color,
    val Border: Color,
    val NavInactive: Color,
    val DotDiary: Color,
    val MoodMint: Color,
    val MoodYellow: Color
)

private val MainCalendarColors = MainCalendarColorScheme(
    BackgroundOuter = White,
    SurfacePhone    = Ivory,
    TextPrimary     = Ink,
    TextMuted       = Stone,
    CalCard         = Dew,
    CalHeader       = SageForest,
    AccentPurple    = SageForest,
    DayNames        = Stone,
    Border          = Linen,
    NavInactive     = Silver,
    DotDiary        = SageForest,
    MoodMint        = Fern,
    MoodYellow      = Dew
)

private val MainCalendarColorsDark = MainCalendarColorScheme(
    BackgroundOuter = BackgroundDark,
    SurfacePhone    = BackgroundDark,
    TextPrimary     = TextPrimaryDark,
    TextMuted       = TextSecondaryDark,
    CalCard         = DewDark,
    CalHeader       = SageForestDark,
    AccentPurple    = SageForestDark,
    DayNames        = TextSecondaryDark,
    Border          = BorderDark,
    NavInactive     = TextSecondaryDark,
    DotDiary        = SageForestDark,
    MoodMint        = FernDark,
    MoodYellow      = DewDark
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    diaries: List<DiaryEntry> = emptyList(),
    onLogout: () -> Unit = {},
    onStartDiary: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {}
) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    var visibleMonth by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(mc.BackgroundOuter)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            color = mc.SurfacePhone,
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
                .background(mc.TextPrimary.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun TopBarSection(yearMonth: YearMonth) {
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.year_label, yearMonth.year),
            fontSize = 12.sp,
            color = mc.TextMuted,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = stringResource(R.string.month_record_title, yearMonth.monthValue),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = mc.TextPrimary
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
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()
    val first = yearMonth.atDay(1)
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
            color = mc.CalCard,
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
                        Text(text = "‹", fontSize = 22.sp, color = mc.AccentPurple)
                    }
                    Text(
                        text = stringResource(R.string.month_year_label, yearMonth.year, yearMonth.monthValue),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.CalHeader
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNextMonth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "›", fontSize = 22.sp, color = mc.AccentPurple)
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
                            color = mc.DayNames,
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
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday    -> mc.AccentPurple
                    isSelected -> mc.CalCard
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
                    isToday    -> mc.CalCard
                    isSelected -> mc.AccentPurple
                    else       -> mc.TextPrimary
                }
            )
            if (hasDiary) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isToday) mc.CalCard else mc.DotDiary)
                )
            }
        }
    }
}

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
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors

    val weekLabels = stringArrayResource(R.array.week_days_mon_first).toList()
    val diaryTitleTemplate = stringResource(R.string.diary_title_date)
    val items = remember(diaries, selectedDate, weekLabels, diaryTitleTemplate) {
        val source = if (selectedDate != null) {
            diaries.filter { it.date == selectedDate.toString() }
        } else {
            diaries.take(5)
        }
        source.mapNotNull { entry ->
            runCatching {
                val localDate = LocalDate.parse(entry.date)
                DiaryListItemUi(
                    day = localDate.dayOfMonth,
                    weekdayLabel = weekLabels[localDate.dayOfWeek.value - 1],
                    title = String.format(diaryTitleTemplate, localDate.year, localDate.monthValue, localDate.dayOfMonth),
                    preview = entry.content.replace("\n", " "),
                    moodColor = when (entry.mood) {
                        "happy" -> mc.MoodMint
                        "sad" -> MoodSad
                        else -> mc.MoodYellow
                    },
                    entry = entry
                )
            }.getOrNull()
        }
    }
    val sectionTitle = if (selectedDate != null)
        stringResource(R.string.date_record_title, selectedDate.monthValue, selectedDate.dayOfMonth)
    else stringResource(R.string.recent_record)
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(
            text = sectionTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = mc.TextMuted,
            letterSpacing = 0.05.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (items.isEmpty()) {
            Text(
                text = if (selectedDate != null) stringResource(R.string.no_diary_on_date)
                       else stringResource(R.string.no_diaries_yet),
                fontSize = 14.sp,
                color = mc.TextMuted,
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
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    val cardBg = if (isDark) SurfaceDark else White
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, mc.Border)
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
                    .background(mc.BackgroundOuter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.day.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = mc.TextPrimary,
                    lineHeight = 16.sp
                )
                Text(
                    text = item.weekdayLabel,
                    fontSize = 9.sp,
                    color = mc.TextMuted
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
                    color = mc.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.preview,
                    fontSize = 12.sp,
                    color = mc.TextMuted,
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
    val isDark = LocalDarkTheme.current
    val mc = if (isDark) MainCalendarColorsDark else MainCalendarColors
    Surface(
        color = mc.SurfacePhone,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(mc.Border)
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
                        tint = mc.AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.nav_calendar),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.AccentPurple
                    )
                }
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.size(50.dp),
                    containerColor = mc.AccentPurple,
                    contentColor = mc.CalCard,
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
                        tint = mc.NavInactive,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.nav_profile),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.NavInactive
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
