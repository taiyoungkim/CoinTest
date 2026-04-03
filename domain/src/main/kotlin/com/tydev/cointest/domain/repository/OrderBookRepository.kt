package com.tydev.cointest.domain.repository

import com.tydev.cointest.domain.model.OrderBook
import kotlinx.coroutines.flow.Flow

interface OrderBookRepository {
    fun getOrderBook(market: String): Flow<OrderBook>
}
