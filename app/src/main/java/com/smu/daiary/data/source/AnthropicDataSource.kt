package com.smu.daiary.data.source

import com.smu.daiary.BuildConfig
import com.smu.daiary.feature.write.BlockType
import com.smu.daiary.feature.write.ContentBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnthropicDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun generateDiary(blocks: List<ContentBlock>, locale: String): String =
        withContext(Dispatchers.IO) {
            val blocksText = blocks.joinToString("\n") { "- [${it.type.label}] ${it.content}" }
            val prompt = buildPrompt(blocksText, locale)

            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
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

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("빈 응답")

            if (!response.isSuccessful) {
                throw Exception("API 오류 (${response.code}): $responseBody")
            }

            JSONObject(responseBody)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }

    private fun buildPrompt(blocksText: String, locale: String): String = if (locale == "en") {
        """
You are an AI that writes a warm, personal diary entry based on the user's daily data.

Write a 3–5 paragraph diary in first person based on the data below.
- Connect the data into a natural narrative, not a bullet list
- Use plain text only, no markdown

[Data]
$blocksText
        """.trimIndent()
    } else {
        """
당신은 사용자의 하루 데이터를 바탕으로 감성적인 한국어 일기를 쓰는 AI입니다.

아래 데이터를 참고해 1인칭 일기를 3~5 문단으로 작성해 주세요.
- 데이터를 단순 나열하지 말고 하루의 흐름이 느껴지도록 연결해 주세요
- 마크다운 없이 순수 텍스트로만 작성해 주세요

[데이터]
$blocksText
        """.trimIndent()
    }
}
