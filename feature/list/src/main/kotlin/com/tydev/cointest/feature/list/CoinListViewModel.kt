package com.tydev.cointest.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tydev.cointest.domain.repository.CoinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinListViewModel @Inject constructor(
    private val coinRepository: CoinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinListUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<CoinListSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        onIntent(CoinListIntent.LoadCoins)
    }

    fun onIntent(intent: CoinListIntent) {
        when (intent) {
            is CoinListIntent.LoadCoins -> loadCoins()
            is CoinListIntent.Refresh -> refresh()
            is CoinListIntent.OnCoinClick -> navigateToOrderBook(intent.market)
        }
    }

    private fun loadCoins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coinRepository.getCoins()
                .onSuccess { coins ->
                    _uiState.update { it.copy(isLoading = false, coins = coins) }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "알 수 없는 오류가 발생했습니다"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    _sideEffect.send(CoinListSideEffect.ShowError(message))
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            coinRepository.getCoins()
                .onSuccess { coins ->
                    _uiState.update { it.copy(isRefreshing = false, coins = coins) }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "알 수 없는 오류가 발생했습니다"
                    _uiState.update { it.copy(isRefreshing = false) }
                    _sideEffect.send(CoinListSideEffect.ShowError(message))
                }
        }
    }

    private fun navigateToOrderBook(market: String) {
        viewModelScope.launch {
            _sideEffect.send(CoinListSideEffect.NavigateToOrderBook(market))
        }
    }
}
