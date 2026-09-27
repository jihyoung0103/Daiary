package com.smu.daiary.feature.retrospect

import com.smu.daiary.data.model.RetrospectReport

/** 배너 노출 상태. LOADING = 판정 전(앱 시작 직후). UPDATABLE = 저장된 회고가 있지만 그 뒤 기간 내 일기 수가 달라져 다시 만들 수 있음 */
enum class BannerStatus { LOADING, NOT_CREATED, SAVED, INSUFFICIENT, UPDATABLE }

/** 회고를 만들 수 있는 최소 일기 수 */
internal const val MIN_DIARY_COUNT = 3

/**
 * 저장된 회고의 일기 수와 지금 기간 내 일기 수로 배너 상태를 정한다.
 * 반환의 두 번째 값은 저장 뒤 달라진 일기 수(늘면 양수, 지웠으면 음수), UPDATABLE이 아니면 0.
 *
 * 개수만 비교하므로 기존 일기의 내용만 고친 경우는 잡지 못한다.
 * 지금 개수를 못 읽었거나(null) 다시 만들 최소 개수에 못 미치면 저장된 회고를 그대로 보여 준다.
 */
internal fun bannerStatusOf(savedCount: Int?, currentCount: Int?): Pair<BannerStatus, Int> {
    if (savedCount == null) {
        val count = currentCount ?: 0
        return (if (count < MIN_DIARY_COUNT) BannerStatus.INSUFFICIENT else BannerStatus.NOT_CREATED) to 0
    }
    if (currentCount == null || currentCount == savedCount || currentCount < MIN_DIARY_COUNT) {
        return BannerStatus.SAVED to 0
    }
    return BannerStatus.UPDATABLE to (currentCount - savedCount)
}

/**
 * 회고가 어느 시점 기준인지: "9월 1일 ~ 25일 기준 · 일기 17개".
 * 끝 날짜는 기간 끝과 만든 날 중 이른 쪽 — 기간 중에 만든 회고는 만든 날까지의 기록이다.
 */
internal fun basisLabelOf(report: RetrospectReport, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
    val start = runCatching { java.time.LocalDate.parse(report.periodStart) }.getOrNull() ?: return ""
    val periodEnd = runCatching { java.time.LocalDate.parse(report.periodEnd) }.getOrNull() ?: return ""
    val made = if (report.createdAt > 0L) java.time.Instant.ofEpochMilli(report.createdAt).atZone(zone).toLocalDate() else periodEnd
    val end = minOf(periodEnd, made)
    val endText = if (end.monthValue == start.monthValue) "${end.dayOfMonth}일" else "${end.monthValue}월 ${end.dayOfMonth}일"
    return "${start.monthValue}월 ${start.dayOfMonth}일 ~ $endText 기준 · 일기 ${report.diaryCount}개"
}

/** AI 회고 호출 결과 (내러티브 + 키워드 + 기억에 남는 하루) */
data class RetrospectAiResult(
    val narrative: String,
    val keywords: List<String>,
    val memorableDate: String,
    val memorableReason: String
)

/** RetrospectViewModel의 화면 전환 상태 */
sealed class RetrospectState {
    object Idle : RetrospectState()
    object Loading : RetrospectState()          // 저장된 회고 재조회 (openSaved)
    object LoadingData : RetrospectState()      // Firestore 쿼리
    object Aggregating : RetrospectState()      // 로컬 집계
    object GeneratingAI : RetrospectState()     // AI 호출
    data class CardView(val report: RetrospectReport) : RetrospectState()   // 카드 표시
    data class Summary(val report: RetrospectReport) : RetrospectState()   // 재진입 요약
    data class Error(val message: String) : RetrospectState()
}
