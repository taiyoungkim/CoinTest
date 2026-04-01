package com.tydev.cointest.domain.model

data class Coin(
    val market: String,
    val koreanName: String,
    val symbol: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
    val signedChangePrice: Double,
    val change: String,
    val accTradePrice24h: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val prevClosingPrice: Double,
)
