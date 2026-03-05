package com.example.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object GamesApi {

    suspend fun fetchLastPlayed(): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = ApiClient.token
                    ?: return@withContext Result.success(emptyMap())

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/games/last-played")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.success(emptyMap())
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.success(emptyMap())

                    val json = JSONObject(body)
                    val lastPlayedJson = json.optJSONObject("lastPlayed") ?: JSONObject()

                    val map = mutableMapOf<String, String>()
                    lastPlayedJson.keys().forEach { key ->
                        map[key] = lastPlayedJson.optString(key, "")
                    }
                    Result.success(map)
                }
            } catch (e: Exception) {
                Result.success(emptyMap())
            }
        }
    }
}
