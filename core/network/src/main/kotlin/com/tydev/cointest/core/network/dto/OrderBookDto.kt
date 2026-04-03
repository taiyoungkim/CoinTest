package com.tydev.cointest.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderBookDto(
    @SerialName("type") val type: String,
    @SerialName("code") val code: String,
    @SerialName("total_ask_size") val totalAskSize: Double,
    @SerialName("total_bid_size") val totalBidSize: Double,
    @SerialName("orderbook_units") val orderbookUnits: List<OrderBookUnitDto>,
    @SerialName("timestamp") val timestamp: Long,
)

@Serializable
data class OrderBookUnitDto(
    @SerialName("ask_price") val askPrice: Double,
    @SerialName("bid_price") val bidPrice: Double,
    @SerialName("ask_size") val askSize: Double,
    @SerialName("bid_size") val bidSize: Double,
)
