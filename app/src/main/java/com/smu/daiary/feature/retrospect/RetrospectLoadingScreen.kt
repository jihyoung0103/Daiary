package com.smu.daiary.feature.retrospect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.TextPrimaryDark

@Composable
fun RetrospectLoadingScreen(
    periodLabel: String,
    state: RetrospectState,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) BackgroundDark else Ivory
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest

    val (progress, message) = when (state) {
        is RetrospectState.LoadingData -> 0.25f to "일기를 읽는 중..."
        is RetrospectState.Aggregating -> 0.5f to "기록을 정리하는 중..."
        is RetrospectState.GeneratingAI -> 0.8f to "회고를 작성하는 중..."
        is RetrospectState.CardView -> 1f to "거의 다 됐어요 :)"
        else -> 0.1f to "준비하는 중..."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cardBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "✨", fontSize = 40.sp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "${periodLabel}를 돌아보는 중이에요",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(28.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = accentColor,
            trackColor = textColor.copy(alpha = 0.15f),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}
