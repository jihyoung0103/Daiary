package com.smu.daiary.data.source.weather

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val TAG = "WeatherScheduler"

/**
 * 백그라운드 날씨 수집 스케줄러.
 *
 * 하루 2회 [WeatherCollectionWorker]를 실행:
 * - 09:00 — 아침(출근/등교 시간대) 날씨
 * - 15:00 — 오후(낮 최고 온도 근처) 날씨
 * 저녁은 사용자가 일기 작성 시점에 자동으로 커버되므로 별도 예약 없음.
 *
 * WorkManager PeriodicWorkRequest는 정확한 정시 실행을 보장하지 않으며 (Doze/배터리 최적화),
 * "대략 그 시간대"에 실행된다. 하루 종일의 대략적인 상황을 잡는 목적에는 충분함.
 *
 * 로그인 성공 후 [scheduleAll]을 호출해 두 워커를 등록한다.
 * ExistingPeriodicWorkPolicy.KEEP 정책이라 재로그인 시 중복 예약되지 않음.
 */
object WeatherScheduler {

    private const val WORK_MORNING = "weather-collection-morning"
    private const val WORK_AFTERNOON = "weather-collection-afternoon"
    private const val WORK_ONESHOT = "weather-collection-oneshot"

    private const val MORNING_HOUR = 9
    private const val AFTERNOON_HOUR = 15

    /** 로그인 시 호출. 아침/오후 두 스케줄을 등록한다(이미 있으면 유지). */
    fun scheduleAll(context: Context) {
        scheduleAt(context, WORK_MORNING, MORNING_HOUR)
        scheduleAt(context, WORK_AFTERNOON, AFTERNOON_HOUR)
        Log.d(TAG, "🗓️ 날씨 백그라운드 수집 스케줄 등록 완료 (09:00, 15:00)")
    }

    /** 개발/테스트용. 즉시 한 번 수집 실행. */
    fun triggerNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherCollectionWorker>()
            .setConstraints(networkConstraint())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d(TAG, "⚡ 즉시 수집 트리거")
    }

    /** 로그아웃 등에서 취소하고 싶으면 호출. */
    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_MORNING)
        wm.cancelUniqueWork(WORK_AFTERNOON)
        Log.d(TAG, "🚫 날씨 백그라운드 수집 스케줄 취소")
    }

    /**
     * targetHour에 실행되는 24시간 주기 워커 등록.
     * initialDelay = 다음 targetHour까지 남은 시간.
     */
    private fun scheduleAt(context: Context, workName: String, targetHour: Int) {
        val delayMs = delayUntilNext(targetHour)
        val request = PeriodicWorkRequestBuilder<WeatherCollectionWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraint())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "🕘 $workName 예약 | 첫 실행까지 ${delayMs / 60_000}분 남음")
    }

    /** 다음 targetHour(로컬 시각) 까지 남은 millis. 이미 지났으면 내일 targetHour. */
    private fun delayUntilNext(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun networkConstraint(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
