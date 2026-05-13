package com.smu.daiary.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smu.daiary.R
import com.smu.daiary.ui.theme.BackgroundDark
import com.smu.daiary.ui.theme.BorderDark
import com.smu.daiary.ui.theme.Ink
import com.smu.daiary.ui.theme.Ivory
import com.smu.daiary.ui.theme.Linen
import com.smu.daiary.ui.theme.LocalDarkTheme
import com.smu.daiary.ui.theme.SageForest
import com.smu.daiary.ui.theme.SageForestDark
import com.smu.daiary.ui.theme.Stone
import com.smu.daiary.ui.theme.SurfaceDark
import com.smu.daiary.ui.theme.TextPrimaryDark
import com.smu.daiary.ui.theme.TextSecondaryDark
import com.smu.daiary.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val bg = if (isDark) BackgroundDark else Ivory
    val cardBg = if (isDark) SurfaceDark else White
    val textPrimary = if (isDark) TextPrimaryDark else Ink
    val textMuted = if (isDark) TextSecondaryDark else Stone
    val borderColor = if (isDark) BorderDark else Linen
    val accentColor = if (isDark) SageForestDark else SageForest

    Scaffold(
        modifier = modifier,
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privacy_policy),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.privacy_effective_date),
                fontSize = 12.sp,
                color = textMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            LegalCard(cardBg, borderColor) {
                Text(
                    text = stringResource(R.string.privacy_intro),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = textPrimary
                )
            }

            val sections = listOf(
                R.string.privacy_s1_title to R.string.privacy_s1_body,
                R.string.privacy_s2_title to R.string.privacy_s2_body,
                R.string.privacy_s3_title to R.string.privacy_s3_body,
                R.string.privacy_s4_title to R.string.privacy_s4_body,
                R.string.privacy_s5_title to R.string.privacy_s5_body,
                R.string.privacy_s6_title to R.string.privacy_s6_body,
            )

            sections.forEach { (titleRes, bodyRes) ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(titleRes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                LegalCard(cardBg, borderColor) {
                    Text(
                        text = stringResource(bodyRes),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun LegalCard(
    cardBg: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
