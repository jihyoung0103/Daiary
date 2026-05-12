package com.smu.daiary.feature.write

import androidx.compose.ui.graphics.Color

internal data class WriteColorScheme(
    val Bg: Color,
    val SurfaceBg: Color,
    val TextPrimary: Color,
    val TextMuted: Color,
    val Purple: Color,
    val PurpleLight: Color,
    val Border: Color,
    val MintGreen: Color
)

internal val WriteColors = WriteColorScheme(
    Bg          = Color(0xFFFFFFFF),
    SurfaceBg   = Color(0xFFF7F7F7),
    TextPrimary = Color(0xFF1C1C1E),
    TextMuted   = Color(0xFF6C6C70),
    Purple      = Color(0xFF3D7A5C),
    PurpleLight = Color(0xFFEAF2EC),
    Border      = Color(0xFFE5E0D8),
    MintGreen   = Color(0xFF5A9478)
)

internal val WriteColorsDark = WriteColorScheme(
    Bg          = Color(0xFF121714),
    SurfaceBg   = Color(0xFF1C2420),
    TextPrimary = Color(0xFFF0F0F0),
    TextMuted   = Color(0xFFA0A0A0),
    Purple      = Color(0xFF6BAF8A),
    PurpleLight = Color(0xFF1E3329),
    Border      = Color(0xFF2C3530),
    MintGreen   = Color(0xFF4E8A6A)
)
