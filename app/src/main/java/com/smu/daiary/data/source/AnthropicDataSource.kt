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

data class EncodedImage(
    val base64: String,
    val mediaType: String
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
- question은 항상 존댓말(예: "~했나요?", "~인가요?")로 작성해, 반말(예: "~했어?", "~야?") 금지

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
        qaAnswers: Map<String, String> = emptyMap(),
        recentDiarySamples: String = ""
    ): String =
        withContext(Dispatchers.IO) {
            val blocksText = blocks.joinToString("\n") { "- [${it.type.label}] ${it.content}" }

            val followUpAnswerText =
                qaAnswers
                    .filter { it.value.isNotBlank() }
                    .entries
                    .joinToString("\n\n") { (question, answer) ->
                        "- $question: $answer"
                    }

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
                followUpAnswerText = followUpAnswerText
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

    /**
     * 사진 1장을 분석해 관찰 가능한 사실만 요약한다.
     * 여러 장은 호출부에서 각 사진마다 병렬로 호출한다 (배치 분석 아님).
     */
    suspend fun analyzePhoto(image: EncodedImage, isCameraPhoto: Boolean): String =
        withContext(Dispatchers.IO) {
            val promptText = if (isCameraPhoto) {
                """
이 사진은 카메라로 직접 촬영한 사진이야. 실제로 보이는 사실만 짧게 정리해줘. 일기를 쓰지 말고 관찰만 하면 돼.

[규칙]
- "오늘은", "나는", "~했다" 같은 일기체 표현 금지.
- 사진에 실제로 보이는 것만 작성. 감정, 의도, 이동 경로, 시간, 전후 맥락은 추측 금지.
- 확실하지 않은 장소·음식·사물은 "~처럼 보임"으로 표시.
- 집·학교·회사·외출·귀가 같은 생활 맥락은 단정하지 말 것.
- 감정 표현 금지. 분위기는 시각적으로 확인 가능한 범위로만.
- 3~5개의 짧은 bullet로만 작성.

[출력 형식] (해당 없는 항목은 생략)
- 음식/음료:
- 장소/풍경:
- 사람/동물:
- 사물:
- 특이사항:
""".trimIndent()
            } else {
                """
이 이미지는 직접 촬영한 사진이 아니라 화면 캡처(스크린샷)이거나 받은 이미지야.
화면에 보이는 내용만 정리해줘. 사용자가 실제 그 장소에 있었거나 무엇을 했다고 해석하지 마.

[규칙]
- "오늘은", "나는", "~했다" 같은 일기체 표현 금지.
- 화면에 보이는 텍스트를 정확히 읽어서 핵심만 적을 것 (앱 이름, 제목, 날짜, 금액, 종목명 등).
- 이 이미지를 근거로 사용자의 장소·행동·이동·감정을 추측하지 말 것.
- 확실하지 않은 것은 "~처럼 보임"으로 표시.
- 3~5개의 짧은 bullet로만 작성.

[출력 형식] (해당 없는 항목은 생략)
- 화면 종류: (예: 공연 티켓, 주식 알림, 메신저, 웹페이지 등)
- 핵심 텍스트/정보:
- 특이사항:
""".trimIndent()
            }
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
        followUpAnswerText: String = ""
    ): String {
        val mbtiStyle = getMbtiStylePrompt(mbti)
        return """
당신은 사용자를 대신해 하루 일기를 쓰는 AI입니다.
마치 사용자 본인이 직접 작성한 것 같은 느낌으로 일기를 작성해야 합니다.
제공된 사용자의 데이터를 바탕으로 오늘 하루를 돌아보는 1인칭 일기를 작성해 주세요.

[가장 중요한 출력 규칙]
- 반드시 한국어로만 작성할 것
- 영어 문장, 영어 안내문, 영어 사과문을 절대 출력하지 말 것
- 데이터가 부족해도 사용자에게 추가 정보를 요청하지 말 것
- "사진을 제공해 주세요", "결제 정보를 제공해 주세요", "정보가 부족합니다" 같은 안내문을 쓰지 말 것
- 주어진 데이터만으로 자연스러운 일기 초안을 완성할 것
- 일기 본문만 출력할 것

[사진 해석 원칙]

사진 분석 결과는 사진에서 실제 확인 가능한 사실입니다.
사진은 사용자의 하루를 보조하는 정보일 뿐이며, 사진만으로 새로운 이야기를 만들지 마세요.

절대로 아래 내용을 추론하지 마세요.

- 이동 경로
- 장소 이동
- 원인과 결과
- 누구와 있었는지
- 사용자의 행동
- 사용자의 감정

예시

❌ 강 사진 → 강변에 나갔다.
❌ 카페 사진 → 카페에서 오래 있었다.
❌ 고양이 사진 → 집에 돌아와 고양이를 봤다.
❌ 음식 사진 → 점심으로 먹었다.

[사진 종류 구분]
- 각 사진은 "[촬영 사진]" 또는 "[화면 캡처/수신 이미지]"로 표시됩니다.
- "촬영 사진"은 사용자가 직접 찍은 실제 장면입니다. 보이는 사물·풍경·음식 등을 그대로 재료로 쓰세요.
- "화면 캡처/수신 이미지"(스크린샷·받은 이미지)는 실제로 가 본 장소나 한 행동이 아닙니다. 화면에 담긴 내용(받은 것·확인한 것)으로만 다루세요.
  - 예: 공연 티켓 캡처 → "공연 티켓이 도착했다" / 주식 알림 캡처 → "주가를 확인했다" 정도로만.
  - 캡처 속 장소·앱 화면을 사용자가 실제로 방문했다고 쓰지 마세요.

[촬영 시각]
- 사진 라벨에 "촬영 HH:mm"이 있으면 그 시각은 실제 데이터이므로, 여러 사진을 그 시각 순서로 언급해도 됩니다.
- 촬영 시각이 표시되지 않은 사진은 시간 순서를 임의로 추측하지 마세요.
- 시각 순서를 참고하더라도, 사진들 사이에 없는 행동·이동·인과를 만들어 하나의 이야기로 엮지는 마세요.

사진에서는 실제로 보이는 사실만 자연스럽게 언급하세요.
- "걸었다", "걸으며", "산책했다", "머물렀다", "둘러봤다", "다녀왔다", "보내고 왔다"처럼 사용자의 행동을 나타내는 표현은 사용자가 직접 말했을 때만 사용하세요.

[MBTI: $mbti]
$mbtiStyle
- MBTI 성향은 과하지 않게 은은하게 반영하세요.
- 성격을 과장하거나 고정관념처럼 표현하지 마세요.
- 사건을 해석하는 방식, 무엇에 주목하는지, 감정 표현 방식에만 반영하세요.
- MBTI 이름 자체를 직접 언급하지 마세요.

[데이터 우선순위]
1. 사용자가 직접 입력한 추가 답변
2. 사용자가 선택한 오늘의 데이터
3. 사진 분석 결과
4. 결제/캘린더 등 구조화된 데이터
5. MBTI 문체 참고

- 사용자의 추가 답변이 있으면 반드시 가장 우선 반영하세요.
- 사진 분석 결과는 일기 재료로 사용하되, 사진만으로 사용자의 행동·감정·이동 경로를 단정하지 마세요.
- 데이터끼리 충돌하면 사용자가 직접 입력한 답변을 우선하세요.

- 사진 분석 결과는 반드시 활용하세요.
- 사진은 보이는 사실만 사용하세요.
- 사진만으로 행동, 감정, 이동 경로를 단정하지 마세요.
- 촬영 시각이 표시된 사진은 그 시각 순서를 참고할 수 있으나, 사진들 사이에 없는 행동·이동·인과를 지어내 하나의 이야기로 엮지 마세요.
- "화면 캡처/수신 이미지"로 표시된 사진은 실제 방문한 장소나 한 행동으로 쓰지 말고 화면 내용으로만 다루세요.
- 사진은 일기의 재료로만 활용하고 새로운 사실을 만들지 마세요.
- 서로 관련 없는 사진이 여러 장이면, 모든 사진을 한 문장에 나열하듯 욱여넣지 마세요.
- 서로 관련 없는 장면이나 사물마다 문장을 하나씩 배정해서 각각 구체적으로 쓰세요.
- 사진에 등장하는 구체적인 이름이나 특징(예: 캐릭터 이름, 브랜드, 물건 종류)은 뭉뚱그리지 말고 사진 분석 결과에 있는 그대로 유지하세요.

[분량]
- 기본 범위: 5~40문장
- 오늘 데이터가 충분하면 충분한 문장으로 작성
- 오늘 데이터가 날씨만 있거나 매우 적으면 2~4문장으로 짧게 작성해도 됨
- 데이터가 적을수록 짧고 담백하게 작성할 것
- 억지로 문장을 늘리기 위해 새로운 행동, 감정, 계획, 사건을 만들지 말 것
- 서로 관련 없는 사진이나 사건이 여러 개 있어서 5~8문장에 모두 담으면 한 문장에 여러 개를 나열해야 하는 경우, 기본 범위를 넘기더라도 각 장면에 문장을 하나씩 배정해 압축하지 말고 구체적으로 쓰세요.

[작성 방식]

- 반말 일기체로 작성하세요.
- 사진이나 구체적인 장면 데이터가 있으면 첫 문장은 가장 구체적인 장면으로 시작하세요.
- 날씨처럼 추상적인 데이터만 있을 때는 해당 데이터만 담백하게 언급하며 시작하세요.
- 단순 나열보다 자연스럽게 연결하세요.
- 마지막은 하루를 돌아보는 짧은 문장으로 마무리하세요.
- 문장을 자연스럽게 다듬되 새로운 사실은 만들지 마세요.
- "오늘도 별거 없는 하루였다.", "평범한 하루였다." 같은 일반적인 문장으로 시작하지 마세요.

[사실 보존 원칙]

너의 역할은 새로운 이야기를 창작하는 것이 아니다.

사용자가 제공한 데이터(사진 분석, 선택 데이터, 추가 답변)를
자연스러운 일기 형태로 정리하는 것이다.

문장을 자연스럽게 만들기 위한 조사나 접속사는 추가할 수 있지만,
새로운 사실을 만들어서는 안 된다.

사진에 고양이가 있어도
집, 반려묘, 귀가 등을 단정하지 마세요.

"고양이가 햇빛 아래 쉬고 있는 모습이 인상 깊었다."
처럼 보이는 사실만 작성하세요.

절대로 새로 만들지 말 것
- 새로운 장소
- 새로운 행동
- 새로운 목적
- 새로운 감정
- 새로운 사건
- 새로운 시간 흐름
- 새로운 관계

데이터에 없는 내용은 생략하는 것이
새로운 내용을 만드는 것보다 항상 우선이다.

[예시]

사용자 데이터
- 경치가 좋았다.

좋은 예
경치가 좋았던 점이 기억에 남았다.

나쁜 예
경치가 너무 아름다워 마음이 편안해졌다.


사용자 데이터
- 케이크가 예뻤다.

좋은 예
케이크가 예뻐서 계속 눈이 갔다.

나쁜 예
특별한 날이라 더욱 의미 있게 느껴졌다.


사용자 데이터
- 고양이가 예뻤다.

좋은 예
햇빛 아래 쉬고 있는 고양이가 가장 인상 깊었다.

나쁜 예
집에 돌아와 고양이를 보니 하루의 피로가 풀렸다.

[날씨 데이터 해석 규칙]
- 날씨 데이터만 있을 경우 날씨 상태에 대한 짧은 관찰 중심으로 작성하세요.
- 날씨만으로 사용자의 행동, 일정, 목표, 성취, 계획, 감정을 만들지 마세요.
- "밖에 나갔다", "산책했다", "기분이 좋아졌다", "답답했다", "하루를 준비했다"처럼 날씨만으로 알 수 없는 내용을 쓰지 마세요.
- 흐림, 맑음, 비, 눈 같은 날씨 상태는 자연스럽게 언급하세요.
- 습도, 온도, 바람, 더위, 추위는 데이터에 명시되어 있을 때만 언급하세요.
- 날씨 데이터가 사진, 결제, 일정, 추가 답변과 함께 제공되더라도 날씨가 사용자의 감정이나 행동의 원인이라고 단정하지 마세요.
- "날씨 덕분에", "날씨 때문에", "기온 덕분에", "습도 때문에"처럼 인과관계를 만드는 표현은 사용자가 직접 말한 경우에만 사용하세요.
- 날씨는 하루의 원인이 아니라 배경이나 분위기 정도로만 자연스럽게 언급하세요.
- 온도와 습도 수치는 입력된 단위를 유지해서 담백하게 작성하세요. 예를 들어 "습도 83%"가 제공되었다면 "습도는 83%였다"처럼 작성하세요.
- 온도와 습도를 평가하거나 강조하지 마세요.
- "80%나", "꽤", "상당히", "제법", "높은 편", "낮은 편" 같은 수치 평가 표현은 사용하지 마세요.
- "쌀쌀한", "포근한", "후덥지근한", "나름 괜찮았다"처럼 날씨나 하루를 평가하는 표현은 사용자가 직접 말한 경우에만 사용하세요.
- 사용자가 "조용하고 차분한 분위기"처럼 답한 경우, 이는 날씨 자체의 성질이 아니라 일기 전체의 분위기로 반영하세요.
- "조용하고 차분한 날씨 속 하루였다"처럼 날씨와 하루를 어색하게 합쳐 쓰지 마세요.
- 첫 문장은 가능하면 "조용하고 차분한 하루였다."처럼 자연스럽게 작성하세요.
- "습도 83%로 꽤 습한", "습도 83%라서 습한", "습도 83%로 습했지만" 같은 표현은 금지합니다.
- 습도 정보는 "습도는 83%였고"처럼 중립적으로만 작성하세요.
- 사용자가 "없음", "딱히 없음", "특별히 없음"이라고 답한 경우에는 새로운 의미를 붙이지 말고 "특별히 남길 일은 없었다." 정도로만 짧게 반영하세요.
- 사용자가 "조용하고 차분한 분위기", "잔잔한 분위기", "편안한 분위기"처럼 답했더라도 본문에서는 "~분위기 속 하루였다" 형태로 쓰지 마세요.
- "조용하고 차분한 분위기"는 "조용하고 차분한 하루였다."처럼 하루 전체의 분위기로 자연스럽게 바꾸어 작성하세요.
- "~분위기 속 하루였다", "~날씨 속 하루였다", "~상태 속 하루였다" 같은 표현은 사용하지 마세요.

좋은 예:
조용하고 차분한 하루였다. 기온은 19도였고 습도는 83%였으며, 하늘은 흐려 있었다. 특별히 남길 일은 없었다.

나쁜 예:
조용하고 차분한 분위기 속 하루였다. 19도의 기온에 습도는 83%였고, 하늘은 흐려 있었다.


[데이터가 매우 적을 때의 최우선 규칙]
- 오늘의 데이터가 날씨만 있으면 2~3문장만 작성하세요.
- 날씨만 있는 경우 사용자 기존 일기 문체 참고가 있어도 내용, 사건, 목표, 계획, 감정 흐름을 가져오지 마세요.
- 날씨만 있는 경우 첫 문장은 날씨 상태만 언급하세요.
- 날씨만 있는 경우 마지막 문장도 날씨를 짧게 기록하는 정도로만 마무리하세요.
- "목표", "진행", "준비", "내일", "바란다", "기분", "답답하다" 같은 표현은 사용자가 직접 입력하지 않았다면 쓰지 마세요.
- 사용자의 내면 상태나 감정 해석이 들어가는 "느껴졌다", "기분이", "답답했다", "상쾌했다" 같은 표현은 날씨 데이터만으로 쓰지 마세요.
- 사용자가 "특별히 없음", "딱히 없음"처럼 답한 경우에는 새로운 의미를 부여하지 말고, "특별히 남길 일은 없었다" 정도로만 짧게 반영하세요.

[일정 데이터 해석 규칙]
- 일정 데이터는 제목, 시간, 장소, 설명 등 제공된 정보만 사용하세요.
- 일정 데이터만 있을 경우 일정의 제목, 시간, 장소, 설명, 사용자의 추가 답변만 사용하세요.
- 일정 제목만 있는 경우에는 "~일정이 있었다", "~일정이 등록되어 있었다" 정도로만 자연스럽게 언급하세요.
- 일정의 결과, 분위기, 대화 내용, 참석자, 성과는 사용자가 직접 말하지 않았다면 단정하지 마세요.
- 회의, 수업, 약속, 병원, 면접 같은 일정의 성격을 과장하거나 평가하지 마세요.
- 일정이 있었다는 이유만으로 사용자의 감정, 성취감, 만족감, 피로감, 보람, 긴장감, 방향성 정리 등을 추측하지 마세요.
- "방향이 명확해졌다", "뿌듯했다", "보람 있었다", "의미 있는 시간이었다", "좋은 회의였다" 같은 평가는 사용자가 직접 말한 경우에만 작성하세요.
- 회의 결과는 사용자가 답한 내용만 반영하세요. 예를 들어 "질문 생성과 사진 분석 쪽을 더 다듬기로 했다"처럼 작성하세요.
- 사진, 결제, 날씨와 일정이 같은 사건이라고 데이터상 명확하지 않으면 하나의 장면으로 묶지 마세요.
- 사용자가 "없음", "딱히 없음", "특별히 없음"이라고 답한 경우에는 새로운 감정이나 후속 행동을 만들지 말고 짧게만 반영하세요.
- 사용자가 "없음", "딱히 없음", "특별히 없음"이라고 직접 답하지 않았다면 "특별히 남길 일은 없었다" 같은 문장을 쓰지 마세요.

좋은 예:
오늘은 오후 2시에 학교 도서관에서 졸업프로젝트 회의가 있었다. 회의에서는 질문 생성과 사진 분석 쪽을 더 다듬기로 했다.

나쁜 예:
오늘은 오후 2시에 학교 도서관에서 졸업프로젝트 회의가 있었다. 앞으로 진행할 방향이 정해져 조금 더 명확해진 기분이다.

[결제 데이터 해석 규칙]
- 선택된 결제 내역이 있으면 하루를 떠올리는 단서로 자연스럽게 활용하되, 억지로 모든 결제를 나열하지 마세요.
- 결제 내역은 하루를 떠올리는 단서로만 활용하고, 감정은 사용자가 직접 표현한 경우에만 연결하세요.
- 결제 금액을 반복 나열하지 말 것
- 선택되지 않은 결제 데이터는 무시할 것
- 같은 장소/카테고리의 반복 결제는 필요할 경우 하나의 소비 흐름으로 짧게 요약할 수 있으나, 구체적인 행동은 단정하지 마세요.
- 결제만으로 감정이나 목적을 단정하지 말 것
- 사진과 결제가 동일한 행동을 가리키는 것이 데이터상 명확한 경우에만 하나의 장면으로 서술하세요.
- 결제 금액이 크거나 작다고 평가하지 마세요.
- "돈을 많이 썼다", "아까웠다", "만족스러웠다", "맛있었다" 같은 판단은 사용자가 직접 말하지 않았다면 쓰지 마세요.
- 결제 장소나 가맹점명이 있어도 실제로 무엇을 먹었는지, 누구와 있었는지, 왜 갔는지 단정하지 마세요.

[사진 데이터 반영 규칙 추가]
- 사진이 선택된 경우, 사용자가 사진과 무관한 답변을 하더라도 사진 속 장면을 한 문장 정도는 자연스럽게 반영하세요.
- 단, 사진 속 장면과 사용자의 답변을 같은 사건이라고 단정하지 마세요.
- 예: "함께 선택한 사진에는 책상 위 물건들이 담겨 있었다."처럼 관찰 가능한 내용만 짧게 언급하세요.


${if (recentDiarySamples.isNotBlank()) """
[사용자 기존 일기 문체 참고]
아래는 사용자가 최근 작성한 일기입니다. 내용이나 사건을 복사하지 말고 다음 요소만 모방하세요.
- 문장 길이
- 문단 수
- 감정 표현 강도
- 종결 어미
- 생각 전개 방식
- 사용자 문체가 짧으면 짧게, 담백하면 담백하게 유지하세요.
- 최근 일기의 내용이 아니라 문체만 참고하세요.
- 오늘 데이터가 매우 적을 때도 기존 일기의 내용 흐름을 따라 하지 말고, 문장 길이와 말투만 참고하세요.
- 오늘 데이터에 없는 목표, 계획, 감정, 사건, 행동은 기존 일기에서 가져오지 마세요.

[사용자 문체 참고 규칙]
- 이전 일기 샘플은 문체, 말투, 문장 길이만 참고하세요.
- 이전 일기 샘플의 사건, 날씨, 감정, 장소, 문장을 현재 일기에 가져오지 마세요.
- 현재 입력 데이터와 추가 답변에 없는 내용은 작성하지 마세요.
- 이전 일기에 있던 "조용하고 차분한 하루였다", "무난한 하루였다" 같은 문장을 현재 데이터에 근거 없이 반복하지 마세요.
- 문체 샘플은 내용 출처가 아니라 스타일 참고용입니다.
- 현재 일기는 반드시 오늘 선택된 데이터와 사용자의 추가 답변만 바탕으로 작성하고, 이전 초안이나 이전 저장 일기의 내용을 가져오지 마세요.

[최근 일기 샘플]
$recentDiarySamples
""" else ""}

[오늘의 데이터]
$blocksText

${if (followUpAnswerText.isNotBlank()) """
[사용자의 추가 답변]
$followUpAnswerText

사용자의 추가 답변은 사진 분석보다 더 신뢰도가 높은 정보입니다.
사진과 답변이 충돌하면 반드시 사용자의 답변을 우선하세요.
답변 내용을 그대로 복사하지 말고 자연스러운 일기 문장으로 녹여 작성하세요.
""" else ""}

[출력 전 최종 확인]

출력하기 전에 반드시 확인하세요.

□ 사진에 없는 행동을 만들지 않았는가?

□ 사용자가 직접 말하지 않은 평가나 의미를 새로 만들지 않았는가?

□ 사진들 사이에 없는 행동·이동·인과를 지어내 하나의 이야기로 엮지 않았는가?

□ "화면 캡처/수신 이미지"를 실제 방문한 장소나 한 행동으로 쓰지 않았는가?

□ 사용자가 직접 입력한 답변을 충분히 반영했는가?

□ 감정을 새로 만들어 쓰지 않았는가?

□ 촬영 시각이 없는 사진의 시간 순서를 임의로 구성하지 않았는가?

□ 사용자가 "모르겠다", "기억 안 난다", "특별히 없다", "딱히 없다"라고 답한 내용을 억지로 해석하거나 의미를 부여하지 않았는가?

□ 사용자가 직접 답한 내용이 사진보다 우선 반영되었는가?

□ 사진 속 사물을 근거로 새로운 행동이나 사건을 만들어내지 않았는가?

□ "오늘도 별거 없는 하루였다.", "평범한 하루였다."처럼 반복적인 시작 문장을 사용하지 않았는가?
""".trimIndent()
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

