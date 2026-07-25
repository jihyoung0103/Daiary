package com.smu.daiary.feature.write.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smu.daiary.feature.write.model.DiaryBodyBlock
import com.smu.daiary.ui.theme.LocalDarkTheme

/**
 * 일기 본문을 블록 단위로 렌더링한다. 사진 블록은 사진과 문단이 한 덩어리로 붙는다.
 * blocks가 비면(블록화 이전에 저장된 일기) fallbackText를 한 문단으로 보여준다.
 */
@Composable
fun DiaryBodyBlocks(
    blocks: List<DiaryBodyBlock>,
    fallbackText: String,
    modifier: Modifier = Modifier,
    onPhotoClick: (String) -> Unit = {}
) {
    val wc = if (LocalDarkTheme.current) WriteColorsDark else WriteColors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = wc.SurfaceBg,
        border = BorderStroke(0.5.dp, wc.Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (blocks.isEmpty()) {
                BodyText(fallbackText, wc.TextPrimary)
                return@Column
            }
            blocks.forEach { block ->
                block.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(uri) }
                    )
                }
                BodyText(block.text, wc.TextPrimary)
            }
        }
    }
}

@Composable
private fun BodyText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 15.sp,
        lineHeight = 24.sp,
        color = color
    )
}
