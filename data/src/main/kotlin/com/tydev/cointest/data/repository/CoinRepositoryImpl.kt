package com.tydev.cointest.data.repository

import com.tydev.cointest.core.network.api.UpbitApiService
import com.tydev.cointest.data.mapper.mapToCoins
import com.tydev.cointest.domain.model.Coin
import com.tydev.cointest.domain.repository.CoinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CoinRepositoryImpl @Inject constructor(
    private val apiService: UpbitApiService,
) : CoinRepository {

    override suspend fun getCoins(): Result<List<Coin>> = withContext(Dispatchers.Default) {
        runCatching {
            val markets = apiService.getMarkets()
                .filter { it.market.startsWith("KRW-") }
            val marketCodes = markets.joinToString(",") { it.market }
            val tickers = apiService.getTickers(marketCodes)
            mapToCoins(markets, tickers)
                .sortedByDescending { it.accTradePrice24h }
        }
    }
}
