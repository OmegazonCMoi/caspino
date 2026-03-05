package com.example.mobile.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mobile.network.ApiClient
import com.example.mobile.network.AuthApi
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

    var isLoading by mutableStateOf(false)
        private set

    fun restoreSessionLocal() {
        val savedToken = ApiClient.getSavedToken()
        val savedUsername = ApiClient.getSavedUsername()
        if (savedToken != null && savedUsername != null) {
            ApiClient.token = savedToken
            username = savedUsername
            email = ApiClient.getSavedEmail() ?: ""
            BalanceState.balance.intValue = ApiClient.getSavedBalance()
            isLoggedIn = true
        }
    }

    suspend fun refreshFromServer() {
        if (!isLoggedIn) return
        AuthApi.fetchMe().onSuccess { me ->
            username = me.username
            email = me.email
            BalanceState.balance.intValue = me.balance
            ApiClient.saveBalance(me.balance)
            if (me.hasClaimedDailyBonus) {
                BalanceState.markFreeClaimedToday()
            }
        }.onFailure {
            logout()
        }
    }

    suspend fun login(inputUsername: String, inputPassword: String): Result<Unit> {
        isLoading = true
        val result = AuthApi.login(inputUsername, inputPassword)
        isLoading = false

        return result.map { auth ->
            username = auth.username
            email = ""
            if (memberSince == null) memberSince = LocalDate.now()
            isLoggedIn = true
            BalanceState.balance.intValue = auth.balance
            ApiClient.saveSession(auth.token, auth.username, "")
            ApiClient.saveBalance(auth.balance)
        }
    }

    suspend fun register(
        inputUsername: String,
        inputEmail: String,
        inputPassword: String,
        inputConfirmPassword: String
    ): Result<Unit> {
        if (inputUsername.length < 3)
            return Result.failure(IllegalStateException("Le pseudo doit faire 3 caractères minimum."))
        if (!inputEmail.contains("@"))
            return Result.failure(IllegalStateException("Email invalide."))
        if (inputPassword.length < 6)
            return Result.failure(IllegalStateException("Le mot de passe doit faire 6 caractères minimum."))
        if (inputPassword != inputConfirmPassword)
            return Result.failure(IllegalStateException("Les mots de passe ne correspondent pas."))

        isLoading = true
        val result = AuthApi.signup(inputUsername, inputEmail, inputPassword)
        isLoading = false

        return result.map { auth ->
            username = auth.username
            email = inputEmail
            memberSince = LocalDate.now()
            isLoggedIn = true
            BalanceState.balance.intValue = auth.balance
            ApiClient.saveSession(auth.token, auth.username, inputEmail)
            ApiClient.saveBalance(auth.balance)
        }
    }

    fun logout() {
        isLoggedIn = false
        username = ""
        email = ""
        BalanceState.balance.intValue = 0
        ApiClient.saveBalance(0)
        ApiClient.clearSession()
    }
}
