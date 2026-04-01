package com.tydev.cointest.data.mapper

import com.tydev.cointest.core.network.dto.MarketDto
import com.tydev.cointest.core.network.dto.TickerDto
import com.tydev.cointest.domain.model.Coin

fun mapToCoins(markets: List<MarketDto>, tickers: List<TickerDto>): List<Coin> {
    val marketMap = markets.associateBy { it.market }
    return tickers.mapNotNull { ticker ->
        val market = marketMap[ticker.market] ?: return@mapNotNull null
        Coin(
            market = ticker.market,
            koreanName = market.koreanName,
            symbol = ticker.market.substringAfter("-"),
            tradePrice = ticker.tradePrice,
            signedChangeRate = ticker.signedChangeRate,
            signedChangePrice = ticker.signedChangePrice,
            change = ticker.change,
            accTradePrice24h = ticker.accTradePrice24h,
            highPrice = ticker.highPrice,
            lowPrice = ticker.lowPrice,
            prevClosingPrice = ticker.prevClosingPrice,
        )
    }
}
