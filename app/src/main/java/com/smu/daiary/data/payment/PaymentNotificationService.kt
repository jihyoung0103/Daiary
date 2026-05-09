package com.smu.daiary.data.payment

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.smu.daiary.diary.DailyDataRepository
import com.smu.daiary.diary.PaymentData
import com.smu.daiary.diary.DailyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class PaymentNotificationService : NotificationListenerService() {

    private val repository = DailyDataRepository()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex() // Race condition 방지

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        // 지원하는 앱 패키지명 목록
        val supportedApps = mapOf(
            "viva.republica.toss"   to ::parseToss,     // 토스
            "com.kakaobank.channel" to ::parseKakaoBank  // 카카오뱅크 (확장 예정)
        )

        val parser = supportedApps.entries
            .firstOrNull { packageName.contains(it.key) }
            ?.value ?: return

        val payment = parser(title, text) ?: return

        // Firestore에 저장 (mutex로 동시 저장 시 race condition 방지)
        scope.launch {
            val userId = getUserId() ?: return@launch
            val date = LocalDate.now().toString()
            mutex.withLock {
                val existing = repository.getDailyData(userId, date).getOrNull()
                if (existing == null) {
                    // 오늘 첫 번째 결제 → 문서 새로 생성
                    repository.saveDailyData(userId, DailyData(date = date, payments = listOf(payment)))
                } else {
                    repository.updatePayments(userId, date, existing.payments + payment)
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // 토스 알림 파싱
    // 예시 title: "[출금]", text: "스타벅스 4,500원"
    private fun parseToss(title: String, text: String): PaymentData? {
        if (!title.contains("출금") && !title.contains("결제")) return null
        return parseAmountAndMerchant(text)
    }

    // 카카오뱅크 알림 파싱 (추후 형식 확인 후 업데이트 예정)
    private fun parseKakaoBank(title: String, text: String): PaymentData? {
        if (!title.contains("출금") && !title.contains("결제")) return null
        return parseAmountAndMerchant(text)
    }

    // 가맹점명, 금액 추출 공통 로직
    // 예시: "스타벅스 4,500원" → merchant: "스타벅스", amount: 4500
    private fun parseAmountAndMerchant(text: String): PaymentData? {
        val amountRegex = Regex("""([\d,]+)원""")
        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toIntOrNull() ?: return null
        val merchant = text.substringBefore(amountMatch.value).trim()
        if (merchant.isBlank()) return null

        return PaymentData(
            merchant = merchant,
            amount = amount,
            paidAt = System.currentTimeMillis()
        )
    }

    // 현재 로그인된 userId 가져오기 (Firebase Auth)
    private fun getUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
}
