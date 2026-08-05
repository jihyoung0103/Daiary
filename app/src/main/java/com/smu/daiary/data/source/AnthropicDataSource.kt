package com.smu.daiary.data.source

import com.smu.daiary.BuildConfig
import com.smu.daiary.data.model.RetrospectType
import com.smu.daiary.data.source.prompt.contextQuestionsPrompt
import com.smu.daiary.data.source.prompt.diaryPrompt
import com.smu.daiary.data.source.prompt.photoAnalysisPrompt
import com.smu.daiary.data.source.prompt.retrospectPrompt
import com.smu.daiary.feature.retrospect.RetrospectAiResult
import com.smu.daiary.feature.write.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class EncodedImage(
    val base64: String,
    val mediaType: String
)

/** AI가 소스 하나당 작성한 일기 문단. sourceId로 원래 소스와 다시 이어붙인다. */
data class GeneratedBlock(
    val sourceId: String,
    val text: String
)

class AnthropicDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun generateContextQuestions(blocks: List<ContentBlock>): List<ContextQuestion> =
        withContext(Dispatchers.IO) {
            if (blocks.isEmpty()) return@withContext emptyList()

            val blocksText = blocks.joinToString("\n") { "- ID: ${it.id} | [${it.type.label}] ${it.content}" }

            val prompt = contextQuestionsPrompt(blocksText)

            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                // 블록 수만큼 질문이 나올 수 있다. 모자라면 JSON이 잘려 파싱에 실패하고
                // 질문이 0개가 되므로(조용한 실패) 여유를 둔다.
                put("max_tokens", 1024)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val text = JSONObject(responseBody)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                val questionsArray = JSONObject(text).getJSONArray("questions")
                (0 until questionsArray.length()).map { i ->
                    val q = questionsArray.getJSONObject(i)
                    // quickOptions는 더 이상 요청하지 않는다(답변은 자유 입력).
                    // 모델이 습관적으로 붙여 보내더라도 무시하고 넘어간다.
                    val opts = q.optJSONArray("quickOptions")
                    ContextQuestion(
                        blockId      = q.getString("blockId"),
                        question     = q.getString("question"),
                        quickOptions = (0 until (opts?.length() ?: 0)).map { opts!!.getString(it) }
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * 소스별로 일기 문단을 생성한다. 소스 1개 → 블록 1개(엄격한 1:1).
     * 쓸 내용이 마땅치 않은 소스는 AI가 응답에서 생략할 수 있다(없는 사실 창작 방지).
     * 호출은 1회 — 전체를 함께 보되 출력만 소스별로 쪼갠다.
     */
    suspend fun generateDiaryBlocks(
        sources: List<DiarySource>,
        locale: String,
        mbti: String,
        qaAnswers: List<QaAnswer> = emptyList(),
        recentDiarySamples: String = ""
    ): List<GeneratedBlock> =
        withContext(Dispatchers.IO) {
            if (sources.isEmpty()) return@withContext emptyList()

            val answered = qaAnswers.filter { it.answer.isNotBlank() }
            val answersBySource = answered.groupBy { it.sourceId }

            // 답변을 해당 소스 안에 넣어준다. 평평한 목록으로 넘기면
            // 사진이 여러 장일 때 어느 답이 어느 사진 것인지 모델이 추측하게 된다.
            val sourcesJson = JSONArray().apply {
                sources.forEach { source ->
                    put(
                        JSONObject()
                            .put("sourceId", source.sourceId)
                            .put("type", source.type.label)
                            .put("content", source.content)
                            .apply {
                                val own = answersBySource[source.sourceId].orEmpty()
                                if (own.isNotEmpty()) {
                                    put("userAnswers", JSONArray().apply {
                                        own.forEach {
                                            put(
                                                JSONObject()
                                                    .put("question", it.question)
                                                    .put("answer", it.answer)
                                            )
                                        }
                                    })
                                }
                            }
                    )
                }
            }.toString(2)

            // 특정 소스에 속하지 않는 답변(감정 등)만 따로 모은다
            val sourceIds = sources.map { it.sourceId }.toSet()
            val followUpAnswerText = answered
                .filter { it.sourceId !in sourceIds }
                .joinToString("\n") { "- ${it.question}: ${it.answer}" }

            val prompt = diaryPrompt(
                sourcesJson = sourcesJson,
                mbti = mbti,
                recentDiarySamples = recentDiarySamples,
                followUpAnswerText = followUpAnswerText
            )

            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                // 소스 수만큼 문단이 나오므로 통짜 일기(1024)보다 여유를 둔다
                put("max_tokens", 2048)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("빈 응답")

            if (!response.isSuccessful) {
                throw Exception("API 오류 (${response.code}): $responseBody")
            }

            val text = JSONObject(responseBody)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val blocksArray = JSONObject(text).getJSONArray("blocks")
            val validIds = sources.map { it.sourceId }.toSet()

            (0 until blocksArray.length()).mapNotNull { i ->
                val obj = blocksArray.getJSONObject(i)
                val sourceId = obj.optString("sourceId").takeIf { it in validIds }
                    ?: return@mapNotNull null   // 모르는 sourceId를 지어냈으면 버린다
                val blockText = obj.optString("text").trim().takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                GeneratedBlock(sourceId = sourceId, text = blockText)
            }
        }

    /**
     * 사진 1장을 분석해 관찰 가능한 사실만 요약한다.
     * 여러 장은 호출부에서 각 사진마다 병렬로 호출한다 (배치 분석 아님).
     */
    suspend fun analyzePhoto(image: EncodedImage, isCameraPhoto: Boolean): String =
        withContext(Dispatchers.IO) {
            val promptText = photoAnalysisPrompt(isCameraPhoto)
            val contentArray = JSONArray().apply {
                put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", promptText)
                )
                put(
                    JSONObject()
                        .put("type", "image")
                        .put(
                            "source",
                            JSONObject()
                                .put("type", "base64")
                                .put("media_type", image.mediaType)
                                .put("data", image.base64)
                        )
                )
            }

            val body = JSONObject().apply {
                put("model", "claude-sonnet-4-6")
                put("max_tokens", 400)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                })
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (responseBody.isBlank()) return@withContext ""

                val json = JSONObject(responseBody)
                if (!json.has("content")) {
                    android.util.Log.e("AnthropicDataSource", "사진 분석 응답에 content 없음 = $responseBody")
                    return@withContext ""
                }

                json.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
                android.util.Log.e("AnthropicDataSource", "사진 분석 API 호출 실패", e)
                ""
            }
        }

    /**
     * 주간/월간 회고를 위한 AI 호출 — 내러티브 + 키워드 + 기억에 남는 하루를 1회 호출로 받는다.
     * 실패 시 예외를 던지며, 호출부(AiRepository/ViewModel)에서 로컬 폴백으로 대체한다.
     */
    suspend fun generateRetrospect(
        type: RetrospectType,
        periodLabel: String,
        diarySummaries: String,
        emotionSummary: String,
        healthSummary: String,
        spendingSummary: String,
        scheduleSummary: String
    ): RetrospectAiResult =
        withContext(Dispatchers.IO) {
            val prompt = retrospectPrompt(
                type = type,
                periodLabel = periodLabel,
                diarySummaries = diarySummaries,
                emotionSummary = emotionSummary,
                healthSummary = healthSummary,
                spendingSummary = spendingSummary,
                scheduleSummary = scheduleSummary
            )

            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                put("max_tokens", 512)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("빈 응답")

            if (!response.isSuccessful) {
                throw Exception("API 오류 (${response.code}): $responseBody")
            }

            val text = JSONObject(responseBody)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json = JSONObject(text)
            val keywordsArray = json.getJSONArray("keywords")
            val memorableDay = json.getJSONObject("memorableDay")

            RetrospectAiResult(
                narrative = json.getString("narrative"),
                keywords = (0 until keywordsArray.length()).map { keywordsArray.getString(it) },
                memorableDate = memorableDay.getString("date"),
                memorableReason = memorableDay.getString("reason")
            )
        }
}
