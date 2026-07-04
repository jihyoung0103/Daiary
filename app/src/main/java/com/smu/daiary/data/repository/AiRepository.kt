package com.smu.daiary.data.repository

import com.smu.daiary.data.model.RetrospectType
import com.smu.daiary.data.source.AnthropicDataSource
import com.smu.daiary.data.source.EncodedImage
import com.smu.daiary.feature.retrospect.RetrospectAiResult
import com.smu.daiary.feature.write.model.*

class AiRepository(
    private val dataSource: AnthropicDataSource = AnthropicDataSource()
) {
    suspend fun analyzePhoto(image: EncodedImage): String =
        dataSource.analyzePhoto(image)

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

    suspend fun generateRetrospect(
        type: RetrospectType,
        periodLabel: String,
        diarySummaries: String,
        emotionSummary: String,
        healthSummary: String,
        spendingSummary: String,
        scheduleSummary: String
    ): Result<RetrospectAiResult> =
        runCatching {
            dataSource.generateRetrospect(
                type = type,
                periodLabel = periodLabel,
                diarySummaries = diarySummaries,
                emotionSummary = emotionSummary,
                healthSummary = healthSummary,
                spendingSummary = spendingSummary,
                scheduleSummary = scheduleSummary
            )
        }
}
