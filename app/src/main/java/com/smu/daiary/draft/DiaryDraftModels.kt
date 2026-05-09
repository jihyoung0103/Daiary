package com.smu.daiary.draft

enum class BlockType(val label: String) {
    PAYMENT("결제 내역"),
    PHOTO("사진"),
    CALENDAR("일정"),
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
