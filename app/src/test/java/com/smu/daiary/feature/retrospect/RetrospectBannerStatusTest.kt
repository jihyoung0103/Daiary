package com.smu.daiary.feature.retrospect

import com.smu.daiary.data.model.RetrospectReport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RetrospectBannerStatusTest {

    @Test
    fun `저장된 회고가 없으면 일기 수로 생성 가능·부족을 가른다`() {
        assertEquals(BannerStatus.INSUFFICIENT to 0, bannerStatusOf(null, 2))
        assertEquals(BannerStatus.NOT_CREATED to 0, bannerStatusOf(null, 3))
        assertEquals(BannerStatus.INSUFFICIENT to 0, bannerStatusOf(null, null))
    }

    @Test
    fun `저장 뒤 일기 수가 같으면 저장됨, 달라지면 업데이트`() {
        assertEquals(BannerStatus.SAVED to 0, bannerStatusOf(16, 16))
        assertEquals(BannerStatus.UPDATABLE to 1, bannerStatusOf(16, 17))
        assertEquals(BannerStatus.UPDATABLE to -1, bannerStatusOf(16, 15))
    }

    @Test
    fun `지금 개수를 못 읽었거나 최소 개수 미만이면 저장된 회고를 그대로 둔다`() {
        assertEquals(BannerStatus.SAVED to 0, bannerStatusOf(16, null))
        assertEquals(BannerStatus.SAVED to 0, bannerStatusOf(4, 2))
    }

    @Test
    fun `기준 라벨은 기간 끝과 만든 날 중 이른 날까지`() {
        val zone = ZoneId.of("Asia/Seoul")
        val madeOn25 = LocalDateTime.of(2026, 9, 25, 22, 0).atZone(zone).toInstant().toEpochMilli()
        val report = RetrospectReport(periodStart = "2026-09-01", periodEnd = "2026-09-30", createdAt = madeOn25, diaryCount = 17)
        assertEquals("9월 1일 ~ 25일 기준 · 일기 17개", basisLabelOf(report, zone))

        val weekAcrossMonth = RetrospectReport(periodStart = "2026-09-28", periodEnd = "2026-10-04", createdAt = 0L, diaryCount = 5)
        assertEquals("9월 28일 ~ 10월 4일 기준 · 일기 5개", basisLabelOf(weekAcrossMonth, zone))
    }
}
