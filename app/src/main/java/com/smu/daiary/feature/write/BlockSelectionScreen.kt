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
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smu.daiary.R
import com.smu.daiary.ui.theme.DaiaryTheme
import com.smu.daiary.ui.theme.LocalDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockSelectionScreen(
    viewModel: WriteViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingBlocks.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generateError by viewModel.generateError.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val selectedCount = blocks.count { it.isSelected }

    val snackbarHostState = remember { SnackbarHostState() }

    // AI 초안 생성 완료 → 다음 화면 자동 전환
    LaunchedEffect(draft) {
        if (draft != null) onNext()
    }

    // 생성 오류 → 스낵바 표시
    LaunchedEffect(generateError) {
        val error = generateError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearGenerateError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = wc.Bg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = Color(0xFFB00020), contentColor = Color.White)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_block_selection),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg)
            )
        },
        bottomBar = {
            Surface(color = wc.SurfaceBg, shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.generateDraft() },
                    enabled = selectedCount > 0 && !isLoading && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = wc.Purple,
                        disabledContainerColor = wc.Border
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.btn_select_done),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = wc.Purple)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_data),
                        fontSize = 14.sp,
                        color = wc.TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (blocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.block_empty_message),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = wc.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(
                            text = stringResource(R.string.btn_retry),
                            color = wc.Purple,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
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
                        enabled = !isGenerating,
                        onClick = { viewModel.toggleBlock(block.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockItem(block: ContentBlock, enabled: Boolean = true, onClick: () -> Unit) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (block.isSelected) wc.PurpleLight else wc.Bg,
        border = if (block.isSelected)
            BorderStroke(1.5.dp, wc.Purple)
        else
            BorderStroke(0.5.dp, wc.Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
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
                    .background(if (block.isSelected) wc.Purple else wc.SurfaceBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = blockTypeIcon(block.type),
                    contentDescription = null,
                    tint = if (block.isSelected) Color.White else wc.Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (block.type) {
                        BlockType.PAYMENT  -> stringResource(R.string.block_type_payment)
                        BlockType.PHOTO    -> stringResource(R.string.block_type_photo)
                        BlockType.CALENDAR -> stringResource(R.string.block_type_calendar)
                        BlockType.HEALTH   -> stringResource(R.string.block_type_health)
                        BlockType.WEATHER  -> stringResource(R.string.block_type_weather)
                    },
                    fontSize = 11.sp,
                    color = if (block.isSelected) wc.Purple else wc.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = block.content,
                    fontSize = 14.sp,
                    color = wc.TextPrimary
                )
            }
            Checkbox(
                checked = block.isSelected,
                onCheckedChange = { if (enabled) onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = wc.Purple,
                    uncheckedColor = wc.Border
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

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockSelectionScreenPreview() {
    val sampleBlocks = listOf(
        ContentBlock("1", BlockType.WEATHER,  "맑음, 23°C", isSelected = true),
        ContentBlock("2", BlockType.CALENDAR, "오후 3시 팀 미팅", isSelected = false),
        ContentBlock("3", BlockType.PAYMENT,  "스타벅스 4,500원", isSelected = true),
        ContentBlock("4", BlockType.HEALTH,   "걸음 수: 8,342보", isSelected = false),
        ContentBlock("5", BlockType.PHOTO,    "사진 3장", isSelected = false),
    )
    DaiaryTheme {
        val wc = WriteColors
        Scaffold(
            containerColor = wc.Bg,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(R.string.screen_block_selection), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = wc.TextPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = wc.TextPrimary)
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
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = wc.Purple)
                    ) {
                        Text(text = stringResource(R.string.btn_select_done), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp, end = 24.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sampleBlocks) { block -> BlockItem(block = block, onClick = {}) }
            }
        }
    }
}
