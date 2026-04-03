package com.tydev.cointest.domain.model

data class OrderBook(
    val ticker: OrderBookTicker,
    val askUnits: List<OrderBookUnit>,
    val bidUnits: List<OrderBookUnit>,
    val totalAskSize: Double,
    val totalBidSize: Double,
)
