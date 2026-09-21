package com.smu.daiary.data.source.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 네이버페이 결제 알림 파서 단위 테스트.
 *
 * 주의: 알림 원문 형식이 실기기로 아직 확정되지 않았다. 아래 text는 스크린샷(앱 알림센터) 기준
 * "줄바꿈으로 직렬화된다"는 가정으로 작성한 것이므로, 실제 로그 확인 후 형식이 다르면 갱신해야 한다.
 */
class NaverPayParserTest {

    private val parser = NaverPayParser()

    @Test
    fun `머니·포인트 결제(형식①) 금액과 가맹점을 추출한다`() {
        val text = """
            결제완료 결제되었습니다. 결제 이후 취소 등 관련 사항은 매장에 문의해 주세요.
            씨유 일산백마점
            결제 수단   네이버페이 포인트·머니
            결제 금액   5,700원
        """.trimIndent()

        val result = parser.parse("네이버페이", text)
        assertEquals(5700, result?.amount)
        assertEquals("씨유 일산백마점", result?.merchant)
        assertEquals("편의점", result?.category)
    }

    @Test
    fun `연동카드 결제(형식②) 금액을 추출한다`() {
        val text = """
            결제완료 결제되었습니다. 결제 이후 취소 등 관련 사항은 결제처에 문의해 주세요.
            로티세리 바비큐 치킨
            써브웨이 고양백마학원가점
            주문금액   8,000원
            카드 간편결제   8,000원
            판매자 상담   02-1544-3850
        """.trimIndent()

        val result = parser.parse("네이버페이", text)
        assertEquals(8000, result?.amount)
    }

    @Test
    fun `결제 마커가 없는 일반 알림(뉴스·광고)은 무시한다`() {
        assertNull(parser.parse("네이버", "속보 오늘의 주요 뉴스를 확인하세요"))
    }

    @Test
    fun `금액 라벨이 없으면 null을 반환한다`() {
        assertNull(parser.parse("네이버페이", "결제완료 결제되었습니다."))
    }
}
