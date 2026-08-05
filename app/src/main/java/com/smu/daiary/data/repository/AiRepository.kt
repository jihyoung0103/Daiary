package com.smu.daiary.data.repository

import com.smu.daiary.data.model.RetrospectType
import com.smu.daiary.data.source.AnthropicDataSource
import com.smu.daiary.data.source.EncodedImage
import com.smu.daiary.data.source.GeneratedBlock
import com.smu.daiary.feature.retrospect.RetrospectAiResult
import com.smu.daiary.feature.write.model.*

class AiRepository(
    private val dataSource: AnthropicDataSource = AnthropicDataSource()
) {
    suspend fun analyzePhoto(image: EncodedImage, isCameraPhoto: Boolean): String =
        dataSource.analyzePhoto(image, isCameraPhoto)

    suspend fun generateContextQuestions(blocks: List<ContentBlock>): List<ContextQuestion> =
        dataSource.generateContextQuestions(blocks)

    suspend fun generateFollowUpQuestion(
        sourceContent: String,
        question: String,
        answer: String
    ): String = dataSource.generateFollowUpQuestion(sourceContent, question, answer)

    suspend fun generateDiaryBlocks(
        sources: List<DiarySource>,
        locale: String,
        mbti: String,
        recentDiarySamples: String = "",
        qaAnswers: List<QaAnswer> = emptyList()
    ): Result<List<GeneratedBlock>> =
        runCatching {
            dataSource.generateDiaryBlocks(
                sources = sources,
                locale = locale,
                mbti = mbti,
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
