package com.smu.daiary.feature.retrospect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smu.daiary.data.model.DailyData
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.data.model.RetrospectReport
import com.smu.daiary.data.model.RetrospectType
import com.smu.daiary.data.repository.AiRepository
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.data.repository.DiaryRepository
import com.smu.daiary.data.repository.RetrospectRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** 배너 활성화 기준 — 기간 내 일기 3개 미만이면 "일기 부족" 상태 */
private const val MIN_DIARY_COUNT = 3

class RetrospectViewModel : ViewModel() {

    private val diaryRepository = DiaryRepository()
    private val dailyDataRepository = DailyDataRepository()
    private val retrospectRepository = RetrospectRepository()
    private val aiRepository = AiRepository()

    val weeklyPeriod: RetrospectPeriod = RetrospectPeriodCalculator.currentWeek()
    val monthlyPeriod: RetrospectPeriod = RetrospectPeriodCalculator.currentMonth()

    private val _weeklyBannerStatus = MutableStateFlow(BannerStatus.NOT_CREATED)
    val weeklyBannerStatus: StateFlow<BannerStatus> = _weeklyBannerStatus.asStateFlow()

    private val _monthlyBannerStatus = MutableStateFlow(BannerStatus.NOT_CREATED)
    val monthlyBannerStatus: StateFlow<BannerStatus> = _monthlyBannerStatus.asStateFlow()

    private val _state = MutableStateFlow<RetrospectState>(RetrospectState.Idle)
    val state: StateFlow<RetrospectState> = _state.asStateFlow()

    /** 캘린더 화면 진입 시 두 배너의 상태를 판정 */
    fun loadBannerStatuses(userId: String) {
        viewModelScope.launch {
            val weeklyDeferred = async { resolveBannerStatus(userId, weeklyPeriod) }
            val monthlyDeferred = async { resolveBannerStatus(userId, monthlyPeriod) }
            _weeklyBannerStatus.value = weeklyDeferred.await()
            _monthlyBannerStatus.value = monthlyDeferred.await()
        }
    }

    private suspend fun resolveBannerStatus(userId: String, period: RetrospectPeriod): BannerStatus {
        val saved = retrospectRepository.getRetrospect(userId, period.id).getOrNull()
        if (saved != null) return BannerStatus.SAVED

        val diaryCount = diaryRepository.getDiariesInRange(userId, period.startDate, period.endDate)
            .getOrNull()?.size ?: 0
        return if (diaryCount < MIN_DIARY_COUNT) BannerStatus.INSUFFICIENT else BannerStatus.NOT_CREATED
    }

    /** 배너 "생성→" 탭 — 전체 파이프라인 실행 */
    fun generateRetrospect(userId: String, type: RetrospectType) {
        viewModelScope.launch {
            val period = if (type == RetrospectType.WEEKLY) weeklyPeriod else monthlyPeriod

            _state.value = RetrospectState.LoadingData
            val diariesDeferred = async { diaryRepository.getDiariesInRange(userId, period.startDate, period.endDate) }
            val dailyDataDeferred = async { dailyDataRepository.getDailyDataInRange(userId, period.startDate, period.endDate) }
            val diaries = diariesDeferred.await().getOrElse { emptyList() }
            val dailyDataList = dailyDataDeferred.await().getOrElse { emptyList() }

            if (diaries.size < MIN_DIARY_COUNT) {
                _state.value = RetrospectState.Error("일기를 더 작성하면 볼 수 있어요")
                return@launch
            }

            _state.value = RetrospectState.Aggregating
            val agg = aggregate(diaries, dailyDataList)

            _state.value = RetrospectState.GeneratingAI
            val aiResult = aiRepository.generateRetrospect(
                type = type,
                periodLabel = period.titleLabel,
                diarySummaries = buildDiarySummaries(diaries),
                emotionSummary = buildEmotionSummary(agg.emotionDistribution),
                healthSummary = buildHealthSummary(agg),
                spendingSummary = buildSpendingSummary(agg),
                scheduleSummary = buildScheduleSummary(dailyDataList)
            ).getOrNull()

            val fallbackDiary = pickFallbackMemorableDiary(diaries)
            val narrative = aiResult?.narrative
                ?: if (type == RetrospectType.WEEKLY) "이번 주의 기록을 돌아봤어요" else "이번 달의 기록을 돌아봤어요"
            val keywords = aiResult?.keywords?.takeIf { it.isNotEmpty() }
                ?: agg.emotionDistribution.entries.sortedByDescending { it.value }.take(5).map { it.key }
            val memorableDate = aiResult?.memorableDate?.takeIf { it.isNotBlank() }
                ?: fallbackDiary?.date.orEmpty()
            val memorableReason = aiResult?.memorableReason?.takeIf { it.isNotBlank() }
                ?: "이 날의 기록이 유독 인상 깊게 남아있어요"

            val report = RetrospectReport(
                id = period.id,
                type = type.name,
                periodStart = period.startDate,
                periodEnd = period.endDate,
                periodLabel = period.titleLabel,
                createdAt = System.currentTimeMillis(),
                diaryCount = diaries.size,
                emotionDistribution = agg.emotionDistribution,
                aiNarrative = narrative,
                totalSteps = agg.totalSteps,
                avgSleepHours = agg.avgSleepHours,
                bestActivityDay = agg.bestActivityDay,
                totalSpending = agg.totalSpending,
                spendingByCategory = agg.spendingByCategory,
                topMerchant = agg.topMerchant,
                keywords = keywords,
                memorableDate = memorableDate,
                memorableReason = memorableReason
            )

            retrospectRepository.saveRetrospect(userId, report)
            if (type == RetrospectType.WEEKLY) _weeklyBannerStatus.value = BannerStatus.SAVED
            else _monthlyBannerStatus.value = BannerStatus.SAVED

            _state.value = RetrospectState.CardView(report)
        }
    }

    /** 배너 "보기→" 탭 — 저장된 회고를 요약 형태로 표시 */
    fun openSaved(userId: String, type: RetrospectType) {
        viewModelScope.launch {
            val period = if (type == RetrospectType.WEEKLY) weeklyPeriod else monthlyPeriod
            val report = retrospectRepository.getRetrospect(userId, period.id).getOrNull()
            _state.value = if (report != null) RetrospectState.Summary(report)
            else RetrospectState.Error("회고를 불러오지 못했어요")
        }
    }

    /** 요약 화면의 "전체 다시 보기" — 카드 1부터 재생 */
    fun showFullRecap() {
        val current = _state.value
        if (current is RetrospectState.Summary) {
            _state.value = RetrospectState.CardView(current.report)
        }
    }

    fun resetState() {
        _state.value = RetrospectState.Idle
    }

    // ─────────────────────────────────────────────────────────────
    // 로컬 집계
    // ─────────────────────────────────────────────────────────────

    private data class Aggregation(
        val emotionDistribution: Map<String, Int>,
        val totalSteps: Int,
        val avgSleepHours: Float,
        val bestActivityDay: String,
        val totalSpending: Int,
        val spendingByCategory: Map<String, Int>,
        val topMerchant: String
    )

    private fun aggregate(diaries: List<DiaryEntry>, dailyDataList: List<DailyData>): Aggregation {
        val emotionDistribution = diaries
            .filter { it.emotion.isNotBlank() }
            .groupingBy { it.emotion }
            .eachCount()

        val totalSteps = dailyDataList.sumOf { it.health?.steps ?: 0 }

        val sleepMinutesList = dailyDataList.mapNotNull { it.health?.sleepDurationMinutes?.takeIf { m -> m > 0 } }
        val avgSleepHours = if (sleepMinutesList.isNotEmpty())
            (sleepMinutesList.average() / 60.0).toFloat()
        else 0f

        val bestDay = dailyDataList.maxByOrNull { it.health?.steps ?: 0 }
        val bestActivityDay = bestDay?.takeIf { (it.health?.steps ?: 0) > 0 }
            ?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            ?.let { koreanWeekday(it.dayOfWeek) }
            ?: ""

        val allPayments = dailyDataList.flatMap { it.payments }
        val totalSpending = allPayments.sumOf { it.amount }
        val spendingByCategory = allPayments
            .groupBy { it.category.ifBlank { "기타" } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val topMerchant = allPayments
            .groupingBy { it.merchant }
            .eachCount()
            .entries
            .filter { it.key.isNotBlank() }
            .maxByOrNull { it.value }
            ?.let { "${it.key} ${it.value}회" }
            ?: ""

        return Aggregation(
            emotionDistribution = emotionDistribution,
            totalSteps = totalSteps,
            avgSleepHours = avgSleepHours,
            bestActivityDay = bestActivityDay,
            totalSpending = totalSpending,
            spendingByCategory = spendingByCategory,
            topMerchant = topMerchant
        )
    }

    private fun koreanWeekday(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }

    private val positiveEmotionRank = listOf("기쁨", "설렘", "평온")

    private fun pickFallbackMemorableDiary(diaries: List<DiaryEntry>): DiaryEntry? {
        for (emotion in positiveEmotionRank) {
            diaries.firstOrNull { it.emotion == emotion }?.let { return it }
        }
        return diaries.firstOrNull()
    }

    // ─────────────────────────────────────────────────────────────
    // AI 프롬프트 입력 문자열 빌더
    // ─────────────────────────────────────────────────────────────

    private fun buildDiarySummaries(diaries: List<DiaryEntry>): String =
        diaries.sortedBy { it.date }.joinToString("\n") {
            "- ${it.date} [${it.emotion.ifBlank { "미기록" }}] ${it.content.take(100)}"
        }

    private fun buildEmotionSummary(emotionDistribution: Map<String, Int>): String =
        if (emotionDistribution.isEmpty()) "기록 없음"
        else emotionDistribution.entries.joinToString(", ") { "${it.key} ${it.value}일" }

    private fun buildHealthSummary(agg: Aggregation): String =
        if (agg.totalSteps == 0 && agg.avgSleepHours == 0f) "건강 데이터 없음"
        else "총 걸음수 ${agg.totalSteps}보, 평균 수면 ${"%.1f".format(agg.avgSleepHours)}시간, 가장 활발한 날 ${agg.bestActivityDay}"

    private fun buildSpendingSummary(agg: Aggregation): String =
        if (agg.totalSpending == 0) "결제 데이터 없음"
        else "총 지출 ${agg.totalSpending}원, 카테고리별: " +
            agg.spendingByCategory.entries.joinToString(", ") { "${it.key} ${it.value}원" }

    private fun buildScheduleSummary(dailyDataList: List<DailyData>): String {
        val titles = dailyDataList.flatMap { it.calendar }.map { it.title }.filter { it.isNotBlank() }
        return if (titles.isEmpty()) "없음" else titles.take(10).joinToString(", ")
    }
}
