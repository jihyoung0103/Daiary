package com.smu.daiary.feature.home

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

/** 일기 배너 상태 — 선택된(또는 오늘) 날짜 기준으로 결정된다. */
enum class DiaryBannerState {
    /** 해당 날짜에 일기가 있음 → 탭하면 조회 */
    HAS_DIARY,

    /** 일기가 없고 과거/오늘 → 탭하면 생성 */
    WRITABLE,

    /** 미래 날짜 → 작성 불가(비활성 안내) */
    FUTURE
}

/**
 * 홈(캘린더) 화면 회고 배너 위에 놓이는 일기 배너.
 * 선택된 날짜(없으면 오늘)에 일기가 있으면 '보기→', 없으면 '생성→',
 * 미래 날짜면 비활성 안내를 표시한다.
 */
@Composable
fun DiaryBanner(
    title: String,
    subLabel: String,
    state: DiaryBannerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val background = if (isDark) DewDark else Dew
    val textColor = if (isDark) TextPrimaryDark else Ink

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = state != DiaryBannerState.FUTURE, onClick = onClick),
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
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                Text(
                    text = if (state == DiaryBannerState.FUTURE) "아직 작성할 수 없어요" else subLabel,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            when (state) {
                DiaryBannerState.HAS_DIARY -> Text(
                    text = "보기→",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                DiaryBannerState.WRITABLE -> Text(
                    text = "생성→",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                DiaryBannerState.FUTURE -> {}
            }
        }
    }
}
