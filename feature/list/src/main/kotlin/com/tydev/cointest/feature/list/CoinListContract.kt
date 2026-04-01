package com.tydev.cointest.feature.list

import com.tydev.cointest.domain.model.Coin

data class CoinListUiState(
    val isLoading: Boolean = false,
    val coins: List<Coin> = emptyList(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
)

sealed interface CoinListIntent {
    data object LoadCoins : CoinListIntent
    data object Refresh : CoinListIntent
    data class OnCoinClick(val market: String) : CoinListIntent
}

sealed interface CoinListSideEffect {
    data class NavigateToOrderBook(val market: String) : CoinListSideEffect
    data class ShowError(val message: String) : CoinListSideEffect
}
