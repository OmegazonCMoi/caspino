package com.example.mobile.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * État global simple pour partager la balance (pinos) entre les écrans
 * et quelques drapeaux liés aux récompenses.
 */
object BalanceState {
    val balance = mutableIntStateOf(0)

    // Observable pour que l'UI se mette à jour après un claim (évite plusieurs récup)
    var hasClaimedFreeTodayState by mutableStateOf(false)
        private set

    private var lastFreeClaimDate: LocalDate? = null

    fun addPinos(amount: Int) {
        balance.intValue += amount
    }

    fun hasClaimedFreeToday(): Boolean {
        val today = LocalDate.now()
        return hasClaimedFreeTodayState || lastFreeClaimDate == today
    }

    fun markFreeClaimedToday() {
        lastFreeClaimDate = LocalDate.now()
        hasClaimedFreeTodayState = true
    }

    fun resetDailyClaim() {
        lastFreeClaimDate = null
        hasClaimedFreeTodayState = false
    }
}

