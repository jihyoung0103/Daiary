package com.smu.daiary.feature.write.screen

import com.smu.daiary.feature.write.WriteViewModel
import com.smu.daiary.feature.write.model.*


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.smu.daiary.R
import com.smu.daiary.ui.theme.LocalDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSelectionScreen(
    viewModel: WriteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val wc = if (isDark) WriteColorsDark else WriteColors

    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val selectedCount = photos.count { it.isSelected }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->

            uris.forEach { uri ->
                viewModel.addSelectablePhoto(
                    uri.toString()
                )
            }

            viewModel.syncPhotoBlockSelection()
        }

    Scaffold(
        containerColor = wc.Bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_photo_selection)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = wc.SurfaceBg
                ),
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = {
            Surface(color = wc.SurfaceBg, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        viewModel.syncPhotoBlockSelection()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = wc.Purple)
                ) {
                    Text(stringResource(R.string.btn_select_done))
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    Text(stringResource(R.string.photo_selection_count, selectedCount, photos.size), color = wc.TextPrimary)

                    Row {
                        TextButton(
                            onClick = { viewModel.setAllPhotosSelected(true) }
                        ) {
                            Text(stringResource(R.string.btn_select_all), color = wc.Purple)
                        }

                        TextButton(
                            onClick = { viewModel.setAllPhotosSelected(false) }
                        ) {
                            Text(stringResource(R.string.btn_deselect_all), color = wc.Purple)
                        }
                    }

                }
            }
            item {
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = wc.Purple)
                ) {
                    Text(stringResource(R.string.btn_add_from_gallery))
                }
            }

            items(photos) { photo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.togglePhoto(photo.uri)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (photo.isSelected)
                            wc.PurpleLight
                        else
                            wc.Bg
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "사진",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Checkbox(
                            checked = photo.isSelected,
                            onCheckedChange = {
                                viewModel.togglePhoto(photo.uri)
                            }
                        )
                    }
                }
            }
        }
    }
}