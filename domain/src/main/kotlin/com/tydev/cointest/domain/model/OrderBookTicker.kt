package com.tydev.cointest.domain.model

data class OrderBookTicker(
    val koreanName: String,
    val market: String,
    val tradePrice: Double,
    val prevClosingPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val accTradePrice24h: Double,
)
