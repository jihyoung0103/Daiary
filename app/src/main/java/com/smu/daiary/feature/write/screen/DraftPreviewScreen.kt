package com.smu.daiary.feature.write.screen

import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.feature.write.model.*

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import android.util.Log
import com.smu.daiary.R
import com.smu.daiary.ui.theme.ButtonCornerRadius
import com.smu.daiary.ui.theme.ButtonHeight
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.White
import com.smu.daiary.ui.theme.emotionColor
import java.time.LocalDate

private val weatherIconMap: Map<String, ImageVector> = mapOf(
    "맑음" to Icons.Outlined.WbSunny,
    "흐림" to Icons.Outlined.Cloud,
    "비"   to Icons.Outlined.Umbrella,
    "눈"   to Icons.Outlined.AcUnit,
    "바람" to Icons.Outlined.Air
)

private val emotionIconMap: Map<String, ImageVector> = mapOf(
    "기쁨" to Icons.Outlined.SentimentVerySatisfied,
    "슬픔" to Icons.Outlined.SentimentDissatisfied,
    "평온" to Icons.Outlined.SentimentNeutral,
    "화남" to Icons.Outlined.SentimentVeryDissatisfied,
    "설렘" to Icons.Outlined.FavoriteBorder
)

@Composable
private fun formatDate(raw: String): String {
    val template = stringResource(R.string.date_format_full)
    return runCatching {
        val d = LocalDate.parse(raw)
        String.format(template, d.year, d.monthValue, d.dayOfMonth)
    }.getOrDefault(raw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftPreviewScreen(
    viewModel: WriteViewModel,
    userId: String,
    onEdit: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var showPhotoDialog by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf("") }
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val selectedWeather by viewModel.selectedWeather.collectAsStateWithLifecycle()
    val selectedEmotion by viewModel.selectedEmotion.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val photoAnalysisDebug by viewModel.photoAnalysisDebug.collectAsStateWithLifecycle()
    val selectedPhotos = photos.filter { it.isSelected }
    val displayText = draft?.editedContent ?: draft?.aiContent ?: ""
    var selectedImageUri by remember { mutableStateOf<String?>(null) }


    BackHandler {
        if (selectedImageUri != null) {
            selectedImageUri = null
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = wc.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_preview),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = wc.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = wc.TextPrimary
                        )
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 16.dp),
                            color = wc.Accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.saveDraft(userId) { success ->
                                    if (success) onSaved()
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.btn_save),
                                color = wc.Accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg),
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = {
            Surface(color = wc.SurfaceBg, shadowElevation = 8.dp) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPaddingHorizontal, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(ButtonHeight),
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = wc.Accent)
                ) {
                    Text(text = stringResource(R.string.btn_edit), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            draft?.let {
                Text(
                    text = formatDate(it.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = wc.Accent
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaPickerChip(
                    selected = selectedWeather,
                    options = weatherIconMap,
                    placeholder = stringResource(R.string.label_weather),
                    placeholderIcon = Icons.Outlined.WbSunny,
                    labelOf = { localizedWeatherLabel(it) },
                    tintOf = { wc.Accent },
                    onSelect = { viewModel.updateWeatherSelection(it) }
                )
                MetaPickerChip(
                    selected = selectedEmotion,
                    options = emotionIconMap,
                    placeholder = stringResource(R.string.label_emotion),
                    placeholderIcon = Icons.Outlined.SentimentNeutral,
                    labelOf = { localizedEmotionLabel(it) },
                    tintOf = { emotionColor(it, isDark) },
                    onSelect = { viewModel.updateEmotionSelection(it) }
                )
            }

            DiaryBodyBlocks(
                blocks = draft?.blocks.orEmpty(),
                fallbackText = displayText,
                onPhotoClick = { selectedPhotoUri = it; showPhotoDialog = true }
            )

            // [개발용] 일기 생성에 사용된 사진별 분석 내용 확인 (접이식)
            if (photoAnalysisDebug.isNotBlank()) {
                var showAnalysis by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = wc.SurfaceBg,
                    border = BorderStroke(0.5.dp, wc.Border)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAnalysis = !showAnalysis },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.photo_analysis_debug_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = wc.TextMuted
                            )
                            Text(
                                text = stringResource(if (showAnalysis) R.string.btn_hide else R.string.btn_show),
                                fontSize = 13.sp,
                                color = wc.Accent
                            )
                        }
                        if (showAnalysis) {
                            Text(
                                text = photoAnalysisDebug,
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = wc.TextPrimary
                            )
                        }
                    }
                }
            }

            val photos = draft?.photos.orEmpty()
            if (photos.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.attached_photos),
                    fontSize = 12.sp,
                    color = wc.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photos) { photo ->
                        PhotoThumbnail(
                            model = photo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clickable {
                                    selectedPhotoUri = photo
                                    showPhotoDialog = true
                                },
                            shape = RoundedCornerShape(CardCornerRadius)
                        )
                    }
                }
            }
        }
    }

    if (showPhotoDialog) {
        Dialog(
            onDismissRequest = { showPhotoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            var dialogVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { dialogVisible = true }

            AnimatedVisibility(
                visible = dialogVisible,
                enter = fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.9f, animationSpec = tween(250))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    PhotoThumbnail(
                        model = selectedPhotoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        shape = RectangleShape,
                        contentScale = ContentScale.Fit,
                        errorIconSize = 32.dp
                    )
                    AnimatedVisibility(
                        visible = dialogVisible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 250, delayMillis = 150)),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        IconButton(
                            onClick = { showPhotoDialog = false },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close_desc),
                                tint = White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 날짜 아래의 날씨·감정 표시. 탭하면 말풍선이 열려 선택지를 바로 고를 수 있다.
 * 미리보기와 편집 화면이 함께 쓴다(편집은 상세에서 바로 진입해 미리보기를 거치지 않는다).
 */
@Composable
internal fun MetaPickerChip(
    selected: String?,
    options: Map<String, ImageVector>,
    placeholder: String,
    placeholderIcon: ImageVector,
    labelOf: @Composable (String) -> String,
    /** 선택된 값의 아이콘 색. 감정은 5색 체계(emotionColor), 날씨는 accent */
    tintOf: @Composable (String) -> Color,
    onSelect: (String?) -> Unit
) {
    val wc = if (LocalDarkTheme.current) WriteColorsDark else WriteColors
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = options[selected] ?: placeholderIcon,
                contentDescription = null,
                tint = selected?.let { tintOf(it) } ?: wc.TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = selected?.let { labelOf(it) } ?: placeholder,
                fontSize = 13.sp,
                color = wc.TextMuted
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = wc.SurfaceBg
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                options.forEach { (key, icon) ->
                    val isSelected = selected == key
                    IconButton(
                        onClick = {
                            onSelect(if (isSelected) null else key)
                            expanded = false
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = labelOf(key),
                            tint = if (isSelected) tintOf(key) else wc.TextMuted,
                            modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftPreviewScreenPreview() {
    val sampleDraft = DiaryDraft(
        date = "2026-05-11",
        aiContent = "오늘은 날씨가 맑았다. 스타벅스에서 아메리카노를 마시며 팀 미팅을 준비했다. " +
                "오후에는 8,342걸음을 걸으며 산책을 즐겼고, 저녁엔 사진 정리를 했다. 전반적으로 알차고 기분 좋은 하루였다.",
        editedContent = null,
        photos = emptyList()
    )
    val wc = WriteColors
    DaiaryTheme {
        Scaffold(
            containerColor = wc.Bg,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.screen_preview), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = wc.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = wc.TextPrimary)
                        }
                    },
                    actions = {
                        TextButton(onClick = {}) {
                            Text(stringResource(R.string.btn_save), color = wc.Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg)
                )
            },
            bottomBar = {
                Surface(color = wc.SurfaceBg, shadowElevation = 8.dp) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPaddingHorizontal, vertical = 16.dp)
                            .padding(bottom = 8.dp)
                            .height(ButtonHeight),
                        shape = RoundedCornerShape(ButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = wc.Accent)
                    ) {
                        Text(stringResource(R.string.btn_edit), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenPaddingHorizontal, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = formatDate(sampleDraft.date), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = wc.Accent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaPickerChip(
                        selected = "맑음", options = weatherIconMap,
                        placeholder = "날씨", placeholderIcon = Icons.Outlined.WbSunny,
                        labelOf = { it }, tintOf = { wc.Accent }, onSelect = {}
                    )
                    MetaPickerChip(
                        selected = "기쁨", options = emotionIconMap,
                        placeholder = "감정", placeholderIcon = Icons.Outlined.SentimentNeutral,
                        labelOf = { it }, tintOf = { emotionColor(it, false) }, onSelect = {}
                    )
                }
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = wc.SurfaceBg,
                    border = BorderStroke(0.5.dp, wc.Border)
                ) {
                    Text(
                        text = sampleDraft.aiContent,
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = wc.TextPrimary
                    )
                }
            }
        }
    }
}
