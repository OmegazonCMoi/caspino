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

data class BlackjackCard(
    val suit: String,
    val rank: String
)

data class BlackjackHand(
    val cards: List<BlackjackCard>,
    val bet: Int,
    val status: String,
    val isDoubled: Boolean,
    val value: Int
)

data class BlackjackGameState(
    val hands: List<BlackjackHand>,
    val activeHandIndex: Int,
    val dealerUpCard: BlackjackCard,
    val phase: String,
    val insuranceBet: Int,
    val canInsure: Boolean,
    val deckRemaining: Int,
    val deckTotal: Int,
    val reshuffled: Boolean
)

data class BlackjackHandResult(
    val bet: Int,
    val gains: Int,
    val playerValue: Int,
    val status: String
)

data class BlackjackPlayerHand(
    val cards: List<BlackjackCard>,
    val bet: Int,
    val isDoubled: Boolean,
    val value: Int
)

data class BlackjackBetResult(
    val gains: Int,
    val hands: List<BlackjackHandResult>,
    val playerHands: List<BlackjackPlayerHand>,
    val dealerHand: List<BlackjackCard>,
    val dealerValue: Int,
    val insuranceGain: Int,
    val balance: Int
)

object BlackjackApi {
    private const val WS_URL = "ws://10.109.150.92:5800"

    private var ws: WebSocket? = null
    private var pendingOpen: CompletableDeferred<Boolean>? = null

    var onGameState: ((BlackjackGameState) -> Unit)? = null
    var onBetResult: ((BlackjackBetResult) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            ws = webSocket
            pendingOpen?.complete(true)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "GAME_STATE" -> {
                    val payload = json.getJSONObject("payload")
                    val gameState = parseGameState(payload)
                    onGameState?.invoke(gameState)
                }
                "BET_RESULT" -> {
                    val payload = json.getJSONObject("payload")
                    val betResult = parseBetResult(payload)
                    onBetResult?.invoke(betResult)
                }
                "ERROR" -> {
                    val msg = json.optJSONObject("payload")?.optString("message") ?: "Erreur serveur"
                    onError?.invoke(msg)
                }
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

    private fun parseCard(json: JSONObject): BlackjackCard {
        return BlackjackCard(
            suit = json.getString("suit"),
            rank = json.getString("rank")
        )
    }

    private fun parseGameState(payload: JSONObject): BlackjackGameState {
        val handsArray = payload.getJSONArray("hands")
        val hands = mutableListOf<BlackjackHand>()
        for (handIndex in 0 until handsArray.length()) {
            val handJson = handsArray.getJSONObject(handIndex)
            val cardsArray = handJson.getJSONArray("cards")
            val cards = mutableListOf<BlackjackCard>()
            for (cardIndex in 0 until cardsArray.length()) {
                cards.add(parseCard(cardsArray.getJSONObject(cardIndex)))
            }
            hands.add(
                BlackjackHand(
                    cards = cards,
                    bet = handJson.getInt("bet"),
                    status = handJson.getString("status"),
                    isDoubled = handJson.getBoolean("isDoubled"),
                    value = handJson.getInt("value")
                )
            )
        }

        val dealerUpCardJson = payload.getJSONObject("dealerUpCard")

        return BlackjackGameState(
            hands = hands,
            activeHandIndex = payload.getInt("activeHandIndex"),
            dealerUpCard = parseCard(dealerUpCardJson),
            phase = payload.getString("phase"),
            insuranceBet = payload.getInt("insuranceBet"),
            canInsure = payload.getBoolean("canInsure"),
            deckRemaining = payload.optInt("deckRemaining", 0),
            deckTotal = payload.optInt("deckTotal", 312),
            reshuffled = payload.optBoolean("reshuffled", false)
        )
    }

    private fun parseBetResult(payload: JSONObject): BlackjackBetResult {
        val handsArray = payload.getJSONArray("hands")
        val hands = mutableListOf<BlackjackHandResult>()
        for (handIndex in 0 until handsArray.length()) {
            val handJson = handsArray.getJSONObject(handIndex)
            hands.add(
                BlackjackHandResult(
                    bet = handJson.getInt("bet"),
                    gains = handJson.getInt("gains"),
                    playerValue = handJson.getInt("playerValue"),
                    status = handJson.getString("status")
                )
            )
        }

        val playerHandsArray = payload.getJSONArray("playerHands")
        val playerHands = mutableListOf<BlackjackPlayerHand>()
        for (handIndex in 0 until playerHandsArray.length()) {
            val handJson = playerHandsArray.getJSONObject(handIndex)
            val cardsArray = handJson.getJSONArray("cards")
            val cards = mutableListOf<BlackjackCard>()
            for (cardIndex in 0 until cardsArray.length()) {
                cards.add(parseCard(cardsArray.getJSONObject(cardIndex)))
            }
            playerHands.add(
                BlackjackPlayerHand(
                    cards = cards,
                    bet = handJson.getInt("bet"),
                    isDoubled = handJson.getBoolean("isDoubled"),
                    value = handJson.getInt("value")
                )
            )
        }

        val dealerHandArray = payload.getJSONArray("dealerHand")
        val dealerCards = mutableListOf<BlackjackCard>()
        for (cardIndex in 0 until dealerHandArray.length()) {
            dealerCards.add(parseCard(dealerHandArray.getJSONObject(cardIndex)))
        }

        return BlackjackBetResult(
            gains = payload.getInt("gains"),
            hands = hands,
            playerHands = playerHands,
            dealerHand = dealerCards,
            dealerValue = payload.getInt("dealerValue"),
            insuranceGain = payload.getInt("insuranceGain"),
            balance = payload.getInt("balance")
        )
    }

    private suspend fun ensureConnected(): Boolean {
        if (ws != null) return true

        val token = ApiClient.token ?: return false

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
        return opened
    }

    private fun sendMessage(type: String, payload: Any? = null) {
        val msg = JSONObject().apply {
            put("type", type)
            if (payload != null) put("payload", payload)
        }
        ws?.send(msg.toString())
    }

    suspend fun placeBet(bet: Int): Boolean {
        return withContext(Dispatchers.IO) {
            if (!ensureConnected()) {
                onError?.invoke("Connexion impossible")
                return@withContext false
            }
            sendMessage("PLACE_BET", bet)
            true
        }
    }

    fun hit() {
        sendMessage("HIT")
    }

    fun stand() {
        sendMessage("STAND")
    }

    fun doubleDown() {
        sendMessage("DOUBLE_DOWN")
    }

    fun split() {
        sendMessage("SPLIT")
    }

    fun insurance() {
        sendMessage("INSURANCE")
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }
}
