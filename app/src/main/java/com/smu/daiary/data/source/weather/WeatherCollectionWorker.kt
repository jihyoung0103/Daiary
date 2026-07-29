package com.smu.daiary.data.source.weather

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.data.source.WeatherDataSource
import com.smu.daiary.util.DiaryDateUtil

private const val TAG = "WeatherWorker"

/**
 * 백그라운드에서 오늘 날씨 스냅샷을 수집해 Firestore에 추가
 *
 * WeatherScheduler가 하루 중 14시에 이 워커를 실행하도록 예약한다.
 * 워커는 실행 시점의 로그인 사용자 uid로 저장하며, 미로그인 상태면 조용히 성공 처리하고 종료한다.
 *
 * 실패 유형:
 * - 위치 캐시 없음 / API 실패 → Result.retry() (WorkManager가 백오프 재시도)
 * - 미로그인 → Result.success() (다음 예약 유지)
 */
class WeatherCollectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d(TAG, "⏭️ 미로그인 상태 — 수집 건너뜀")
            return Result.success()
        }

        val date = DiaryDateUtil.diaryDate().toString()
        val dataSource = WeatherDataSource(applicationContext)
        val repository = DailyDataRepository()

        return try {
            val snapshot = dataSource.fetchTodaySnapshot()
            val result = repository.appendWeatherSnapshot(userId, date, snapshot)
            if (result.isSuccess) {
                Log.d(TAG, "✅ $date 날씨 수집 완료: ${snapshot.description} ${snapshot.temperature}°C @ ${snapshot.city}")
                Result.success()
            } else {
                Log.w(TAG, "⚠️ Firestore 저장 실패 → 재시도")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 수집 실패 → 재시도", e)
            Result.retry()
        }
    }
}
