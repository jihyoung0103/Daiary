package com.smu.daiary.data.source.payment

import com.smu.daiary.data.model.PaymentData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 결제 중복 저장 방지 로직 단위 테스트.
 * 알림 재발송(같은 결제가 두 번 posted) / 네이버 앱·네이버페이 앱 이중 알림을 걸러야 한다.
 */
class PaymentDuplicateTest {

    private val now = 1_754_000_000_000L
    private val payment = PaymentData("씨유 화곡시원점", 4500, now, "편의점")

    @Test
    fun `같은 가맹점_금액이 잠시 뒤 다시 오면 중복`() {
        // 알림 갱신으로 재수신 — paidAt만 몇 초 다르다
        val again = payment.copy(paidAt = now + 3_000)
        assertTrue(PaymentParsing.isDuplicate(listOf(payment), again))
    }

    @Test
    fun `5분이 지나면 별개 결제`() {
        val later = payment.copy(paidAt = now + 6 * 60_000)
        assertFalse(PaymentParsing.isDuplicate(listOf(payment), later))
    }

    @Test
    fun `금액이나 가맹점이 다르면 별개 결제`() {
        assertFalse(PaymentParsing.isDuplicate(listOf(payment), payment.copy(amount = 4600)))
        assertFalse(PaymentParsing.isDuplicate(listOf(payment), payment.copy(merchant = "메가커피")))
    }

    @Test
    fun `저장된 결제가 없으면 중복 아님`() {
        assertFalse(PaymentParsing.isDuplicate(emptyList(), payment))
    }
}
