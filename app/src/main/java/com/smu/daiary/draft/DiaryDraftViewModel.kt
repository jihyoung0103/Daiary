package com.smu.daiary.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smu.daiary.diary.DiaryEntry
import com.smu.daiary.diary.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun mockBlocks(): List<ContentBlock> = listOf(
    ContentBlock("1", BlockType.PAYMENT, "스타벅스 아메리카노 4,500원"),
    ContentBlock("2", BlockType.PAYMENT, "점심 김치찌개 9,000원"),
    ContentBlock("3", BlockType.PHOTO, "갤러리 사진 3장 (오전 10:23)"),
    ContentBlock("4", BlockType.CALENDAR, "팀 미팅 오후 2시–3시"),
    ContentBlock("5", BlockType.HEALTH, "걸음 수 8,432보 · 칼로리 340kcal"),
    ContentBlock("6", BlockType.WEATHER, "맑음 22°C · 습도 45%"),
)

class DiaryDraftViewModel : ViewModel() {

    private val repository = DiaryRepository()

    private val _blocks = MutableStateFlow(mockBlocks())
    val blocks: StateFlow<List<ContentBlock>> = _blocks.asStateFlow()

    private val _draft = MutableStateFlow<DiaryDraft?>(null)
    val draft: StateFlow<DiaryDraft?> = _draft.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun toggleBlock(id: String) {
        _blocks.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    fun generateDraft() {
        val selected = _blocks.value.filter { it.isSelected }
        if (selected.isEmpty()) return
        val today = LocalDate.now().toString()
        val content = buildString {
            appendLine("오늘 하루를 되돌아보면,")
            appendLine()
            selected.forEach { block ->
                when (block.type) {
                    BlockType.PAYMENT  -> appendLine("• ${block.content}를 이용했다.")
                    BlockType.PHOTO    -> appendLine("• ${block.content}을 남겼다.")
                    BlockType.CALENDAR -> appendLine("• ${block.content}이 있었다.")
                    BlockType.HEALTH   -> appendLine("• 오늘의 건강: ${block.content}")
                    BlockType.WEATHER  -> appendLine("• 날씨는 ${block.content}이었다.")
                }
            }
            appendLine()
            append("오늘도 수고했다.")
        }
        _draft.value = DiaryDraft(date = today, aiContent = content)
    }

    fun updateEditedContent(content: String) {
        _draft.update { it?.copy(editedContent = content) }
    }

    fun addPhoto(uri: String) {
        _draft.update { it?.copy(photos = it.photos + uri) }
    }

    fun removePhoto(uri: String) {
        _draft.update { it?.copy(photos = it.photos.filter { p -> p != uri }) }
    }

    fun saveDraft(userId: String, onComplete: (Boolean) -> Unit) {
        val d = _draft.value ?: return
        _isSaving.value = true
        viewModelScope.launch {
            val entry = DiaryEntry(
                title = "${d.date} 일기",
                content = d.editedContent ?: d.aiContent,
                date = d.date
            )
            val result = repository.addDiary(userId, entry)
            _isSaving.value = false
            _draft.update { it?.copy(status = if (result.isSuccess) DraftStatus.SAVED else DraftStatus.IDLE) }
            onComplete(result.isSuccess)
        }
    }

    fun resetDraft() {
        _draft.value = null
        _blocks.update { list -> list.map { it.copy(isSelected = false) } }
    }
}
