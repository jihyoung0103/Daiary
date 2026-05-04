package com.smu.daiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smu.daiary.auth.AuthState
import com.smu.daiary.auth.AuthViewModel
import com.smu.daiary.auth.LoginScreen
import com.smu.daiary.draft.BlockSelectionScreen
import com.smu.daiary.draft.DiaryDraftViewModel
import com.smu.daiary.draft.DiaryEditScreen
import com.smu.daiary.draft.DraftPreviewScreen
import com.smu.daiary.ui.theme.DaiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaiaryTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (authState) {
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF1EFE8)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF533AB7))
                            }
                        }

                        is AuthState.Authenticated -> {
                            val userId = (authState as AuthState.Authenticated).user.uid
                            val navController = rememberNavController()
                            val diaryDraftViewModel: DiaryDraftViewModel = viewModel()

                            NavHost(
                                navController = navController,
                                startDestination = "main"
                            ) {
                                composable("main") {
                                    MainCalendarScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        onLogout = { authViewModel.logout() },
                                        onStartDiary = {
                                            navController.navigate("block_selection")
                                        }
                                    )
                                }
                                composable("block_selection") {
                                    BlockSelectionScreen(
                                        viewModel = diaryDraftViewModel,
                                        onNext = { navController.navigate("draft_preview") },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("draft_preview") {
                                    DraftPreviewScreen(
                                        viewModel = diaryDraftViewModel,
                                        userId = userId,
                                        onEdit = { navController.navigate("diary_edit") },
                                        onSaved = {
                                            diaryDraftViewModel.resetDraft()
                                            navController.popBackStack(
                                                route = "main",
                                                inclusive = false
                                            )
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("diary_edit") {
                                    DiaryEditScreen(
                                        viewModel = diaryDraftViewModel,
                                        onDone = { navController.popBackStack() },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        is AuthState.Unauthenticated,
                        is AuthState.Error,
                        is AuthState.LoginSuccess,
                        is AuthState.SignUpSuccess -> {
                            LoginScreen(
                                authViewModel = authViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
