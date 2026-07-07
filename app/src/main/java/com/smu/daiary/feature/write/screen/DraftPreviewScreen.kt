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
import androidx.compose.material.icons.outlined.Favorite
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
import coil.compose.AsyncImage
import com.smu.daiary.R
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.LocalDarkTheme
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
    "설렘" to Icons.Outlined.Favorite
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
    val photoAnalysis by viewModel.photoAnalysis.collectAsStateWithLifecycle()
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
                            color = wc.Purple,
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
                                color = wc.Purple,
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
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = wc.Purple)
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            draft?.let {
                Text(
                    text = formatDate(it.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = wc.Purple
                )
            }

            if (selectedWeather != null || selectedEmotion != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    selectedWeather?.let { key ->
                        weatherIconMap[key]?.let { icon ->
                            MetaChip(icon = icon, label = localizedWeatherLabel(key))
                        }
                    }
                    selectedEmotion?.let { key ->
                        emotionIconMap[key]?.let { icon ->
                            MetaChip(icon = icon, label = localizedEmotionLabel(key))
                        }
                    }
                }
            }

            val photoAnalysisSnapshot = photoAnalysis
            if (!photoAnalysisSnapshot.isNullOrBlank()) {
                val materials = extractDiaryMaterials(photoAnalysisSnapshot)
                if (materials != null) {
                    var photoCardExpanded by remember { mutableStateOf(false) }
                    val summaryText = materials.lines().filter { it.isNotBlank() }.take(3).joinToString("\n")
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = wc.SurfaceBg,
                        border = BorderStroke(0.5.dp, wc.Border)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { photoCardExpanded = !photoCardExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.photo_analysis_card_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = wc.TextPrimary
                                )
                                Icon(
                                    imageVector = if (photoCardExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = wc.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (photoCardExpanded) materials else summaryText,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = wc.TextMuted
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = wc.SurfaceBg,
                border = BorderStroke(0.5.dp, wc.Border)
            ) {
                Text(
                    text = displayText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = wc.TextPrimary
                )
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
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(wc.PurpleLight)
                                .clickable {
                                    selectedPhotoUri = photo
                                    showPhotoDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = photo,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
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
                                contentDescription = "닫기",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun extractDiaryMaterials(text: String): String? {
    val headerPattern = Regex("##\\s*일기 작성에 활용하기 좋은 소재")
    val startIdx = headerPattern.find(text)?.range?.last?.plus(1) ?: return null
    val nextHeaderIdx = text.indexOf("\n##", startIdx).let { if (it == -1) text.length else it }
    return text.substring(startIdx, nextHeaderIdx).trim().ifBlank { null }
}

@Composable
private fun MetaChip(icon: ImageVector, label: String) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = wc.Purple,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = wc.TextMuted
        )
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
                            Text(stringResource(R.string.btn_save), color = wc.Purple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .padding(bottom = 8.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = wc.Purple)
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = formatDate(sampleDraft.date), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = wc.Purple)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaChip(icon = Icons.Outlined.WbSunny, label = "맑음")
                    MetaChip(icon = Icons.Outlined.SentimentVerySatisfied, label = "기쁨")
                }
                Surface(
                    shape = RoundedCornerShape(28.dp),
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
