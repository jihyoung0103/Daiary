package com.smu.daiary.data.repository

import com.smu.daiary.data.source.AnthropicDataSource
import com.smu.daiary.data.source.EncodedImage
import com.smu.daiary.feature.write.model.*

class AiRepository(
    private val dataSource: AnthropicDataSource = AnthropicDataSource()
) {
    suspend fun analyzePhotos(images: List<EncodedImage>): String =
        dataSource.analyzePhotos(images)

    suspend fun generateContextQuestions(blocks: List<ContentBlock>): List<ContextQuestion> =
        dataSource.generateContextQuestions(blocks)

    suspend fun generateDraft(
        blocks: List<ContentBlock>,
        locale: String,
        mbti: String,
        photoSummary: String? = null,
        recentDiarySamples: String = "",
        qaAnswers: Map<String, String> = emptyMap()
    ): Result<String> =
        runCatching {
            dataSource.generateDiary(
                blocks = blocks,
                locale = locale,
                mbti = mbti,
                photoSummary = photoSummary,
                recentDiarySamples = recentDiarySamples,
                qaAnswers = qaAnswers
            )
        }
}
