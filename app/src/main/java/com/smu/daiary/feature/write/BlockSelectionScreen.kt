package com.smu.daiary.feature.write

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockSelectionScreen(
    viewModel: WriteViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingBlocks.collectAsStateWithLifecycle()
    val selectedCount = blocks.count { it.isSelected }

    Scaffold(
        modifier = modifier,
        containerColor = WriteColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "블록 선택",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = WriteColors.TextPrimary
                        )
                        Text(
                            text = if (isLoading) "오늘 데이터를 수집하는 중..." else "오늘 하루의 데이터를 선택해주세요",
                            fontSize = 12.sp,
                            color = WriteColors.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                            tint = WriteColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WriteColors.SurfaceBg)
            )
        },
        bottomBar = {
            Surface(color = WriteColors.SurfaceBg, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        viewModel.generateDraft()
                        onNext()
                    },
                    enabled = selectedCount > 0 && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WriteColors.Purple,
                        disabledContainerColor = WriteColors.Border
                    )
                ) {
                    Text(
                        text = if (selectedCount > 0) "초안 생성 (${selectedCount}개 선택됨)" else "초안 생성",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            // 수집 중 로딩 화면
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = WriteColors.Purple)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "날씨, 캘린더, 사진 데이터를\n불러오는 중입니다...",
                        fontSize = 14.sp,
                        color = WriteColors.TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(blocks) { block ->
                    BlockItem(
                        block = block,
                        onClick = { viewModel.toggleBlock(block.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockItem(block: ContentBlock, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (block.isSelected) WriteColors.PurpleLight else Color.White,
        border = if (block.isSelected)
            BorderStroke(1.5.dp, WriteColors.Purple)
        else
            BorderStroke(0.5.dp, WriteColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (block.isSelected) WriteColors.Purple else WriteColors.Bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = blockTypeIcon(block.type),
                    contentDescription = null,
                    tint = if (block.isSelected) Color.White else WriteColors.Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.type.label,
                    fontSize = 11.sp,
                    color = if (block.isSelected) WriteColors.Purple else WriteColors.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = block.content,
                    fontSize = 14.sp,
                    color = WriteColors.TextPrimary
                )
            }
            Checkbox(
                checked = block.isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = WriteColors.Purple,
                    uncheckedColor = WriteColors.Border
                )
            )
        }
    }
}

private fun blockTypeIcon(type: BlockType): ImageVector = when (type) {
    BlockType.PAYMENT  -> Icons.Outlined.CreditCard
    BlockType.PHOTO    -> Icons.Outlined.PhotoCamera
    BlockType.CALENDAR -> Icons.Outlined.CalendarMonth
    BlockType.HEALTH   -> Icons.Outlined.FitnessCenter
    BlockType.WEATHER  -> Icons.Outlined.WbSunny
}
