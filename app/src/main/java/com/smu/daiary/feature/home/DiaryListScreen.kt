package com.smu.daiary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.ui.components.SkeletonBox
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
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
import java.time.LocalDate

/**
 * 작성한 모든 일기를 일기 날짜(작성 날짜 아님) 내림차순으로 보여주는 리스트 화면.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    diaries: List<DiaryEntry>,
    isLoading: Boolean = false,
    onDiaryClick: (DiaryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val bg = if (isDark) BackgroundDark else Ivory
    val surface = if (isDark) SurfaceDark else White
    val textPrimary = if (isDark) TextPrimaryDark else Ink
    val textMuted = if (isDark) TextSecondaryDark else Stone
    val accent = if (isDark) SageForestDark else SageForest
    val border = if (isDark) BorderDark else Linen

    // 일기 날짜(entry.date, "YYYY-MM-DD") 기준 내림차순. 문자열 사전식 정렬 = 날짜 정렬.
    val sorted = remember(diaries) { diaries.sortedByDescending { it.date } }

    Scaffold(
        modifier = modifier,
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "모든 일기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(5) { DiaryRowSkeleton(surface = surface, border = border) }
            }
        } else if (sorted.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "작성한 일기가 없어요", fontSize = 14.sp, color = textMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sorted) { entry ->
                    DiaryRow(
                        entry = entry,
                        surface = surface,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted,
                        accent = accent,
                        onClick = { onDiaryClick(entry) }
                    )
                }
            }
        }
    }
}

/** 일기 리스트 행 자리의 로딩 스켈레톤 — shimmer 교체 예정. */
@Composable
private fun DiaryRowSkeleton(
    surface: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 날짜 라벨 자리
            SkeletonBox(
                modifier = Modifier
                    .width(96.dp)
                    .height(13.dp),
                color = border,
                shape = RoundedCornerShape(4.dp)
            )
            // 내용 미리보기 자리(2줄)
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                color = border,
                shape = RoundedCornerShape(4.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp),
                color = border,
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

@Composable
private fun DiaryRow(
    entry: DiaryEntry,
    surface: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textMuted: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val dateLabel = remember(entry.date) {
        runCatching {
            val d = LocalDate.parse(entry.date)
            "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일"
        }.getOrDefault(entry.date)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = dateLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = accent
            )
            Text(
                text = entry.content.replace("\n", " ").ifBlank { "(내용 없음)" },
                fontSize = 14.sp,
                color = textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
