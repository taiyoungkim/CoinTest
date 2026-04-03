package com.tydev.cointest.data.mapper

import com.tydev.cointest.core.network.dto.OrderBookDto
import com.tydev.cointest.core.network.dto.WebSocketTickerDto
import com.tydev.cointest.domain.model.OrderBook
import com.tydev.cointest.domain.model.OrderBookTicker
import com.tydev.cointest.domain.model.OrderBookUnit

fun mapToOrderBook(ticker: WebSocketTickerDto, orderBook: OrderBookDto): OrderBook {
    val allSizes = orderBook.orderbookUnits.flatMap { listOf(it.askSize, it.bidSize) }
    val maxSize = allSizes.maxOrNull() ?: 1.0

    val askUnits = orderBook.orderbookUnits.map { unit ->
        OrderBookUnit(
            price = unit.askPrice,
            size = unit.askSize,
            sizeRatio = if (maxSize > 0.0) unit.askSize / maxSize else 0.0,
        )
    }.sortedByDescending { it.price }

    val bidUnits = orderBook.orderbookUnits.map { unit ->
        OrderBookUnit(
            price = unit.bidPrice,
            size = unit.bidSize,
            sizeRatio = if (maxSize > 0.0) unit.bidSize / maxSize else 0.0,
        )
    }.sortedByDescending { it.price }

    return OrderBook(
        ticker = OrderBookTicker(
            koreanName = ticker.koreanName,
            market = ticker.code,
            tradePrice = ticker.tradePrice,
            prevClosingPrice = ticker.prevClosingPrice,
            highPrice = ticker.highPrice,
            lowPrice = ticker.lowPrice,
            accTradePrice24h = ticker.accTradePrice24h,
        ),
        askUnits = askUnits,
        bidUnits = bidUnits,
        totalAskSize = orderBook.totalAskSize,
        totalBidSize = orderBook.totalBidSize,
    )
}
