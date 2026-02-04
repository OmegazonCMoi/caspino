package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mobile.ui.screens.BlackjackScreen
import com.example.mobile.ui.theme.MobileTheme

class BlackjackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MobileTheme {
                BlackjackScreen(onBackClick = { finish() })
            }
        }
    }
}
