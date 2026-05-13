package com.smu.daiary.data.model

/**
 * Firestore에 저장되는 일기 데이터 모델.
 * 필드 기본값은 Firestore 역직렬화를 위해 반드시 필요합니다.
 */
data class DiaryEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = "neutral",              // "happy" | "neutral" | "sad"
    val emotion: String = "",                  // "기쁨" | "슬픔" | "평온" | "화남" | "설렘"
    val weather: String = "",                  // "맑음" | "흐림" | "비" | "눈" | "바람"
    val photos: List<String> = emptyList(),    // 첨부 사진 URI 목록
    val date: String = "",                     // "YYYY-MM-DD"
    val createdAt: Long = System.currentTimeMillis()
)
