package com.smu.daiary.data.source

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * OpenWeather condition code → canonical 날씨 5종 매핑 테스트.
 * 기기/네트워크 불필요, JVM에서 실행.
 *
 * when 분기는 위에서부터 평가되므로 순서를 바꾸면 조용히 깨진다.
 * 아래 케이스들은 그 순서에 의존하는 지점만 골라 고정한 것이다.
 */
class WeatherMappingTest {

    @Test
    fun `511 우박은 5xx지만 비가 아니라 눈으로 간다`() {
        // 비(500..531) 분기가 위로 올라가면 여기서 깨진다
        assertEquals("눈", mapToCanonical(511))
        assertEquals("비", mapToCanonical(510))
        assertEquals("비", mapToCanonical(512))
    }

    @Test
    fun `구름 경계는 801과 802 사이다`() {
        assertEquals("맑음", mapToCanonical(800)) // 청천
        assertEquals("맑음", mapToCanonical(801)) // 구름 조금 11~25%
        assertEquals("흐림", mapToCanonical(802)) // 구름 낀 25~50%
        assertEquals("흐림", mapToCanonical(804)) // 온흐림
    }

    @Test
    fun `2xx는 비가 아니라 뇌우다`() {
        assertEquals("뇌우", mapToCanonical(200))
        assertEquals("뇌우", mapToCanonical(232))
    }

    @Test
    fun `7xx 대기현상은 전부 흐림으로 흡수된다`() {
        assertEquals("흐림", mapToCanonical(701)) // 박무
        assertEquals("흐림", mapToCanonical(741)) // 안개
        assertEquals("흐림", mapToCanonical(751)) // 모래 — 황사 계열
        assertEquals("흐림", mapToCanonical(761)) // 먼지 — 황사 계열
        assertEquals("흐림", mapToCanonical(771)) // 돌풍
    }

    @Test
    fun `나머지 그룹은 그룹 번호대로 간다`() {
        assertEquals("비", mapToCanonical(300))
        assertEquals("비", mapToCanonical(500))
        assertEquals("눈", mapToCanonical(600))
        assertEquals("눈", mapToCanonical(622))
    }

    @Test
    fun `알 수 없는 코드는 맑음이 아니라 흐림이다`() {
        // 안 맑았을 수 있는 날을 "맑음"으로 단정하지 않는다
        assertEquals("흐림", mapToCanonical(0))
        assertEquals("흐림", mapToCanonical(999))
    }
}
