package com.smu.daiary.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.smu.daiary.data.model.CalendarEvent
import com.smu.daiary.data.model.DailyData
import com.smu.daiary.data.model.HealthData
import com.smu.daiary.data.model.PaymentData
import com.smu.daiary.data.model.PhotoMeta
import com.smu.daiary.data.model.WeatherData
import kotlinx.coroutines.tasks.await

private const val TAG = "DailyDataRepo"

/**
 * Firestore CRUD를 담당하는 Repository.
 *
 * 컬렉션 구조:
 *   users/{userId}/dailyData/{date}   date = "YYYY-MM-DD"
 *
 * update* 메서드는 SetOptions.merge()를 사용하므로,
 * 문서가 없어도 자동 생성됩니다.
 */
class DailyDataRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun dailyDataRef(userId: String) =
        db.collection("users").document(userId).collection("dailyData")

    /** 특정 날짜의 dailyData를 가져옵니다. 없으면 null. */
    suspend fun getDailyData(userId: String, date: String): Result<DailyData?> = runCatching {
        dailyDataRef(userId).document(date).get().await().toObject<DailyData>()
    }

    /** dailyData 전체를 저장(덮어쓰기)합니다. */
    suspend fun saveDailyData(userId: String, data: DailyData): Result<Unit> = runCatching {
        Log.d(TAG, "📦 saveDailyData 시작 | userId=$userId, date=${data.date}")
        dailyDataRef(userId).document(data.date).set(data).await()
        Log.d(TAG, "✅ saveDailyData 완료 | date=${data.date}")
        Unit
    }.onFailure { Log.e(TAG, "❌ saveDailyData 실패", it) }

    /** 날씨 데이터만 업데이트합니다. 문서가 없으면 자동 생성됩니다. */
    suspend fun updateWeather(userId: String, date: String, weather: WeatherData): Result<Unit> = runCatching {
        Log.d(TAG, "🌤️ updateWeather 시작 | date=$date")
        dailyDataRef(userId).document(date)
            .set(mapOf("weather" to weather, "date" to date, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
        Log.d(TAG, "✅ updateWeather 완료")
        Unit
    }.onFailure { Log.e(TAG, "❌ updateWeather 실패", it) }

    /** 캘린더 일정만 업데이트합니다. 문서가 없으면 자동 생성됩니다. */
    suspend fun updateCalendar(userId: String, date: String, events: List<CalendarEvent>): Result<Unit> = runCatching {
        Log.d(TAG, "📅 updateCalendar 시작 | date=$date, 일정 수=${events.size}")
        dailyDataRef(userId).document(date)
            .set(mapOf("calendar" to events, "date" to date, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
        Log.d(TAG, "✅ updateCalendar 완료")
        Unit
    }.onFailure { Log.e(TAG, "❌ updateCalendar 실패", it) }

    /** 사진 메타데이터만 업데이트합니다. 문서가 없으면 자동 생성됩니다. */
    suspend fun updatePhotos(userId: String, date: String, photos: List<PhotoMeta>): Result<Unit> = runCatching {
        Log.d(TAG, "🖼️ updatePhotos 시작 | date=$date, 사진 수=${photos.size}")
        dailyDataRef(userId).document(date)
            .set(mapOf("photos" to photos, "date" to date, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
        Log.d(TAG, "✅ updatePhotos 완료")
        Unit
    }.onFailure { Log.e(TAG, "❌ updatePhotos 실패", it) }

    /** 건강 데이터만 업데이트합니다. 문서가 없으면 자동 생성됩니다. */
    suspend fun updateHealth(userId: String, date: String, health: HealthData): Result<Unit> = runCatching {
        Log.d(TAG, "🏃 updateHealth 시작 | date=$date")
        dailyDataRef(userId).document(date)
            .set(mapOf("health" to health, "date" to date, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
        Log.d(TAG, "✅ updateHealth 완료")
        Unit
    }.onFailure { Log.e(TAG, "❌ updateHealth 실패", it) }

    /** 결제 내역만 업데이트합니다. 문서가 없으면 자동 생성됩니다. */
    suspend fun updatePayments(userId: String, date: String, payments: List<PaymentData>): Result<Unit> = runCatching {
        Log.d(TAG, "💳 updatePayments 시작 | date=$date, 결제 수=${payments.size}")
        dailyDataRef(userId).document(date)
            .set(mapOf("payments" to payments, "date" to date, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
        Log.d(TAG, "✅ updatePayments 완료")
        Unit
    }.onFailure { Log.e(TAG, "❌ updatePayments 실패", it) }
}
