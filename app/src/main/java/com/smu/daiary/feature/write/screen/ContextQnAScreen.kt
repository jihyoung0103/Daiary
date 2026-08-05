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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * 질의응답 화면.
 *
 * 카드 1장 = 소재 1개다. 같은 소재를 파고드는 후속 질문은 카드 안에서 아래로 이어지고(수직),
 * 다른 소재로 넘어갈 때만 카드가 좌우로 바뀐다(수평). 배치가 곧 구조라
 * 사용자가 지금 받는 질문이 이어지는 질문인지 새 주제인지 설명 없이 알 수 있다.
 */
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

    val cards by viewModel.qnaCards.collectAsStateWithLifecycle()
    val cardIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
    val isLoadingFollowUp by viewModel.isLoadingFollowUp.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isGeneratingQuestions by viewModel.isGeneratingQuestions.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val generateError by viewModel.generateError.collectAsStateWithLifecycle()

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
    LaunchedEffect(cards) {
        if (cards != null && cards!!.isEmpty()) viewModel.skipCurrentCard()
    }

    val cardList = cards ?: emptyList()

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
        if (isGenerating || isGeneratingQuestions || (cards != null && cardList.isEmpty())) {
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

        if (cardList.isEmpty() || cardIndex >= cardList.size) {
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

        val card = cardList[cardIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenPaddingHorizontal)
        ) {
            Spacer(Modifier.height(8.dp))

            // 진행 바 — 카드 기준. 후속 질문이 붙어도 눈금은 늘지 않는다(같은 소재이므로)
            LinearProgressIndicator(
                progress = { (cardIndex + 1).toFloat() / cardList.size },
                modifier = Modifier.fillMaxWidth(),
                color = wc.Accent,
                trackColor = wc.Border
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${cardIndex + 1} / ${cardList.size}",
                fontSize = 12.sp,
                color = wc.TextMuted
            )

            Spacer(Modifier.height(32.dp))

            // 카드 전환만 좌우 슬라이드. 카드 안에서 턴이 늘어나는 건 같은 카드라 애니메이션 없음
            AnimatedContent(
                targetState = cardIndex,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                },
                label = "card_anim"
            ) { idx ->
                val shown = cardList.getOrNull(idx) ?: return@AnimatedContent
                QnaCardBody(
                    card = shown,
                    wc = wc,
                    isLoadingFollowUp = isLoadingFollowUp,
                    onAnswer = { viewModel.answerCurrentTurn(it) },
                    onSkip = { viewModel.skipCurrentCard() }
                )
            }
        }
    }
}

/**
 * 카드 1장의 본문. 이미 답한 턴은 위에 문답 기록으로 쌓이고,
 * 아직 답하지 않은 마지막 턴만 입력 UI를 갖는다.
 */
@Composable
private fun QnaCardBody(
    card: QnaCard,
    wc: WriteColorScheme,
    isLoadingFollowUp: Boolean,
    onAnswer: (String) -> Unit,
    onSkip: () -> Unit
) {
    val pendingIndex = card.pendingTurnIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        card.turns.forEachIndexed { index, turn ->
            if (index > 0) Spacer(Modifier.height(24.dp))

            // 이어지는 질문임을 왼쪽 세로선으로 표시
            Row(modifier = Modifier.fillMaxWidth()) {
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(if (turn.answer == null) 60.dp else 40.dp)
                            .padding(top = 4.dp)
                    ) {
                        Surface(color = wc.Border, modifier = Modifier.fillMaxSize()) {}
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = turn.question,
                        fontSize = if (index == 0) 22.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = wc.TextPrimary,
                        lineHeight = if (index == 0) 30.sp else 26.sp
                    )

                    // 이미 답한 턴 — 답변을 회색으로 남겨 대화 기록처럼 보이게
                    turn.answer?.let { answer ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = answer,
                            fontSize = 15.sp,
                            color = wc.TextMuted
                        )
                    }
                }
            }

            // 아직 답하지 않은 턴 — 입력 UI
            if (index == pendingIndex) {
                Spacer(Modifier.height(20.dp))
                if (card.options.isEmpty()) {
                    FreeAnswerInput(wc = wc, onSubmit = onAnswer)
                } else {
                    OptionAnswerInput(options = card.options, wc = wc, onSubmit = onAnswer)
                }
            }
        }

        // 후속 질문을 받아오는 중
        if (isLoadingFollowUp) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = wc.TextMuted,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(text = "…", fontSize = 15.sp, color = wc.TextMuted)
            }
        }

        Spacer(Modifier.height(32.dp))

        // 건너뛰기 — 감정은 반드시 답해야 하므로 감정 카드에서는 숨긴다
        if (card.sourceId != "emotion" && !isLoadingFollowUp) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(R.string.btn_skip),
                    fontSize = 14.sp,
                    color = wc.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 자유 입력 답변 — AI가 만든 질문 전부가 이 형태다 */
@Composable
private fun FreeAnswerInput(
    wc: WriteColorScheme,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_ANSWER_LENGTH) text = it },
            placeholder = {
                Text(stringResource(R.string.hint_custom_answer), color = wc.TextMuted, fontSize = 14.sp)
            },
            supportingText = {
                Text(
                    text = "${text.length} / $MAX_ANSWER_LENGTH",
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
            onClick = { onSubmit(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight),
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

/**
 * 고정 선택지 답변 — 감정처럼 답이 앱 데이터로 저장되는 질문에만 쓴다.
 * "기타"를 고르면 자유 입력으로 넘어간다.
 */
@Composable
private fun OptionAnswerInput(
    options: List<String>,
    wc: WriteColorScheme,
    onSubmit: (String) -> Unit
) {
    var isCustom by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            options.forEach { option ->
                val isSelected = isCustom && option == "기타"
                Surface(
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    color = if (isSelected) wc.Accent else wc.SurfaceBg,
                    onClick = {
                        if (option == "기타") isCustom = true else onSubmit(option)
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

        if (isCustom) {
            Spacer(Modifier.height(16.dp))
            FreeAnswerInput(wc = wc, onSubmit = onSubmit)
        }
    }
}
