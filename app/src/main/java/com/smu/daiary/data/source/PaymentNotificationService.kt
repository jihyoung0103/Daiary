package com.smu.daiary.data.source

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.smu.daiary.data.model.DailyData
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.data.source.payment.PaymentParserRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.smu.daiary.util.DiaryDateUtil

class PaymentNotificationService : NotificationListenerService() {

    private val repository = DailyDataRepository()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex() // Race condition 방지

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text =
            extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()
                ?: ""

        android.util.Log.d("PAYMENT", "package=$packageName title=$title text=$text")

        // 패키지명에 맞는 결제 파서를 골라 파싱. 지원 앱이 아니거나 결제 알림이 아니면 무시.
        val parser = PaymentParserRegistry.parserFor(packageName) ?: return
        val payment = parser.parse(title, text) ?: return

        // Firestore에 저장 (mutex로 동시 저장 시 race condition 방지)
        scope.launch {
            val userId = getUserId() ?: return@launch
            // 오전 4시 이전 결제는 전날 일기 데이터로 귀속
            val date = DiaryDateUtil.diaryDate().toString()
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

    // 현재 로그인된 userId 가져오기 (Firebase Auth)
    private fun getUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
}
