package com.smu.daiary.data.source

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smu.daiary.data.model.DailyData
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.data.source.payment.PaymentParserRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.smu.daiary.util.DiaryDateUtil

private const val TAG = "PAYMENT"

// [진단용] 결제/은행/카드 앱 후보 패키지 힌트. 파서 등록 여부와 무관하게 원본 로그를 남겨,
// 실제 승인 알림이 어느 패키지에서 오는지 확인한다. (검증 끝나면 정리 가능)
private val PAYMENT_CANDIDATE_HINTS = listOf(
    "toss", "shcard", "shinhan", "kakaobank", "kakaopay", "kakao.pay",
    "naver", "nhn", "payco", "spay", "kbcard", "hyundaicard", "wooricard",
    "hanaskcard", "lottecard", "bccard", "nonghyup", "nhbank", "kbank"
)

class PaymentNotificationService : NotificationListenerService() {

    private val repository = DailyDataRepository()
    // SupervisorJob으로 한 저장 실패가 다른 저장을 죽이지 않게 한다.
    // scope는 onDestroy에서만 취소한다(연결 끊김/재연결 시 취소하면 이후 저장이 조용히 무시되는 버그).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex() // Race condition 방지

    override fun onListenerConnected() {
        Log.d(TAG, "🔔 알림 리스너 연결됨")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text =
            extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()
                ?: ""

        // [진단] 결제/은행앱 후보 패키지는 파서 매칭 여부와 무관하게 원본을 남긴다.
        // 등록 안 된 패키지(예: 신한 슈퍼SOL)에서 알림이 오는지 확인하기 위한 사각지대 제거용.
        val isCandidate = PAYMENT_CANDIDATE_HINTS.any { packageName.contains(it, ignoreCase = true) }
        if (isCandidate) {
            Log.d(TAG, "🔎 [후보] package=$packageName title=$title text=$text")
            Log.d(TAG, "🔎 [후보] extras keys=${extras.keySet().joinToString()}")
        }

        // 지원 결제앱만 파서를 찾는다. 아니면 조용히 무시(로그 스팸 방지).
        val parser = PaymentParserRegistry.parserFor(packageName)
        if (parser == null) {
            // 후보인데 파서가 없으면 = 패키지 미등록. 이게 저장 실패의 핵심 원인일 수 있음.
            if (isCandidate) Log.w(TAG, "⛔ 후보 패키지지만 등록된 파서 없음 → 레지스트리에 추가 필요: $packageName")
            return
        }

        Log.d(TAG, "알림 수신 package=$packageName title=$title text=$text")
        Log.d(TAG, "✅ 파서 매칭: ${parser::class.simpleName}")

        val payment = parser.parse(title, text)
        if (payment == null) {
            Log.w(TAG, "⚠️ 파싱 실패(결제 아님 또는 형식 불일치)")
            return
        }
        Log.d(TAG, "💳 파싱 성공: ${payment.merchant} / ${payment.amount}원 / ${payment.category}")

        // Firestore에 저장 (mutex로 동시 저장 시 race condition 방지)
        scope.launch {
            val userId = getUserId()
            if (userId == null) {
                Log.w(TAG, "⚠️ 저장 건너뜀 — 로그인 안 됨(userId null)")
                return@launch
            }
            // 오전 4시 이전 결제는 전날 일기 데이터로 귀속
            val date = DiaryDateUtil.diaryDate().toString()
            mutex.withLock {
                val existing = repository.getDailyData(userId, date).getOrNull()
                val result = if (existing == null) {
                    // 오늘 첫 번째 결제 → 문서 새로 생성
                    repository.saveDailyData(userId, DailyData(date = date, payments = listOf(payment)))
                } else {
                    repository.updatePayments(userId, date, existing.payments + payment)
                }
                Log.d(TAG, "💾 저장 결과 success=${result.isSuccess} date=$date")
            }
        }
    }

    override fun onListenerDisconnected() {
        // scope를 취소하지 않는다. 대신 재바인딩을 요청해 리스너를 되살린다.
        Log.d(TAG, "🔕 리스너 연결 끊김 — 재바인딩 요청")
        runCatching {
            requestRebind(ComponentName(this, PaymentNotificationService::class.java))
        }
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
