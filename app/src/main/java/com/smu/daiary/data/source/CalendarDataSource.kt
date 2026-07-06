package com.smu.daiary.data.source

import android.content.Context
import android.provider.CalendarContract
import com.smu.daiary.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import com.smu.daiary.util.DiaryDateUtil

class CalendarDataSource(private val context: Context) {

    // 일기 기준 날짜부터 3일치(오늘/내일/모레) 시작/끝 시각을 epoch millis로 계산.
    // 오전 4시 이전이면 전날 기준으로 조회한다.
    private fun upcomingRange(): Pair<Long, Long> {
        val today = DiaryDateUtil.diaryDate()
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(3).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    suspend fun fetchEventsForDate(date: LocalDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        queryEvents(start, end)
    }

    suspend fun fetchUpcomingEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val (start, end) = upcomingRange()
        queryEvents(start, end)
    }

    private suspend fun queryEvents(start: Long, end: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEvent>()

        // Instances URI를 쓰면 반복 일정이 전개되고 all-day 이벤트도 올바르게 조회됨
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val cursor = context.contentResolver.query(
            uri, projection, null, null, CalendarContract.Instances.BEGIN + " ASC"
        )

        cursor?.use {
            val titleIdx    = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val startIdx    = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx      = it.getColumnIndex(CalendarContract.Instances.END)
            val locationIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            while (it.moveToNext()) {
                events.add(
                    CalendarEvent(
                        title = it.getString(titleIdx) ?: "",
                        startTime = it.getLong(startIdx),
                        endTime = it.getLong(endIdx),
                        location = it.getString(locationIdx) ?: ""
                    )
                )
            }
        }
        events
    }
}
