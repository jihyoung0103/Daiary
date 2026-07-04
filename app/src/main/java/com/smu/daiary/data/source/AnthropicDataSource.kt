package com.smu.daiary.data.source

import com.smu.daiary.BuildConfig
import com.smu.daiary.data.model.RetrospectType
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

            val prompt = """
아래는 오늘 하루의 데이터 블럭이야.
각 블럭에서 일기를 더 풍성하게 만들 수 있는 맥락이 빠져있다면,
짧고 대답하기 쉬운 질문을 만들어줘.

규칙:
- 전체 질문 수는 최대 8개. 맥락이 충분하면 0개도 괜찮아.
- 이미 데이터로 알 수 있는 것은 묻지 마
- quickOptions는 3~4개, 10자 이내로 짧게
- 마지막 선택지는 항상 "기타"
- 대답하기 귀찮을 것 같은 질문은 하지 마

데이터 블럭:
$blocksText

반드시 아래 JSON 형식으로만 응답해. 다른 텍스트는 포함하지 마:
{
  "questions": [
    {
      "blockId": "블럭 id",
      "question": "질문 텍스트",
      "quickOptions": ["선택지1", "선택지2", "기타"]
    }
  ]
}
            """.trimIndent()

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
                    val opts = q.getJSONArray("quickOptions")
                    ContextQuestion(
                        blockId      = q.getString("blockId"),
                        question     = q.getString("question"),
                        quickOptions = (0 until opts.length()).map { opts.getString(it) }
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun generateDiary(
        blocks: List<ContentBlock>,
        locale: String,
        mbti: String,
        photoSummary: String? = null,
        recentDiarySamples: String = "",
        qaAnswers: Map<String, String> = emptyMap()
    ): String =
        withContext(Dispatchers.IO) {
            val blocksText = blocks.joinToString("\n") { "- [${it.type.label}] ${it.content}" }

            val photoText = photoSummary
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    if (locale == "en") {
                        "\n\nPhoto analysis result:\n$it"
                    } else {
                        "\n\n사진 분석 결과:\n$it"
                    }
                }
                ?: ""

            val prompt = buildPrompt(
                blocksText = blocksText + photoText,
                locale = locale,
                mbti = mbti,
                recentDiarySamples = recentDiarySamples,
                qaAnswers = qaAnswers
            )

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

    suspend fun analyzePhotos(photoBase64List: List<String>): String =
        withContext(Dispatchers.IO) {
            if (photoBase64List.isEmpty()) return@withContext ""

            val contentArray = JSONArray().apply {
                put(
                    JSONObject()
                        .put("type", "text")
                        .put(
                            "text",
                            """
                            다음 사진들을 보고 사용자의 일기에 참고할 수 있도록 요약해줘.

                            [요약 조건]
                            - 사진 속 장소, 상황, 행동, 분위기를 자연스럽게 설명
                            - 확실하지 않은 내용은 단정하지 말 것
                            - 사용자가 하루를 회상하는 데 도움이 되는 정보만 작성
                            - 5문장 이내로 요약
                            """.trimIndent()
                        )
                )
                photoBase64List.forEach { base64 ->
                    put(
                        JSONObject()
                            .put("type", "image")
                            .put(
                                "source",
                                JSONObject()
                                    .put("type", "base64")
                                    .put("media_type", "image/jpeg")
                                    .put("data", base64)
                            )
                    )
                }
            }

            val body = JSONObject().apply {
                put("model", "claude-sonnet-4-6")
                put("max_tokens", 700)
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
                val responseBody = response.body?.string() ?: return@withContext ""
                JSONObject(responseBody)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
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
            val periodInstruction = if (type == RetrospectType.WEEKLY) {
                "- 하루하루의 구체적 사건 언급 가능\n- 감정 흐름 변화 반영"
            } else {
                "- 한 달의 큰 흐름과 변화에 초점\n- 월초/월말 차이나 성장 반영\n- 세세한 하루 언급보다 전체 색채 표현"
            }

            val prompt = """
당신은 따뜻하고 공감 어린 시선을 가진 회고 작가입니다.
아래 사용자의 $periodLabel 데이터를 바탕으로 회고를 작성하세요.

[일기 목록 (날짜 · 감정 · 내용 일부)]
$diarySummaries

[감정 집계]
$emotionSummary

[건강 집계]
$healthSummary

[소비 집계]
$spendingSummary

[주요 일정]
$scheduleSummary

작성 규칙:
- narrative: 2~3문장, 1인칭 공감형, 과거형으로 작성
$periodInstruction
- keywords: 명사형 3~5개, 해시태그(#) 없이
- memorableDay: 위 일기 목록 중 하나의 날짜를 골라, 그 이유를 1~2문장으로 데이터 근거를 포함해서 작성
- 이모지 사용 금지
- JSON 외 텍스트 출력 금지

반드시 아래 JSON 형식으로만 응답하세요:
{
  "narrative": "...",
  "keywords": ["...", "..."],
  "memorableDay": { "date": "YYYY-MM-DD", "reason": "..." }
}
            """.trimIndent()

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

    private fun buildPrompt(
        blocksText: String,
        locale: String,
        mbti: String,
        recentDiarySamples: String = "",
        qaAnswers: Map<String, String> = emptyMap()
    ): String {
        val qaSection = if (qaAnswers.isNotEmpty()) {
            if (locale == "en") {
                "\n\n[User's additional context]\n" +
                qaAnswers.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
            } else {
                "\n\n[사용자 추가 답변]\n" +
                qaAnswers.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
            }
        } else ""
        val mbtiStyle = getMbtiStylePrompt(mbti)
        return if (locale == "en") {
            """
You are an AI that writes a warm, personal diary entry based on the user's daily data.

Write a 3–5 paragraph diary in first person based on the data below.
- Use plain text only, no markdown

[MBTI: $mbti]
$mbtiStyle
- Do not exaggerate or stereotype the personality. Reflect it subtly (20–30%) in how events are interpreted, what is noticed, and how emotions are expressed.
- Never mention the MBTI type by name.

[Data priority order]
Photos > Selected payments > Calendar > General inference
- If photo analysis exists, it must appear in at least one full paragraph.
- Reference at least 2 specific details from the photos (place, food, action, atmosphere).
- Never write abstractly like "I took a photo" — weave the scene naturally into the diary.

[Payment rules]
- If payment data exists, include it naturally at least once.
- Connect spending to action and feeling, not just the amount.
- Do not list amounts repeatedly. Do not mention unselected payments.

${if (recentDiarySamples.isNotBlank()) """
[User writing style samples]
Do not copy events or phrases. Only refer to sentence length, emotional tone, reflection style, and wording habits.
Prioritize the most recent sample. Do not exceed 1.5x the user's average sentence length.

$recentDiarySamples
""" else ""}

[Data]
$blocksText$qaSection
            """.trimIndent()
        } else {
            """
당신은 사용자를 대신해 하루 일기를 쓰는 AI입니다.
마치 사용자 본인이 직접 작성한 것 같은 느낌으로 일기를 작성해야 합니다.
제공된 사용자의 데이터를 바탕으로 오늘 하루를 돌아보는 1인칭 일기를 작성해 주세요.

[MBTI: $mbti]
$mbtiStyle
- MBTI 성향은 과하지 않게 은은하게 반영하세요.
- 성격을 과장하거나 고정관념처럼 표현하지 마세요.
- 사건을 해석하는 방식, 무엇에 주목하는지, 감정 표현 방식에만 반영하세요.
- MBTI 이름 자체를 직접 언급하지 마세요.

[데이터 우선순위]
사진 > 선택된 결제 > 캘린더 > 일반 추론
- 사진 분석 결과가 있으면 반드시 한 문단 이상 실제로 사용해야 합니다.
- 사진 속 구체적인 장소, 음식, 행동, 분위기를 2개 이상 본문에 반영하세요.
- "사진을 찍었다", "사진을 남겼다"처럼 추상적으로 쓰지 말고 장면을 자연스럽게 녹여 쓰세요.
- "사진 분석 내용에 따르면" 같은 표현은 쓰지 마세요.
- 사진 내용이 있다면 날씨보다 사진 내용을 우선해서 서술하세요.

[분량]
    - 기본 범위: 5~8문장
    - 오늘 데이터가 적으면 5문장 내외, 많으면 8문장 내외로 자연스럽게 조정
    - 억지로 늘리거나 줄이지 말 것 — 쓸 내용이 없으면 짧아도 됨

[작성 규칙]
    - 문체: 반말 일기체 (예: "~했다", "~이었다", "~인 것 같다")
    - 첫 문장: 오늘의 날씨나 기분으로 하루를 여는 문장으로 시작
    - 중간 문단: 하루의 흐름(아침→낮→저녁) 순서로 사건과 그때의 감정, 생각을 연결
    - 마지막 문단: 오늘 하루를 돌아보며 느낀 점이나 내일에 대한 짧은 생각으로 마무리
    - 단순한 사실 나열 금지 — 그 순간 어떤 감정이었는지 내면을 담을 것
    - 마크다운 없이 순수 텍스트로만 작성
    - 감정 해석이나 교훈을 추가하지 말 것
    - 문장을 늘리기 위해 배경 설명을 만들지 말 것
    - 하루를 평가하거나 교훈을 내리지 말 것
    - 사용자가 쓰지 않은 감정 해석을 추가하지 말 것
    - 같은 문장 구조를 반복하지 말 것
    - 일기 본문만 출력

[결제 데이터 해석 규칙]
    - 결제 내역이 있으면 최소 1회 이상 자연스럽게 일기에 녹여라
    - 소비 → 행동 → 감정 흐름으로 연결 (예: "카페에 잠깐 들러 숨을 고르고 이동했다")
    - 결제 금액을 반복 나열하지 말 것
    - 선택되지 않은 결제 데이터는 무시할 것
    - 같은 장소/카테고리의 반복 결제는 하나의 행동으로 묶어 해석할 수 있음
    - 결제만으로 감정이나 목적을 단정하지 말 것
    - 사진과 결제가 연결 가능하면 하나의 장면처럼 묶어 서술한다

${if (recentDiarySamples.isNotBlank()) """
[사용자 기존 일기 문체 참고]
    아래는 사용자가 최근 작성한 일기입니다. 내용이나 사건을 복사하지 말고 다음 요소만 모방하세요.
    - 감정 표현 강도, 종결 어미, 생각 전개 방식, 어휘 선택, 문장 리듬
    - 사용자 문체가 짧으면 짧게, 담백하면 담백하게 유지하세요.
    - 최근 일기 3개까지를 가장 우선 참고하세요.

$recentDiarySamples
""" else ""}

[오늘의 데이터]
$blocksText$qaSection
""".trimIndent()
        }
    }

    private fun getMbtiStylePrompt(mbti: String): String = when (mbti) {
        "INTJ" -> "분석적이고 구조적인 문장으로 작성한다. 감정보다는 관찰과 해석을 우선한다. 하루에서 의미와 개선점을 찾는다."
        "INTP" -> "호기심 많고 탐구적인 시선으로 작성한다. 단정하지 않고 생각이 이어지는 흐름을 만든다."
        "ENTJ" -> "목표 중심적이고 자신감 있게 작성한다. 성과와 선택의 이유, 다음 행동 계획을 자연스럽게 포함한다."
        "ENTP" -> "유쾌하고 자유로운 사고 흐름으로 작성한다. 새로운 관점과 즉흥적인 생각을 넣는다."
        "INFJ" -> "내면의 감정과 의미를 깊게 탐색한다. 하루를 단순 기록하지 말고 해석하고 성찰한다."
        "INFP" -> "감성적이고 따뜻한 문체로 작성한다. 사소한 순간에도 감정과 의미를 부여한다."
        "ENFJ" -> "사람과 관계 중심으로 작성한다. 긍정적이고 다정하며 성장의 의미를 담는다."
        "ENFP" -> "생동감 있고 자유로운 감정 표현을 사용한다. 감정 변화와 기대를 자연스럽게 담는다."
        "ISTJ" -> "현실적이고 차분하게 작성한다. 사실 중심으로 기록하며 과장된 감정을 피한다."
        "ISFJ" -> "따뜻하고 섬세하게 작성한다. 배려와 안정감이 느껴지는 말투를 사용한다."
        "ESTJ" -> "명확하고 효율적인 문체로 작성한다. 하루를 정리하고 평가하는 느낌을 준다."
        "ESFJ" -> "사람과 교류 중심으로 작성한다. 긍정적이고 친근한 분위기를 만든다."
        "ISTP" -> "담백하고 실용적인 문장으로 작성한다. 감정을 길게 설명하지 않는다."
        "ISFP" -> "조용하고 감성적으로 작성한다. 순간의 분위기와 감각을 중요하게 표현한다."
        "ESTP" -> "에너지 있고 행동 중심으로 작성한다. 현재 경험과 즉흥성을 살린다."
        "ESFP" -> "밝고 생생한 감정을 표현한다. 순간의 즐거움과 분위기를 강조한다."
        else   -> "자연스럽고 담백한 일기체로 작성한다."
    }
}
