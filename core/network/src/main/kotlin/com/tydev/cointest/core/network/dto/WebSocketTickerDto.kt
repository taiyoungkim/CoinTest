package com.tydev.cointest.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebSocketTickerDto(
    @SerialName("type") val type: String,
    @SerialName("code") val code: String,
    @SerialName("trade_price") val tradePrice: Double,
    @SerialName("prev_closing_price") val prevClosingPrice: Double,
    @SerialName("high_price") val highPrice: Double,
    @SerialName("low_price") val lowPrice: Double,
    @SerialName("acc_trade_price_24h") val accTradePrice24h: Double,
    @SerialName("korean_name") val koreanName: String = "",
)
