package com.example.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class DailyBonusResponse(
    val amount: Int,
    val balance: Int
)

object WalletApi {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun claimDailyBonus(): Result<DailyBonusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = ApiClient.token
                    ?: return@withContext Result.failure(IllegalStateException("Non connecté"))

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/bonus/daily")
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(jsonMedia))
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    if (!response.isSuccessful) {
                        val msg = json.optString("message", "Erreur")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    Result.success(
                        DailyBonusResponse(
                            amount = json.getInt("amount"),
                            balance = json.getInt("balance")
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
