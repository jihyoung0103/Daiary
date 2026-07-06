package com.smu.daiary.feature.schedule

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

    Scaffold(
        containerColor = sc.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = sc.textPrimary
                        )
                        Text(
                            text = "예정된 일정",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = sc.surface)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = sc.accent)
                }
            }

            !permissionGranted -> {
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

            events.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이 날에 예정된 일정이 없습니다",
                        fontSize = 14.sp,
                        color = sc.textMuted
                    )
                }
            }

            else -> {
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
