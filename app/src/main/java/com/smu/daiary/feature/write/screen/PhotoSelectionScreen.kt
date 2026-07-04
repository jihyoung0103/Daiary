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
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSelectionScreen(
    viewModel: WriteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        topBar = {
            TopAppBar(
                title = { Text("사진 선택") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFAF5)
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.syncPhotoBlockSelection()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp)
            ) {
                Text("선택 완료")
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
                    Text("사진 선택 $selectedCount/${photos.size}")

                    Row {
                        TextButton(
                            onClick = { viewModel.setAllPhotosSelected(true) }
                        ) {
                            Text("전체 선택")
                        }

                        TextButton(
                            onClick = { viewModel.setAllPhotosSelected(false) }
                        ) {
                            Text("전체 해제")
                        }
                    }

                }
            }
            item {
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                ) {
                    Text("갤러리에서 사진 추가")
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
                            Color(0xFFE8F5E9)
                        else
                            MaterialTheme.colorScheme.surface
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