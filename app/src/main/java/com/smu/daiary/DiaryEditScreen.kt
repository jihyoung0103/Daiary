package com.smu.daiary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.ui.theme.DaiaryTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private object DiaryEditColors {
    val Background = Color(0xFFFAFAF8)
    val TextPrimary = Color(0xFF2C2C2A)
    val TextMuted = Color(0xFF888780)
    val TextHint = Color(0xFFB4B2A9)
    val AccentPurple = Color(0xFF533AB7)
    val Border = Color(0xFFD3D1C7)
    val AiCardBg = Color(0xFFEEEDFE)
    val AiCardText = Color(0xFF3C3489)
    val SaveCompleteBg = Color(0xFF4BB898)
    val MoodBg = Color(0xFFE8F5EF)
    val MoodText = Color(0xFF2E7D5E)
    val TagBg = Color(0xFFF1EFE8)
    val ToolbarBg = Color(0xFFF7F6F2)
}

/**
 * 일기 편집 화면.
 *
 * @param date      편집 대상 날짜 (기본값: 오늘).
 * @param onBack    뒤로 가기 콜백.
 * @param onSave    저장 완료 콜백.
 * @param onTempSave 임시저장 콜백.
 */
@Composable
fun DiaryEditScreen(
    date: LocalDate = LocalDate.now(),
    onBack: () -> Unit = {},
    onSave: (title: String, content: String) -> Unit = { _, _ -> },
    onTempSave: (title: String, content: String) -> Unit = { _, _ -> }
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var aiExpanded by remember { mutableStateOf(false) }
    val tags = remember { mutableStateListOf("스타벅스", "강남역") }

    val dateLabel = remember(date) { formatDate(date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiaryEditColors.Background)
    ) {
        // 상단 바
        TopBar(
            onBack = onBack,
            onSave = { onSave(title, content) }
        )

        HorizontalDivider(color = DiaryEditColors.Border, thickness = 0.5.dp)

        // 날짜 + 감정 행
        DateMoodRow(dateLabel = dateLabel)

        HorizontalDivider(color = DiaryEditColors.Border, thickness = 0.5.dp)

        // 제목 입력
        TitleInput(
            value = title,
            onValueChange = { title = it }
        )

        HorizontalDivider(color = DiaryEditColors.Border, thickness = 0.5.dp)

        // 서식 툴바
        FormattingToolbar()

        HorizontalDivider(color = DiaryEditColors.Border, thickness = 0.5.dp)

        // 스크롤 가능 영역: AI 제안 + 본문 + 글자수
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            AiSuggestionCard(
                expanded = aiExpanded,
                onToggle = { aiExpanded = !aiExpanded }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ContentEditor(
                value = content,
                onValueChange = { content = it }
            )
        }

        HorizontalDivider(color = DiaryEditColors.Border, thickness = 0.5.dp)

        // 태그 행
        TagsRow(
            tags = tags,
            onRemoveTag = { tags.remove(it) },
            onAddTag = { if (it.isNotBlank()) tags.add(it) }
        )

        HorizontalDivider(color = DiaryEditColors.Border, thickness = 0.5.dp)

        // 하단 버튼
        BottomButtons(
            onTempSave = { onTempSave(title, content) },
            onSaveComplete = { onSave(title, content) }
        )
    }
}

// ─── 상단 바 ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.diary_back),
                tint = DiaryEditColors.TextPrimary
            )
        }
        Text(
            text = stringResource(R.string.diary_edit_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DiaryEditColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = DiaryEditColors.SaveCompleteBg),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 6.dp
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = stringResource(R.string.diary_save),
                fontSize = 13.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

// ─── 날짜 + 감정 ───────────────────────────────────────────────────────────────

@Composable
private fun DateMoodRow(dateLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            fontSize = 13.sp,
            color = DiaryEditColors.TextMuted
        )
        // TODO: AI 감정 분석 결과로 교체
        MoodBadge(emoji = "😊", label = stringResource(R.string.diary_mood_happy), percent = 85)
    }
}

@Composable
private fun MoodBadge(emoji: String, label: String, percent: Int) {
    Surface(
        color = DiaryEditColors.MoodBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Text(
                text = "$label ${percent}%",
                fontSize = 12.sp,
                color = DiaryEditColors.MoodText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── 제목 입력 ─────────────────────────────────────────────────────────────────

@Composable
private fun TitleInput(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = DiaryEditColors.TextPrimary
        ),
        cursorBrush = SolidColor(DiaryEditColors.AccentPurple),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.diary_title_hint),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = DiaryEditColors.TextHint
                )
            }
            inner()
        }
    )
}

// ─── 서식 툴바 ─────────────────────────────────────────────────────────────────

@Composable
private fun FormattingToolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryEditColors.ToolbarBg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToolbarButton(icon = Icons.Outlined.FormatBold, label = "Bold")
        ToolbarButton(icon = Icons.Outlined.FormatItalic, label = "Italic")
        ToolbarButton(icon = Icons.Outlined.FormatUnderlined, label = "Underline")
        ToolbarDivider()
        ToolbarButton(icon = Icons.AutoMirrored.Outlined.FormatAlignLeft, label = "Align")
        ToolbarDivider()
        ToolbarButton(icon = Icons.AutoMirrored.Outlined.FormatListBulleted, label = "List")
        ToolbarDivider()
        ToolbarButton(icon = Icons.Outlined.Info, label = "Info")
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String) {
    IconButton(
        onClick = { /* TODO: 서식 적용 */ },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = DiaryEditColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(DiaryEditColors.Border)
    )
}

// ─── AI 초안 카드 ──────────────────────────────────────────────────────────────

@Composable
private fun AiSuggestionCard(expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        color = DiaryEditColors.AiCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DiaryEditColors.AccentPurple.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "⊙",
                    fontSize = 14.sp,
                    color = DiaryEditColors.AccentPurple,
                    modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                )
                Text(
                    text = stringResource(R.string.diary_ai_draft_label) + " — " +
                        stringResource(R.string.diary_ai_draft_preview),
                    fontSize = 13.sp,
                    color = DiaryEditColors.AiCardText,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = stringResource(R.string.diary_ai_draft_full),
                    fontSize = 13.sp,
                    color = DiaryEditColors.AiCardText,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 20.dp)
                )
            }

            Text(
                text = if (expanded)
                    stringResource(R.string.diary_ai_collapse)
                else
                    stringResource(R.string.diary_ai_expand),
                fontSize = 12.sp,
                color = DiaryEditColors.AccentPurple,
                modifier = Modifier
                    .padding(top = 6.dp, start = 20.dp)
                    .clickable(onClick = onToggle)
            )
        }
    }
}

// ─── 본문 에디터 ───────────────────────────────────────────────────────────────

@Composable
private fun ContentEditor(value: String, onValueChange: (String) -> Unit) {
    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = DiaryEditColors.TextPrimary,
                lineHeight = 24.sp
            ),
            cursorBrush = SolidColor(DiaryEditColors.AccentPurple),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.diary_content_hint),
                        fontSize = 15.sp,
                        color = DiaryEditColors.TextHint,
                        lineHeight = 24.sp
                    )
                }
                inner()
            }
        )
        // 글자 수
        Text(
            text = stringResource(R.string.diary_char_count, value.length),
            fontSize = 11.sp,
            color = DiaryEditColors.TextMuted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// ─── 태그 행 ───────────────────────────────────────────────────────────────────

@Composable
private fun TagsRow(
    tags: List<String>,
    onRemoveTag: (String) -> Unit,
    onAddTag: (String) -> Unit
) {
    var showAddField by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.forEach { tag ->
            TagChip(label = tag, onRemove = { onRemoveTag(tag) })
        }

        if (showAddField) {
            BasicTextField(
                value = newTag,
                onValueChange = { newTag = it },
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    color = DiaryEditColors.TextPrimary
                ),
                cursorBrush = SolidColor(DiaryEditColors.AccentPurple),
                singleLine = true,
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DiaryEditColors.TagBg)
                    .border(1.dp, DiaryEditColors.AccentPurple, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                decorationBox = { inner ->
                    if (newTag.isEmpty()) {
                        Text(
                            text = stringResource(R.string.diary_tag_hint),
                            fontSize = 12.sp,
                            color = DiaryEditColors.TextHint
                        )
                    }
                    inner()
                }
            )
            // 입력 완료: 엔터 대신 ✓ 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DiaryEditColors.AccentPurple)
                    .clickable {
                        onAddTag(newTag)
                        newTag = ""
                        showAddField = false
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = "✓", fontSize = 12.sp, color = Color.White)
            }
        } else {
            // + 태그 추가 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DiaryEditColors.TagBg)
                    .border(1.dp, DiaryEditColors.Border, RoundedCornerShape(20.dp))
                    .clickable { showAddField = true }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = stringResource(R.string.diary_add_tag),
                    fontSize = 12.sp,
                    color = DiaryEditColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun TagChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DiaryEditColors.TagBg)
            .border(1.dp, DiaryEditColors.Border, RoundedCornerShape(20.dp))
            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = DiaryEditColors.TextPrimary)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = DiaryEditColors.TextMuted,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRemove)
        )
    }
}

// ─── 하단 버튼 ─────────────────────────────────────────────────────────────────

@Composable
private fun BottomButtons(onTempSave: () -> Unit, onSaveComplete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onTempSave,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DiaryEditColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DiaryEditColors.TextPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.diary_temp_save),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Button(
            onClick = onSaveComplete,
            modifier = Modifier
                .weight(2f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DiaryEditColors.SaveCompleteBg)
        ) {
            Text(
                text = stringResource(R.string.diary_save_complete),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

// ─── 유틸 ──────────────────────────────────────────────────────────────────────

private fun formatDate(date: LocalDate): String {
    val dayOfWeekLabel = when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "월요일"
        java.time.DayOfWeek.TUESDAY -> "화요일"
        java.time.DayOfWeek.WEDNESDAY -> "수요일"
        java.time.DayOfWeek.THURSDAY -> "목요일"
        java.time.DayOfWeek.FRIDAY -> "금요일"
        java.time.DayOfWeek.SATURDAY -> "토요일"
        java.time.DayOfWeek.SUNDAY -> "일요일"
        else -> ""
    }
    return "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 $dayOfWeekLabel"
}

// ─── 프리뷰 ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DiaryEditScreenPreview() {
    DaiaryTheme {
        DiaryEditScreen(date = LocalDate.of(2025, 3, 20))
    }
}
