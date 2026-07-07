package com.smu.daiary.feature.write.screen

import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.feature.write.model.*

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Umbrella
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.smu.daiary.R
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.util.DiaryDateUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val isGeneratingQuestions by viewModel.isGeneratingQuestions.collectAsStateWithLifecycle()
    val contextQuestions by viewModel.contextQuestions.collectAsStateWithLifecycle()

    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    // 카테고리 블록 펼침 상태
    var calendarExpanded by remember { mutableStateOf(false) }
    var upcomingExpanded by remember { mutableStateOf(false) }
    var photoExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addSelectablePhoto(uri.toString()) }
        viewModel.syncPhotoBlockSelection()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val isLateNight = remember { DiaryDateUtil.isLateNight() }
    val writingDate by viewModel.writingDate.collectAsStateWithLifecycle()

    var hasNavigatedToQnA by remember { mutableStateOf(false) }
    LaunchedEffect(contextQuestions) {
        if (contextQuestions != null && !hasNavigatedToQnA) {
            hasNavigatedToQnA = true
            onNext()
        }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wc.SurfaceBg),
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = {
            Surface(color = wc.SurfaceBg, shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.prepareGeneration() },
                    enabled = !isLoading && !isGeneratingQuestions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = wc.Purple,
                        disabledContainerColor = wc.Border
                    )
                ) {
                    if (isGeneratingQuestions) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Text(text = stringResource(R.string.btn_select_done), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = wc.Purple)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.loading_data), fontSize = 14.sp, color = wc.TextMuted, textAlign = TextAlign.Center)
                }
            }
        } else if (blocks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateBanner(wc = wc, date = writingDate, isLateNight = isLateNight)
                    Text(text = stringResource(R.string.block_empty_message), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = wc.TextPrimary, textAlign = TextAlign.Center)
                    TextButton(onClick = onRetry) {
                        Text(text = stringResource(R.string.btn_retry), color = wc.Purple, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp, end = 24.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DateBanner(wc = wc, date = writingDate, isLateNight = isLateNight)
                }

                items(blocks) { block ->
                    when (block.type) {

                        BlockType.CALENDAR -> {
                            val selectedCount = calendarEvents.count { it.isSelected }
                            val hasEvents = calendarEvents.isNotEmpty()
                            CategoryBlockItem(
                                block = block,
                                enabled = !isGeneratingQuestions,
                                isExpanded = calendarExpanded,
                                displayText = if (!hasEvents) block.content
                                              else if (selectedCount == 0) "선택된 일정 없음"
                                              else "일정 ${selectedCount}개 선택됨",
                                isExpandable = hasEvents,
                                onClick = { if (hasEvents) calendarExpanded = !calendarExpanded }
                            )
                            if (calendarExpanded && hasEvents) {
                                Spacer(Modifier.height(4.dp))
                                CalendarDetailSelector(
                                    events = calendarEvents,
                                    onToggle = { id -> viewModel.toggleCalendarEvent(id) }
                                )
                            }
                        }

                        BlockType.CALENDAR_UPCOMING -> {
                            val selectedCount = upcomingEvents.count { it.isSelected }
                            val hasEvents = upcomingEvents.isNotEmpty()
                            CategoryBlockItem(
                                block = block,
                                enabled = !isGeneratingQuestions,
                                isExpanded = upcomingExpanded,
                                displayText = if (!hasEvents) block.content
                                              else if (selectedCount == 0) "선택된 일정 없음"
                                              else "향후 일정 ${selectedCount}개 선택됨",
                                isExpandable = hasEvents,
                                onClick = { if (hasEvents) upcomingExpanded = !upcomingExpanded }
                            )
                            if (upcomingExpanded && hasEvents) {
                                Spacer(Modifier.height(4.dp))
                                CalendarDetailSelector(
                                    events = upcomingEvents,
                                    onToggle = { id -> viewModel.toggleUpcomingEvent(id) }
                                )
                            }
                        }

                        BlockType.PHOTO -> {
                            val selectedCount = photos.count { it.isSelected }
                            CategoryBlockItem(
                                block = block,
                                enabled = !isGeneratingQuestions,
                                isExpanded = photoExpanded,
                                displayText = if (selectedCount == 0) "선택된 사진 없음"
                                              else "사진 ${selectedCount}장 선택됨",
                                isExpandable = true,
                                onClick = { photoExpanded = !photoExpanded }
                            )
                            if (photoExpanded) {
                                Spacer(Modifier.height(4.dp))
                                PhotoDetailSelector(
                                    photos = photos,
                                    onToggle = { uri -> viewModel.togglePhoto(uri) },
                                    onRemove = { uri -> viewModel.removeSelectablePhoto(uri) },
                                    onAddFromGallery = { galleryLauncher.launch("image/*") }
                                )
                            }
                        }

                        BlockType.PAYMENT -> {
                            val selectedCount = payments.count { it.isSelected }
                            val hasPayments = payments.isNotEmpty()
                            CategoryBlockItem(
                                block = block,
                                enabled = !isGeneratingQuestions,
                                isExpanded = paymentExpanded,
                                displayText = if (!hasPayments) block.content
                                              else if (selectedCount == 0) "선택된 결제 없음"
                                              else "결제 ${selectedCount}건 선택됨",
                                isExpandable = hasPayments,
                                onClick = { if (hasPayments) paymentExpanded = !paymentExpanded }
                            )
                            if (paymentExpanded && hasPayments) {
                                Spacer(Modifier.height(4.dp))
                                PaymentDetailSelector(
                                    payments = payments,
                                    onToggle = { id -> viewModel.togglePayment(id) }
                                )
                            }
                        }

                        else -> {
                            // WEATHER, WEATHER_TOMORROW, PHOTO_LOCATION, HEALTH — 단일 블록, 체크박스 유지
                            SingleBlockItem(
                                block = block,
                                enabled = !isGeneratingQuestions,
                                onClick = { viewModel.toggleBlock(block.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 카테고리 헤더 블록 (▼/▲ 아이콘)
// ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryBlockItem(
    block: ContentBlock,
    enabled: Boolean = true,
    isExpanded: Boolean = false,
    isExpandable: Boolean = true,
    displayText: String,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (block.isSelected) wc.PurpleLight else wc.Bg,
        border = if (block.isSelected) BorderStroke(1.5.dp, wc.Purple) else BorderStroke(0.5.dp, wc.Border),
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
                modifier = Modifier.size(40.dp).clip(CircleShape)
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
                    text = blockTypeLabel(block.type),
                    fontSize = 11.sp,
                    color = if (block.isSelected) wc.Purple else wc.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(text = displayText, fontSize = 14.sp, color = wc.TextPrimary)
            }
            if (isExpandable) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = if (block.isSelected) wc.Purple else wc.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 단일 블록 (날씨, 건강 — 체크박스)
// ─────────────────────────────────────────────────────────────

@Composable
private fun SingleBlockItem(
    block: ContentBlock,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (block.isSelected) wc.PurpleLight else wc.Bg,
        border = if (block.isSelected) BorderStroke(1.5.dp, wc.Purple) else BorderStroke(0.5.dp, wc.Border),
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
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (block.isSelected) wc.Purple else wc.SurfaceBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (block.type == BlockType.WEATHER || block.type == BlockType.WEATHER_TOMORROW) weatherIconFor(block.content) else blockTypeIcon(block.type),
                    contentDescription = null,
                    tint = if (block.isSelected) Color.White else wc.Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blockTypeLabel(block.type),
                    fontSize = 11.sp,
                    color = if (block.isSelected) wc.Purple else wc.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(text = block.content, fontSize = 14.sp, color = wc.TextPrimary)
            }
            Checkbox(
                checked = block.isSelected,
                onCheckedChange = { if (enabled) onClick() },
                colors = CheckboxDefaults.colors(checkedColor = wc.Purple, uncheckedColor = wc.Border)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 서브아이템 공통 블록 컨테이너
// ─────────────────────────────────────────────────────────────

@Composable
private fun SubItemBlock(
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    val baseModifier = Modifier
        .fillMaxWidth()
        .padding(start = 20.dp)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) wc.PurpleLight else wc.SurfaceBg,
        border = if (isSelected) BorderStroke(1.dp, wc.Purple.copy(alpha = 0.5f)) else BorderStroke(0.5.dp, wc.Border),
        modifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 캘린더 서브아이템
// ─────────────────────────────────────────────────────────────

@Composable
private fun CalendarDetailSelector(
    events: List<CalendarSelectableItem>,
    onToggle: (Int) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        events.forEach { event ->
            SubItemBlock(
                isSelected = event.isSelected,
                onClick = { onToggle(event.id) }
            ) {
                Text(
                    text = event.displayText,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = wc.TextPrimary
                )
                Checkbox(
                    checked = event.isSelected,
                    onCheckedChange = { onToggle(event.id) },
                    colors = CheckboxDefaults.colors(checkedColor = wc.Purple, uncheckedColor = wc.Border)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 사진 서브아이템
// ─────────────────────────────────────────────────────────────

@Composable
private fun PhotoDetailSelector(
    photos: List<PhotoSelectableItem>,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddFromGallery: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 첫 번째 서브블록: 사진 추가
        SubItemBlock(isSelected = false, onClick = onAddFromGallery) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "사진 추가",
                tint = wc.Purple,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "갤러리에서 사진 추가",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = wc.Purple,
                fontWeight = FontWeight.Medium
            )
        }

        // 사진 목록
        if (photos.isEmpty()) {
            SubItemBlock(isSelected = false) {
                Text(
                    text = "오늘 찍은 사진이 없습니다",
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = wc.TextMuted
                )
            }
        } else {
            photos.forEach { photo ->
                SubItemBlock(
                    isSelected = photo.isSelected,
                    onClick = { onToggle(photo.uri) }
                ) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = "사진",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.weight(1f))
                    Checkbox(
                        checked = photo.isSelected,
                        onCheckedChange = { onToggle(photo.uri) },
                        colors = CheckboxDefaults.colors(checkedColor = wc.Purple, uncheckedColor = wc.Border)
                    )
                    IconButton(
                        onClick = { onRemove(photo.uri) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "사진 삭제",
                            tint = wc.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 결제 서브아이템
// ─────────────────────────────────────────────────────────────

@Composable
private fun PaymentDetailSelector(
    payments: List<PaymentSelectableItem>,
    onToggle: (Int) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors
    if (payments.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        payments.forEach { payment ->
            SubItemBlock(
                isSelected = payment.isSelected,
                onClick = { onToggle(payment.id) }
            ) {
                Text(
                    text = payment.displayText,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = wc.TextPrimary
                )
                Checkbox(
                    checked = payment.isSelected,
                    onCheckedChange = { onToggle(payment.id) },
                    colors = CheckboxDefaults.colors(checkedColor = wc.Purple, uncheckedColor = wc.Border)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 야간 배너
// ─────────────────────────────────────────────────────────────

@Composable
private fun DateBanner(wc: WriteColorScheme, date: LocalDate, isLateNight: Boolean) {
    val today = remember { DiaryDateUtil.diaryDate() }
    val isPastDate = date.isBefore(today)
    if (!isPastDate && !isLateNight) return

    val formatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
    val dateText = date.format(formatter)
    val subtitle = if (isPastDate) "${dateText}의 일기를 작성하고 있어요"
                   else "자정이 넘었지만 오전 4시까지는 어제 일기로 저장돼요"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = wc.PurpleLight,
        border = BorderStroke(1.5.dp, wc.Purple),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(wc.Purple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Nightlight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$dateText 일기를 작성하고 있어요", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = wc.Purple)
                Text(text = subtitle, fontSize = 11.sp, color = wc.Purple.copy(alpha = 0.7f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 공통 유틸
// ─────────────────────────────────────────────────────────────

private fun blockTypeIcon(type: BlockType): ImageVector = when (type) {
    BlockType.PAYMENT           -> Icons.Outlined.CreditCard
    BlockType.PHOTO             -> Icons.Outlined.PhotoCamera
    BlockType.CALENDAR          -> Icons.Outlined.CalendarMonth
    BlockType.CALENDAR_UPCOMING -> Icons.Outlined.DateRange
    BlockType.HEALTH            -> Icons.Outlined.FitnessCenter
    BlockType.WEATHER           -> Icons.Outlined.WbSunny
    BlockType.WEATHER_TOMORROW  -> Icons.Outlined.WbSunny
    BlockType.PHOTO_LOCATION    -> Icons.Outlined.LocationOn
}

/** 날씨 블록의 content(예: "맑음 22°C · 습도 60%")에서 날씨 종류를 읽어 아이콘 매핑. 매칭 실패 시 WbSunny로 fallback */
private fun weatherIconFor(content: String): ImageVector {
    val weatherIconMap = mapOf(
        "맑음" to Icons.Outlined.WbSunny,
        "흐림" to Icons.Outlined.Cloud,
        "비" to Icons.Outlined.Umbrella,
        "눈" to Icons.Outlined.AcUnit,
        "바람" to Icons.Outlined.Air
    )
    return weatherIconMap.entries.firstOrNull { content.startsWith(it.key) }?.value
        ?: Icons.Outlined.WbSunny
}

@Composable
private fun blockTypeLabel(type: BlockType): String = when (type) {
    BlockType.PAYMENT           -> stringResource(R.string.block_type_payment)
    BlockType.PHOTO             -> stringResource(R.string.block_type_photo)
    BlockType.CALENDAR          -> stringResource(R.string.block_type_calendar)
    BlockType.CALENDAR_UPCOMING -> stringResource(R.string.block_type_calendar_upcoming)
    BlockType.HEALTH            -> stringResource(R.string.block_type_health)
    BlockType.WEATHER           -> stringResource(R.string.block_type_weather)
    BlockType.WEATHER_TOMORROW  -> stringResource(R.string.block_type_weather_tomorrow)
    BlockType.PHOTO_LOCATION    -> stringResource(R.string.block_type_photo_location)
}
