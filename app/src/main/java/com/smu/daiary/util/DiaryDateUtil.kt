package com.smu.daiary.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 일기 작성 기준 날짜 유틸리티.
 *
 * 일반적으로 자정(00:00)이 지나면 다음 날로 전환되지만,
 * 사용자는 주로 밤 늦게 일기를 작성하므로 오전 4시 전까지는
 * 전날 일기를 쓰는 것으로 간주한다.
 *
 *  00:00 ~ 03:59  →  어제 날짜 (전날 일기)
 *  04:00 ~ 23:59  →  오늘 날짜
 *
 * 날짜뿐 아니라 데이터 수집 구간도 이 경계를 따른다.
 * 기준일 D의 실제 구간은 [D 04:00, D+1 04:00) 이다. → [dayRange]
 */
object DiaryDateUtil {

    /** 일기 작성 기준이 되는 날짜의 경계 시각 (오전 4시) */
    private val DAY_BOUNDARY = LocalTime.of(4, 0)

    /**
     * 일기 기준 날짜를 반환.
     * 오전 4시 이전이면 어제, 이후면 오늘.
     */
    fun diaryDate(): LocalDate {
        return if (isLateNight()) {
            LocalDate.now().minusDays(1)
        } else {
            LocalDate.now()
        }
    }

    /**
     * 현재 시각이 자정~오전 4시 사이인지 여부.
     * true이면 UI에서 "어제 일기 작성 중" 배너를 표시해야 한다.
     */
    fun isLateNight(): Boolean {
        val now = LocalTime.now()
        return now.isBefore(DAY_BOUNDARY)
    }

    /**
     * 기준일 [date]에 해당하는 실제 시각 구간 [start, end).
     * start = date 04:00, end = date+1 04:00.
     *
     * 사진·건강·캘린더처럼 시각으로 데이터를 긁는 쪽은 자정이 아니라 이 구간을 써야
     * 새벽 0~4시의 기록이 전날 일기에 붙는다.
     */
    fun dayRange(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Instant, Instant> =
        date.atTime(DAY_BOUNDARY).atZone(zone).toInstant() to
            date.plusDays(1).atTime(DAY_BOUNDARY).atZone(zone).toInstant()

    /** [millis] 시각이 속한 일기 기준일 (그날 04:00 이전이면 전날). */
    fun diaryDateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        return if (dateTime.toLocalTime().isBefore(DAY_BOUNDARY)) {
            dateTime.toLocalDate().minusDays(1)
        } else {
            dateTime.toLocalDate()
        }
    }
}
