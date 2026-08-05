package com.smu.daiary.data.source.payment

import com.smu.daiary.data.model.PaymentData

/**
 * 결제 알림 파서 전략(Strategy). 결제앱(provider)마다 하나씩 구현한다.
 *
 * 새 결제앱 추가 = 이 인터페이스 구현체를 하나 만들어 [PaymentParserRegistry.all]에 등록하면 끝.
 * (기존 파서·Service는 건드리지 않는다 — 개방-폐쇄 원칙)
 */
interface PaymentParser {

    /** 이 파서가 처리할 앱 패키지명. 알림 패키지명과 부분 일치(contains)로 매칭한다. */
    val packageId: String

    /** 알림 title/text에서 결제 정보를 추출한다. 결제 알림이 아니면 null. */
    fun parse(title: String, text: String): PaymentData?
}

/**
 * 결제 파싱 공통 유틸. 금액·가맹점 추출 로직을 한곳에 모아 파서마다 중복되지 않게 한다.
 */
object PaymentParsing {

    /** "1,000원" 형태를 잡는 정규식. */
    private val amountRegex = Regex("""([\d,]+)원""")

    /** 문자열에서 첫 번째 "숫자원"의 정수 금액(예: "1,000원" → 1000)을 추출. 없으면 null. */
    fun wonAmount(source: String): Int? =
        amountRegex.find(source)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

    /**
     * "가맹점 4,500원"처럼 [가맹점][금액원] 형태에서 (가맹점, 금액)을 추출하는 범용 파서.
     * 금액 토큰 앞부분을 가맹점으로 본다. 금액이 없거나 가맹점이 비면 null.
     */
    fun merchantBeforeAmount(text: String): Pair<String, Int>? {
        val match = amountRegex.find(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toIntOrNull() ?: return null
        val merchant = text.substringBefore(match.value).trim()
        if (merchant.isBlank()) return null
        return merchant to amount
    }

    /** 같은 결제로 볼 시간 창. paidAt이 "알림 수신 시각"인 파서가 있어 밀리초 비교가 불가능하다. */
    private const val DUPLICATE_WINDOW_MS = 5 * 60_000L

    /**
     * 이미 저장된 결제 중 같은 건이 있는지 판단한다. 중복이 생기는 경로:
     * - 알림이 갱신되면 onNotificationPosted가 같은 내용으로 다시 호출된다.
     * - 네이버 앱 / 네이버페이 앱처럼 두 패키지가 같은 결제를 각각 알린다.
     *
     * ponytail: 가맹점+금액+5분 창 휴리스틱. 같은 가게에서 같은 금액을 5분 내 두 번 결제하면
     *           하나가 누락된다. 알림에 승인번호가 잡히면 그걸로 교체할 것.
     */
    fun isDuplicate(existing: List<PaymentData>, new: PaymentData): Boolean =
        existing.any {
            it.merchant == new.merchant &&
                it.amount == new.amount &&
                kotlin.math.abs(it.paidAt - new.paidAt) < DUPLICATE_WINDOW_MS
        }
}

/**
 * 가맹점명 → 소비 카테고리 분류. 키워드 매칭 기반이라 provider와 무관하게 공용으로 쓴다.
 */
object PaymentCategory {

    fun of(merchant: String): String = when {
        merchant.containsAny("스타벅스", "투썸", "메가커피", "컴포즈") -> "카페"
        merchant.containsAny("GS25", "지에스25", "CU", "씨유", "세븐", "이마트24") -> "편의점"
        merchant.containsAny("버스", "지하철", "카카오T")            -> "교통"
        merchant.containsAny("맥도날드", "버거킹", "롯데리아", "써브웨이", "치킨") -> "식사"
        else                                                        -> "기타"
    }

    private fun String.containsAny(vararg keys: String): Boolean = keys.any { contains(it) }
}
