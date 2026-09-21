package com.smu.daiary.ui.theme

import androidx.compose.ui.graphics.Color

// Light
val SageForest = Color(0xFF3D7A5C)
val Fern = Color(0xFF5A9478)
val GreenDeep = Color(0xFF2E5E46)
val Dew = Color(0xFFEAF2EC)
val Ivory = Color(0xFFFDFAF5)
val WarmSand = Color(0xFFF2EDE4)
val Linen = Color(0xFFE5E0D8)
val Ink = Color(0xFF1C1C1E)
val Stone = Color(0xFF6C6C70)
val Silver = Color(0xFFAEAEB2)

// Dark
val SageForestDark = Color(0xFF4E8C6F)
val FernDark = Color(0xFF4E8A6A)
val DewDark = Color(0xFF1E3329)
val BackgroundDark = Color(0xFF121714)
val SurfaceDark = Color(0xFF1C2420)
val BorderDark = Color(0xFF2C3530)
val TextPrimaryDark = Color(0xFFF0F0F0)
val TextSecondaryDark = Color(0xFFA0A0A0)

// Mood (다크모드 별도 버전 없음 — 라이트/다크 공통으로 사용)
val MoodHappy  = Color(0xFFFFD166)   // 노란색
val MoodNeutral = Fern               // 초록색 (Fern #5A9478)
val MoodSad    = Color(0xFF94B8FF)   // 파란색

// Emotion (Mood와 겹치지 않는 감정만 신규 추가 — 화남/설렘)
val EmotionAnger = Color(0xFFD35C50)       // 화남 - 브릭레드, Error(D32F2F)와 톤 구분
val EmotionAngerDark = Color(0xFFE3897E)
val EmotionExcited = Color(0xFFE0829F)     // 설렘 - 더스티 로즈 핑크
val EmotionExcitedDark = Color(0xFFEDA7C2)

// Weather — 감정 칩과 나란히 놓이므로 Mood/Emotion 색과 의도적으로 구분한다.
// (맑음에 MoodHappy를, 비에 MoodSad를 그대로 쓰면 옆칸 기쁨/슬픔과 같은 색이 되어
//  두 칩이 같은 분류처럼 보인다.) 감정보다 채도를 낮춘 대기 톤으로 묶었다.
val WeatherSunny = Color(0xFFE8A33D)       // 맑음 - 앰버, MoodHappy(FFD166)보다 진함
val WeatherSunnyDark = Color(0xFFF2BC6B)
val WeatherCloudy = Color(0xFF7C848C)      // 흐림 - 블루그레이, Stone(6C6C70) 중립회색과 구분
val WeatherCloudyDark = Color(0xFFA6ADB5)
val WeatherRain = Color(0xFF5B8DB8)        // 비 - 스틸블루, MoodSad(94B8FF)보다 깊음
val WeatherRainDark = Color(0xFF86B0D4)
val WeatherThunder = Color(0xFF7A6BA8)     // 뇌우 - 뮤트 인디고
val WeatherThunderDark = Color(0xFFA296C9)
val WeatherSnow = Color(0xFF7FB3C4)        // 눈 - 아이스 틸
val WeatherSnowDark = Color(0xFFA3CBD8)

// Base
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// Error / 경고
val Error = Color(0xFFD32F2F)
val ErrorDark = Color(0xFFC85C56)
