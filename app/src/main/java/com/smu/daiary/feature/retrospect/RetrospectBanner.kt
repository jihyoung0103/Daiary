package com.smu.daiary.feature.retrospect

import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import com.smu.daiary.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.ui.components.waveHeaderColor
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.Dew
import com.smu.daiary.ui.theme.DewDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.White

/**
 * 홈(캘린더) 화면에서 주간·월간이 반씩 나란히 놓이는 회고 배너.
 * 폭이 좁아 제목 / 기간 / (저장됨 · 버튼) 세 줄로 쌓는다.
 * 지금 생성할 수 있으면(NOT_CREATED) 헤더색으로 칠해 눈에 띄게 한다.
 */
@Composable
fun RetrospectBanner(
    title: String,
    subLabel: String,
    status: BannerStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** UPDATABLE일 때 저장 뒤 달라진 일기 수(늘면 양수) */
    changedCount: Int = 0
) {
    val isDark = LocalDarkTheme.current
    // 새로 만들 수 있거나(미생성) 다시 만들 게 생겼으면(업데이트) 헤더색으로 눈에 띄게 한다
    val creatable = status == BannerStatus.NOT_CREATED || status == BannerStatus.UPDATABLE
    val background = if (creatable) waveHeaderColor() else if (isDark) DewDark else Dew
    val textColor = if (creatable) White else if (isDark) TextPrimaryDark else Ink

    Surface(
        modifier = modifier.clickable(enabled = status != BannerStatus.INSUFFICIENT && status != BannerStatus.LOADING, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(CardCornerRadius)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
            Text(
                // 반폭 카드라 360dp에서 글자 폭이 123dp뿐이다. 문구는 그 안에 한 줄로 들어가게 짧게 둔다
                text = if (status == BannerStatus.INSUFFICIENT) stringResource(R.string.retrospect_banner_insufficient) else subLabel,
                fontSize = 12.sp,
                // 초록 배경 위 흰 글씨는 0.7이면 대비가 모자라 더 진하게
                color = textColor.copy(alpha = if (creatable) 0.9f else 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 옆 배너와 높이를 맞출 때 남는 공간은 버튼 줄 위로 몰아 버튼이 바닥에 붙게 한다
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.weight(1f))
            // 판정 전(LOADING)엔 생성/보기 버튼 줄을 숨긴다
            if (status != BannerStatus.INSUFFICIENT && status != BannerStatus.LOADING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            status == BannerStatus.SAVED -> "✓ 저장됨"
                            status == BannerStatus.UPDATABLE && changedCount > 0 -> "새 일기 ${changedCount}개"
                            status == BannerStatus.UPDATABLE -> "기록이 바뀌었어요"
                            else -> ""
                        },
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Text(
                        text = when (status) {
                            BannerStatus.SAVED -> stringResource(R.string.banner_action_view)
                            BannerStatus.UPDATABLE -> stringResource(R.string.banner_action_update)
                            else -> stringResource(R.string.banner_action_create)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
