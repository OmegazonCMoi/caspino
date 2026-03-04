package com.example.mobile.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

data class SlotSpinResponse(
    val result: List<Int>,
    val gain: Int,
    val balance: Int
)

object SlotsApi {
    private const val WS_URL = "ws://10.109.150.92:5700"

    private var ws: WebSocket? = null
    private var pendingOpen: CompletableDeferred<Boolean>? = null
    private var pendingSpin: CompletableDeferred<Result<SlotSpinResponse>>? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            pendingOpen?.complete(true)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "BET_RESULT" -> {
                    val payload = json.getJSONObject("payload")
                    val resultArray = payload.getJSONArray("slotResult")
                    val reels = mutableListOf<Int>()
                    for (reelIndex in 0 until resultArray.length()) {
                        reels.add(resultArray.getInt(reelIndex))
                    }
                    pendingSpin?.complete(
                        Result.success(
                            SlotSpinResponse(
                                result = reels,
                                gain = payload.getInt("gains"),
                                balance = payload.getInt("balance")
                            )
                        )
                    )
                }
                "ERROR" -> {
                    val msg = json.optJSONObject("payload")?.optString("message") ?: "Erreur serveur"
                    pendingSpin?.complete(Result.failure(IllegalStateException(msg)))
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            ws = null
            pendingOpen?.complete(false)
            pendingSpin?.complete(Result.failure(t))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            ws = null
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (ws != null) return true

        val token = ApiClient.token ?: return false

        ws?.cancel()

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $token")
            .build()

        pendingOpen = CompletableDeferred()
        val socket = ApiClient.http.newWebSocket(request, listener)
        ws = socket

        val opened = withTimeoutOrNull(5000) { pendingOpen?.await() } ?: false
        pendingOpen = null
        if (!opened) {
            ws = null
        }
        return opened
    }

    suspend fun spin(bet: Int): Result<SlotSpinResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ensureConnected()) {
                    return@withContext Result.failure(IllegalStateException("Connexion impossible"))
                }

                pendingSpin = CompletableDeferred()
                val msg = JSONObject().apply {
                    put("type", "PLACE_BET")
                    put("payload", bet)
                }
                ws?.send(msg.toString())

                val result = withTimeoutOrNull(10000) { pendingSpin?.await() }
                    ?: Result.failure(IllegalStateException("Timeout"))
                pendingSpin = null
                result
            } catch (e: Exception) {
                pendingSpin = null
                Result.failure(e)
            }
        }
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }
}
