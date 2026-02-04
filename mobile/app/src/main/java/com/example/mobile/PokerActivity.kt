package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mobile.ui.screens.PokerScreen
import com.example.mobile.ui.theme.MobileTheme

class PokerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MobileTheme {
                PokerScreen(onBackClick = { finish() })
            }
        }
    }
}
