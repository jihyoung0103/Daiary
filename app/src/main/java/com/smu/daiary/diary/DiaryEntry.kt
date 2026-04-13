package com.smu.daiary.diary

/**
 * Firestore에 저장되는 일기 데이터 모델.
 * 필드 기본값은 Firestore 역직렬화를 위해 반드시 필요합니다.
 */
data class DiaryEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = "neutral",   // "happy" | "neutral" | "sad"
    val date: String = "",           // "YYYY-MM-DD"
    val createdAt: Long = System.currentTimeMillis()
)
