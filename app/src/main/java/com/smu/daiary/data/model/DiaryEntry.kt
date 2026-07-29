package com.smu.daiary.data.model

import com.smu.daiary.feature.write.model.DiaryBodyBlock

/**
 * Firestore에 저장되는 일기 데이터 모델.
 * 필드 기본값은 Firestore 역직렬화를 위해 반드시 필요합니다.
 */
data class DiaryEntry(
    val id: String = "",
    val title: String = "",
    /** 본문 평문 — blocks의 텍스트를 이어붙인 값. 회고·검색이 이 필드를 읽는다 */
    val content: String = "",
    /** 소스별 본문 블록. 옛 일기는 비어 있으며, 그때는 content를 쓴다 */
    val blocks: List<DiaryBodyBlock> = emptyList(),
    val emotion: String = "",                  // "기쁨" | "슬픔" | "평온" | "화남" | "설렘"
    val weather: String = "",                  // "맑음" | "흐림" | "비" | "뇌우" | "눈"
    /** 질답에서 "기타"로 자유입력한 감정 원문. 고정 감정 5종 밖의 값이라 emotion과 별도로 보관 */
    val customEmotionText: String = "",
    /** 질답에서 "기타"로 자유입력한 날씨 원문. 고정 날씨 5종 밖의 값이라 weather와 별도로 보관 */
    val customWeatherText: String = "",
    val photos: List<String> = emptyList(),    // 첨부 사진 URI 목록
    val date: String = "",                     // "YYYY-MM-DD"
    val createdAt: Long = System.currentTimeMillis()
)
