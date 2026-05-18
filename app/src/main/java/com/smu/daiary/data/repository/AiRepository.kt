package com.smu.daiary.data.repository

import com.smu.daiary.data.source.AnthropicDataSource
import com.smu.daiary.feature.write.ContentBlock

class AiRepository(
    private val dataSource: AnthropicDataSource = AnthropicDataSource()
) {
    suspend fun generateDraft(blocks: List<ContentBlock>, locale: String): Result<String> =
        runCatching { dataSource.generateDiary(blocks, locale) }
}
