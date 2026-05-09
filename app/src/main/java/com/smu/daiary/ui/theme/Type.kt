package com.smu.daiary.ui.theme

import androidx.compose.ui.graphics.Color
import com.smu.daiary.ui.theme.MainGreen
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 앱 전체 기본 글꼴 스타일(Material [Typography]). 제목·본문 등에 공통으로 쓰입니다.
 */
val Typography = Typography(
    // 1. 큰 제목 (Headline/Display) 스타일 추가
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = MainGreen // ✨
    ),

    // 2. 기본 본문 스타일 (기존 코드에 color 추가)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = Color.Black // 본문은 가독성을 위해 검정이나 어두운 회색 추천
    )
)
