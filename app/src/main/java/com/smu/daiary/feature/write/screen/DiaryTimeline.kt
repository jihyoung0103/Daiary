package com.smu.daiary.feature.write.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smu.daiary.R
import com.smu.daiary.feature.write.model.DiaryBodyBlock
import com.smu.daiary.feature.write.model.TimeSlot
import com.smu.daiary.feature.write.model.sourceType
import com.smu.daiary.feature.write.model.timeSlot
import com.smu.daiary.ui.theme.CardCornerRadius
import com.smu.daiary.ui.theme.LocalDarkTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val RailWidth = 32.dp
/** 카드 첫 줄(시각·태그) 높이의 가운데. 점과 선 끝이 여기에 맞는다 */
private val DotCenterY = 23.dp
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 일기 본문을 하루의 흐름대로 세로 선 위에 블록 카드로 보여준다.
 * 블록은 저장된 순서 그대로 두고, 시간대가 바뀌는 곳에만 구분 라벨을 넣는다.
 * blocks가 비면(블록화 이전 일기) 기존 본문 카드로 보여준다.
 */
@Composable
fun DiaryTimeline(
    blocks: List<DiaryBodyBlock>,
    fallbackText: String,
    modifier: Modifier = Modifier,
    onPhotoClick: (String) -> Unit = {}
) {
    if (blocks.isEmpty()) {
        DiaryBodyBlocks(blocks = blocks, fallbackText = fallbackText, modifier = modifier)
        return
    }
    val wc = if (LocalDarkTheme.current) WriteColorsDark else WriteColors

    Column(modifier = modifier.fillMaxWidth()) {
        var prevSlot: TimeSlot? = null
        blocks.forEachIndexed { index, block ->
            val slot = block.timeSlot()
            if (slot != prevSlot) SlotLabel(slot, isFirst = index == 0, wc = wc)
            prevSlot = slot
            TimelineRow(block, isLast = index == blocks.lastIndex, wc = wc, onPhotoClick = onPhotoClick)
        }
    }
}

/** 행 전체 높이에 걸쳐 레일 중앙에 세로선을 그린다. to가 있으면 그 높이에서 끊는다 */
private fun Modifier.rail(color: Color, to: Dp? = null) = drawBehind {
    val x = RailWidth.toPx() / 2
    drawLine(color, Offset(x, 0f), Offset(x, to?.toPx() ?: size.height), strokeWidth = 2.dp.toPx())
}

@Composable
private fun SlotLabel(slot: TimeSlot, isFirst: Boolean, wc: WriteColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFirst) Modifier else Modifier.rail(wc.Border))
            .padding(top = if (isFirst) 0.dp else 8.dp, bottom = 8.dp)
    ) {
        Spacer(Modifier.width(RailWidth + 12.dp))
        Text(
            text = slotLabel(slot),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (slot == TimeSlot.TOMORROW) wc.TextMuted else wc.Accent
        )
    }
}

@Composable
private fun TimelineRow(
    block: DiaryBodyBlock,
    isLast: Boolean,
    wc: WriteColorScheme,
    onPhotoClick: (String) -> Unit
) {
    val timed = block.occurredAt > 0L
    val tomorrow = block.timeSlot() == TimeSlot.TOMORROW
    val type = block.sourceType()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rail(wc.Border, to = if (isLast) DotCenterY else null)
            .padding(bottom = if (isLast) 0.dp else 12.dp)
    ) {
        Box(
            modifier = Modifier.width(RailWidth).padding(top = DotCenterY - 7.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 바깥 링을 배경색으로 칠해 점 뒤로 선이 비치지 않게 한다
            val dot = Modifier.size(14.dp).background(wc.Bg, CircleShape).padding(2.dp)
            Box(
                modifier = when {
                    tomorrow -> dot.border(2.dp, wc.TextMuted, CircleShape)
                    timed -> dot.background(wc.Accent, CircleShape)
                    else -> dot.border(2.dp, wc.Accent, CircleShape)
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(CardCornerRadius),
            color = wc.SurfaceBg,
            border = BorderStroke(0.5.dp, wc.Border)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (timed || type != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (timed) {
                            Text(
                                text = Instant.ofEpochMilli(block.occurredAt)
                                    .atZone(ZoneId.systemDefault()).format(TimeFormat),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = wc.TextPrimary
                            )
                        }
                        if (type != null) {
                            Text(
                                text = blockTypeLabel(type),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = wc.TextMuted,
                                modifier = Modifier
                                    .background(wc.AccentLight, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                block.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(CardCornerRadius))
                            .clickable { onPhotoClick(uri) }
                    )
                }
                Text(
                    text = block.text,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = if (tomorrow) wc.TextMuted else wc.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun slotLabel(slot: TimeSlot): String = stringResource(
    when (slot) {
        TimeSlot.ALL_DAY -> R.string.timeline_slot_all_day
        TimeSlot.DAWN -> R.string.timeline_slot_dawn
        TimeSlot.MORNING -> R.string.timeline_slot_morning
        TimeSlot.AFTERNOON -> R.string.timeline_slot_afternoon
        TimeSlot.EVENING -> R.string.timeline_slot_evening
        TimeSlot.NIGHT -> R.string.timeline_slot_night
        TimeSlot.TOMORROW -> R.string.timeline_slot_tomorrow
    }
)
