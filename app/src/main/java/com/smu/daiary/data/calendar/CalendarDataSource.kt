package com.smu.daiary.data.calendar

import android.content.Context
import android.provider.CalendarContract
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.smu.daiary.diary.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val CALENDAR_SCOPE = "oauth2:https://www.googleapis.com/auth/calendar.readonly"
private const val CALENDAR_API_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events"

class CalendarDataSource(private val context: Context) {

    // 특정 날짜의 시작/끝 epoch millis 반환
    private fun dateRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    // 오늘 일정 (일기 작성용)
    suspend fun fetchTodayEvents(): List<CalendarEvent> {
        return fetchEvents(LocalDate.now(), LocalDate.now())
    }

    // 이후 3일 일정 (알람용)
    suspend fun fetchUpcomingEvents(): List<CalendarEvent> {
        val start = LocalDate.now().plusDays(1)
        val end = LocalDate.now().plusDays(3)
        return fetchEvents(start, end)
    }

    // 특정 기간 일정 → Google Calendar API + 로컬 합쳐서 반환
    private suspend fun fetchEvents(from: LocalDate, to: LocalDate): List<CalendarEvent> {
        val apiEvents = fetchFromGoogleApi(from, to)
        val localEvents = fetchFromLocal(from, to)

        // Google API 성공 시 API 결과 우선, 실패 시 로컬만 반환
        return if (apiEvents.isNotEmpty()) {
            (apiEvents + localEvents).distinctBy { it.title + it.startTime }
        } else {
            localEvents
        }
    }

    // Google Calendar REST API 호출
    private suspend fun fetchFromGoogleApi(from: LocalDate, to: LocalDate): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            try {
                // 로그인된 구글 계정에서 캘린더 접근 토큰 가져오기
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: return@withContext emptyList()
                val token = GoogleAuthUtil.getToken(context, account.account!!, CALENDAR_SCOPE)

                val timeMin = from.atStartOfDay().toInstant(ZoneOffset.UTC)
                    .toString().replace("Z", "%2B00%3A00")
                val timeMax = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                    .toString().replace("Z", "%2B00%3A00")

                val url = "$CALENDAR_API_URL?timeMin=$timeMin&timeMax=$timeMax" +
                        "&singleEvents=true&orderBy=startTime"

                val connection = URL(url).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $token")
                val response = connection.inputStream.bufferedReader().readText()

                parseApiResponse(response)
            } catch (e: UserRecoverableAuthException) {
                // 캘린더 권한 미승인 → 로컬 데이터로 폴백
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Google Calendar API 응답 JSON 파싱
    private fun parseApiResponse(response: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val items = JSONObject(response).optJSONArray("items") ?: return emptyList()
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val title = item.optString("summary", "")
            val location = item.optString("location", "")
            val startObj = item.optJSONObject("start") ?: continue

            // 시간 있는 일정 vs 종일 일정 구분
            val startStr = startObj.optString("dateTime").ifEmpty {
                startObj.optString("date") + "T00:00:00+09:00"
            }
            val endObj = item.optJSONObject("end")
            val endStr = endObj?.optString("dateTime")?.ifEmpty {
                endObj.optString("date") + "T23:59:59+09:00"
            } ?: startStr

            val startTime = java.time.OffsetDateTime.parse(startStr, fmt).toInstant().toEpochMilli()
            val endTime = java.time.OffsetDateTime.parse(endStr, fmt).toInstant().toEpochMilli()
            val date = startStr.substring(0, 10)

            events.add(CalendarEvent(title, date, startTime, endTime, location))
        }
        return events
    }

    // 로컬 캘린더 (CalendarContract) 조회
    private suspend fun fetchFromLocal(from: LocalDate, to: LocalDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val (start, _) = dateRange(from)
        val (_, end) = dateRange(to)
        val events = mutableListOf<CalendarEvent>()

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )
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
                val startTime = it.getLong(startIdx)
                val date = LocalDate.ofEpochDay(startTime / 86400000).toString()
                events.add(
                    CalendarEvent(
                        title = it.getString(titleIdx) ?: "",
                        date = date,
                        startTime = startTime,
                        endTime = it.getLong(endIdx),
                        location = it.getString(locationIdx) ?: ""
                    )
                )
            }
        }
        events
    }
}
