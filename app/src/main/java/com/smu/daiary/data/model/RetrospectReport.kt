package com.smu.daiary.data.model

/** 회고 종류 */
enum class RetrospectType { WEEKLY, MONTHLY }

/**
 * Firestore에 저장되는 회고 리포트 모델.
 *
 * 컬렉션 구조:
 *   users/{userId}/retrospects/{id}   id = "yyyy-'W'ww"(주간) | "yyyy-MM"(월간)
 *
 * 모든 필드에 기본값이 있어야 Firestore 역직렬화가 가능합니다.
 */
data class RetrospectReport(
    val id: String = "",
    val type: String = "",              // "WEEKLY" | "MONTHLY"
    val periodStart: String = "",       // "yyyy-MM-dd"
    val periodEnd: String = "",         // "yyyy-MM-dd"
    val periodLabel: String = "",       // "6월 첫째 주" | "2026년 6월"
    val createdAt: Long = 0L,

    // Card 1
    val diaryCount: Int = 0,

    // Card 2
    val emotionDistribution: Map<String, Int> = emptyMap(),

    // Card 3
    val aiNarrative: String = "",

    // Card 4
    val totalSteps: Int = 0,
    val avgSleepHours: Float = 0f,
    val bestActivityDay: String = "",

    // Card 5
    val totalSpending: Int = 0,
    val spendingByCategory: Map<String, Int> = emptyMap(),
    val topMerchant: String = "",

    // Card 6
    val keywords: List<String> = emptyList(),

    // Card 7
    val memorableDate: String = "",
    val memorableReason: String = ""
)
