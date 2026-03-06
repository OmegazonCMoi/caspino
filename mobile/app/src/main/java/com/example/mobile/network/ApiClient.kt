package com.example.mobile.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "http://10.109.150.92:5500"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    val wsHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    var token: String? = null

    private lateinit var prefs: SharedPreferences
    private const val PREFS_NAME = "caspino_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_BALANCE = "balance"
    private const val KEY_LAST_PLAYED = "last_played"

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
    fun getSavedBalance(): Int = if (::prefs.isInitialized) prefs.getInt(KEY_BALANCE, 0) else 0

    fun saveBalance(balance: Int) {
        if (::prefs.isInitialized) {
            prefs.edit().putInt(KEY_BALANCE, balance).apply()
        }
    }

    fun saveLastPlayed(map: Map<String, String>) {
        if (!::prefs.isInitialized) return
        val json = JSONObject()
        map.forEach { (k, v) ->
            json.put(k, v)
        }
        prefs.edit().putString(KEY_LAST_PLAYED, json.toString()).apply()
    }

    fun getLastPlayed(): Map<String, String> {
        if (!::prefs.isInitialized) return emptyMap()
        val raw = prefs.getString(KEY_LAST_PLAYED, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val result = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optString(key, "")
                if (value.isNotEmpty()) {
                    result[key] = value
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun clearSession() {
        token = null
        if (::prefs.isInitialized) {
            prefs.edit().clear().apply()
        }
    }
}
