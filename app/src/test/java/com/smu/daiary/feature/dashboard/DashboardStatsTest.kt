package com.smu.daiary.feature.dashboard

import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.feature.write.model.BlockType
import com.smu.daiary.feature.write.model.DiaryBodyBlock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DashboardStatsTest {

    private val today = LocalDate.of(2026, 9, 25)

    private fun entry(date: String, emotion: String = "", weather: String = "", vararg sources: String) =
        DiaryEntry(date = date, emotion = emotion, weather = weather, blocks = sources.map { DiaryBodyBlock(sourceId = it) })

    @Test
    fun `연속 기록은 오늘을 아직 안 썼으면 어제부터 센다`() {
        val stats = dashboardStatsOf(
            listOf(entry("2026-09-22"), entry("2026-09-23"), entry("2026-09-24"), entry("2026-09-20")),
            today
        )
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun `연속이 끊기면 현재 연속은 0, 최장은 과거 기록`() {
        val stats = dashboardStatsOf(
            listOf(entry("2026-08-01"), entry("2026-08-02"), entry("2026-08-03"), entry("2026-08-04"), entry("2026-09-20")),
            today
        )
        assertEquals(0, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
    }

    @Test
    fun `같은 날 일기가 여러 편이면 개수는 편 수, 연속은 날 수로 센다`() {
        val stats = dashboardStatsOf(listOf(entry("2026-09-24"), entry("2026-09-24")), today)
        assertEquals(2, stats.total)
        assertEquals(2, stats.thisMonth)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `감정·월별·출처를 고정 순서와 개수로 센다`() {
        val stats = dashboardStatsOf(
            listOf(
                entry("2026-09-24", "평온", "흐림", "photo_1", "photo_2", "payment_1"),
                entry("2026-09-23", "평온", "맑음", "photo_1"),
                entry("2026-07-10", "기쁨", "비")
            ),
            today
        )
        assertEquals(3, stats.total)
        assertEquals(2, stats.thisMonth)
        assertEquals(listOf("기쁨" to 1, "설렘" to 0, "평온" to 2, "슬픔" to 0, "화남" to 0), stats.emotionCounts)
        assertEquals(YearMonth.of(2026, 4) to 0, stats.monthlyCounts.first())
        assertEquals(YearMonth.of(2026, 9) to 2, stats.monthlyCounts.last())
        assertEquals(1, stats.monthlyCounts.first { it.first == YearMonth.of(2026, 7) }.second)
        assertEquals(listOf(BlockType.PHOTO to 3, BlockType.PAYMENT to 1), stats.sourceCounts)
    }
}
