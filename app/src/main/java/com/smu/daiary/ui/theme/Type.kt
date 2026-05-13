package com.smu.daiary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.smu.daiary.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val NotoSansKR = GoogleFont("Noto Sans KR")

private val NotoSansKRFamily = FontFamily(
    Font(googleFont = NotoSansKR, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NotoSansKR, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = NotoSansKR, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = NotoSansKR, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal,  fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal,  fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal,  fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal,  fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal,  fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal,  fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = NotoSansKRFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
