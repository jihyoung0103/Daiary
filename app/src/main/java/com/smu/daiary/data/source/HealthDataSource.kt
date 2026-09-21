package com.smu.daiary.data.source

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.smu.daiary.data.model.HealthData
import com.smu.daiary.util.DiaryDateUtil
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

private const val TAG = "HealthDataSource"

/** 걸음 수 기준선을 뽑을 기간 */
private const val BASELINE_DAYS = 7L

/** 기준선으로 인정할 최소 표본 수. 이틀치 중앙값은 기준선이 아니다 */
private const val MIN_BASELINE_SAMPLES = 3

/**
 * Health Connect API로 사용자의 건강 데이터를 수집합니다.
 *
 * 수집 항목:
 * - 걸음 수 (StepsRecord)
 * - 수면 시간 (SleepSessionRecord)
 *
 * 전제 조건:
 * - Android 14 이상 권장 (Health Connect 기본 탑재)
 * - 사용자가 Health Connect에 데이터 제공 앱(삼성 헬스, 갤럭시 워치 등) 동기화 설정
 * - 우리 앱에 READ_STEPS, READ_SLEEP 권한 허용
 *
 * 권한 미허용 / Health Connect 미설치 시 빈 HealthData 반환합니다.
 */
class HealthDataSource(private val context: Context) {

    companion object {
        /** 우리 앱이 요청하는 Health Connect 권한 목록 */
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
    }

    /** Health Connect 클라이언트. SDK 사용 불가 시 null 반환 */
    private fun client(): HealthConnectClient? {
        return if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            Log.w(TAG, "⚠️ Health Connect SDK 사용 불가 (미설치 또는 미지원 기기)")
            null
        }
    }

    /** 우리 앱이 필요한 모든 권한을 갖고 있는지 확인 */
    suspend fun hasAllPermissions(): Boolean {
        val client = client() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(REQUIRED_PERMISSIONS)
    }

    /**
     * [date]의 걸음 수 + 수면 시간을 조회합니다. 생략하면 일기 기준일(오전 4시 이전이면 전날).
     * 과거 날짜 일기를 쓸 때 오늘 걸음 수가 딸려오지 않도록 날짜를 인자로 받는다.
     * 권한 없거나 데이터 없을 시 0으로 채운 HealthData 반환.
     */
    suspend fun fetchHealth(date: LocalDate = DiaryDateUtil.diaryDate()): HealthData {
        val client = client() ?: return HealthData()
        if (!hasAllPermissions()) {
            Log.w(TAG, "⚠️ Health Connect 권한 미허용")
            return HealthData()
        }

        val zone = ZoneId.systemDefault()
        // 기준일 구간은 04:00~다음날 04:00. 새벽 0~4시 걸음/수면은 전날 일기에 붙는다.
        val (dayStart, dayEnd) = DiaryDateUtil.dayRange(date, zone)

        // 걸음 수: 오늘 범위만 (startTime 기준 필터)
        val stepsFilter = TimeRangeFilter.between(dayStart, dayEnd)
        val steps = readSteps(client, stepsFilter)

        // 수면: 어젯밤 시작한 세션도 포함되도록 어제 정오부터 넓게 조회.
        // Health Connect는 IntervalRecord를 startTime 기준 필터링하므로,
        // 어제 23시 시작한 수면을 잡으려면 필터 시작이 그보다 앞이어야 함.
        val sleepFilter = TimeRangeFilter.between(
            date.minusDays(1).atTime(12, 0).atZone(zone).toInstant(),
            dayEnd
        )
        val sleepMinutes = readSleep(client, sleepFilter, dayStart, dayEnd)
        val usualSteps = readUsualSteps(client, date)

        Log.d(TAG, "🏃 건강 수집 완료 | steps=$steps | sleep=${sleepMinutes}분 | 평소=$usualSteps")
        return HealthData(
            steps = steps,
            sleepDurationMinutes = sleepMinutes,
            usualSteps = usualSteps
        )
    }

    /**
     * 지난 7일(오늘 제외) 걸음 수의 중앙값. 비교할 표본이 모자라면 0.
     *
     * "8,328보"는 사용자가 이미 아는 숫자라 그 자체로는 물을 것도 쓸 것도 없다.
     * 평소와 얼마나 다른지가 유일하게 데이터에 없던 정보다.
     *
     * 평균이 아니라 중앙값인 건 등산 한 번에 기준선이 무너지기 때문이다.
     * 5천 보대 엿새에 22,000보 하루가 끼면 평균은 7,529로 밀려 올라가 평범한 날이
     * 전부 "평소보다 적은 날"이 된다. 중앙값은 5,200으로 버틴다.
     *
     * 0인 날은 폰을 두고 다녔거나 동기화가 안 된 날이라 표본에서 뺀다. 그런 날이
     * 두엇만 끼어도 기준선이 반토막 난다.
     *
     * 구간은 자정 기준이라 일기 기준일(04:00)과 어긋나지만, 기준선은 대략적인 값이라
     * 새벽 몇 시간의 걸음이 중앙값을 흔들지 않는다.
     */
    private suspend fun readUsualSteps(client: HealthConnectClient, date: LocalDate): Int {
        val daily = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    date.minusDays(BASELINE_DAYS).atStartOfDay(),
                    date.atStartOfDay()
                ),
                timeRangeSlicer = Period.ofDays(1)
            )
        ).mapNotNull { it.result[StepsRecord.COUNT_TOTAL]?.toInt() }
            .filter { it > 0 }
            .sorted()

        return if (daily.size >= MIN_BASELINE_SAMPLES) daily[daily.size / 2] else 0
    }

    /**
     * 시간 범위 내 총 걸음 수.
     *
     * readRecords로 raw 레코드를 직접 더하면 안 된다. 폰 센서 · 삼성 헬스 · 갤럭시 워치가
     * 같은 걸음을 각자 기록하므로 구간이 겹쳐 2배로 집계된다.
     * aggregate는 데이터 소스 우선순위에 따라 겹치는 구간을 제거하고 합산해준다.
     */
    private suspend fun readSteps(client: HealthConnectClient, filter: TimeRangeFilter): Int {
        val result = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), filter))
        return (result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
    }

    /**
     * 수면 세션 조회. 넓은 시간 범위로 읽어온 뒤,
     * 오늘 끝난 세션(endTime이 dayStart ~ dayEnd 안)만 합산.
     * → 어젯밤 23시 시작 ~ 오늘 07시 종료 같은 세션이 정확히 포함됨.
     */
    private suspend fun readSleep(
        client: HealthConnectClient,
        filter: TimeRangeFilter,
        dayStart: java.time.Instant,
        dayEnd: java.time.Instant
    ): Int {
        val response = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, filter)
        )
        val totalMinutes = response.records
            .filter { it.endTime >= dayStart && it.endTime < dayEnd }
            .sumOf { (it.endTime.toEpochMilli() - it.startTime.toEpochMilli()) / 60_000L }
        return totalMinutes.toInt()
    }
}
