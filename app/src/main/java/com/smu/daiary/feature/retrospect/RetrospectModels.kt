package com.smu.daiary.feature.retrospect

import com.smu.daiary.data.model.RetrospectReport

/** 배너 노출 상태 */
enum class BannerStatus { NOT_CREATED, SAVED, INSUFFICIENT }

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
