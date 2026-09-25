package com.smu.daiary.feature.dashboard

import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.write.model.BlockType
import com.smu.daiary.feature.write.model.sourceType
import java.time.LocalDate
import java.time.YearMonth

/** 고정 순서. 개수가 0이어도 항목을 빼지 않아야 달마다 막대 순서가 흔들리지 않는다 */
val EMOTIONS = listOf("기쁨", "설렘", "평온", "슬픔", "화남")
val WEATHERS = listOf("맑음", "흐림", "비", "뇌우", "눈")

data class DashboardStats(
    val total: Int,
    val thisMonth: Int,
    /** 오늘(일기 기준일)까지 이어진 연속 작성 일수. 오늘 것을 아직 안 썼으면 어제까지로 센다 */
    val currentStreak: Int,
    val longestStreak: Int,
    val emotionCounts: List<Pair<String, Int>>,
    val weatherCounts: List<Pair<String, Int>>,
    /** 오래된 달 → 이번 달 순의 최근 6개월 */
    val monthlyCounts: List<Pair<YearMonth, Int>>,
    /** 본문 블록의 출처별 개수, 많은 순. 블록화 이전 일기는 세지 않는다 */
    val sourceCounts: List<Pair<BlockType, Int>>
)

/** @param today 일기 기준일(DiaryDateUtil.diaryDate()). 새벽엔 달력 날짜보다 하루 이르다 */
fun dashboardStatsOf(diaries: List<DiaryEntry>, today: LocalDate): DashboardStats {
    val dates = diaries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSortedSet()

    var current = 0
    var cursor = if (today in dates) today else today.minusDays(1)
    while (cursor in dates) {
        current++
        cursor = cursor.minusDays(1)
    }

    var longest = 0
    var run = 0
    var prev: LocalDate? = null
    for (d in dates) {
        run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
        longest = maxOf(longest, run)
        prev = d
    }

    // 개수는 일기 편 수로 센다(홈·목록과 같은 기준). 같은 날 일기가 여러 편일 수 있어 날짜 수와 다르다.
    // 연속 기록만 "쓴 날"이 기준이라 dates를 쓴다
    val month = YearMonth.from(today)
    val byMonth = diaries.mapNotNull { runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() }
        .groupingBy { it }.eachCount()
    val emotions = diaries.groupingBy { it.emotion }.eachCount()
    val weathers = diaries.groupingBy { it.weather }.eachCount()
    val sources = diaries.flatMap { e -> e.blocks.mapNotNull { it.sourceType() } }
        .groupingBy { it }.eachCount()

    return DashboardStats(
        total = diaries.size,
        thisMonth = byMonth[month] ?: 0,
        currentStreak = current,
        longestStreak = longest,
        emotionCounts = EMOTIONS.map { it to (emotions[it] ?: 0) },
        weatherCounts = WEATHERS.map { it to (weathers[it] ?: 0) },
        monthlyCounts = (5 downTo 0).map { month.minusMonths(it.toLong()) }.map { it to (byMonth[it] ?: 0) },
        sourceCounts = sources.entries.sortedByDescending { it.value }.map { it.key to it.value }
    )
}
