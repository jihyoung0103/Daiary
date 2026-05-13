package com.smu.daiary.feature.write

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smu.daiary.R
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.LocalDarkTheme
import java.time.LocalDate

private data class Weather(val label: String, val icon: ImageVector)
private data class Emotion(val label: String, val icon: ImageVector)

private val weatherList = listOf(
    Weather("맑음", Icons.Outlined.WbSunny),
    Weather("흐림", Icons.Outlined.Cloud),
    Weather("비",   Icons.Outlined.Umbrella),
    Weather("눈",   Icons.Outlined.AcUnit),
    Weather("바람", Icons.Outlined.Air)
)

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
    val accent = wc.Purple
    val accentLight = wc.PurpleLight

    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var text by remember(draft?.date) {
        mutableStateOf(draft?.editedContent ?: draft?.aiContent ?: "")
    }
    val selectedWeather by viewModel.selectedWeather.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        uris.forEach { viewModel.addPhoto(it.toString()) }
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
                    IconButton(onClick = onBack) {
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
                            viewModel.updateEditedContent(text)
                            onDone()
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(36.dp)
                    ) {
                        Text(stringResource(R.string.btn_done), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ChipSection(label = stringResource(R.string.label_weather)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weatherList.forEach { w ->
                        IconSelectChip(
                            icon = w.icon,
                            label = localizedWeatherLabel(w.label),
                            selected = selectedWeather == w.label,
                            onClick = { viewModel.updateWeatherSelection(if (selectedWeather == w.label) null else w.label) }
                        )
                    }
                }
            }

            ChipSection(label = stringResource(R.string.label_emotion)) {
                val selectedEmotion by viewModel.selectedEmotion.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    emotionList.forEach { e ->
                        IconSelectChip(
                            icon = e.icon,
                            label = localizedEmotionLabel(e.label),
                            selected = selectedEmotion == e.label,
                            onClick = { viewModel.updateEmotionSelection(if (selectedEmotion == e.label) null else e.label) }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                color = wc.SurfaceBg,
                shape = RoundedCornerShape(20.dp),
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
                        val today = LocalDate.now()
                        Text(
                            text = stringResource(R.string.date_format_full, today.year, today.monthValue, today.dayOfMonth),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = accent
                        )
                        if (selectedWeather != null) {
                            Text(text = "·", fontSize = 13.sp, color = accent)
                            Text(text = selectedWeather!!, fontSize = 13.sp, color = accent)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.hint_write_diary),
                                color = wc.TextMuted,
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = wc.TextPrimary
                            )
                        )
                    }
                    Text(
                        text = stringResource(R.string.char_count, text.length),
                        fontSize = 11.sp,
                        color = wc.TextMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = wc.Border, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(accentLight)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                val photos = draft?.photos.orEmpty()
                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.add_photo_desc),
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.btn_add_photo), color = accent, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(photos) { uri ->
                            PhotoChip(uri = uri, onRemove = { viewModel.removePhoto(uri) })
                        }
                        item {
                            TextButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.add_photo_desc),
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.btn_add_photo), color = accent, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipSection(label: String, content: @Composable () -> Unit) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = wc.TextMuted,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun IconSelectChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val accent = wc.Purple
    val accentLight = wc.PurpleLight
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

@Composable
private fun PhotoChip(uri: String, onRemove: () -> Unit) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val accent = wc.Purple
    val accentLight = wc.PurpleLight
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentLight,
        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = uri.substringAfterLast("/").take(24),
                fontSize = 12.sp,
                color = accent
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.delete_photo_desc),
                        tint = accent,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
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
        val accent = wc.Purple
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
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    color = wc.SurfaceBg,
                    shape = RoundedCornerShape(20.dp),
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
                ChipSection(label = stringResource(R.string.label_weather)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        weatherList.forEach { w ->
                            IconSelectChip(
                                icon = w.icon, label = w.label,
                                selected = selectedWeather == w.label,
                                onClick = { selectedWeather = if (selectedWeather == w.label) null else w.label }
                            )
                        }
                    }
                }
                ChipSection(label = stringResource(R.string.label_emotion)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        emotionList.forEach { e ->
                            IconSelectChip(
                                icon = e.icon, label = e.label,
                                selected = selectedEmotion == e.label,
                                onClick = { selectedEmotion = if (selectedEmotion == e.label) null else e.label }
                            )
                        }
                    }
                }
                HorizontalDivider(color = wc.Border, thickness = 0.5.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .background(wc.PurpleLight)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = {}) {
                            Icon(imageVector = Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(R.string.add_photo_desc), tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.btn_add_photo), color = accent, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
