package com.smu.daiary.feature.write.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 타임라인 조회가 블록을 어느 시간대·출처로 묶는지 검증한다. */
class DiaryBodyBlockTimelineTest {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun block(sourceId: String?, hour: Int? = null) = DiaryBodyBlock(
        sourceId = sourceId,
        occurredAt = hour?.let {
            LocalDateTime.of(2026, 9, 24, it, 30).atZone(zone).toInstant().toEpochMilli()
        } ?: 0L
    )

    @Test
    fun `시각이 있는 블록은 시간대로 나뉜다`() {
        assertEquals(TimeSlot.DAWN, block("payment_3", 0).timeSlot(zone))
        assertEquals(TimeSlot.MORNING, block("photo_1", 8).timeSlot(zone))
        assertEquals(TimeSlot.AFTERNOON, block("payment_1", 12).timeSlot(zone))
        assertEquals(TimeSlot.EVENING, block("photo_2", 18).timeSlot(zone))
        assertEquals(TimeSlot.NIGHT, block("photo_3", 23).timeSlot(zone))
    }

    @Test
    fun `시각 없는 블록은 종일, 내일 이야기는 시각이 있어도 내일`() {
        assertEquals(TimeSlot.ALL_DAY, block("weather").timeSlot(zone))
        assertEquals(TimeSlot.ALL_DAY, block("health").timeSlot(zone))
        assertEquals(TimeSlot.ALL_DAY, block(null).timeSlot(zone))
        assertEquals(TimeSlot.TOMORROW, block("weather_tomorrow").timeSlot(zone))
        assertEquals(TimeSlot.TOMORROW, block("upcoming_1", 15).timeSlot(zone))
    }

    @Test
    fun `sourceId로 출처 타입을 되찾는다`() {
        assertEquals(BlockType.WEATHER, block("weather").sourceType())
        assertEquals(BlockType.WEATHER_TOMORROW, block("weather_tomorrow").sourceType())
        assertEquals(BlockType.CALENDAR, block("calendar_2").sourceType())
        assertEquals(BlockType.CALENDAR_UPCOMING, block("upcoming_1").sourceType())
        assertEquals(BlockType.PAYMENT, block("payment_1").sourceType())
        assertEquals(BlockType.PHOTO, block("photo_4").sourceType())
        assertEquals(BlockType.HEALTH, block("health").sourceType())
        assertEquals(null, block(null).sourceType())
    }
}
