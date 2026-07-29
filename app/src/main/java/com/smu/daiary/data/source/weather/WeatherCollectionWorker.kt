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
 * - 위치 캐시 없음 / API 실패 → Result.retry() (회복 가능하므로 WorkManager가 백오프 재시도)
 * - 미로그인 / 위치 권한 거부 → Result.success() (재시도해도 결과가 같으므로 다음 예약 유지)
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
        } catch (e: SecurityException) {
            // 위치 권한 거부 상태. 재시도해도 사용자가 설정에서 켜기 전까진 반드시 같은 결과라
            // 백오프 재시도로 프로세스만 반복해서 깨운다. 미로그인과 같은 판단으로 조용히 끝내고
            // 다음 예약 주기를 기다린다. (Exception보다 위에 있어야 잡힌다)
            Log.w(TAG, "⚠️ 위치 권한 없음 — 이번 수집 건너뜀")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 수집 실패 → 재시도", e)
            Result.retry()
        }
    }
}
