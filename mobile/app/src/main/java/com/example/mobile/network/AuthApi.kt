package com.example.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AuthResponse(
    val token: String,
    val username: String
)

object AuthApi {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/login")
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    if (!response.isSuccessful) {
                        val msg = json.optString("message", "Identifiants invalides")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    val token = json.getString("token")
                    ApiClient.token = token

                    Result.success(
                        AuthResponse(token = token, username = username)
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signup(
        username: String,
        email: String,
        password: String
    ): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("username", username)
                    put("email", email)
                    put("password", password)
                }

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/signup")
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    if (!response.isSuccessful) {
                        val msg = json.optString("message", "Inscription échouée")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    val token = json.getString("token")
                    ApiClient.token = token

                    Result.success(
                        AuthResponse(token = token, username = username)
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
