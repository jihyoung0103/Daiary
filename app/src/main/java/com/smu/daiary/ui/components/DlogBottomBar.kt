package com.smu.daiary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.R
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White

/** 하단 바로 오가는 최상위 화면. route는 MainActivity NavHost의 route와 같아야 한다 */
enum class MainTab(val route: String) {
    CALENDAR("main"),
    DIARIES("diary_list"),
    DASHBOARD("dashboard"),
    PROFILE("profile")
}

/**
 * 앱 공통 하단 바: 캘린더 · 일기 | 일기 쓰기 | 대시보드 · 프로필.
 * 시스템 내비게이션 바 자리까지 바 색으로 채우도록 그 높이를 [bottomInset]으로 받아 안쪽에 둔다.
 */
@Composable
fun DlogBottomBar(
    current: MainTab,
    onTabClick: (MainTab) -> Unit,
    onWriteClick: () -> Unit,
    bottomInset: Dp
) {
    val isDark = LocalDarkTheme.current
    val accent = if (isDark) SageForestDark else SageForest

    Surface(color = if (isDark) BackgroundDark else Ivory) {
        Column(modifier = Modifier.padding(bottom = bottomInset)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(if (isDark) BorderDark else Linen)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem(MainTab.CALENDAR, Icons.Outlined.CalendarMonth, R.string.nav_calendar, current, onTabClick)
                TabItem(MainTab.DIARIES, Icons.Outlined.AutoStories, R.string.nav_diaries, current, onTabClick)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = onWriteClick,
                        modifier = Modifier.size(48.dp),
                        containerColor = accent,
                        contentColor = White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.nav_write),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                TabItem(MainTab.DASHBOARD, Icons.Outlined.Insights, R.string.nav_dashboard, current, onTabClick)
                TabItem(MainTab.PROFILE, Icons.Outlined.Person, R.string.nav_profile, current, onTabClick)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    tab: MainTab,
    icon: ImageVector,
    labelRes: Int,
    current: MainTab,
    onTabClick: (MainTab) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val selected = tab == current
    // 비활성도 Stone 이상으로 둬 라벨이 배경 대비 4.5:1을 넘게 한다 (Silver는 너무 옅다)
    val color = when {
        selected -> if (isDark) SageForestDark else SageForest
        else -> if (isDark) TextSecondaryDark else Stone
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .selectable(selected = selected, role = Role.Tab, onClick = { onTabClick(tab) })
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
