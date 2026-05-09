package com.smu.daiary.data.source

import android.content.Context
import android.provider.CalendarContract
import com.smu.daiary.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class CalendarDataSource(private val context: Context) {

    // 오늘 하루(00:00 ~ 23:59)의 시작/끝 시각을 epoch millis로 계산
    private fun todayRange(): Pair<Long, Long> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    suspend fun fetchTodayEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val (start, end) = todayRange()
        val events = mutableListOf<CalendarEvent>()

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )
        // 오늘 시작/종료 범위에 걸치는 일정 쿼리
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND " +
                "(${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(start.toString(), end.toString())

        val cursor = context.contentResolver.query(
            uri, projection, selection, selectionArgs, CalendarContract.Events.DTSTART + " ASC"
        )

        cursor?.use {
            val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
            val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIdx = it.getColumnIndex(CalendarContract.Events.DTEND)
            val locationIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

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
