package com.example.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.RouletteWheel
import com.example.mobile.ui.components.SpinCommand
import kotlin.random.Random

@Composable
fun RouletteScreen(
    onBackClick: () -> Unit
) {
    var spinCommandId by remember { mutableStateOf(0) }
    var currentSpinCommand by remember { mutableStateOf<SpinCommand?>(null) }

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
                isConnected = true,
                externalSpinCommand = currentSpinCommand,
                onExternalSpinConsumed = {
                    currentSpinCommand = null
                },
                onSpinRequest = { forcedNumber ->
                    // Générer un nombre aléatoire si aucun nombre n'est forcé
                    val targetNumber = forcedNumber ?: Random.nextInt(0, 37)
                    spinCommandId++
                    currentSpinCommand = SpinCommand(
                        id = spinCommandId,
                        number = targetNumber
                    )
                }
            )
        }
    }
}
