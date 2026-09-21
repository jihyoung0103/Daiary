package com.smu.daiary.feature.retrospect

import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.data.model.HealthData
import com.smu.daiary.data.model.PaymentData
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.data.repository.DiaryRepository
import java.time.LocalDate
import kotlin.random.Random

/**
 * 디버그 빌드 전용 — 주간/월간 회고 기능을 테스트하기 위해
 * 최근 N일치 가짜 일기 + dailyData(건강/결제)를 Firestore에 채워 넣는다.
 *
 * ProfileScreen의 "회고 테스트 데이터 생성" 버튼에서만 호출된다.
 */
object RetrospectDebugSeeder {

    private val emotions = listOf("기쁨", "평온", "설렘", "슬픔", "화남")

    private val contents = listOf(
        "오늘은 아침부터 캡스톤 과제 때문에 정신없었다. 그래도 팀원들과 카페에 들러 잠깐 숨을 돌리니 마음이 한결 편해졌다.",
        "알바가 끝나고 나니 몸은 피곤했지만, 집에 오는 길에 본 노을이 예뻐서 기분이 조금 나아졌다.",
        "오랜만에 친구를 만나서 맛있는 걸 먹었다. 별일 없는 하루였지만 이야기를 나누니 즐거웠다.",
        "과제 마감이 다가와서 하루 종일 도서관에 있었다. 집중이 잘 안 돼서 조금 답답했다.",
        "운동을 다녀왔더니 몸이 개운했다. 저녁엔 일찍 잠들어야겠다고 생각했다.",
        "생각보다 일이 잘 풀린 하루였다. 작은 성취였지만 뿌듯했다.",
        "비가 와서 하루 종일 실내에 있었다. 조용히 책을 읽으며 시간을 보냈다.",
        "약속이 취소돼서 혼자 시간을 보냈다. 심심했지만 나름 여유로웠다.",
        "발표 준비 때문에 긴장했는데 무사히 끝나서 다행이었다. 끝나고 나니 몸에 힘이 풀렸다.",
        "특별한 일은 없었지만 평범하게 하루를 마무리했다. 내일은 조금 더 부지런히 움직여야겠다."
    )

    private val merchants = listOf(
        "스타벅스" to "카페",
        "GS25" to "편의점",
        "맥도날드" to "식사",
        "교보문고" to "기타",
        "올리브영" to "기타"
    )

    /** 오늘부터 최근 [days]일 동안의 테스트 일기/건강/결제 데이터를 생성 */
    suspend fun seedTestData(userId: String, days: Int = 10) {
        val diaryRepository = DiaryRepository()
        val dailyDataRepository = DailyDataRepository()
        val today = LocalDate.now()

        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            val emotion = emotions[i % emotions.size]

            diaryRepository.addDiary(
                userId,
                DiaryEntry(
                    title = "${date.monthValue}월 ${date.dayOfMonth}일의 기록",
                    content = contents[i % contents.size],
                    emotion = emotion,
                    weather = "",
                    date = dateStr
                )
            )

            dailyDataRepository.updateHealth(
                userId, dateStr,
                HealthData(
                    steps = Random.nextInt(3000, 12000),
                    sleepDurationMinutes = Random.nextInt(300, 480)
                )
            )

            val paymentCount = Random.nextInt(1, 3)
            val payments = (0 until paymentCount).map {
                val (merchant, category) = merchants.random()
                PaymentData(
                    merchant = merchant,
                    amount = Random.nextInt(3000, 25000),
                    paidAt = date.atTime(12 + it, 0)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                    category = category
                )
            }
            dailyDataRepository.updatePayments(userId, dateStr, payments)
        }
    }
}
