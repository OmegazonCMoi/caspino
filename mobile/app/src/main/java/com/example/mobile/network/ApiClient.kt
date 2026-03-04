package com.example.mobile.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "http://10.109.110.27:5500"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    var token: String? = null

    private lateinit var prefs: SharedPreferences
    private const val PREFS_NAME = "caspino_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        token = prefs.getString(KEY_TOKEN, null)
    }

    fun saveSession(token: String, username: String, email: String) {
        this.token = token
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getSavedUsername(): String? = if (::prefs.isInitialized) prefs.getString(KEY_USERNAME, null) else null
    fun getSavedEmail(): String? = if (::prefs.isInitialized) prefs.getString(KEY_EMAIL, null) else null
    fun getSavedToken(): String? = if (::prefs.isInitialized) prefs.getString(KEY_TOKEN, null) else null

    fun clearSession() {
        token = null
        if (::prefs.isInitialized) {
            prefs.edit().clear().apply()
        }
    }
}
