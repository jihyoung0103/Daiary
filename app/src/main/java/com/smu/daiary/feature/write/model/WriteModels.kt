package com.smu.daiary.feature.write.model

enum class BlockType(val label: String) {
    PAYMENT("결제 내역"),
    PHOTO("사진"),
    CALENDAR("오늘 일정"),
    CALENDAR_UPCOMING("향후 일정"),
    HEALTH("건강"),
    WEATHER("날씨"),
    WEATHER_TOMORROW("내일 날씨"),
    PHOTO_LOCATION("촬영 장소")
}

enum class DraftStatus { IDLE, EDITING, SAVED }

data class ContentBlock(
    val id: String,
    val type: BlockType,
    val content: String,
    val isSelected: Boolean = false,
    /** 날씨/일정 등 실제 데이터가 없어 안내 문구만 담긴 폴백 블록인지. true면 선택 불가(초안 생성 소스로 안 씀) */
    val isFallback: Boolean = false
)

data class DiaryDraft(
    val date: String,
    val aiContent: String,
    val editedContent: String? = null,
    val photos: List<String> = emptyList(),
    /** 소스별로 나뉜 본문 블록. 노션식 블록 편집/재배치의 단위 */
    val blocks: List<DiaryBodyBlock> = emptyList(),
    val status: DraftStatus = DraftStatus.IDLE
)

/**
 * AI에 넘기는 소스 아이템 — 본문 블록 1개의 재료가 된다.
 * 사진은 장별로 photo_1..N 으로 전개되며 각자의 분석 결과를 content로 갖는다.
 */
data class DiarySource(
    val sourceId: String,
    val type: BlockType,
    val content: String,
    /** PHOTO 소스일 때 해당 사진의 URI */
    val imageUri: String? = null
)

/**
 * AI가 소스 하나를 근거로 작성한 본문 블록. 블록 1개 = 소스 1개(엄격한 1:1).
 * sourceId를 들고 있어 블록 단위 재생성·출처 표시가 가능하다.
 */
/** 필드 기본값은 Firestore 역직렬화(무인자 생성자)를 위해 반드시 필요합니다. */
data class DiaryBodyBlock(
    /** 리스트 key / 재배치용 고유 id */
    val id: String = "",
    /** 대응하는 DiarySource.sourceId("photo_1", "weather"…). 옛 일기·수동 블록이면 null */
    val sourceId: String? = null,
    val text: String = "",
    /** 사진 블록이면 본문에 함께 표시할 사진. 저장 시 Storage URL로 치환됨 */
    val imageUri: String? = null
)

/**
 * 저장 시 업로드/치환 대상이 되는 사진 URI 목록.
 *
 * 원본은 **초안 자신**이다. WriteViewModel의 _photos는 수집 화면(loadBlocks)을 거치는
 * 신규 작성 경로에서만 채워지므로, 기존 일기 편집에는 비어 있거나 직전 세션 사진이 남아 있다.
 *
 * 첨부 목록(photos)과 본문 블록 사진(blocks[].imageUri)을 합치는 이유:
 * 분석 결과가 없어 블록이 안 만들어진 선택 사진은 photos에만, 옛 일기의 블록 사진은
 * blocks에만 있을 수 있다.
 */
fun DiaryDraft.photoUrisToSave(): List<String> =
    (photos + blocks.mapNotNull { it.imageUri }).distinct()

data class ContextQuestion(
    val blockId: String,
    val question: String,
    val quickOptions: List<String>
)

/** 카드 안의 문답 한 턴. answer가 null이면 아직 답하지 않은 턴이다. */
data class QnaTurn(
    val question: String,
    val answer: String? = null
)

/**
 * 질의응답 카드 1장 = 소재 1개.
 *
 * 같은 소재를 파고드는 후속 질문은 [turns]에 쌓여 카드 안에서 아래로 이어지고,
 * 다른 소재로 넘어갈 때만 카드가 바뀐다. 수평 이동은 화제 전환, 수직은 심화라
 * 사용자가 지금 어떤 질문을 받고 있는지 배치만 보고 알 수 있다.
 */
data class QnaCard(
    /** 대응하는 DiarySource.sourceId. 감정처럼 소스가 없는 카드는 "emotion" */
    val sourceId: String,
    /** 후속 질문을 만들 때 함께 넘길 원본 데이터. 감정 카드는 빈 문자열 */
    val sourceContent: String,
    /** 소재 종류("사진", "결제 내역"…). 대화 기록을 소재별로 묶어 보여줄 때 쓴다 */
    val sourceLabel: String = "",
    /**
     * 사진 소재면 그 사진의 URI. 질문 위에 썸네일로 띄운다.
     * 질문이 "이 치킨은…"처럼 지시어로 시작할 때 무엇을 가리키는지 보여줄 유일한 방법이다.
     */
    val imageUri: String? = null,
    val turns: List<QnaTurn>,
    /** 앱이 고정한 선택지. 비어 있으면 자유 입력 카드다 */
    val options: List<String> = emptyList(),
    /**
     * 이 소재의 대화를 마치며 남기는 한마디. 표시한 뒤 다음 카드로 넘어간다.
     * 답변하자마자 화면이 넘어가면 마지막 답이 허공에 떨어진 느낌이 들어서 둔다.
     */
    val closing: String? = null
) {
    /** 아직 답하지 않은 마지막 턴의 인덱스. 전부 답했으면 null */
    val pendingTurnIndex: Int? get() = turns.indexOfLast { it.answer == null }.takeIf { it >= 0 }
}

/**
 * 질의응답 결과 1건.
 * 답변은 선택지 탭이라 대부분 두세 글자다("그냥 그랬어"). 질문 텍스트 없이 답변만 넘기면
 * AI는 무엇에 대한 답인지 알 수 없으므로 질문을 반드시 함께 들고 다닌다.
 * sourceId는 질문이 나온 소스 id — 초안 생성 시 해당 소스에 답변을 붙이는 데 쓴다.
 */
data class QaAnswer(
    val sourceId: String,
    val question: String,
    val answer: String
)

data class CalendarSelectableItem(
    val id: Int,
    val displayText: String,
    val startTime: Long,
    val isSelected: Boolean = true
)

data class PaymentSelectableItem(
    val id: Int,
    val displayText: String,
    val amount: Int,
    val category: String = "기타",
    val isSelected: Boolean = true
)

data class PhotoSelectableItem(
    val uri: String,
    val isSelected: Boolean = true,
    /** 촬영 시각 (epoch millis). 갤러리 수동 추가 등 정보 없으면 0 */
    val takenAt: Long = 0L,
    /** EXIF 위치 (없으면 0.0) */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** EXIF 카메라 정보가 있으면 직접 촬영한 사진. 없으면 스크린샷/수신 이미지 */
    val isCameraPhoto: Boolean = false,
    /** Claude Vision 사진별 분석 결과 (세션 캐시). 미분석이면 null */
    val analysis: String? = null
)
