package com.smu.daiary.diary

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

/** 날씨 정보 (OpenWeatherMap API) */
data class WeatherData(
    // 오늘
    val description: String = "",       // 예: "맑음", "흐림"
    val temperature: Double = 0.0,      // 섭씨
    val humidity: Int = 0,              // %
    val city: String = "",
    // 내일
    val tomorrowDescription: String = "",
    val tomorrowTemperature: Double = 0.0,
    val tomorrowHumidity: Int = 0
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
    val longitude: Double = 0.0
)

/** 건강 정보 (Health Connect API) */
data class HealthData(
    val steps: Int = 0,
    val sleepDurationMinutes: Int = 0
)

/** 결제 내역 (NotificationListenerService) */
data class PaymentData(
    val merchant: String = "",      // 가맹점명
    val amount: Int = 0,            // 금액 (원)
    val paidAt: Long = 0L           // epoch millis
)
