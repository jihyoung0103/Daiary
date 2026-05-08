package com.smu.daiary.diary

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

/**
 * Firestore CRUD를 담당하는 Repository.
 *
 * 컬렉션 구조:
 *   users/{userId}/dailyData/{date}   date = "YYYY-MM-DD"
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
        dailyDataRef(userId).document(data.date).set(data).await()
    }

    /** 날씨 데이터만 업데이트합니다. */
    suspend fun updateWeather(userId: String, date: String, weather: WeatherData): Result<Unit> = runCatching {
        dailyDataRef(userId).document(date).update("weather", weather).await()
    }

    /** 캘린더 일정만 업데이트합니다. */
    suspend fun updateCalendar(userId: String, date: String, events: List<CalendarEvent>): Result<Unit> = runCatching {
        dailyDataRef(userId).document(date).update("calendar", events).await()
    }

    /** 사진 메타데이터만 업데이트합니다. */
    suspend fun updatePhotos(userId: String, date: String, photos: List<PhotoMeta>): Result<Unit> = runCatching {
        dailyDataRef(userId).document(date).update("photos", photos).await()
    }

    /** 건강 데이터만 업데이트합니다. */
    suspend fun updateHealth(userId: String, date: String, health: HealthData): Result<Unit> = runCatching {
        dailyDataRef(userId).document(date).update("health", health).await()
    }

    /** 결제 내역만 업데이트합니다. */
    suspend fun updatePayments(userId: String, date: String, payments: List<PaymentData>): Result<Unit> = runCatching {
        dailyDataRef(userId).document(date).update("payments", payments).await()
    }
}
