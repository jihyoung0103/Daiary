package com.smu.daiary.data.source.payment

import com.smu.daiary.data.model.PaymentData

/**
 * 토스 알림 파서.
 * 실제 알림 형식:
 *   title: "1,000원 결제"
 *   text:  "비씨체크 | 씨유(CU)화곡시원점(일시불)"
 * → 금액은 title에서, 가맹점은 text의 "|" 뒤에서 추출.
 */
class TossParser : PaymentParser {
    override val packageId = "viva.republica.toss"

    override fun parse(title: String, text: String): PaymentData? {
        if (!title.contains("출금") && !title.contains("결제")) return null
        val amount = PaymentParsing.wonAmount(title) ?: return null
        val merchant = text.substringAfter("|", "").trim()
        if (merchant.isBlank()) return null
        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis(),
            category = PaymentCategory.of(merchant)
        )
    }
}

/**
 * 카카오뱅크 알림 파서.
 *
 * TODO: 실제 카카오뱅크 결제 알림의 title/text 형식을 캡처해 정확히 맞춰야 함.
 *       현재는 "가맹점 금액원" 범용 패턴만 시도하는 미검증 구현이다.
 */
class KakaoBankParser : PaymentParser {
    override val packageId = "com.kakaobank.channel"

    override fun parse(title: String, text: String): PaymentData? {
        if (!title.contains("출금") && !title.contains("결제")) return null
        val (merchant, amount) = PaymentParsing.merchantBeforeAmount(text) ?: return null
        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis(),
            category = PaymentCategory.of(merchant)
        )
    }
}

/**
 * 네이버페이 알림 파서 — 골격(stub).
 *
 * TODO: 패키지명과 알림 형식 모두 실제 캡처 필요.
 *       - packageId 후보: 네이버페이 앱(com.naver.android.ndrive 계열 아님) / 네이버 앱 알림 경유 여부 확인.
 *       - 현재는 "가맹점 금액원" 범용 패턴만 시도하므로 실제 형식과 다르면 수집되지 않는다.
 */
class NaverPayParser : PaymentParser {
    override val packageId = "com.nhn.android.search" // TODO: 실제 패키지명 확인 후 교체

    override fun parse(title: String, text: String): PaymentData? {
        if (!title.contains("결제") && !text.contains("결제")) return null
        val (merchant, amount) = PaymentParsing.merchantBeforeAmount(text) ?: return null
        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis(),
            category = PaymentCategory.of(merchant)
        )
    }
}

/**
 * 신한카드 알림 파서 — 골격(stub).
 *
 * TODO: 패키지명과 알림 형식 모두 실제 캡처 필요.
 *       - packageId 후보: 신한 SOL페이/신한플레이(com.shcard.smartpay) 등 앱마다 다름.
 *       - 현재는 "가맹점 금액원" 범용 패턴만 시도하므로 실제 형식과 다르면 수집되지 않는다.
 */
class ShinhanCardParser : PaymentParser {
    override val packageId = "com.shcard.smartpay" // TODO: 실제 패키지명 확인 후 교체

    override fun parse(title: String, text: String): PaymentData? {
        if (!title.contains("승인") && !title.contains("결제")) return null
        val (merchant, amount) = PaymentParsing.merchantBeforeAmount(text) ?: return null
        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis(),
            category = PaymentCategory.of(merchant)
        )
    }
}

/** 앱 자체에서 쏘는 테스트 결제 알림 파서(개발용). */
class TestPaymentParser : PaymentParser {
    override val packageId = "com.smu.daiary"

    override fun parse(title: String, text: String): PaymentData? {
        if (!title.contains("결제") && !text.contains("원")) return null
        val (merchant, amount) = PaymentParsing.merchantBeforeAmount(text) ?: return null
        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis(),
            category = PaymentCategory.of(merchant)
        )
    }
}

/**
 * 등록된 결제 파서 레지스트리. 새 결제앱 지원은 여기 목록에 추가만 하면 된다.
 */
object PaymentParserRegistry {

    val all: List<PaymentParser> = listOf(
        TossParser(),
        KakaoBankParser(),
        NaverPayParser(),
        ShinhanCardParser(),
        TestPaymentParser()
    )

    /** 알림 패키지명에 맞는 파서를 찾는다(부분 일치). 없으면 null. */
    fun parserFor(packageName: String): PaymentParser? =
        all.firstOrNull { packageName.contains(it.packageId) }
}
