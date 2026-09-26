package com.smu.daiary.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KoreanHolidaysTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `추석 연휴 3일과 대체공휴일이 공휴일이다`() {
        assertTrue(KoreanHolidays.isHoliday(d("2026-09-24")))
        assertEquals("추석", KoreanHolidays.nameOf(d("2026-09-25")))
        assertTrue(KoreanHolidays.isHoliday(d("2026-09-26")))
        assertTrue(KoreanHolidays.isHoliday(d("2026-03-02")))
        assertTrue(KoreanHolidays.isHoliday(d("2026-10-05")))
    }

    @Test
    fun `평일과 표에 없는 날은 공휴일이 아니다`() {
        assertFalse(KoreanHolidays.isHoliday(d("2026-09-23")))
        assertFalse(KoreanHolidays.isHoliday(d("2026-09-27")))
        assertEquals(null, KoreanHolidays.nameOf(d("2026-07-01")))
    }
}
