package com.tydev.cointest.core.network.websocket

import com.tydev.cointest.core.network.dto.OrderBookDto
import com.tydev.cointest.core.network.dto.WebSocketTickerDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.retry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject

sealed interface WsMessage {
    data class Ticker(val dto: WebSocketTickerDto) : WsMessage
    data class OrderBook(val dto: OrderBookDto) : WsMessage
}

class UpbitWebSocketService @Inject constructor(
    private val client: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var retryDelay = INITIAL_RETRY_DELAY

    fun connect(market: String): Flow<WsMessage> = channelFlow {
        client.webSocket("wss://api.upbit.com/websocket/v1") {
            retryDelay = INITIAL_RETRY_DELAY

            val subscribeMessage = buildSubscribeMessage(market)
            send(Frame.Text(subscribeMessage))

            for (frame in incoming) {
                val text = when (frame) {
                    is Frame.Text -> frame.readText()
                    is Frame.Binary -> frame.readBytes().decodeToString()
                    else -> continue
                }
                parseWsMessage(text)?.let { send(it) }
            }
        }
    }.retry(Long.MAX_VALUE) { _ ->
        delay(backoffDelay())
        true
    }

    private fun parseWsMessage(text: String): WsMessage? {
        val element = json.parseToJsonElement(text)
        if (element !is JsonObject) return null
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "ticker" -> WsMessage.Ticker(json.decodeFromJsonElement(WebSocketTickerDto.serializer(), element))
            "orderbook" -> WsMessage.OrderBook(json.decodeFromJsonElement(OrderBookDto.serializer(), element))
            else -> null
        }
    }

    private fun backoffDelay(): Long {
        val current = retryDelay
        retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
        return current
    }

    companion object {
        private const val INITIAL_RETRY_DELAY = 1_000L
        private const val MAX_RETRY_DELAY = 30_000L

        private fun buildSubscribeMessage(market: String): String {
            val ticket = UUID.randomUUID().toString()
            return """[{"ticket":"$ticket"},{"type":"ticker","codes":["$market"]},{"type":"orderbook","codes":["$market"]}]"""
        }
    }
}
