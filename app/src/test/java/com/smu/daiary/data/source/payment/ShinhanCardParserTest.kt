package com.smu.daiary.data.source.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * 신한카드(신한플레이) 결제 알림 파서 단위 테스트.
 * 실제 알림 문자열을 그대로 넣어 검증한다 — 기기/결제/Firebase 불필요, JVM에서 실행.
 */
class ShinhanCardParserTest {

    private val parser = ShinhanCardParser()

    // 실제 신한카드 승인 알림 형식
    private val approvalTitle = "[신한카드 (3695) 승인] 조형"
    private val approvalText = """
        - 승인금액 : 2,500원 (일시불)
        승인일시: 07/07 20:21
        - 가맹점명: 지에스25 백송 9단지점
        - 누적금액 : 486,769원
        [신한카드 1544-7000]
    """.trimIndent()

    @Test
    fun `승인 알림에서 금액을 정확히 추출한다`() {
        val result = parser.parse(approvalTitle, approvalText)
        assertEquals(2500, result?.amount)
    }

    @Test
    fun `누적금액(486,769원)을 결제금액으로 오인하지 않는다`() {
        val result = parser.parse(approvalTitle, approvalText)
        // 승인금액 라벨을 앵커로 잡으므로 누적금액이 아닌 2,500원이 나와야 한다.
        assertEquals(2500, result?.amount)
    }

    @Test
    fun `가맹점명을 정확히 추출한다`() {
        val result = parser.parse(approvalTitle, approvalText)
        assertEquals("지에스25 백송 9단지점", result?.merchant)
    }

    @Test
    fun `지에스25 가맹점은 편의점으로 분류된다`() {
        val result = parser.parse(approvalTitle, approvalText)
        assertEquals("편의점", result?.category)
    }

    @Test
    fun `승인일시(07-07 20-21)를 결제 시각으로 파싱한다`() {
        val result = parser.parse(approvalTitle, approvalText)
        val cal = Calendar.getInstance().apply { timeInMillis = result!!.paidAt }
        assertEquals(7, cal.get(Calendar.MONTH) + 1)      // 07월
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH))   // 07일
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))   // 20시
        assertEquals(21, cal.get(Calendar.MINUTE))        // 21분
    }

    @Test
    fun `승인이 아닌 알림(취소)은 무시한다`() {
        val cancelTitle = "[신한카드 (3695) 취소] 조형"
        val cancelText = "- 취소금액 : 2,500원\n- 가맹점명: 지에스25 백송 9단지점"
        assertNull(parser.parse(cancelTitle, cancelText))
    }

    @Test
    fun `승인금액이 없으면 null을 반환한다`() {
        val result = parser.parse(approvalTitle, "- 가맹점명: 지에스25 백송 9단지점")
        assertNull(result)
    }
}
