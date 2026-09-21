package com.smu.daiary.util

import org.junit.Assert.assertEquals
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

/** 04시 경계: 기준일 구간과 시각→기준일 매핑이 맞는지. */
class DiaryDateUtilTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val day = LocalDate.of(2026, 8, 8)

    private fun millisAt(date: LocalDate, hour: Int, minute: Int = 0) =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `기준일 구간은 04시부터 다음날 04시까지`() {
        val (start, end) = DiaryDateUtil.dayRange(day, zone)
        assertEquals(millisAt(day, 4), start.toEpochMilli())
        assertEquals(millisAt(day.plusDays(1), 4), end.toEpochMilli())
    }

    @Test
    fun `새벽 0~4시는 전날로 귀속`() {
        assertEquals(day, DiaryDateUtil.diaryDateOf(millisAt(day.plusDays(1), 2), zone))
        assertEquals(day, DiaryDateUtil.diaryDateOf(millisAt(day.plusDays(1), 3, 59), zone))
    }

    @Test
    fun `04시부터는 당일`() {
        assertEquals(day, DiaryDateUtil.diaryDateOf(millisAt(day, 4), zone))
        assertEquals(day, DiaryDateUtil.diaryDateOf(millisAt(day, 23, 59), zone))
        assertEquals(day.plusDays(1), DiaryDateUtil.diaryDateOf(millisAt(day.plusDays(1), 4), zone))
    }
}
