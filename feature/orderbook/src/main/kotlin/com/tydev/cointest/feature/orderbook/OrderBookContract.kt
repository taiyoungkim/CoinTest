package com.tydev.cointest.feature.orderbook

import com.tydev.cointest.domain.model.OrderBook

data class OrderBookUiState(
    val market: String = "",
    val orderBook: OrderBook? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface OrderBookIntent {
    data object Retry : OrderBookIntent
    data object NavigateBack : OrderBookIntent
}

sealed interface OrderBookSideEffect {
    data class ShowError(val message: String) : OrderBookSideEffect
    data object NavigateBack : OrderBookSideEffect
}
