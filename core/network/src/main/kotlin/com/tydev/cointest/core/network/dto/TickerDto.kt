package com.tydev.cointest.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TickerDto(
    @SerialName("market") val market: String,
    @SerialName("trade_price") val tradePrice: Double,
    @SerialName("prev_closing_price") val prevClosingPrice: Double,
    @SerialName("change") val change: String,
    @SerialName("change_price") val changePrice: Double,
    @SerialName("change_rate") val changeRate: Double,
    @SerialName("signed_change_price") val signedChangePrice: Double,
    @SerialName("signed_change_rate") val signedChangeRate: Double,
    @SerialName("high_price") val highPrice: Double,
    @SerialName("low_price") val lowPrice: Double,
    @SerialName("acc_trade_price") val accTradePrice: Double,
    @SerialName("acc_trade_price_24h") val accTradePrice24h: Double,
    @SerialName("acc_trade_volume") val accTradeVolume: Double,
    @SerialName("acc_trade_volume_24h") val accTradeVolume24h: Double,
    @SerialName("timestamp") val timestamp: Long,
)
