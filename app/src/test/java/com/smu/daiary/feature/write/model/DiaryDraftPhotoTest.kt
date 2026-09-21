package com.smu.daiary.feature.write.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 저장 시 사진이 초안에서만 나오는지 검증한다.
 * (수집 상태 _photos를 원본으로 쓰다가 기존 일기 편집·저장에서 사진이 통째로 사라지고
 *  다른 날짜 일기를 이전 사진으로 덮어쓰던 버그의 회귀 테스트)
 */
class DiaryDraftPhotoTest {

    private fun block(id: String, uri: String?) =
        DiaryBodyBlock(id = id, text = "본문", imageUri = uri)

    @Test
    fun `기존 일기 편집 초안은 저장된 https URL을 그대로 모은다`() {
        val draft = DiaryDraft(
            date = "2026-07-29",
            aiContent = "",
            photos = listOf("https://storage/a.jpg", "https://storage/b.jpg"),
            blocks = listOf(block("1", "https://storage/a.jpg"), block("2", "https://storage/b.jpg"))
        )

        assertEquals(
            listOf("https://storage/a.jpg", "https://storage/b.jpg"),
            draft.photoUrisToSave()
        )
    }

    @Test
    fun `첨부에만 있는 사진과 블록에만 있는 사진을 모두 포함하고 중복은 제거한다`() {
        val draft = DiaryDraft(
            date = "2026-07-29",
            aiContent = "",
            // 분석 결과가 없어 블록이 안 만들어진 선택 사진
            photos = listOf("content://a", "content://only-attached"),
            // 옛 일기라 photos에는 없고 블록에만 달린 사진
            blocks = listOf(block("1", "content://a"), block("2", "content://only-block"), block("3", null))
        )

        assertEquals(
            listOf("content://a", "content://only-attached", "content://only-block"),
            draft.photoUrisToSave()
        )
    }

    @Test
    fun `사진이 없으면 빈 목록`() {
        val draft = DiaryDraft(date = "2026-07-29", aiContent = "", blocks = listOf(block("1", null)))
        assertEquals(emptyList<String>(), draft.photoUrisToSave())
    }
}
