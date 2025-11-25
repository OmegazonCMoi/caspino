package com.example.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.RouletteWheel
import com.example.mobile.ui.theme.DarkTextPrimary

@Composable
fun RouletteScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Roulette",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RouletteWheel(
                onNumberSelected = { number ->
                    // Gérer le nombre sélectionné
                }
            )
        }
    }
}

