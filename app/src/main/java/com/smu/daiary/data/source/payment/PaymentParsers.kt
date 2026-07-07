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
 * 실제 알림 형식:
 *   title: "[신한카드 (3695) 승인] 조형"
 *   text:  "- 승인금액 : 2,500원 (일시불)
 *           승인일시: 07/07 20:21
 *           - 가맹점명: 지에스25 백송 9단지점
 *           - 누적금액 : 486,769원
 *           [신한카드 1544-7000]"
 * → 금액은 "승인금액" 라벨에서(누적금액과 혼동 방지), 가맹점은 "가맹점명" 라벨에서 추출.
 */
class ShinhanCardParser : PaymentParser {
    override val packageId = "com.shcard.smartpay"

    // 라벨을 앵커로 삼아 승인금액/가맹점명만 정확히 뽑는다. (누적금액·취소 등에 오염되지 않도록)
    private val amountRegex = Regex("""승인금액\s*[:：]\s*([\d,]+)원""")
    private val merchantRegex = Regex("""가맹점명\s*[:：]\s*(.+)""")
    // "승인일시: 07/07 20:21" → (월, 일, 시, 분). 연도는 알림 수신 시점 기준으로 채운다.
    private val paidAtRegex = Regex("""승인일시\s*[:：]\s*(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{1,2})""")

    override fun parse(title: String, text: String): PaymentData? {
        // 승인 알림만 수집(취소·거절 등 제외). 라벨은 title/text 어디에 있어도 되도록 합쳐서 검색.
        if (!title.contains("승인")) return null
        val body = "$title\n$text"

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

    /**
     * "승인일시: 07/07 20:21"을 timestamp로 변환. 알림에 연도가 없으므로 수신 시점의 연도를 사용한다.
     * 단, 연말/연초 경계(예: 1/1에 도착한 12/31 결제)에서 미래로 계산되면 작년으로 보정한다.
     * 파싱 실패 시 알림 수신 시각으로 폴백.
     */
    private fun parsePaidAt(body: String): Long {
        val m = paidAtRegex.find(body) ?: return System.currentTimeMillis()
        val (month, day, hour, minute) = m.destructured
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, month.toInt() - 1)
            set(Calendar.DAY_OF_MONTH, day.toInt())
            set(Calendar.HOUR_OF_DAY, hour.toInt())
            set(Calendar.MINUTE, minute.toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 시계 오차 여유(1일)를 넘어 미래로 계산되면 작년 결제로 간주.
        if (cal.timeInMillis > System.currentTimeMillis() + 24 * 60 * 60 * 1000L) {
            cal.add(Calendar.YEAR, -1)
        }
        return cal.timeInMillis
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
        TestPaymentParser()
    )

    /** 알림 패키지명에 맞는 파서를 찾는다(부분 일치). 없으면 null. */
    fun parserFor(packageName: String): PaymentParser? =
        all.firstOrNull { packageName.contains(it.packageId) }
}
