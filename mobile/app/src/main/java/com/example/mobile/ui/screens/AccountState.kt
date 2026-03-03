package com.example.mobile.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

object AccountState {
    var isLoggedIn by mutableStateOf(false)
        private set

    var username by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var memberSince by mutableStateOf<LocalDate?>(null)
        private set

    fun login(inputEmail: String, inputPassword: String): Boolean {
        if (inputEmail.isBlank() || inputPassword.length < 4) return false
        if (!inputEmail.contains("@")) return false

        email = inputEmail.trim()
        username = inputEmail.substringBefore("@").ifBlank { "Joueur" }
        if (memberSince == null) memberSince = LocalDate.now()
        isLoggedIn = true
        return true
    }

    fun register(
        inputUsername: String,
        inputEmail: String,
        inputPassword: String,
        inputConfirmPassword: String
    ): Boolean {
        if (inputUsername.length < 3) return false
        if (!inputEmail.contains("@")) return false
        if (inputPassword.length < 6) return false
        if (inputPassword != inputConfirmPassword) return false

        username = inputUsername.trim()
        email = inputEmail.trim()
        memberSince = LocalDate.now()
        isLoggedIn = true
        return true
    }

    fun logout() {
        isLoggedIn = false
    }
}
