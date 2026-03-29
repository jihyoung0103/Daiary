package com.smu.daiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.smu.daiary.ui.theme.DaiaryTheme

/**
 * 앱의 진입 화면(Activity). 시스템이 앱을 실행할 때 이 클래스가 먼저 열립니다.
 */
class MainActivity : ComponentActivity() {
    /**
     * Activity가 생성될 때 한 번 호출됩니다. Compose UI를 설정하고 [MainCalendarScreen]을 표시합니다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaiaryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainCalendarScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
