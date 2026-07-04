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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RetrospectLoadingScreen(
    periodLabel: String,
    state: RetrospectState,
    modifier: Modifier = Modifier
) {
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
            .background(RetroDeepGreen)
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
            color = Color.White,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(28.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = RetroMint,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
