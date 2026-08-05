package com.smu.daiary.feature.write.screen

import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.feature.write.model.*
import com.smu.daiary.ui.theme.ButtonCornerRadius
import com.smu.daiary.ui.theme.ButtonHeight
import com.smu.daiary.ui.theme.Error
import com.smu.daiary.ui.theme.ErrorDark
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.White

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smu.daiary.R

/**
 * 질의응답 자유 답변의 최대 글자 수.
 * 답변은 일기 초안의 재료일 뿐이라 길 필요가 없어 짧게 제한한다. 늘리려면 이 값만 고치면 된다.
 */
private const val MAX_ANSWER_LENGTH = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextQnAScreen(
    viewModel: WriteViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.smu.daiary.ui.theme.LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    val questions by viewModel.contextQuestions.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isGeneratingQuestions by viewModel.isGeneratingQuestions.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val generateError by viewModel.generateError.collectAsStateWithLifecycle()

    val answers = remember { mutableStateMapOf<String, String>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 일기 생성 완료 시 다음 화면으로 이동
    var hasNavigated by remember { mutableStateOf(false) }
    LaunchedEffect(draft) {
        if (draft != null && !hasNavigated) {
            hasNavigated = true
            onComplete()
        }
    }

    // 에러 스낵바
    LaunchedEffect(generateError) {
        val error = generateError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearGenerateError()
    }

    // 질문이 없으면 바로 일기 생성
    LaunchedEffect(questions) {
        if (questions != null && questions!!.isEmpty()) {
            viewModel.submitAnswers(emptyMap())
        }
    }

    val questionList = questions ?: emptyList()

    Scaffold(
        modifier = modifier,
        containerColor = wc.Bg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = if (isDark) ErrorDark else Error, contentColor = White)
            }
        },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isGenerating) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = if (isGenerating) wc.Border else wc.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.Bg),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->

        // 질문 생성 중이거나 일기 생성 중 — 로딩 화면
        if (isGenerating || isGeneratingQuestions || (questions != null && questionList.isEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = wc.Accent, strokeWidth = 3.dp)
                    Text(
                        text = stringResource(R.string.generating_diary),
                        fontSize = 15.sp,
                        color = wc.TextMuted
                    )
                }
            }
            return@Scaffold
        }

        if (questionList.isEmpty()) return@Scaffold

        // 모든 질문 완료
        if (currentIndex >= questionList.size) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = wc.Accent, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        val question = questionList[currentIndex]
        val selectedOption = answers[question.blockId]
        val isCustomSelected = selectedOption == "기타"
        val customText = answers[question.blockId + "_custom"] ?: ""

        // 다음 질문으로 넘어가거나, 마지막이면 답변을 제출한다.
        val goNext: () -> Unit = {
            val nextIndex = currentIndex + 1
            if (nextIndex >= questionList.size) {
                viewModel.submitAnswers(buildFinalAnswers(answers, questionList))
            } else {
                currentIndex = nextIndex
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 진행 바
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questionList.size },
                modifier = Modifier.fillMaxWidth(),
                color = wc.Accent,
                trackColor = wc.Border
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${currentIndex + 1} / ${questionList.size}",
                fontSize = 12.sp,
                color = wc.TextMuted
            )

            Spacer(Modifier.height(48.dp))

            // 질문 카드 — 좌→우 슬라이드 애니메이션
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
                },
                label = "question_anim"
            ) { idx ->
                val q = questionList[idx]
                Text(
                    text = q.question,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = wc.TextPrimary,
                    lineHeight = 30.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // 선택지가 없는 질문(AI가 만든 질문 전부)은 자유 입력으로 받는다.
            // 선택지가 있는 질문은 감정처럼 답변을 앱 데이터로 저장해야 하는 경우뿐이다.
            if (question.quickOptions.isEmpty()) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = {
                        if (it.length <= MAX_ANSWER_LENGTH) answers[question.blockId + "_custom"] = it
                    },
                    placeholder = {
                        Text(stringResource(R.string.hint_custom_answer), color = wc.TextMuted, fontSize = 14.sp)
                    },
                    supportingText = {
                        Text(
                            text = "${customText.length} / $MAX_ANSWER_LENGTH",
                            fontSize = 12.sp,
                            color = wc.TextMuted,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = wc.Accent,
                        unfocusedBorderColor = wc.Border,
                        focusedTextColor = wc.TextPrimary,
                        unfocusedTextColor = wc.TextPrimary,
                        cursorColor = wc.Accent
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        answers[question.blockId] = customText
                        goNext()
                    },
                    enabled = customText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = wc.Accent,
                        disabledContainerColor = wc.Border
                    )
                ) {
                    Text(stringResource(R.string.btn_next), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                // 빠른 선택 버튼 (한 줄 배치, 다 안 들어가면 가로 스크롤)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    question.quickOptions.forEach { option ->
                        val isSelected = selectedOption == option
                        Surface(
                            shape = RoundedCornerShape(ButtonCornerRadius),
                            color = if (isSelected) wc.Accent else wc.SurfaceBg,
                            onClick = {
                                answers[question.blockId] = option
                                if (option != "기타") goNext()
                            }
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) White else wc.TextPrimary
                            )
                        }
                    }
                }

                // "기타" 선택 시 직접 입력 필드
                if (isCustomSelected) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = {
                            if (it.length <= MAX_ANSWER_LENGTH) answers[question.blockId + "_custom"] = it
                        },
                        placeholder = {
                            Text(stringResource(R.string.hint_custom_answer), color = wc.TextMuted, fontSize = 14.sp)
                        },
                        supportingText = {
                            Text(
                                text = "${customText.length} / $MAX_ANSWER_LENGTH",
                                fontSize = 12.sp,
                                color = wc.TextMuted,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = wc.Accent,
                            unfocusedBorderColor = wc.Border,
                            focusedTextColor = wc.TextPrimary,
                            unfocusedTextColor = wc.TextPrimary,
                            cursorColor = wc.Accent
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            answers[question.blockId] = customText.ifBlank { "기타" }
                            goNext()
                        },
                        enabled = customText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                        shape = RoundedCornerShape(ButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = wc.Accent,
                            disabledContainerColor = wc.Border
                        )
                    ) {
                        Text(stringResource(R.string.btn_next), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 건너뛰기 — 감정은 반드시 답해야 하므로 감정 카드에서는 숨긴다
            if (question.blockId != "emotion") {
            TextButton(
                onClick = goNext,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_skip),
                    fontSize = 14.sp,
                    color = wc.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
            }
        }
    }
}

/**
 * answers 맵에서 "_custom" 키를 정리하고 최종 답변 맵을 반환.
 * "기타"를 선택했지만 실제 입력값이 있으면 입력값으로 대체.
 */
private fun buildFinalAnswers(
    answers: Map<String, String>,
    questions: List<ContextQuestion>
): Map<String, String> = questions
    .mapNotNull { q ->
        val answer = answers[q.blockId] ?: return@mapNotNull null
        val finalAnswer = if (answer == "기타") {
            answers[q.blockId + "_custom"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        } else answer
        q.blockId to finalAnswer
    }
    .toMap()
