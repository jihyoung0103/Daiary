package com.smu.daiary.feature.write.model

enum class BlockType(val label: String) {
    PAYMENT("결제 내역"),
    PHOTO("사진"),
    CALENDAR("오늘 일정"),
    CALENDAR_UPCOMING("향후 일정"),
    HEALTH("건강"),
    WEATHER("날씨")
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
    val isSelected: Boolean = true
)
