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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private object ScheduleColors {
    val Bg = Color(0xFFFFFDF9)
    val Surface = Color(0xFFE8F5E9)
    val TextPrimary = Color(0xFF2C2C2A)
    val TextMuted = Color(0xFF888780)
    val Accent = Color(0xFF2E4739)
    val Border = Color(0xFFD3D1C7)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleViewScreen(
    date: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
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
        containerColor = ScheduleColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = ScheduleColors.TextPrimary
                        )
                        Text(
                            text = "예정된 일정",
                            fontSize = 12.sp,
                            color = ScheduleColors.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                            tint = ScheduleColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScheduleColors.Surface)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ScheduleColors.Accent)
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
                        color = ScheduleColors.TextMuted
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
                        color = ScheduleColors.TextMuted
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
                        EventItem(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    val startStr = Instant.ofEpochMilli(event.startTime).atZone(zone).format(fmt)
    val endStr = Instant.ofEpochMilli(event.endTime).atZone(zone).format(fmt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(0.5.dp, ScheduleColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ScheduleColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                tint = ScheduleColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = event.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScheduleColors.TextPrimary
            )
            Text(
                text = "$startStr ~ $endStr",
                fontSize = 12.sp,
                color = ScheduleColors.TextMuted
            )
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    fontSize = 12.sp,
                    color = ScheduleColors.TextMuted
                )
            }
        }
    }
}
