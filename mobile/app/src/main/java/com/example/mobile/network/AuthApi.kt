package com.example.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AuthResponse(
    val token: String,
    val username: String,
    val balance: Int
)

object AuthApi {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun parseJsonBody(response: okhttp3.Response): Result<JSONObject> {
        val contentType = response.header("Content-Type") ?: ""
        val body = response.body?.string() ?: ""

        if (!contentType.contains("application/json")) {
            return Result.failure(
                IllegalStateException("Serveur injoignable (réponse non-JSON)")
            )
        }

        return Result.success(JSONObject(body))
    }

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
                    val parsed = parseJsonBody(response)
                    if (parsed.isFailure) return@withContext Result.failure(parsed.exceptionOrNull()!!)
                    val json = parsed.getOrThrow()

                    if (!response.isSuccessful) {
                        val msg = json.optString("message", "Identifiants invalides")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    val token = json.getString("token")
                    val balance = json.optInt("balance", 0)
                    ApiClient.token = token

                    Result.success(
                        AuthResponse(token = token, username = username, balance = balance)
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
                    val parsed = parseJsonBody(response)
                    if (parsed.isFailure) return@withContext Result.failure(parsed.exceptionOrNull()!!)
                    val json = parsed.getOrThrow()

                    if (!response.isSuccessful) {
                        val msg = json.optString("message", "Inscription échouée")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    val token = json.getString("token")
                    val balance = json.optInt("balance", 0)
                    ApiClient.token = token

                    Result.success(
                        AuthResponse(token = token, username = username, balance = balance)
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun claimDailyBonus(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val token = ApiClient.token
                    ?: return@withContext Result.failure(IllegalStateException("No token"))

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/claim-daily-bonus")
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(jsonMedia))
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    val parsed = parseJsonBody(response)
                    if (parsed.isFailure) return@withContext Result.failure(parsed.exceptionOrNull()!!)
                    val json = parsed.getOrThrow()

                    if (response.code == 409) {
                        return@withContext Result.failure(
                            AlreadyClaimedException(json.optString("message", "Déjà récupéré aujourd'hui"))
                        )
                    }

                    if (!response.isSuccessful) {
                        val msg = json.optString("message", "Erreur serveur")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    val balance = json.optInt("balance", 0)
                    Result.success(balance)
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }

    suspend fun fetchMe(): Result<MeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = ApiClient.token
                    ?: return@withContext Result.failure(IllegalStateException("No token"))

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/me")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("HTTP ${response.code}")
                        )
                    }

                    val parsed = parseJsonBody(response)
                    if (parsed.isFailure) return@withContext Result.failure(parsed.exceptionOrNull()!!)
                    val json = parsed.getOrThrow().getJSONObject("user")

                    Result.success(
                        MeResponse(
                            username = json.optString("username"),
                            email = json.optString("email"),
                            balance = json.optInt("balance", 0),
                            createdAt = json.optString("createdAt"),
                            hasClaimedDailyBonus = json.optBoolean("hasClaimedDailyBonus", false)
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

data class MeResponse(
    val username: String,
    val email: String,
    val balance: Int,
    val createdAt: String,
    val hasClaimedDailyBonus: Boolean
)

class AlreadyClaimedException(message: String) : Exception(message)
