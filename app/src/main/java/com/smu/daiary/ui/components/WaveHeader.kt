package com.smu.daiary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.ui.theme.GreenDeep
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.White

/**
 * 헤더 바탕색. 헤더 위에 붙는 앱바도 이 색을 써야 이어져 보인다.
 * 다크에서 DewDark를 쓰면 그 위에 겹치는 카드(DewDark·SurfaceDark)와 구분이 안 돼 한 톤 밝게 둔다.
 */
@Composable
fun waveHeaderColor(): Color = if (LocalDarkTheme.current) GreenDeep else SageForest

/**
 * 화면 상단 공통 헤더: 초록 띠에 흰 제목, 하단은 직선.
 * 높이는 텍스트에 맞춘다(글자 크기를 키워도 잘리지 않게). 아래 카드는 헤더와 겹치지 않고
 * ScreenPaddingVertical만큼 띄워 아이보리 배경에서 시작한다.
 */
@Composable
fun WaveHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    /** 위에 앱바가 없는 화면(홈)은 제목이 상태바에 붙지 않게 띄운다 */
    topPadding: Dp = 0.dp,
    /** 텍스트 아래 여백. 기본은 위 여백과 같게 */
    bottomPadding: Dp = topPadding,
    /** 텍스트 블록 왼쪽·오른쪽 끝에 붙는 요소(홈의 달 이동 버튼). 있으면 텍스트를 가운데 정렬하고 세로 중앙을 맞춘다 */
    navigationStart: (@Composable () -> Unit)? = null,
    navigationEnd: (@Composable () -> Unit)? = null
) {
    val hasNav = navigationStart != null || navigationEnd != null
    val bg = waveHeaderColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = if (hasNav) 12.dp else 24.dp)
            .padding(top = topPadding, bottom = bottomPadding)
    ) {
        if (hasNav) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                navigationStart?.invoke()
                WaveHeaderTexts(overline, title, subtitle, centered = true, modifier = Modifier.weight(1f))
                navigationEnd?.invoke()
            }
        } else {
            WaveHeaderTexts(overline, title, subtitle, centered = false)
        }
    }
}

/** 헤더의 세 줄 텍스트. centered=false는 기존 왼쪽 정렬 그대로 */
@Composable
private fun WaveHeaderTexts(
    overline: String?,
    title: String,
    subtitle: String?,
    centered: Boolean,
    modifier: Modifier = Modifier
) {
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    Column(modifier, horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        if (overline != null) {
            Text(text = overline, fontSize = 13.sp, color = White.copy(alpha = 0.85f), textAlign = textAlign)
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium, // Pretendard 28sp
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp,
            color = White,
            textAlign = textAlign
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, fontSize = 13.sp, color = White.copy(alpha = 0.85f), textAlign = textAlign)
        }
    }
}
