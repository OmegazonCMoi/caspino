package com.example.mobile.ui.screens

import androidx.compose.runtime.mutableIntStateOf

/**
 * État global simple pour partager la balance (pinos) entre les écrans.
 */
object BalanceState {
    val balance = mutableIntStateOf(1000)

    fun addPinos(amount: Int) {
        balance.intValue += amount
    }
}

