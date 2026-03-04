package com.example.mobile.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import java.time.LocalDate

/**
 * État global simple pour partager la balance (pinos) entre les écrans
 * et quelques drapeaux liés aux récompenses.
 */
object BalanceState {
    val balance = mutableIntStateOf(0)

    // Date du dernier claim du pack gratuit de pinos (en mémoire, par session)
    private var lastFreeClaimDate: LocalDate? = null

    fun addPinos(amount: Int) {
        balance.intValue += amount
    }

    fun hasClaimedFreeToday(): Boolean {
        val today = LocalDate.now()
        return lastFreeClaimDate == today
    }

    fun markFreeClaimedToday() {
        lastFreeClaimDate = LocalDate.now()
    }
}

