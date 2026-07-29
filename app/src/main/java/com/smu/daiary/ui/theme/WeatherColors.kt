package com.smu.daiary.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 날씨 라벨("맑음"/"흐림"/"비"/"뇌우"/"눈") → 아이콘 tint 컬러.
 * [emotionColor]와 같은 방식 — canonical 값을 받아 라이트/다크 색을 돌려준다.
 *
 * 고정 5종 밖의 값(질답 "기타" 자유입력 등)은 감정과 동일하게 무채색으로 떨어뜨린다.
 */
fun weatherColor(label: String, isDarkTheme: Boolean): Color = when (label) {
    "맑음" -> if (isDarkTheme) WeatherSunnyDark else WeatherSunny
    "흐림" -> if (isDarkTheme) WeatherCloudyDark else WeatherCloudy
    "비"   -> if (isDarkTheme) WeatherRainDark else WeatherRain
    "뇌우" -> if (isDarkTheme) WeatherThunderDark else WeatherThunder
    "눈"   -> if (isDarkTheme) WeatherSnowDark else WeatherSnow
    else -> if (isDarkTheme) TextSecondaryDark else Stone
}
