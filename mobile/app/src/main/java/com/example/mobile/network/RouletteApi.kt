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

data class RouletteSpinResult(
    val winningNumber: Int,
    val gains: Int
)

data class RoulettePhaseState(
    val phase: String,
    val endsAt: Long,
    val lastResult: Int?
)

object RouletteApi {
    private val WS_URL: String
        get() {
            val host = ApiClient.BASE_URL
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore(":")
            return "ws://$host:5600"
        }

    private var ws: WebSocket? = null
    private var pendingConnect: CompletableDeferred<Boolean>? = null
    private var eventListener: ((String, JSONObject) -> Unit)? = null

    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            pendingConnect?.complete(true)
            val sync = JSONObject().apply { put("type", "SYNC_REQUEST") }
            webSocket.send(sync.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = JSONObject(text)
            val type = json.optString("type")
            val payload = json.optJSONObject("payload") ?: JSONObject()
            eventListener?.invoke(type, payload)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            ws = null
            pendingConnect?.complete(false)
            eventListener?.invoke(
                "ERROR",
                JSONObject().apply { put("message", t.message ?: "Connexion échouée") }
            )
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            ws = null
        }
    }

    fun setListener(listener: ((String, JSONObject) -> Unit)?) {
        this.eventListener = listener
    }

    suspend fun connect(): Boolean {
        return withContext(Dispatchers.IO) {
            if (ws != null) return@withContext true

            try {
                pendingConnect = CompletableDeferred()
                val request = Request.Builder().url(WS_URL).build()
                ws = ApiClient.http.newWebSocket(request, this@RouletteApi.wsListener)
                val ok = withTimeoutOrNull(5000) { pendingConnect?.await() } ?: false
                pendingConnect = null
                if (!ok) {
                    ws?.cancel()
                    ws = null
                }
                ok
            } catch (e: Exception) {
                ws = null
                pendingConnect = null
                false
            }
        }
    }

    suspend fun placeBet(
        selectedNumbers: Set<Int>,
        selectedGroups: Set<String>,
        betPerSelection: Int
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!connect()) {
                    return@withContext Result.failure(
                        IllegalStateException("Connexion impossible")
                    )
                }

                val betsArray = JSONArray()

                // Pleins (NumberBet)
                selectedNumbers.forEach { number ->
                    val obj = JSONObject().apply {
                        put("numbers", JSONArray().apply { put(number) })
                        put("amount", betPerSelection)
                    }
                    betsArray.put(obj)
                }

                // Mises 2x (TwoTimesBet)
                selectedGroups.forEach { id ->
                    when (id) {
                        "RED", "BLACK", "PAIR", "IMPAIR", "LOW", "HIGH" -> {
                            val choice = when (id) {
                                "RED" -> "red"
                                "BLACK" -> "black"
                                "PAIR" -> "even"
                                "IMPAIR" -> "odd"
                                "LOW" -> "1-18"
                                "HIGH" -> "19-36"
                                else -> null
                            }
                            if (choice != null) {
                                val obj = JSONObject().apply {
                                    put("choice", choice)
                                    put("amount", betPerSelection)
                                }
                                betsArray.put(obj)
                            }
                        }
                    }
                }

                // Douzaines (ThreeTimesBet)
                selectedGroups.forEach { id ->
                    val dozen = when (id) {
                        "D_1ère 12" -> "1-12"
                        "D_2ème 12" -> "13-24"
                        "D_3ème 12" -> "25-36"
                        else -> null
                    }
                    if (dozen != null) {
                        val obj = JSONObject().apply {
                            put("dozen", dozen)
                            put("amount", betPerSelection)
                        }
                        betsArray.put(obj)
                    }
                }

                if (betsArray.length() == 0) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Aucune mise")
                    )
                }

                val msg = JSONObject().apply {
                    put("type", "PLACE_BET")
                    put("payload", betsArray)
                }
                val sent = ws?.send(msg.toString()) ?: false
                if (!sent) {
                    return@withContext Result.failure(IllegalStateException("Envoi impossible"))
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }
}

