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
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.DewDark
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.MoodHappy
import com.smu.daiary.ui.theme.MoodNeutral
import com.smu.daiary.ui.theme.MoodSad
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.smu.daiary.ui.theme.Dew
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
    backgroundOuter = White,
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
    onDiaryClick: (DiaryEntry) -> Unit = {}
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
                    RecentDiaryList(
                        diaries = diaries,
                        selectedDate = selectedDate,
                        isLoading = isLoading,
                        error = error,
                        onRetry = onRetry,
                        onDiaryClick = onDiaryClick
                    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
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
    val moodByDay = remember(yearMonth, diaries) {
        val prefix = "${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}"
        diaries
            .filter { it.date.startsWith(prefix) }
            .mapNotNull { entry ->
                entry.date.substringAfterLast("-").toIntOrNull()?.let { day ->
                    day to when (entry.mood) {
                        "happy" -> MoodHappy
                        "sad"   -> MoodSad
                        else    -> MoodNeutral
                    }
                }
            }.toMap()
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Surface(
            color = mc.calCard,
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
                                diaryMoodColor = if (day != null) moodByDay[day] else null,
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
    diaryMoodColor: Color?,
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
                    isToday    -> mc.accentPurple
                    isSelected -> mc.calCard
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
                    isToday    -> mc.calCard
                    isSelected -> mc.accentPurple
                    else       -> mc.textPrimary
                }
            )
            if (diaryMoodColor != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isToday) mc.calCard else diaryMoodColor)
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
    val moodIcon: ImageVector,
    val entry: DiaryEntry
)

@Composable
private fun RecentDiaryList(
    diaries: List<DiaryEntry>,
    selectedDate: LocalDate? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
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
                        "happy"   -> MoodHappy
                        "sad"     -> MoodSad
                        else      -> MoodNeutral
                    },
                    moodIcon = when (entry.mood) {
                        "happy"   -> Icons.Outlined.SentimentVerySatisfied
                        "sad"     -> Icons.Outlined.SentimentDissatisfied
                        else      -> Icons.Outlined.SentimentNeutral
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
            color = mc.textMuted,
            letterSpacing = 0.05.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (isLoading && selectedDate == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = mc.accentPurple,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp
                )
            }
        } else if (error != null && selectedDate == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.error_load_failed),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = mc.textPrimary
                    )
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
        } else if (items.isEmpty()) {
            if (selectedDate != null) {
                Text(
                    text = stringResource(R.string.no_diary_on_date),
                    fontSize = 14.sp,
                    color = mc.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Create,
                            contentDescription = null,
                            tint = mc.accentPurple.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.no_diaries_yet),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = mc.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.no_diaries_subtitle),
                            fontSize = 13.sp,
                            color = mc.textMuted
                        )
                    }
                }
            }
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
        border = BorderStroke(0.5.dp, mc.border)
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
                    .background(mc.backgroundOuter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.day.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = mc.textPrimary,
                    lineHeight = 16.sp
                )
                Text(
                    text = item.weekdayLabel,
                    fontSize = 9.sp,
                    color = mc.textMuted
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
                    color = mc.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.preview,
                    fontSize = 12.sp,
                    color = mc.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = item.moodIcon,
                contentDescription = null,
                tint = item.moodColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
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
