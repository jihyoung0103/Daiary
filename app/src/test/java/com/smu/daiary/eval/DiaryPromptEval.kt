package com.smu.daiary.eval

import com.smu.daiary.data.source.AnthropicDataSource
import com.smu.daiary.feature.write.model.BlockType
import com.smu.daiary.feature.write.model.DiarySource
import com.smu.daiary.feature.write.model.QaAnswer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import java.io.File

/**
 * 프롬프트 평가 도구.
 *
 * 이름은 테스트지만 통과/실패를 판정하지 않는다. 고정 입력(fixture)으로 일기를 생성해
 * 결과를 파일로 떨구는 것이 전부다. 프롬프트를 고치기 전후로 한 번씩 돌려 비교하는 데 쓴다.
 *
 * ⚠️ 실행하면 실제 Claude API를 호출한다. fixture 1개당 1회, 수십 원 수준.
 *    자동으로 돌 경로(CI)가 없으므로 @Ignore는 붙이지 않았다. 붙이면 IDE에서
 *    수동 실행도 막혀 도구로 쓸 수 없다.
 *
 * 사용법
 *   1. 프롬프트를 고치기 전에 label = "v0"으로 실행 → app/build/eval/{날짜}_v0.md
 *   2. 프롬프트 수정
 *   3. label = "v1"로 바꿔 실행 → app/build/eval/{날짜}_v1.md
 *   4. git diff --no-index app/build/eval/{날짜}_v0.md app/build/eval/{날짜}_v1.md
 *
 * 출력 경로가 app/ 아래인 것은 Gradle 유닛 테스트의 작업 디렉터리가 모듈 폴더이기 때문이다.
 * 라벨만 바꿔 다시 돌릴 때는 소스가 안 바뀌어 Gradle이 건너뛰므로 --rerun-tasks가 필요하다.
 *
 * 같은 입력이어도 모델 출력은 매번 조금씩 다르다. 1회 비교로 판단하지 말고
 * 같은 라벨로 두세 번 돌려 경향을 보는 편이 안전하다.
 */
class DiaryPromptEval {

    /** 결과 파일에 붙는 꼬리표. 프롬프트를 고칠 때마다 v1, v2… 로 올린다. */
    private val label = "v9"

    private val fixtureDir = File("src/test/resources/fixtures")
    private val outputDir = File("build/eval")

    @Test
    fun `고정 입력으로 일기 생성`() = runBlocking {
        val fixtures = fixtureDir.listFiles { f -> f.extension == "json" }?.sorted().orEmpty()
        check(fixtures.isNotEmpty()) { "fixture가 없다: ${fixtureDir.absolutePath}" }

        outputDir.mkdirs()
        val dataSource = AnthropicDataSource()

        fixtures.forEach { file ->
            val fx = parseFixture(file)
            println("▶ ${file.name} — 소스 ${fx.sources.size}개, 답변 ${fx.qaAnswers.size}건")

            val blocks = dataSource.generateDiaryBlocks(
                sources = fx.sources,
                locale = "ko",
                mbti = fx.mbti,
                recentDiarySamples = fx.recentDiarySamples,
                qaAnswers = fx.qaAnswers
            )

            val name = file.nameWithoutExtension.removePrefix("fixture_")
            val out = File(outputDir, "${name}_$label.md")
            out.writeText(render(name, fx, blocks.map { it.sourceId to it.text }))
            println("  → ${out.path} (블록 ${blocks.size}개)")
        }
    }

    // ─────────────────────────────────────────────────────────────

    private class Fixture(
        val mbti: String,
        val recentDiarySamples: String,
        val sources: List<DiarySource>,
        val qaAnswers: List<QaAnswer>
    )

    private fun parseFixture(file: File): Fixture {
        val json = JSONObject(file.readText())

        val sources = json.getJSONArray("sources").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DiarySource(
                    sourceId = o.getString("sourceId"),
                    // 저장할 때 label이 아니라 enum 이름으로 넣어둔 덕에 그대로 복원된다
                    type = BlockType.valueOf(o.getString("type")),
                    content = o.getString("content")
                )
            }
        }

        val answers = json.getJSONArray("qaAnswers").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                QaAnswer(
                    sourceId = o.getString("sourceId"),
                    question = o.getString("question"),
                    answer = o.getString("answer")
                )
            }
        }

        return Fixture(
            mbti = json.optString("mbti", "INFP"),
            recentDiarySamples = json.optString("recentDiarySamples"),
            sources = sources,
            qaAnswers = answers
        )
    }

    /**
     * 소재마다 입력·문답·결과를 함께 적는다.
     *
     * 결과만 있으면 "이 문장이 어디서 왔나"를 판단하려고 매번 fixture JSON을 따로 열어야 한다.
     * 셋을 나란히 두면 지어낸 문장인지 답변에서 온 문장인지 한눈에 보인다.
     * 문단이 나오지 않은 소스도 남긴다 — 생략이 개선인지 회귀인지는 그걸 봐야 안다.
     */
    private fun render(
        name: String,
        fx: Fixture,
        blocks: List<Pair<String, String>>
    ): String = buildString {
        val textById = blocks.toMap()
        val answersBySource = fx.qaAnswers.groupBy { it.sourceId }
        val sourceIds = fx.sources.map { it.sourceId }.toSet()

        // 생성된 순서를 그대로 따르고, 문단이 안 나온 소스를 뒤에 붙인다
        val ordered = blocks.map { it.first } + fx.sources.map { it.sourceId }.filterNot { it in textById }
        val sourceById = fx.sources.associateBy { it.sourceId }

        appendLine("# $name · $label")
        appendLine()
        appendLine("- 소스 ${fx.sources.size}개 → 블록 ${blocks.size}개")
        appendLine("- 답변 ${fx.qaAnswers.size}건")
        appendLine("- MBTI: ${fx.mbti}")
        appendLine()

        ordered.forEach { id ->
            val source = sourceById[id] ?: return@forEach
            val answers = answersBySource[id].orEmpty()
            val text = textById[id]

            appendLine("---")
            appendLine()
            appendLine("## $id · ${source.type.label}")
            appendLine()

            appendLine("**입력**")
            appendLine()
            appendLine("```")
            appendLine(source.content.trim())
            appendLine("```")
            appendLine()

            appendLine("**문답** ${if (answers.isEmpty()) "— 없음" else "(${answers.size}턴)"}")
            appendLine()
            answers.forEach {
                appendLine("- Q. ${it.question}")
                appendLine("  A. ${it.answer}")
            }
            if (answers.isNotEmpty()) appendLine()

            appendLine("**결과**")
            appendLine()
            if (text == null) {
                appendLine("_(문단 생성 안 됨)_")
            } else {
                text.trim().lines().forEach { appendLine("> $it") }
            }
            appendLine()
        }

        // 소재에 붙지 않는 답변(감정 등). 일기 전체에 영향을 주므로 따로 적는다
        val loose = fx.qaAnswers.filterNot { it.sourceId in sourceIds }
        if (loose.isNotEmpty()) {
            appendLine("---")
            appendLine()
            appendLine("## 하루 전체 답변")
            appendLine()
            loose.forEach {
                appendLine("- Q. ${it.question}")
                appendLine("  A. ${it.answer}")
            }
            appendLine()
        }
    }
}
