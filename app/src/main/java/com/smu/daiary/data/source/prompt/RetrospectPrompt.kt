package com.smu.daiary.data.source.prompt

import com.smu.daiary.data.model.RetrospectType

/** 주간/월간 회고 — 내러티브 + 키워드 + 기억에 남는 하루를 1회 호출로 받는다. */
internal fun retrospectPrompt(
    type: RetrospectType,
    periodLabel: String,
    diarySummaries: String,
    emotionSummary: String,
    healthSummary: String,
    spendingSummary: String,
    scheduleSummary: String
): String {
    val periodInstruction = if (type == RetrospectType.WEEKLY) {
        "- 하루하루의 구체적 사건 언급 가능\n- 감정 흐름 변화 반영"
    } else {
        "- 한 달의 큰 흐름과 변화에 초점\n- 월초/월말 차이나 성장 반영\n- 세세한 하루 언급보다 전체 색채 표현"
    }

    return """
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
}
