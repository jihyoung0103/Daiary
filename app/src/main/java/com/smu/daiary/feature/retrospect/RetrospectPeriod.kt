package com.smu.daiary.feature.retrospect

import com.smu.daiary.data.model.RetrospectType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields

/** 회고 대상 기간 정보 */
data class RetrospectPeriod(
    val id: String,             // "2026-W23" | "2026-06"
    val type: RetrospectType,
    val start: LocalDate,
    val end: LocalDate,
    val titleLabel: String,     // "6월 첫째 주" | "2026년 6월"
    val rangeLabel: String      // "6월 1일 ~ 6월 7일" | "2026년 6월"
) {
    val startDate: String get() = start.toString()
    val endDate: String get() = end.toString()
}

private val weekOrdinals = listOf("첫", "둘", "셋", "넷", "다섯", "여섯")

object RetrospectPeriodCalculator {

    /**
     * 주(월~일)가 속한 "월"을 판정 — ISO 8601 관례대로 그 주의 목요일이 속한 달을 기준으로 삼는다.
     * 월~수요일이 5월, 목~일요일이 6월이면 이 주는 6월 소속.
     */
    private fun ownerYearMonth(weekStart: LocalDate): YearMonth = YearMonth.from(weekStart.plusDays(3))

    /**
     * ownerMonth 안에서 이 주가 몇 번째 주인지 계산.
     * ownerMonth의 1일이 속한 주(월요일)부터 순서대로 세어, 목요일 기준으로 ownerMonth 소속인 주만 카운트한다.
     * 경계주가 이전/다음 달로 흡수되면 그만큼 자연스럽게 앞뒤 달의 주 수가 줄어든다.
     */
    private fun weekOrdinalInMonth(weekStart: LocalDate, ownerMonth: YearMonth): Int {
        val firstOfMonth = ownerMonth.atDay(1)
        var monday = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
        var ordinal = 0
        while (!monday.isAfter(weekStart)) {
            if (ownerYearMonth(monday) == ownerMonth) ordinal++
            if (monday == weekStart) return ordinal
            monday = monday.plusDays(7)
        }
        return ordinal.coerceAtLeast(1)
    }

    /** 오늘이 속한 주(월~일)의 회고 기간을 반환 */
    fun currentWeek(today: LocalDate = LocalDate.now()): RetrospectPeriod {
        val start = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val end = start.plusDays(6)

        val weekFields = WeekFields.ISO
        val isoYear = start.get(weekFields.weekBasedYear())
        val isoWeek = start.get(weekFields.weekOfWeekBasedYear())
        val id = "%04d-W%02d".format(isoYear, isoWeek)

        val ownerMonth = ownerYearMonth(start)
        val weekOfMonth = (weekOrdinalInMonth(start, ownerMonth) - 1).coerceIn(0, weekOrdinals.lastIndex)
        val titleLabel = "${ownerMonth.monthValue}월 ${weekOrdinals[weekOfMonth]}째 주"
        val rangeLabel = "${start.monthValue}월 ${start.dayOfMonth}일 ~ ${end.monthValue}월 ${end.dayOfMonth}일"

        return RetrospectPeriod(id, RetrospectType.WEEKLY, start, end, titleLabel, rangeLabel)
    }

    /** 오늘이 속한 달의 회고 기간을 반환 */
    fun currentMonth(today: LocalDate = LocalDate.now()): RetrospectPeriod {
        val start = today.withDayOfMonth(1)
        val end = today.withDayOfMonth(today.lengthOfMonth())
        val id = "%04d-%02d".format(start.year, start.monthValue)
        val label = "${start.year}년 ${start.monthValue}월"

        return RetrospectPeriod(id, RetrospectType.MONTHLY, start, end, label, label)
    }

    fun period(type: RetrospectType, today: LocalDate = LocalDate.now()): RetrospectPeriod =
        if (type == RetrospectType.WEEKLY) currentWeek(today) else currentMonth(today)
}
