package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mobile.ui.screens.RouletteScreen
import com.example.mobile.ui.theme.MobileTheme

class RouletteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Temporairement : pas de connexion WebSocket, fonctionnement local uniquement
        setContent {
            MobileTheme {
                RouletteScreen(onBackClick = { finish() })
            }
        }
    }
}
