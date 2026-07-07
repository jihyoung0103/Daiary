package com.smu.daiary.feature.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.smu.daiary.R
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.White


data class MbtiOption(
    val type: String,
    val keywords: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, onConfirm: () -> Unit = {}) {

    val context = LocalContext.current

    val prefs =
        context.getSharedPreferences(
            "user_settings",
            Context.MODE_PRIVATE
        )

    val mbtiList = listOf(
        MbtiOption("ISTJ", stringResource(R.string.mbti_istj)),
        MbtiOption("ISFJ", stringResource(R.string.mbti_isfj)),
        MbtiOption("INFJ", stringResource(R.string.mbti_infj)),
        MbtiOption("INTJ", stringResource(R.string.mbti_intj)),

        MbtiOption("ISTP", stringResource(R.string.mbti_istp)),
        MbtiOption("ISFP", stringResource(R.string.mbti_isfp)),
        MbtiOption("INFP", stringResource(R.string.mbti_infp)),
        MbtiOption("INTP", stringResource(R.string.mbti_intp)),

        MbtiOption("ESTP", stringResource(R.string.mbti_estp)),
        MbtiOption("ESFP", stringResource(R.string.mbti_esfp)),
        MbtiOption("ENFP", stringResource(R.string.mbti_enfp)),
        MbtiOption("ENTP", stringResource(R.string.mbti_entp)),

        MbtiOption("ESTJ", stringResource(R.string.mbti_estj)),
        MbtiOption("ESFJ", stringResource(R.string.mbti_esfj)),
        MbtiOption("ENFJ", stringResource(R.string.mbti_enfj)),
        MbtiOption("ENTJ", stringResource(R.string.mbti_entj))
    )

    val isDark = LocalDarkTheme.current
    val bgColor = if (isDark) BackgroundDark else Ivory
    val cardBg = if (isDark) SurfaceDark else White
    val textColor = if (isDark) TextPrimaryDark else Ink
    val accentColor = if (isDark) SageForestDark else SageForest
    val borderColor = if (isDark) BorderDark else Linen

    var selected by remember {
        mutableStateOf<String?>(
            prefs.getString("mbti", "INFP")
        )
    }
    var showSavedMessage by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_mbti_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(bgColor)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                LazyVerticalGrid(columns = GridCells.Fixed(4)) {
                    items(mbtiList) { mbti ->
                        Card(
                            onClick = {
                                selected = if (selected == mbti.type) null else mbti.type
                            },
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(
                                    width = if (selected == mbti.type) 2.dp else 1.dp,
                                    color = if (selected == mbti.type) accentColor else borderColor,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = cardBg
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        ){
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = mbti.type,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (selected == mbti.type) accentColor else textColor
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = mbti.keywords,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))

                if (selected != null) {
                    Text(
                        text = stringResource(R.string.my_mbti, selected ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        prefs.edit().apply {
                            if (selected == null) remove("mbti") else putString("mbti", selected)
                            apply()
                        }
                        showSavedMessage = true
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.btn_save_mbti))
                }
            }

            if (showSavedMessage) {
                AlertDialog(
                    onDismissRequest = { showSavedMessage = false },
                    title = { Text(stringResource(R.string.mbti_saved_title), color = textColor) },
                    text = { Text(stringResource(R.string.mbti_saved_message), color = textColor) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSavedMessage = false
                                onConfirm()
                            }
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    containerColor = cardBg
                )
            }
        }
    }
}
