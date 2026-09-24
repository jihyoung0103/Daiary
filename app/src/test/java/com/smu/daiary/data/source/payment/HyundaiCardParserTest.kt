package com.smu.daiary.data.source.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 현대카드 알림 파싱 테스트.
 *
 * 본문은 실기기 알림 기록에서 그대로 옮겼다(2026-09). 형식이 바뀌면 여기가 먼저 깨진다.
 * 카드사 알림은 문구가 조용히 바뀌므로, 추측으로 쓴 정규식은 언젠가 반드시 틀린다.
 */
class HyundaiCardParserTest {

    private val parser = HyundaiCardParser()

    private fun body(vararg lines: String) = lines.joinToString("\n")

    @Test
    fun `승인 금액과 가맹점을 뽑는다`() {
        val result = parser.parse(
            title = "현대카드",
            text = body(
                "조지형 님, 현대 네이버 승인 9,200원 일시불, 9/22 21:18",
                "바니스비떼",
                "누적423,985원"
            )
        )!!

        assertEquals("바니스비떼", result.merchant)
        assertEquals(9_200, result.amount)
    }

    @Test
    fun `누적금액을 결제금액으로 잘못 읽지 않는다`() {
        // "누적423,985원"은 콜론도 공백도 없이 붙어 온다. 금액을 "승인" 뒤에서 잡지 않으면
        // 여기에 걸린다. 누적이 승인보다 작은 경우를 일부러 골라 앵커가 실제로 도는지 본다.
        val result = parser.parse(
            title = "현대카드",
            text = body(
                "조지형 님, 현대 네이버 승인 20,400원 일시불, 9/21 21:02",
                "네이버페이",
                "누적1,940원"
            )
        )!!

        assertEquals(20_400, result.amount)
    }

    @Test
    fun `가맹점명에 숫자나 지점명이 붙어도 그대로 살린다`() {
        val result = parser.parse(
            title = "현대카드",
            text = body(
                "조지형 님, 현대 네이버 승인 9,100원 일시불, 9/21 18:06",
                "버거킹고양터미널점",
                "누적381,540원"
            )
        )!!

        assertEquals("버거킹고양터미널점", result.merchant)
        assertEquals("식사", result.category)
    }

    @Test
    fun `승인일시를 결제 시각으로 쓴다`() {
        val result = parser.parse(
            title = "현대카드",
            text = body(
                "조지형 님, 현대 네이버 승인 2,000원 일시불, 9/21 17:29",
                "별코인노래방연습장",
                "누적372,440원"
            )
        )!!

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = result.paidAt }
        assertEquals(9, cal.get(java.util.Calendar.MONTH) + 1)
        assertEquals(21, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(17, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(29, cal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun `승인이 아닌 알림은 무시한다`() {
        assertNull(parser.parse("현대카드", "이번 달 명세서가 도착했습니다"))
    }

    @Test
    fun `가맹점 줄이 없으면 금액만 남기지 않고 버린다`() {
        // 알림이 접혀 첫 줄만 왔을 때. 가맹점을 지어내면 그대로 일기 문장이 된다.
        assertNull(parser.parse("현대카드", "조지형 님, 현대 네이버 승인 9,200원 일시불, 9/22 21:18"))
    }
}
