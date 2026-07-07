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
}

/**
 * 가맹점명 → 소비 카테고리 분류. 키워드 매칭 기반이라 provider와 무관하게 공용으로 쓴다.
 */
object PaymentCategory {

    fun of(merchant: String): String = when {
        merchant.containsAny("스타벅스", "투썸", "메가커피", "컴포즈") -> "카페"
        merchant.containsAny("GS25", "CU", "세븐")                  -> "편의점"
        merchant.containsAny("버스", "지하철", "카카오T")            -> "교통"
        merchant.containsAny("맥도날드", "버거킹", "롯데리아")        -> "식사"
        else                                                        -> "기타"
    }

    private fun String.containsAny(vararg keys: String): Boolean = keys.any { contains(it) }
}
