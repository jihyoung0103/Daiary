package com.smu.daiary.feature.write.screen

import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.feature.write.model.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smu.daiary.R
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.ScreenPaddingHorizontal
import com.smu.daiary.ui.theme.emotionColor
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.White
import java.time.LocalDate

private data class Emotion(val label: String, val icon: ImageVector)

private val weatherPickerOptions: Map<String, ImageVector> = mapOf(
    "맑음" to Icons.Outlined.WbSunny,
    "흐림" to Icons.Outlined.Cloud,
    "비"   to Icons.Outlined.Umbrella,
    "눈"   to Icons.Outlined.AcUnit,
    "바람" to Icons.Outlined.Air
)

private val emotionPickerOptions: Map<String, ImageVector>
    get() = emotionList.associate { it.label to it.icon }

private val emotionList = listOf(
    Emotion("기쁨", Icons.Outlined.SentimentVerySatisfied),
    Emotion("슬픔", Icons.Outlined.SentimentDissatisfied),
    Emotion("평온", Icons.Outlined.SentimentNeutral),
    Emotion("화남", Icons.Outlined.SentimentVeryDissatisfied),
    Emotion("설렘", Icons.Outlined.FavoriteBorder)
)

@Composable
internal fun localizedWeatherLabel(key: String) = when (key) {
    "맑음" -> stringResource(R.string.weather_sunny)
    "흐림" -> stringResource(R.string.weather_cloudy)
    "비"   -> stringResource(R.string.weather_rain)
    "눈"   -> stringResource(R.string.weather_snow)
    "바람" -> stringResource(R.string.weather_wind)
    else  -> key
}

@Composable
internal fun localizedEmotionLabel(key: String) = when (key) {
    "기쁨" -> stringResource(R.string.emotion_joy)
    "슬픔" -> stringResource(R.string.emotion_sad)
    "평온" -> stringResource(R.string.emotion_calm)
    "화남" -> stringResource(R.string.emotion_angry)
    "설렘" -> stringResource(R.string.emotion_excited)
    else  -> key
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditScreen(
    viewModel: WriteViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val accent = wc.Accent
    val accentLight = wc.AccentLight
    val dialogBg = if (isDark) SurfaceDark else White
    val dialogText = if (isDark) TextPrimaryDark else Ink

    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val hasContent = draft?.blocks.orEmpty().any { it.text.isNotBlank() }
    val selectedWeather by viewModel.selectedWeather.collectAsStateWithLifecycle()
    val selectedEmotion by viewModel.selectedEmotion.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasContent) {
        showExitDialog = true
    }
    BackHandler(enabled = isSaving) {}

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.dialog_exit_title), color = dialogText) },
            text  = { Text(stringResource(R.string.dialog_exit_message), color = dialogText) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.dialog_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.dialog_exit_cancel))
                }
            },
            containerColor = dialogBg
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = wc.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_diary_edit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = wc.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasContent) showExitDialog = true else onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = wc.TextPrimary
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onDone()
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = accent
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(36.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(stringResource(R.string.btn_done), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPaddingHorizontal, vertical = 12.dp),
                color = wc.SurfaceBg,
                shape = RoundedCornerShape(CardCornerRadius),
                border = BorderStroke(0.5.dp, wc.Border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        val dateToShow = draft?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
                        Text(
                            text = stringResource(R.string.date_format_full, dateToShow.year, dateToShow.monthValue, dateToShow.dayOfMonth),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = accent
                        )
                        MetaPickerChip(
                            selected = selectedWeather,
                            options = weatherPickerOptions,
                            placeholder = stringResource(R.string.label_weather),
                            placeholderIcon = Icons.Outlined.WbSunny,
                            labelOf = { localizedWeatherLabel(it) },
                            tintOf = { accent },
                            onSelect = { viewModel.updateWeatherSelection(it) }
                        )
                        MetaPickerChip(
                            selected = selectedEmotion,
                            options = emotionPickerOptions,
                            placeholder = stringResource(R.string.label_emotion),
                            placeholderIcon = Icons.Outlined.SentimentNeutral,
                            labelOf = { localizedEmotionLabel(it) },
                            tintOf = { emotionColor(it, isDark) },
                            onSelect = { viewModel.updateEmotionSelection(it) }
                        )
                    }
                    val blocks = draft?.blocks.orEmpty()
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                            BlockEditRow(
                                block = block,
                                isFirst = index == 0,
                                isLast = index == blocks.lastIndex,
                                onTextChange = { viewModel.updateBlockText(block.id, it) },
                                onMoveUp = { viewModel.moveBlock(block.id, -1) },
                                onMoveDown = { viewModel.moveBlock(block.id, 1) },
                                onRemove = { viewModel.removeBlock(block.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        item {
                            TextButton(
                                onClick = { viewModel.addBlock() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(text = stringResource(R.string.btn_add_block), color = accent, fontSize = 13.sp)
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.char_count, blocks.sumOf { it.text.length }),
                        fontSize = 11.sp,
                        color = wc.TextMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }

        }
    }
}

/** 본문 블록 1개 — 사진(있으면) + 편집 가능한 문단 + 하단 우측 조작 줄 */
@Composable
private fun BlockEditRow(
    block: DiaryBodyBlock,
    isFirst: Boolean,
    isLast: Boolean,
    onTextChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    Column(modifier = modifier.fillMaxWidth()) {
        block.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
        }
        BasicTextField(
            value = block.text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = wc.TextPrimary)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlockIconButton(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.block_move_up), enabled = !isFirst, onClick = onMoveUp)
            BlockIconButton(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.block_move_down), enabled = !isLast, onClick = onMoveDown)
            Spacer(Modifier.width(2.dp))
            BlockIconButton(Icons.Outlined.Close, stringResource(R.string.btn_delete), onClick = onRemove)
        }
        HorizontalDivider(color = wc.Border, thickness = 0.5.dp)
    }
}

@Composable
private fun BlockIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val wc = if (LocalDarkTheme.current) WriteColorsDark else WriteColors
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) wc.TextMuted else wc.Border,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun IconSelectChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color? = null
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val accent = accentColor ?: wc.Accent
    val accentLight = wc.AccentLight
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) accent else accentLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) accent else wc.TextMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryEditScreenPreview() {
    val sampleText = "오늘은 날씨가 맑았다. 오후에 카페에서 팀 미팅을 마치고 집에 돌아왔다. 기분 좋은 하루였다."
    var selectedWeather by remember { mutableStateOf<String?>("맑음") }
    var selectedEmotion by remember { mutableStateOf<String?>("기쁨") }
    DaiaryTheme {
        val isDark = LocalDarkTheme.current
        val wc = if (isDark) WriteColorsDark else WriteColors
        val accent = wc.Accent
        Scaffold(
            containerColor = wc.Bg,
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.screen_diary_edit), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = wc.TextPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = wc.TextPrimary)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.padding(end = 12.dp).height(36.dp)
                        ) {
                            Text(stringResource(R.string.btn_done), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPaddingHorizontal, vertical = 16.dp),
                    color = wc.SurfaceBg,
                    shape = RoundedCornerShape(CardCornerRadius),
                    border = BorderStroke(0.5.dp, wc.Border)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text("2026년 5월 11일", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accent)
                            Text(text = "·", fontSize = 13.sp, color = accent)
                            Text(text = "맑음", fontSize = 13.sp, color = accent)
                        }
                        Text(
                            text = sampleText,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = wc.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.char_count, sampleText.length),
                            fontSize = 11.sp,
                            color = wc.TextMuted,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
