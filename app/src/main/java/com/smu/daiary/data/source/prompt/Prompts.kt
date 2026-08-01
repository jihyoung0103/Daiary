package com.smu.daiary.data.source.prompt

import com.smu.daiary.data.model.RetrospectType

/**
 * 일기 초안 외 프롬프트 모음. 사진 분석과 일기 본문은 "보이는 사실만 쓴다"는 같은 원칙을
 * 공유하므로, 한쪽만 고치고 다른 쪽을 놓치지 않도록 [diaryPrompt]와 가까이 둔다.
 */

/** 선택된 블록에서 일기를 풍성하게 할 맥락 질문을 뽑아낸다. */
internal fun contextQuestionsPrompt(blocksText: String): String = """
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

/**
 * 사진 1장에서 관찰 가능한 사실만 뽑아낸다.
 * 직접 촬영한 사진과 스크린샷/수신 이미지는 해석 범위가 달라 프롬프트를 나눈다.
 */
internal fun photoAnalysisPrompt(isCameraPhoto: Boolean): String = if (isCameraPhoto) {
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
