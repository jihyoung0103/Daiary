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
    /** 소스별로 나뉜 본문 블록. 노션식 블록 편집/재배치의 단위 */
    val blocks: List<DiaryBodyBlock> = emptyList(),
    val status: DraftStatus = DraftStatus.IDLE
)

/**
 * AI에 넘기는 소스 아이템 — 본문 블록 1개의 재료가 된다.
 * 사진은 장별로 photo_1..N 으로 전개되며 각자의 분석 결과를 content로 갖는다.
 */
data class DiarySource(
    val sourceId: String,
    val type: BlockType,
    val content: String,
    /** PHOTO 소스일 때 해당 사진의 URI */
    val imageUri: String? = null
)

/**
 * AI가 소스 하나를 근거로 작성한 본문 블록. 블록 1개 = 소스 1개(엄격한 1:1).
 * sourceId를 들고 있어 블록 단위 재생성·출처 표시가 가능하다.
 */
/** 필드 기본값은 Firestore 역직렬화(무인자 생성자)를 위해 반드시 필요합니다. */
data class DiaryBodyBlock(
    /** 리스트 key / 재배치용 고유 id */
    val id: String = "",
    val sourceType: BlockType = BlockType.PHOTO,
    /** 대응하는 DiarySource.sourceId. 사용자가 직접 추가한 블록이면 null */
    val sourceId: String? = null,
    val text: String = "",
    /** PHOTO 블록이면 본문에 함께 표시할 사진. 저장 시 Storage URL로 치환됨 */
    val imageUri: String? = null
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
