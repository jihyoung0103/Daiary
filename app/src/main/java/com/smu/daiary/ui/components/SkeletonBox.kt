package com.smu.daiary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 로딩 중 표시하는 단색 placeholder.
 * shimmer 디자인 확정 전까지 임시로 단색 박스만 사용 — 이 자리는 이후 shimmer로 교체 예정.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    color: Color,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
    )
}
