package com.example.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object PunchlineApi {

    suspend fun fetchPunchline(game: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/ai/punchline?game=$game")
                    .get()
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(IllegalStateException("Empty body"))

                    val json = JSONObject(body)
                    val punchline = json.optString("punchline", "Faites vos jeux !")

                    Result.success(punchline)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
