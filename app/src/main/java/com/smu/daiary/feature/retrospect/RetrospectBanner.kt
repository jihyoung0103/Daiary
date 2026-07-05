package com.smu.daiary.feature.retrospect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.DewDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.TextPrimaryDark

/**
 * 홈(캘린더) 화면 캘린더 카드와 최근 기록 목록 사이에 들어가는 회고 배너.
 * 상태(미생성/저장됨/일기 부족)에 따라 배경색과 우측 버튼이 달라진다.
 */
@Composable
fun RetrospectBanner(
    title: String,
    subLabel: String,
    status: BannerStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val background = if (isDark) DewDark else Dew
    val textColor = if (isDark) TextPrimaryDark else Ink

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = status != BannerStatus.INSUFFICIENT, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                    if (status == BannerStatus.SAVED) {
                        Text(text = "  ✓ 저장됨", fontSize = 11.sp, color = textColor.copy(alpha = 0.85f))
                    }
                }
                Text(
                    text = if (status == BannerStatus.INSUFFICIENT) "일기를 더 작성하면 볼 수 있어요" else subLabel,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            if (status != BannerStatus.INSUFFICIENT) {
                Text(
                    text = if (status == BannerStatus.SAVED) "보기→" else "생성→",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}
