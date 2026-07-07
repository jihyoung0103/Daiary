package com.smu.daiary.feature.schedule

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.smu.daiary.R
import com.smu.daiary.data.model.CalendarEvent
import com.smu.daiary.data.source.CalendarDataSource
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private data class ScheduleColorScheme(
    val bg: Color,
    val surface: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accent: Color,
    val border: Color
)

private val ScheduleColorsLight = ScheduleColorScheme(
    bg = Ivory,
    surface = Dew,
    textPrimary = Ink,
    textMuted = Stone,
    accent = SageForest,
    border = Linen
)

private val ScheduleColorsDark = ScheduleColorScheme(
    bg = BackgroundDark,
    surface = SurfaceDark,
    textPrimary = TextPrimaryDark,
    textMuted = TextSecondaryDark,
    accent = SageForestDark,
    border = BorderDark
)

private enum class ScheduleViewState { LOADING, NO_PERMISSION, EMPTY, CONTENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleViewScreen(
    date: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val sc = if (isDark) ScheduleColorsDark else ScheduleColorsLight
    val calendarDataSource = remember { CalendarDataSource(context) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 8.dp.roundToPx() }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted = it }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect
        isLoading = true
        val localDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        events = runCatching { calendarDataSource.fetchEventsForDate(localDate) }.getOrDefault(emptyList())
        isLoading = false
    }

    val displayDate = runCatching {
        val d = LocalDate.parse(date)
        "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일"
    }.getOrDefault(date)

    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val daysFromToday = parsedDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    val badgeText = when (daysFromToday) {
        null -> null
        0L -> stringResource(R.string.schedule_badge_today)
        1L -> stringResource(R.string.schedule_badge_tomorrow)
        else -> if (daysFromToday >= 2L) stringResource(R.string.schedule_badge_days_later, daysFromToday.toInt()) else null
    }

    val subtitleText = if (daysFromToday != null && daysFromToday < 0L)
        stringResource(R.string.schedule_subtitle_past)
    else
        stringResource(R.string.schedule_subtitle_upcoming)

    val emptyText = when {
        daysFromToday == 0L -> stringResource(R.string.schedule_empty_today)
        daysFromToday != null && daysFromToday < 0L -> stringResource(R.string.schedule_empty_past)
        else -> stringResource(R.string.schedule_empty_upcoming)
    }

    Scaffold(
        containerColor = sc.bg,
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = displayDate,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = sc.textPrimary
                            )
                            if (badgeText != null) {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { slideOffsetPx }
                                ) {
                                    DateBadge(text = badgeText, sc = sc)
                                }
                            }
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 12.sp,
                            color = sc.textMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                            tint = sc.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = sc.bg),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        val viewState = when {
            isLoading -> ScheduleViewState.LOADING
            !permissionGranted -> ScheduleViewState.NO_PERMISSION
            events.isEmpty() -> ScheduleViewState.EMPTY
            else -> ScheduleViewState.CONTENT
        }

        Crossfade(
            targetState = viewState,
            animationSpec = tween(200),
            label = "scheduleContent"
        ) { state ->
            when (state) {
                ScheduleViewState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = sc.accent)
                    }
                }

                ScheduleViewState.NO_PERMISSION -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "캘린더 권한이 필요합니다",
                            fontSize = 14.sp,
                            color = sc.textMuted
                        )
                    }
                }

                ScheduleViewState.EMPTY -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyText,
                            fontSize = 14.sp,
                            color = sc.textMuted
                        )
                    }
                }

                ScheduleViewState.CONTENT -> {
                    var listVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { listVisible = true }
                    AnimatedVisibility(
                        visible = listVisible,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { slideOffsetPx }
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(events) { event ->
                                EventItem(event = event, isDark = isDark, sc = sc)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateBadge(text: String, sc: ScheduleColorScheme) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(sc.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = sc.accent
        )
    }
}

@Composable
private fun EventItem(event: CalendarEvent, isDark: Boolean, sc: ScheduleColorScheme) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    val startStr = Instant.ofEpochMilli(event.startTime).atZone(zone).format(fmt)
    val endStr = Instant.ofEpochMilli(event.endTime).atZone(zone).format(fmt)
    val cardBg = if (isDark) SurfaceDark else White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(0.5.dp, sc.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(sc.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                tint = sc.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = event.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = sc.textPrimary
            )
            Text(
                text = "$startStr ~ $endStr",
                fontSize = 12.sp,
                color = sc.textMuted
            )
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    fontSize = 12.sp,
                    color = sc.textMuted
                )
            }
        }
    }
}
