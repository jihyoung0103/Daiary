package com.smu.daiary.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 감정 라벨("기쁨"/"슬픔"/"평온"/"화남"/"설렘") → 아이콘 tint 컬러.
 * 기쁨/평온/슬픔은 캘린더 mood 점(MoodHappy/MoodNeutral/MoodSad)과 같은 색을 재사용해
 * 캘린더와 감정 아이콘의 색이 어긋나지 않도록 한다.
 */
fun emotionColor(label: String, isDarkTheme: Boolean): Color = when (label) {
    "기쁨" -> MoodHappy
    "평온" -> MoodNeutral
    "슬픔" -> MoodSad
    "화남" -> if (isDarkTheme) EmotionAngerDark else EmotionAnger
    "설렘" -> if (isDarkTheme) EmotionExcitedDark else EmotionExcited
    else -> if (isDarkTheme) TextSecondaryDark else Stone
}
