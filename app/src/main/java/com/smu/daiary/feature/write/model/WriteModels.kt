package com.smu.daiary.feature.write.model

enum class BlockType(val label: String) {
    PAYMENT("결제 내역"),
    PHOTO("사진"),
    CALENDAR("오늘 일정"),
    CALENDAR_UPCOMING("향후 일정"),
    HEALTH("건강"),
    WEATHER("날씨"),
    WEATHER_TOMORROW("내일 날씨"),
    PHOTO_LOCATION("촬영 장소")
}

enum class DraftStatus { IDLE, EDITING, SAVED }

data class ContentBlock(
    val id: String,
    val type: BlockType,
    val content: String,
    val isSelected: Boolean = false
)

data class DiaryDraft(
    val date: String,
    val aiContent: String,
    val editedContent: String? = null,
    val photos: List<String> = emptyList(),
    val status: DraftStatus = DraftStatus.IDLE
)

data class ContextQuestion(
    val blockId: String,
    val question: String,
    val quickOptions: List<String>
)

data class CalendarSelectableItem(
    val id: Int,
    val displayText: String,
    val startTime: Long,
    val isSelected: Boolean = true
)

data class PaymentSelectableItem(
    val id: Int,
    val displayText: String,
    val amount: Int,
    val category: String = "기타",
    val isSelected: Boolean = true
)

data class PhotoSelectableItem(
    val uri: String,
    val isSelected: Boolean = true,
    /** 촬영 시각 (epoch millis). 갤러리 수동 추가 등 정보 없으면 0 */
    val takenAt: Long = 0L,
    /** EXIF 위치 (없으면 0.0) */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** EXIF 카메라 정보가 있으면 직접 촬영한 사진. 없으면 스크린샷/수신 이미지 */
    val isCameraPhoto: Boolean = false,
    /** Claude Vision 사진별 분석 결과 (세션 캐시). 미분석이면 null */
    val analysis: String? = null
)
