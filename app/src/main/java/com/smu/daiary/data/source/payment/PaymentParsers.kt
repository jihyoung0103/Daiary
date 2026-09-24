package com.smu.daiary.data.source.payment

import com.smu.daiary.data.model.PaymentData
import java.util.Calendar

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
 * 네이버페이 알림 파서.
 *
 * 네이버페이는 전용 알림이 아니라 네이버 앱(또는 네이버페이 앱)을 경유해서 오고,
 * 형식이 결제 수단에 따라 2종으로 나뉜다(스크린샷 기준):
 *   ① 머니·포인트 결제: "…씨유 일산백마점 / 결제 수단 네이버페이 포인트·머니 / 결제 금액 5,700원"
 *   ② 연동카드 결제:   "…로티세리 바비큐 치킨 / 써브웨이 고양백마학원가점 / 주문금액 8,000원 / 카드 간편결제 8,000원"
 *
 * 주의: 네이버 앱은 뉴스·쇼핑 등 알림을 많이 쏘므로 "결제 완료" 마커가 확실할 때만 처리한다.
 *
 * TODO(실기기 검증): 알림창(shade) 시스템 알림의 실제 title/text 원문 확인 필요.
 *   - 커스텀 RemoteViews라 금액/가맹점이 text에 안 실릴 가능성 있음 → 그 경우 금액만 잡히거나 폴백됨.
 *   - packageId도 로그로 확정할 것(네이버 앱 vs 네이버페이 앱). 현재는 둘 다 registry에 등록해 둠.
 */
class NaverPayParser(
    override val packageId: String = "com.nhn.android.search"
) : PaymentParser {

    // 금액 라벨 우선순위: 결제 금액(머니·포인트) → 주문금액/카드 간편결제(연동카드).
    // (포인트 잔액 "나의 포인트 …원" 같은 값에 오염되지 않도록 라벨을 앵커로 삼는다.)
    private val amountRegexes = listOf(
        Regex("""결제\s*금액\s*[:：]?\s*([\d,]+)원"""),
        Regex("""주문금액\s*[:：]?\s*([\d,]+)원"""),
        Regex("""카드\s*간편결제\s*[:：]?\s*([\d,]+)원""")
    )

    override fun parse(title: String, text: String): PaymentData? {
        val body = "$title\n$text"
        // 결제 알림만 수집. 네이버 앱의 뉴스/광고 알림을 걸러내는 핵심 가드.
        if (!body.contains("결제완료") && !body.contains("결제되었습니다")) return null

        val amount = amountRegexes.firstNotNullOfOrNull { rx ->
            rx.find(body)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
        } ?: return null

        // 가맹점: 형식이 미확정이라 안내문구/라벨/금액 줄을 제외한 첫 실제 줄을 가맹점으로 본다.
        // 못 찾으면 폴백(금액은 기록되도록).
        val merchant = extractMerchant(body) ?: "네이버페이 결제"

        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis(),
            category = PaymentCategory.of(merchant)
        )
    }

    private fun extractMerchant(body: String): String? =
        body.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotBlank() &&
                    !line.contains("결제완료") &&
                    !line.contains("결제되었습니다") &&
                    !line.contains("결제 수단") &&
                    !line.contains("결제 금액") &&
                    !line.contains("주문금액") &&
                    !line.contains("간편결제") &&
                    !line.contains("판매자") &&
                    !line.contains("네이버페이") &&
                    !line.contains("원")
            }
}

/**
 * 신한카드(신한플레이) 알림 파서.
 *
 * 실제 알림 형식(실기기 로그로 확인, 2026-07):
 *   title: "[신한카드]"                          ← title엔 "승인"이 없다!
 *   text:  "[신한카드(3695)승인] 조*형
 *           - 승인금액: 11,900원(일시불)
 *           - 승인일시: 07/14 16:12
 *           - 가맹점명: 주식회사 우아한형제
 *           - 누적금액: 557,859원
 *           [신한카드 1544-7000]"
 * → "승인" 마커·승인금액·가맹점명이 모두 text(bigText)에 들어오므로 body 전체에서 검사/추출한다.
 *   (금액은 "승인금액" 라벨 앵커로 누적금액과 구분, 가맹점은 "가맹점명" 라벨에서 추출.)
 */
class ShinhanCardParser : PaymentParser {
    override val packageId = "com.shcard.smartpay"

    // 라벨을 앵커로 삼아 승인금액/가맹점명만 정확히 뽑는다. (누적금액·취소 등에 오염되지 않도록)
    private val amountRegex = Regex("""승인금액\s*[:：]\s*([\d,]+)원""")
    private val merchantRegex = Regex("""가맹점명\s*[:：]\s*(.+)""")
    // "승인일시: 07/14 16:12" → (월, 일, 시, 분). 연도는 알림 수신 시점 기준으로 채운다.
    private val paidAtRegex = Regex("""승인일시\s*[:：]\s*(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{1,2})""")

    override fun parse(title: String, text: String): PaymentData? {
        val body = "$title\n$text"
        // 승인 알림만 수집(취소·거절 등 제외). 실제 알림은 title이 "[신한카드]"뿐이고
        // "승인"은 text에 있으므로 body 전체에서 검사한다.
        if (!body.contains("승인")) return null

        val amount = amountRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toIntOrNull() ?: return null
        val merchant = merchantRegex.find(body)?.groupValues?.get(1)
            ?.trim()?.takeIf { it.isNotBlank() } ?: return null

        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = parsePaidAt(body),
            category = PaymentCategory.of(merchant)
        )
    }

    /** "승인일시: 07/07 20:21" → timestamp. 파싱 실패 시 알림 수신 시각으로 폴백. */
    private fun parsePaidAt(body: String): Long {
        val m = paidAtRegex.find(body) ?: return System.currentTimeMillis()
        val (month, day, hour, minute) = m.destructured
        return PaymentParsing.timestampOf(month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
    }
}

/**
 * 현대카드(앱카드) 알림 파서.
 *
 * 실제 알림 형식(알림 기록으로 확인, 2026-09):
 *   title: "현대카드"
 *   text:  "조지형 님, 현대 네이버 승인 9,200원 일시불, 9/22 21:18
 *           바니스비떼
 *           누적423,985원"
 *
 * 신한카드와 다른 점 둘 — 그대로 베끼면 둘 다 틀린다.
 * - 금액이 두 번 나온다("승인 9,200원", "누적423,985원"). 누적은 콜론도 공백도 없이
 *   붙어 있어 신한식 "라벨[:：]" 앵커가 안 먹는다. "승인" 바로 뒤를 앵커로 잡는다.
 * - 가맹점에 라벨이 없다. 둘째 줄에 단독으로 온다. 승인 줄과 누적 줄을 빼고 남는 줄이다.
 *
 * 알림이 접힌 형태로 와서 가맹점 줄이 없으면 null을 돌려준다(금액만 남기지 않는다).
 * 가짜 가맹점명은 그대로 일기 문장이 되어 "현대카드 결제에 들렀다"가 된다.
 */
class HyundaiCardParser : PaymentParser {
    override val packageId = "com.hyundaicard.appcard"

    private val amountRegex = Regex("""승인\s*([\d,]+)원""")
    // "9/22 21:18" — 월에 앞자리 0이 없다
    private val paidAtRegex = Regex("""(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})""")

    override fun parse(title: String, text: String): PaymentData? {
        val body = "$title\n$text"
        if (!body.contains("승인")) return null      // 취소·거절 알림 제외

        val amount = amountRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toIntOrNull() ?: return null
        val merchant = merchantOf(body) ?: run {
            // 금액은 잡혔는데 가맹점 줄이 없다 = 알림이 접혀 왔다는 뜻.
            // "파싱 실패"만 찍히면 결제 알림이 아닌 경우와 구분이 안 되므로 따로 남긴다.
            android.util.Log.w("HyundaiCardParser", "가맹점 줄 없음(알림이 접혀 왔을 수 있음): $body")
            return null
        }

        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = parsePaidAt(body),
            category = PaymentCategory.of(merchant)
        )
    }

    /** 승인 줄·누적 줄·카드사명을 뺀 첫 줄이 가맹점이다. */
    private fun merchantOf(body: String): String? =
        body.lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.isNotBlank() &&
                    !it.contains("승인") &&
                    !it.startsWith("누적") &&
                    it != "현대카드"
            }

    private fun parsePaidAt(body: String): Long {
        val m = paidAtRegex.find(body) ?: return System.currentTimeMillis()
        val (month, day, hour, minute) = m.destructured
        return PaymentParsing.timestampOf(month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
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
        NaverPayParser(),                          // 네이버 앱 경유
        NaverPayParser("com.naverfin.payments"),   // 네이버페이 앱 경유
        ShinhanCardParser(),
        HyundaiCardParser(),
        TestPaymentParser()
    )

    /** 알림 패키지명에 맞는 파서를 찾는다(부분 일치). 없으면 null. */
    fun parserFor(packageName: String): PaymentParser? =
        all.firstOrNull { packageName.contains(it.packageId) }
}
