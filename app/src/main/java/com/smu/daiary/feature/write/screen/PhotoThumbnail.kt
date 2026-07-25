package com.smu.daiary.feature.write.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.smu.daiary.ui.components.SkeletonBox
import com.smu.daiary.ui.theme.LocalDarkTheme

/**
 * 사진 그리드/썸네일/확대 다이얼로그에서 공용으로 쓰는 이미지 로더.
 * 로딩 중엔 SkeletonBox(shimmer 교체 예정), 실패 시 깨진 이미지 아이콘을 보여준다.
 *
 * 크기는 [modifier]가 결정한다(예: `.size(80.dp)`, `.fillMaxWidth()`) — 오버레이(스켈레톤)는
 * [Modifier.matchParentSize]로 그 크기를 그대로 따라가므로 호출부의 레이아웃을 바꾸지 않는다.
 */
@Composable
fun PhotoThumbnail(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop,
    errorIconSize: Dp = 24.dp
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    var imageState by remember(model) { mutableStateOf<AsyncImagePainter.State?>(null) }

    Box(contentAlignment = Alignment.Center) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(shape),
            onState = { imageState = it }
        )

        // 로딩 중 — shimmer 디자인 확정 전까지 SkeletonBox로 대체. 이 자리는 이후 shimmer로 교체 예정.
        if (imageState is AsyncImagePainter.State.Loading) {
            SkeletonBox(
                modifier = Modifier.matchParentSize(),
                color = wc.Border,
                shape = shape
            )
        }

        if (imageState is AsyncImagePainter.State.Error) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = wc.TextMuted,
                modifier = Modifier.size(errorIconSize)
            )
        }
    }
}
