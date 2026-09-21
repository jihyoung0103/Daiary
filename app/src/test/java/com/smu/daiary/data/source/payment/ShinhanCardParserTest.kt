package com.smu.daiary.data.source.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * 신한카드(신한플레이) 결제 알림 파서 단위 테스트.
 * 실제 알림 문자열을 그대로 넣어 검증한다 — 기기/결제/Firebase 불필요, JVM에서 실행.
 *
 * 아래 문자열은 실기기 로그(2026-07-14, com.shcard.smartpay)로 캡처한 실제 형식이다:
 *   title="[신한카드]", 나머지(승인/승인금액/가맹점명 등)는 전부 text(bigText)에 들어온다.
 */
class ShinhanCardParserTest {

    private val parser = ShinhanCardParser()

    // 실기기 캡처: 배달의민족 결제 승인
    private val approvalTitle = "[신한카드]"
    private val approvalText = """
        [신한카드(3695)승인] 조*형
        - 승인금액: 11,900원(일시불)
        - 승인일시: 07/14 16:12
        - 가맹점명: 주식회사 우아한형제
        - 누적금액: 557,859원

        [신한카드 1544-7000]
    """.trimIndent()

    @Test
    fun `title에 승인이 없고 text에만 있어도 파싱한다`() {
        // 실제 알림은 title이 "[신한카드]"뿐이라, 이 케이스가 통과해야 실기기에서 동작한다.
        val result = parser.parse(approvalTitle, approvalText)
        assertEquals(11900, result?.amount)
    }

    @Test
    fun `누적금액(557,859원)을 결제금액으로 오인하지 않는다`() {
        val result = parser.parse(approvalTitle, approvalText)
        // 승인금액 라벨을 앵커로 잡으므로 누적금액이 아닌 11,900원이 나와야 한다.
        assertEquals(11900, result?.amount)
    }

    @Test
    fun `가맹점명을 정확히 추출한다`() {
        val result = parser.parse(approvalTitle, approvalText)
        assertEquals("주식회사 우아한형제", result?.merchant)
    }

    @Test
    fun `승인일시(07-14 16-12)를 결제 시각으로 파싱한다`() {
        val result = parser.parse(approvalTitle, approvalText)
        val cal = Calendar.getInstance().apply { timeInMillis = result!!.paidAt }
        assertEquals(7, cal.get(Calendar.MONTH) + 1)      // 07월
        assertEquals(14, cal.get(Calendar.DAY_OF_MONTH))  // 14일
        assertEquals(16, cal.get(Calendar.HOUR_OF_DAY))   // 16시
        assertEquals(12, cal.get(Calendar.MINUTE))        // 12분
    }

    @Test
    fun `지에스25 가맹점은 편의점으로 분류된다`() {
        val gsText = """
            [신한카드(3695)승인] 조*형
            - 승인금액: 2,500원(일시불)
            - 승인일시: 07/07 20:21
            - 가맹점명: 지에스25 백송 9단지점
            - 누적금액: 486,769원
            [신한카드 1544-7000]
        """.trimIndent()
        val result = parser.parse("[신한카드]", gsText)
        assertEquals(2500, result?.amount)
        assertEquals("편의점", result?.category)
    }

    @Test
    fun `승인이 아닌 알림(취소)은 무시한다`() {
        val cancelText = """
            [신한카드(3695)취소] 조*형
            - 취소금액: 2,500원
            - 가맹점명: 지에스25 백송 9단지점
        """.trimIndent()
        assertNull(parser.parse("[신한카드]", cancelText))
    }

    @Test
    fun `승인금액이 없으면 null을 반환한다`() {
        assertNull(parser.parse("[신한카드]", "[신한카드(3695)승인] 조*형\n- 가맹점명: 지에스25"))
    }
}
