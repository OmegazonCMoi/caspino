package com.example.mobile.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

data class RoulettePhaseUpdate(
    val phase: String,
    val endsAt: Long
)

data class RouletteBetResult(
    val gains: Int,
    val winningNumber: Int,
    val balance: Int
)

object RouletteApi {
    private const val WS_URL = "ws://10.109.150.92:5600"

    private var ws: WebSocket? = null
    private var pendingOpen: CompletableDeferred<Boolean>? = null

    var onPhaseUpdate: ((RoulettePhaseUpdate) -> Unit)? = null
    var onBetResult: ((RouletteBetResult) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            ws = webSocket
            pendingOpen?.complete(true)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                when (json.optString("type")) {
                    "PHASE_UPDATE" -> {
                        val payload = json.getJSONObject("payload")
                        onPhaseUpdate?.invoke(
                            RoulettePhaseUpdate(
                                phase = payload.getString("phase"),
                                endsAt = payload.getLong("endsAt")
                            )
                        )
                    }
                    "BET_RESULT" -> {
                        val payload = json.getJSONObject("payload")
                        onBetResult?.invoke(
                            RouletteBetResult(
                                gains = payload.optDouble("gains", 0.0).toInt(),
                                winningNumber = payload.getInt("roulettteRandomResult"),
                                balance = payload.optInt("balance", 0)
                            )
                        )
                    }
                    "ERROR" -> {
                        val msg = json.optJSONObject("payload")?.optString("message") ?: "Erreur serveur"
                        onError?.invoke(msg)
                    }
                }
            } catch (exception: Exception) {
                onError?.invoke(exception.message ?: "Erreur de parsing")
            }
        }

        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
            ws = null
            pendingOpen?.complete(false)
            onError?.invoke(throwable.message ?: "Connexion perdue")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            ws = null
        }
    }

    suspend fun connect(): Boolean {
        return withContext(Dispatchers.IO) {
            if (ws != null) return@withContext true

            val token = ApiClient.token ?: return@withContext false

            val request = Request.Builder()
                .url(WS_URL)
                .addHeader("Authorization", "Bearer $token")
                .build()

            pendingOpen = CompletableDeferred()
            ApiClient.wsHttp.newWebSocket(request, listener)

            val opened = withTimeoutOrNull(5000) { pendingOpen?.await() } ?: false
            pendingOpen = null
            if (!opened) {
                ws = null
            }
            opened
        }
    }

    /**
     * Send bets to the server. Each bet is one of:
     * - NumberBet: { numbers: [1,2,...], amount: 100 }
     * - TwoTimesBet: { choice: "red"|"black"|"even"|"odd"|"1-18"|"19-36", amount: 100 }
     * - ThreeTimesBet: { dozen: "1-12"|"13-24"|"25-36", amount: 100 }
     */
    fun placeBets(bets: JSONArray) {
        val msg = JSONObject().apply {
            put("type", "PLACE_BET")
            put("payload", bets)
        }
        ws?.send(msg.toString())
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }

    val isConnected: Boolean get() = ws != null
}
