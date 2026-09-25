package com.smu.daiary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.ui.theme.Black
import com.smu.daiary.ui.theme.Fern
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
 * 화면 상단 공통 헤더: 초록 띠에 흰 제목, 아래쪽에 물결과 둥근 모서리.
 * 바로 아래 카드에 [pullUp]을 걸어 헤더 위로 겹쳐 올리는 것까지가 한 벌의 디자인이다.
 */
@Composable
fun WaveHeader(
    title: String,
    height: Dp,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    /** 위에 앱바가 없는 화면(홈)은 제목이 상태바에 붙지 않게 띄운다 */
    topPadding: Dp = 0.dp
) {
    val bg = waveHeaderColor()
    val wave = if (LocalDarkTheme.current) Color(0xFF3A6B52) else Fern

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(bg)
            .drawBehind {
                val w = size.width
                val h = size.height
                val front = Path().apply {
                    moveTo(0f, h - 40.dp.toPx())
                    cubicTo(w * 0.26f, h - 80.dp.toPx(), w * 0.54f, h - 8.dp.toPx(), w, h - 64.dp.toPx())
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                val back = Path().apply {
                    moveTo(0f, h - 12.dp.toPx())
                    cubicTo(w * 0.33f, h - 36.dp.toPx(), w * 0.66f, h, w, h - 22.dp.toPx())
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(front, wave)
                drawPath(back, Black.copy(alpha = 0.15f))
            }
            .padding(start = 24.dp, end = 24.dp, top = topPadding)
    ) {
        Column {
            if (overline != null) {
                Text(text = overline, fontSize = 13.sp, color = White.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                color = White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = subtitle, fontSize = 13.sp, color = White.copy(alpha = 0.85f))
            }
        }
    }
}

/**
 * 위 요소에 [by]만큼 겹쳐 올린다. offset과 달리 자기 높이도 그만큼 줄여 알려서
 * 아래 요소가 따라 올라오고 빈틈이 생기지 않는다.
 */
fun Modifier.pullUp(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val px = by.roundToPx()
    layout(placeable.width, (placeable.height - px).coerceAtLeast(0)) {
        placeable.place(0, -px)
    }
}
