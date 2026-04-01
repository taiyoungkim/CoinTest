package com.tydev.cointest.data.repository

import com.tydev.cointest.core.network.api.UpbitApiService
import com.tydev.cointest.data.mapper.mapToCoins
import com.tydev.cointest.domain.error.NetworkError
import com.tydev.cointest.domain.model.Coin
import com.tydev.cointest.domain.repository.CoinRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class CoinRepositoryImpl @Inject constructor(
    private val apiService: UpbitApiService,
) : CoinRepository {

    override suspend fun getCoins(): Result<List<Coin>> {
        return try {
            val markets = withContext(Dispatchers.IO) {
                apiService.getMarkets().filter { it.market.startsWith("KRW-") }
            }
            val marketCodes = markets.joinToString(",") { it.market }
            val tickers = withContext(Dispatchers.IO) {
                apiService.getTickers(marketCodes)
            }
            val coins = withContext(Dispatchers.Default) {
                mapToCoins(markets, tickers).sortedByDescending { it.accTradePrice24h }
            }
            Result.success(coins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(NetworkError.Connectivity.toException())
        } catch (e: Exception) {
            val networkError = e.toNetworkError()
            Result.failure(networkError.toException())
        }
    }
}

private fun Exception.toNetworkError(): NetworkError {
    val message = message ?: ""
    return when {
        message.contains("5") && message.contains("Server") -> NetworkError.Server(500)
        message.contains("4") && message.contains("Client") -> NetworkError.Api(message)
        else -> NetworkError.Unknown
    }
}

private fun NetworkError.toException(): Exception = Exception(
    when (this) {
        is NetworkError.Connectivity -> "네트워크 연결을 확인해주세요"
        is NetworkError.Server -> "서버 오류가 발생했습니다 ($code)"
        is NetworkError.Api -> message
        is NetworkError.Unknown -> "알 수 없는 오류가 발생했습니다"
    }
)
