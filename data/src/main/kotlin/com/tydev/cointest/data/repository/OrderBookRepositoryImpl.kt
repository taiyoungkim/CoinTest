package com.tydev.cointest.data.repository

import com.tydev.cointest.core.network.websocket.UpbitWebSocketService
import com.tydev.cointest.core.network.websocket.WsMessage
import com.tydev.cointest.data.mapper.mapToOrderBook
import com.tydev.cointest.domain.model.OrderBook
import com.tydev.cointest.domain.monitor.NetworkMonitor
import com.tydev.cointest.domain.repository.OrderBookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.io.IOException
import javax.inject.Inject

class OrderBookRepositoryImpl @Inject constructor(
    private val webSocketService: UpbitWebSocketService,
    private val networkMonitor: NetworkMonitor,
) : OrderBookRepository {

    override fun getOrderBook(market: String): Flow<OrderBook> = channelFlow {
        if (!networkMonitor.isOnline) {
            throw IOException("네트워크 연결 없음")
        }

        val messages = webSocketService.connect(market)
            .shareIn(this, SharingStarted.Eagerly, replay = 1)

        combine(
            messages.filterIsInstance<WsMessage.Ticker>().map { it.dto },
            messages.filterIsInstance<WsMessage.OrderBook>().map { it.dto },
        ) { ticker, orderBook ->
            mapToOrderBook(ticker, orderBook)
        }
            .conflate()
            .collect { send(it) }
    }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            if (e is CancellationException) throw e
        }
}
