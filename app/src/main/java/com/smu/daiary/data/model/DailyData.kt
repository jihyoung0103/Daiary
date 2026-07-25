package com.smu.daiary.data.model

/**
 * Firestore 컬렉션 구조:
 *   users/{userId}/dailyData/{date}
 *
 * 모든 필드에 기본값이 있어야 Firestore 역직렬화가 가능합니다.
 */
data class DailyData(
    val date: String = "",                          // "YYYY-MM-DD"
    val weather: WeatherData? = null,
    val calendar: List<CalendarEvent> = emptyList(),
    val photos: List<PhotoMeta> = emptyList(),
    val health: HealthData? = null,
    val payments: List<PaymentData> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 날씨 정보 (OpenWeatherMap API).
 *
 * 오늘 날씨는 백그라운드에서 여러 시점에 수집돼 [snapshots]로 쌓인다.
 * 내일 날씨는 사용자가 일기 작성 시점에 예보 API로 한 번 조회한 값.
 */
data class WeatherData(
    // 오늘 — 백그라운드에서 여러 시점 수집한 스냅샷 목록 (시간순)
    val snapshots: List<WeatherSnapshot> = emptyList(),
    // 내일 — 사용자 작성 시점 수집
    val tomorrowDescription: String = "",
    val tomorrowTemperature: Double = 0.0,
    val tomorrowHumidity: Int = 0
)

/**
 * 오늘 특정 시점의 날씨 스냅샷. 백그라운드 워커가 시각별로 하나씩 append.
 * timestamp는 수집 시각(epoch millis). 리스트는 시간 오름차순으로 유지된다.
 */
data class WeatherSnapshot(
    val timestamp: Long = 0L,           // 수집 시각 (epoch millis)
    val description: String = "",       // canonical 날씨명 (맑음/흐림/비/눈/바람)
    val temperature: Double = 0.0,      // 섭씨
    val humidity: Int = 0,              // %
    val city: String = ""               // 스냅샷 시점의 도시명 (이동 시 달라질 수 있음)
)

/** 캘린더 일정 (CalendarContract) */
data class CalendarEvent(
    val title: String = "",
    val startTime: Long = 0L,       // epoch millis
    val endTime: Long = 0L,
    val location: String = ""
)

/** 사진 메타데이터 (MediaStore + EXIF) */
data class PhotoMeta(
    val uri: String = "",
    val takenAt: Long = 0L,         // epoch millis
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** EXIF 카메라 정보(Make/Model)가 있으면 직접 촬영한 사진. 없으면 스크린샷/수신 이미지 */
    val isCameraPhoto: Boolean = false
)

/** 건강 정보 (Health Connect API) */
data class HealthData(
    val steps: Int = 0,
    val sleepDurationMinutes: Int = 0
)

/** 결제 내역 (NotificationListenerService) */
data class PaymentData(
    val merchant: String = "",
    val amount: Int = 0,
    val paidAt: Long = 0L,
    val category: String = "기타"
)
