package com.smu.daiary.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.R
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.write.screen.blockTypeLabel
import com.smu.daiary.feature.write.screen.localizedEmotionLabel
import com.smu.daiary.feature.write.screen.localizedWeatherLabel
import com.smu.daiary.ui.components.WaveHeader
import com.smu.daiary.ui.components.pullUp
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White
import com.smu.daiary.ui.theme.emotionColor
import com.smu.daiary.ui.theme.weatherColor
import com.smu.daiary.util.DiaryDateUtil

private data class DashColors(
    val bg: Color, val card: Color, val border: Color, val track: Color,
    val text: Color, val muted: Color, val accent: Color
)

@Composable
private fun dashColors(): DashColors = if (LocalDarkTheme.current)
    DashColors(BackgroundDark, SurfaceDark, BorderDark, BorderDark, TextPrimaryDark, TextSecondaryDark, SageForestDark)
else
    DashColors(Ivory, White, Linen, Linen, Ink, Stone, SageForest)

/** 쓴 일기로 계산한 기록 통계. 수치는 모두 기기에 이미 불러온 일기 목록에서 나온다 */
@Composable
fun DashboardScreen(
    diaries: List<DiaryEntry>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val c = dashColors()
    val isDark = LocalDarkTheme.current
    val stats = remember(diaries) { dashboardStatsOf(diaries, DiaryDateUtil.diaryDate()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
    ) {
        WaveHeader(
            title = stringResource(R.string.dashboard_title),
            subtitle = if (isLoading) null else stringResource(R.string.dashboard_total, stats.total),
            height = 196.dp,
            topPadding = 28.dp
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
            return@Column
        }

        Column(
            modifier = Modifier.padding(horizontal = ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 핵심 수치는 차트가 아니라 숫자 타일로
            Surface(
                modifier = Modifier.fillMaxWidth().pullUp(64.dp),
                shape = RoundedCornerShape(20.dp),
                color = c.card,
                border = BorderStroke(0.5.dp, c.border),
                shadowElevation = 4.dp
            ) {
                Row(Modifier.padding(vertical = 18.dp).height(IntrinsicSize.Min)) {
                    StatTile(stringResource(R.string.dashboard_this_month), stringResource(R.string.dashboard_count_value, stats.thisMonth), c)
                    VerticalDivider(color = c.border)
                    StatTile(stringResource(R.string.dashboard_streak), stringResource(R.string.dashboard_days_value, stats.currentStreak), c)
                    VerticalDivider(color = c.border)
                    StatTile(stringResource(R.string.dashboard_longest), stringResource(R.string.dashboard_days_value, stats.longestStreak), c)
                }
            }

            if (stats.total == 0) {
                Text(
                    text = stringResource(R.string.dashboard_empty),
                    fontSize = 14.sp,
                    color = c.muted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                )
                return@Column
            }

            Section(stringResource(R.string.dashboard_section_monthly), c) {
                MonthlyBars(stats, c)
            }

            Section(stringResource(R.string.dashboard_section_emotion), c) {
                val max = stats.emotionCounts.maxOf { it.second }
                stats.emotionCounts.forEach { (key, n) ->
                    HBar(localizedEmotionLabel(key), n, max, emotionColor(key, isDark), c)
                }
            }

            Section(stringResource(R.string.dashboard_section_weather), c) {
                val max = stats.weatherCounts.maxOf { it.second }
                stats.weatherCounts.forEach { (key, n) ->
                    HBar(localizedWeatherLabel(key), n, max, weatherColor(key, isDark), c)
                }
            }

            if (stats.sourceCounts.isNotEmpty()) {
                Section(stringResource(R.string.dashboard_section_sources), c) {
                    val max = stats.sourceCounts.first().second
                    stats.sourceCounts.forEach { (type, n) ->
                        HBar(blockTypeLabel(type), n, max, c.accent, c)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: String, c: DashColors) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.text)
        Text(text = label, fontSize = 11.sp, color = c.muted)
    }
}

@Composable
private fun Section(title: String, c: DashColors, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.muted, modifier = Modifier.padding(start = 4.dp))
        Surface(
            shape = RoundedCornerShape(CardCornerRadius),
            color = c.card,
            border = BorderStroke(0.5.dp, c.border)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) { content() }
        }
    }
}

/** 가로 막대 한 줄: 이름 · 막대 · 개수. 개수를 옆에 적어 색만으로 읽지 않게 한다 */
@Composable
private fun HBar(label: String, count: Int, max: Int, color: Color, c: DashColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 13.sp, color = c.text, modifier = Modifier.width(72.dp), maxLines = 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(c.track, RoundedCornerShape(4.dp))
        ) {
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(count.toFloat() / max.coerceAtLeast(1))
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = c.text,
            modifier = Modifier.width(36.dp).padding(start = 8.dp)
        )
    }
}

/** 최근 6개월 세로 막대. 한 가지 색, 값은 막대 위에 직접 적는다 */
@Composable
private fun MonthlyBars(stats: DashboardStats, c: DashColors) {
    val max = stats.monthlyCounts.maxOf { it.second }.coerceAtLeast(1)
    val barArea = 96.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.monthlyCounts.forEachIndexed { i, (month, n) ->
            val isCurrent = i == stats.monthlyCounts.lastIndex
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = n.toString(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.text)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.height(barArea).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (n > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(barArea * (n.toFloat() / max))
                                .background(c.accent, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dashboard_month_label, month.monthValue),
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) c.text else c.muted
                )
            }
        }
    }
}
